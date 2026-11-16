package org.telegram.novagram.updater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BetaUpdate;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.novagram.NovaConfig;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.web.HttpGetFileTask;
import org.telegram.ui.web.HttpGetTask;

import java.io.File;
import java.util.Locale;

public class NovaUpdateController {

    private static volatile NovaUpdateController instance;

    public static NovaUpdateController getInstance() {
        if (instance == null) {
            synchronized (NovaUpdateController.class) {
                if (instance == null) {
                    instance = new NovaUpdateController();
                }
            }
        }
        return instance;
    }

    public static class ReleaseInfo {
        public final String version;
        public final int versionCode;
        public final String title;
        public final String changelog;
        public final String apkUrl;
        public final String releaseUrl;
        public final long apkSize;

        public ReleaseInfo(String version, int versionCode, String title, String changelog, String apkUrl, String releaseUrl, long apkSize) {
            this.version = version;
            this.versionCode = versionCode;
            this.title = title;
            this.changelog = changelog;
            this.apkUrl = apkUrl;
            this.releaseUrl = releaseUrl;
            this.apkSize = apkSize;
        }
    }

    private static final long CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L; // 6 hours

    private String version;
    private int versionCode;
    private String title;
    private String changelog;
    private String apkUrl;
    private String releaseUrl;
    private long apkSize;
    private String downloadedPath;
    private long lastCheck;

    private boolean checking;
    private boolean downloading;
    private float downloadingProgress;
    private HttpGetFileTask downloadTask;
    private String lastError;

    private NovaUpdateController() {
        load();
    }

    private SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nova_updater", Context.MODE_PRIVATE);
    }

    private synchronized void load() {
        SharedPreferences prefs = getPrefs();
        version = prefs.getString("version", null);
        versionCode = prefs.getInt("versionCode", 0);
        title = prefs.getString("title", null);
        changelog = prefs.getString("changelog", null);
        apkUrl = prefs.getString("apkUrl", null);
        releaseUrl = prefs.getString("releaseUrl", null);
        apkSize = prefs.getLong("apkSize", 0L);
        downloadedPath = prefs.getString("downloadedPath", null);
        lastCheck = prefs.getLong("lastCheck", 0L);

        if (!TextUtils.isEmpty(version) && compareVersions(version, BuildConfig.NOVA_VERSION_NAME) <= 0) {
            clearPendingLocked();
        } else if (!TextUtils.isEmpty(downloadedPath) && !new File(downloadedPath).exists()) {
            downloadedPath = null;
            saveLocked();
        }
    }

    private void clearPendingLocked() {
        if (!TextUtils.isEmpty(downloadedPath)) {
            try {
                new File(downloadedPath).delete();
            } catch (Throwable ignored) {
            }
        }
        version = null;
        versionCode = 0;
        title = null;
        changelog = null;
        apkUrl = null;
        releaseUrl = null;
        apkSize = 0L;
        downloadedPath = null;
        saveLocked();
    }

    private void saveLocked() {
        getPrefs().edit()
                .putString("version", version)
                .putInt("versionCode", versionCode)
                .putString("title", title)
                .putString("changelog", changelog)
                .putString("apkUrl", apkUrl)
                .putString("releaseUrl", releaseUrl)
                .putLong("apkSize", apkSize)
                .putString("downloadedPath", downloadedPath)
                .putLong("lastCheck", lastCheck)
                .apply();
    }

    public boolean isChecking() {
        return checking;
    }

    public String getLastError() {
        return lastError;
    }

    public synchronized ReleaseInfo getPendingRelease() {
        if (TextUtils.isEmpty(version) || compareVersions(version, BuildConfig.NOVA_VERSION_NAME) <= 0) {
            return null;
        }
        return new ReleaseInfo(version, versionCode, title, changelog, apkUrl, releaseUrl, apkSize);
    }

    public BetaUpdate getUpdateAsBetaUpdate() {
        ReleaseInfo info = getPendingRelease();
        if (info == null) return null;
        return new BetaUpdate(info.version, Math.max(info.versionCode, BuildConfig.NOVA_VERSION_CODE + 1), info.changelog);
    }

    public void checkForUpdates(boolean force, Runnable whenDone) {
        if (checking) return;
        if (!force && !NovaConfig.isAutoCheckUpdates()) {
            if (whenDone != null) whenDone.run();
            return;
        }
        if (!force && System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
            if (whenDone != null) whenDone.run();
            return;
        }

        checking = true;
        lastError = null;
        new HttpGetTask(response -> AndroidUtilities.runOnUIThread(() -> {
            checking = false;
            handleApiResponse(response, force, whenDone);
        }))
                .setHeader("Accept", "application/vnd.github+json")
                .setHeader("User-Agent", "NovaGram-Android/" + BuildConfig.NOVA_VERSION_NAME)
                .execute(NovaConfig.GITHUB_LATEST_RELEASE_API);
    }

    private synchronized void handleApiResponse(String response, boolean manualTrigger, Runnable whenDone) {
        lastCheck = System.currentTimeMillis();
        if (TextUtils.isEmpty(response)) {
            lastError = "Network error";
            saveLocked();
            if (whenDone != null) whenDone.run();
            return;
        }

        try {
            JSONObject json = new JSONObject(response);
            if (json.has("message") && !json.has("tag_name")) {
                // e.g. 404 Not Found when repo has no releases yet
                String msg = json.optString("message", "");
                if (!"Not Found".equalsIgnoreCase(msg)) {
                    lastError = msg;
                }
                clearPendingLocked();
                if (whenDone != null) whenDone.run();
                return;
            }

            String tagName = json.optString("tag_name", "").trim();
            String remoteVersion = normalizeVersion(tagName);
            String relTitle = json.optString("name", tagName);
            String body = json.optString("body", "");
            String htmlUrl = json.optString("html_url", NovaConfig.GITHUB_REPO_URL + "/releases");

            if (TextUtils.isEmpty(remoteVersion) || compareVersions(remoteVersion, BuildConfig.NOVA_VERSION_NAME) <= 0) {
                clearPendingLocked();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
                if (whenDone != null) whenDone.run();
                return;
            }

            String selectedApkUrl = null;
            long selectedSize = 0L;
            JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                selectedApkUrl = pickBestApkUrl(assets);
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset != null && TextUtils.equals(selectedApkUrl, asset.optString("browser_download_url"))) {
                        selectedSize = asset.optLong("size", 0L);
                        break;
                    }
                }
            }

            boolean isNewerThanStored = TextUtils.isEmpty(version) || !TextUtils.equals(version, remoteVersion);
            if (isNewerThanStored) {
                clearPendingLocked();
                version = remoteVersion;
                versionCode = BuildConfig.NOVA_VERSION_CODE + 1;
            }
            title = relTitle;
            changelog = body;
            apkUrl = selectedApkUrl;
            releaseUrl = htmlUrl;
            apkSize = selectedSize;
            saveLocked();

            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);

            if (whenDone != null) {
                whenDone.run();
            } else if (isNewerThanStored && !ApplicationLoader.mainInterfacePaused && LaunchActivity.instance != null) {
                NovaUpdateDialog.show(LaunchActivity.instance, getPendingRelease());
            }
        } catch (Throwable t) {
            FileLog.e("NovaUpdateController parse error", t);
            lastError = t.getMessage();
            saveLocked();
            if (whenDone != null) whenDone.run();
        }
    }

    private static String pickBestApkUrl(JSONArray assets) {
        String[] abis = Build.SUPPORTED_ABIS != null ? Build.SUPPORTED_ABIS : new String[0];
        String primaryAbi = abis.length > 0 ? abis[0].toLowerCase(Locale.ROOT) : "";

        String exactAbiMatch = null;
        String universalMatch = null;
        String fallbackApk = null;

        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset == null) continue;
            String name = asset.optString("name", "").toLowerCase(Locale.ROOT);
            String url = asset.optString("browser_download_url", "");
            if (!name.endsWith(".apk") || TextUtils.isEmpty(url)) continue;

            if (fallbackApk == null) fallbackApk = url;
            if (!TextUtils.isEmpty(primaryAbi) && name.contains(primaryAbi)) {
                exactAbiMatch = url;
            } else if (name.contains("universal") || name.contains("afat") || name.equals("app.apk") || name.startsWith("novagram")) {
                universalMatch = url;
            }
        }
        if (exactAbiMatch != null) return exactAbiMatch;
        if (universalMatch != null) return universalMatch;
        return fallbackApk;
    }

    public synchronized void downloadUpdate() {
        String targetUrl = !TextUtils.isEmpty(apkUrl) ? apkUrl : releaseUrl;
        if (!TextUtils.isEmpty(targetUrl) && LaunchActivity.instance != null) {
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                LaunchActivity.instance.startActivity(browserIntent);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public void cancelDownloading() {
        if (!downloading) return;
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
        downloading = false;
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
    }

    public boolean isDownloading() {
        return downloading;
    }

    public float getDownloadingProgress() {
        return downloadingProgress;
    }

    public synchronized File getDownloadedFile() {
        if (TextUtils.isEmpty(downloadedPath)) return null;
        File f = new File(downloadedPath);
        if (!f.exists() || f.length() == 0) {
            downloadedPath = null;
            saveLocked();
            return null;
        }
        return f;
    }

    public void installDownloadedApk(Activity activity, File file) {
        if (activity == null || file == null || !file.exists()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ApplicationLoader.applicationContext.getPackageManager().canRequestPackageInstalls()) {
            AlertsCreator.createApkRestrictedDialog(activity, null).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", file);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            activity.startActivity(intent);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static String normalizeVersion(String raw) {
        if (TextUtils.isEmpty(raw)) return "";
        String v = raw.trim();
        if (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }
        return v;
    }

    public static int compareVersions(String a, String b) {
        String[] pa = normalizeVersion(a).split("[^0-9]+");
        String[] pb = normalizeVersion(b).split("[^0-9]+");
        int max = Math.max(pa.length, pb.length);
        for (int i = 0; i < max; i++) {
            int va = (i < pa.length && !pa[i].isEmpty()) ? parseSafeInt(pa[i]) : 0;
            int vb = (i < pb.length && !pb[i].isEmpty()) ? parseSafeInt(pb[i]) : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
