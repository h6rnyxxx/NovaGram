package org.telegram.novagram.updater;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BetaUpdate;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.novagram.NovaConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.web.HttpGetTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * In-app Cloud OTA updater for NovaGram.
 * Downloads APK streams directly inside the app with live progress and installs via Android PackageInstaller
 * without sending users out to GitHub in an external browser.
 */
public class NovaUpdateController {

    public interface DownloadProgressListener {
        void onProgressChanged(float progress, long downloadedBytes, long totalBytes, boolean finished, String error);
    }

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
    private static final String ACTION_INSTALL_COMMIT = "org.telegram.novagram.ACTION_INSTALL_COMMIT";

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
    private volatile boolean downloading;
    private volatile boolean cancelRequested;
    private volatile float downloadingProgress;
    private volatile long downloadedBytes;
    private volatile long totalBytes;
    private String lastError;
    private File pendingInstallAfterPermission;
    private final List<DownloadProgressListener> progressListeners = new ArrayList<>();
    private BroadcastReceiver installReceiver;

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

    public synchronized void addProgressListener(DownloadProgressListener listener) {
        if (listener != null && !progressListeners.contains(listener)) {
            progressListeners.add(listener);
        }
    }

    public synchronized void removeProgressListener(DownloadProgressListener listener) {
        progressListeners.remove(listener);
    }

    private void notifyProgressListeners(boolean finished, String error) {
        List<DownloadProgressListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(progressListeners);
        }
        float p = downloadingProgress;
        long d = downloadedBytes;
        long t = totalBytes;
        AndroidUtilities.runOnUIThread(() -> {
            for (DownloadProgressListener l : snapshot) {
                try {
                    l.onProgressChanged(p, d, t, finished, error);
                } catch (Throwable ignored) {
                }
            }
        });
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
        NovaCloudCatalog.getInstance().syncAsync(force, null);
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
            handleApiResponse(response, whenDone);
        }))
                .setHeader("Accept", "application/vnd.github+json")
                .setHeader("User-Agent", "NovaGram-Android/" + BuildConfig.NOVA_VERSION_NAME)
                .execute(NovaConfig.GITHUB_LATEST_RELEASE_API);
    }

    private synchronized void handleApiResponse(String response, Runnable whenDone) {
        lastCheck = System.currentTimeMillis();
        if (TextUtils.isEmpty(response)) {
            lastError = LocaleController.getString(R.string.NovaUpdateNetworkError);
            saveLocked();
            if (whenDone != null) whenDone.run();
            return;
        }

        try {
            JSONObject json = new JSONObject(response);
            if (json.has("message") && !json.has("tag_name")) {
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
        File existing = getDownloadedFile();
        if (existing != null && LaunchActivity.instance != null) {
            installDownloadedApk(LaunchActivity.instance, existing);
            return;
        }
        if (downloading) {
            return;
        }
        final String downloadUrl = apkUrl;
        final String ver = version != null ? version : "latest";
        final long expectedSize = apkSize;

        if (TextUtils.isEmpty(downloadUrl)) {
            if (!TextUtils.isEmpty(releaseUrl) && LaunchActivity.instance != null) {
                try {
                    LaunchActivity.instance.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)));
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            }
            return;
        }

        downloading = true;
        cancelRequested = false;
        downloadingProgress = 0f;
        downloadedBytes = 0L;
        totalBytes = expectedSize;
        lastError = null;
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
        notifyProgressListeners(false, null);

        Thread worker = new Thread(() -> runStreamingDownload(downloadUrl, ver, expectedSize), "NovaCloudOtaDownload");
        worker.setDaemon(true);
        worker.start();
    }

    private void runStreamingDownload(String startUrl, String ver, long expectedSize) {
        File updatesDir = new File(ApplicationLoader.applicationContext.getFilesDir(), "novagram_updates");
        if (!updatesDir.exists()) {
            updatesDir.mkdirs();
        }
        String safeVer = ver.replaceAll("[^a-zA-Z0-9._-]", "_");
        File partFile = new File(updatesDir, "NovaGram-" + safeVer + ".apk.part");
        File finalFile = new File(updatesDir, "NovaGram-" + safeVer + ".apk");
        if (partFile.exists()) {
            partFile.delete();
        }

        HttpURLConnection conn = null;
        try {
            String currentUrl = startUrl;
            int redirects = 0;
            while (redirects < 8) {
                if (cancelRequested) {
                    finishDownloadFailure(null, partFile);
                    return;
                }
                URL url = new URL(currentUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(25000);
                conn.setRequestProperty("User-Agent", "NovaGram-Android/" + BuildConfig.NOVA_VERSION_NAME);
                conn.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*");

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_TEMP
                        || code == HttpURLConnection.HTTP_MOVED_PERM
                        || code == HttpURLConnection.HTTP_SEE_OTHER
                        || code == 307 || code == 308) {
                    String location = conn.getHeaderField("Location");
                    conn.disconnect();
                    conn = null;
                    if (TextUtils.isEmpty(location)) {
                        throw new RuntimeException("Empty redirect location");
                    }
                    currentUrl = new URL(url, location).toString();
                    redirects++;
                    continue;
                }
                if (code < 200 || code >= 300) {
                    throw new RuntimeException("HTTP " + code);
                }
                break;
            }
            if (conn == null) {
                throw new RuntimeException("Too many redirects");
            }

            long contentLen = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    ? conn.getContentLengthLong()
                    : conn.getContentLength();
            long resolvedTotal = contentLen > 0 ? contentLen : expectedSize;
            totalBytes = resolvedTotal;

            long readTotal = 0L;
            long lastNotifyTime = 0L;
            try (InputStream is = new BufferedInputStream(conn.getInputStream(), 32768);
                 FileOutputStream fos = new FileOutputStream(partFile)) {
                byte[] buf = new byte[32768];
                int n;
                while ((n = is.read(buf)) != -1) {
                    if (cancelRequested) {
                        finishDownloadFailure(null, partFile);
                        return;
                    }
                    fos.write(buf, 0, n);
                    readTotal += n;
                    downloadedBytes = readTotal;
                    if (resolvedTotal > 0) {
                        downloadingProgress = Math.max(0f, Math.min(1f, (float) readTotal / (float) resolvedTotal));
                    }
                    long now = SystemClock.uptimeMillis();
                    if (now - lastNotifyTime >= 120) {
                        lastNotifyTime = now;
                        AndroidUtilities.runOnUIThread(() ->
                                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.fileLoadProgressChanged));
                        notifyProgressListeners(false, null);
                    }
                }
                fos.flush();
            }

            if (readTotal < 1_000_000L || !hasApkZipSignature(partFile)) {
                throw new RuntimeException(LocaleController.getString(R.string.NovaUpdateCorrupted));
            }
            if (finalFile.exists()) {
                finalFile.delete();
            }
            if (!partFile.renameTo(finalFile)) {
                throw new RuntimeException("Failed to finalize APK file");
            }

            synchronized (NovaUpdateController.this) {
                downloadedPath = finalFile.getAbsolutePath();
                downloading = false;
                downloadingProgress = 1f;
                downloadedBytes = readTotal;
                saveLocked();
            }

            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
                notifyProgressListeners(true, null);
                if (LaunchActivity.instance != null) {
                    installDownloadedApk(LaunchActivity.instance, finalFile);
                }
            });
        } catch (Throwable t) {
            FileLog.e("NovaGram Cloud OTA download failed", t);
            String errMsg = !TextUtils.isEmpty(t.getMessage()) ? t.getMessage() : LocaleController.getString(R.string.NovaUpdateNetworkError);
            finishDownloadFailure(errMsg, partFile);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void finishDownloadFailure(String errMsg, File partFile) {
        if (partFile != null && partFile.exists()) {
            try {
                partFile.delete();
            } catch (Throwable ignored) {
            }
        }
        synchronized (this) {
            downloading = false;
            cancelRequested = false;
            lastError = errMsg;
        }
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
            notifyProgressListeners(true, errMsg);
            if (!TextUtils.isEmpty(errMsg) && LaunchActivity.instance != null) {
                BaseFragment f = LaunchActivity.getLastFragment();
                if (f != null && f.getLayoutContainer() != null) {
                    BulletinFactory.of(f).createErrorBulletin(errMsg).show();
                }
            }
        });
    }

    private static boolean hasApkZipSignature(File file) {
        if (file == null || !file.exists() || file.length() < 4) {
            return false;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[4];
            return fis.read(header) == 4 && header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public synchronized void cancelDownloading() {
        if (!downloading) return;
        cancelRequested = true;
        downloading = false;
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
        notifyProgressListeners(true, null);
    }

    public boolean isDownloading() {
        return downloading;
    }

    public float getDownloadingProgress() {
        return downloadingProgress;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
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

    public void resumePendingInstall() {
        if (LaunchActivity.instance != null) {
            resumePendingInstallIfReady(LaunchActivity.instance);
        }
    }

    public void resumePendingInstallIfReady(Activity activity) {
        if (activity == null || pendingInstallAfterPermission == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.getPackageManager().canRequestPackageInstalls()) {
            File file = pendingInstallAfterPermission;
            pendingInstallAfterPermission = null;
            installDownloadedApk(activity, file);
        }
    }

    public void promptUnknownSources(Activity activity, File pendingFile) {
        if (activity == null) return;
        pendingInstallAfterPermission = pendingFile;
        new AlertDialog.Builder(activity)
                .setTitle(LocaleController.getString(R.string.NovaInstallPermTitle))
                .setMessage(LocaleController.getString(R.string.NovaInstallPermMessage))
                .setPositiveButton(LocaleController.getString(R.string.PermissionOpenSettings), (d, w) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    } catch (Throwable t) {
                        FileLog.e(t);
                    }
                })
                .setNegativeButton(LocaleController.getString(R.string.Cancel), (d, w) -> pendingInstallAfterPermission = null)
                .show();
    }

    public void installDownloadedApk(Activity activity, File file) {
        if (activity == null || file == null || !file.exists()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ApplicationLoader.applicationContext.getPackageManager().canRequestPackageInstalls()) {
            promptUnknownSources(activity, file);
            return;
        }

        if (!isApkSignatureCompatible(activity, file)) {
            new AlertDialog.Builder(activity)
                    .setTitle(LocaleController.getString(R.string.NovaSignatureMismatchTitle))
                    .setMessage(LocaleController.getString(R.string.NovaSignatureMismatchMessage))
                    .setPositiveButton(LocaleController.getString(R.string.NovaInstallTryAnyway), (d, w) -> installWithPackageInstallerOrIntent(activity, file))
                    .setNeutralButton(LocaleController.getString(R.string.NovaOpenDownloadsFolder), (d, w) -> {
                        if (!TextUtils.isEmpty(releaseUrl)) {
                            try {
                                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)));
                            } catch (Throwable ignored) {
                            }
                        }
                    })
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .show();
            return;
        }

        installWithPackageInstallerOrIntent(activity, file);
    }

    private void installWithPackageInstallerOrIntent(Activity activity, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                registerInstallReceiver(activity.getApplicationContext());
                PackageInstaller installer = activity.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(activity.getPackageName());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                }
                int sessionId = installer.createSession(params);
                try (PackageInstaller.Session session = installer.openSession(sessionId);
                     InputStream in = new FileInputStream(file);
                     OutputStream out = session.openWrite("NovaGramUpdate", 0, file.length())) {
                    byte[] buffer = new byte[32768];
                    int c;
                    while ((c = in.read(buffer)) != -1) {
                        out.write(buffer, 0, c);
                    }
                    session.fsync(out);
                    out.close();

                    Intent callbackIntent = new Intent(ACTION_INSTALL_COMMIT);
                    callbackIntent.setPackage(activity.getPackageName());
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        flags |= PendingIntent.FLAG_MUTABLE;
                    }
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, sessionId, callbackIntent, flags);
                    session.commit(pendingIntent.getIntentSender());
                    return;
                }
            } catch (Throwable t) {
                FileLog.e("PackageInstaller failed, falling back to FileProvider ACTION_VIEW", t);
            }
        }

        // Fallback via FileProvider
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

    private synchronized void registerInstallReceiver(Context appContext) {
        if (installReceiver != null || appContext == null) {
            return;
        }
        installReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                    Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                    if (confirmIntent != null && LaunchActivity.instance != null) {
                        confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            LaunchActivity.instance.startActivity(confirmIntent);
                        } catch (Throwable t) {
                            FileLog.e(t);
                        }
                    }
                } else if (status == PackageInstaller.STATUS_SUCCESS) {
                    BaseFragment f = LaunchActivity.getLastFragment();
                    if (f != null && f.getLayoutContainer() != null) {
                        BulletinFactory.of(f).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.NovaInstallSuccess)).show();
                    }
                } else {
                    String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                    FileLog.e("NovaGram PackageInstaller status=" + status + " msg=" + msg);
                    if (status == PackageInstaller.STATUS_FAILURE_CONFLICT && LaunchActivity.instance != null) {
                        new AlertDialog.Builder(LaunchActivity.instance)
                                .setTitle(LocaleController.getString(R.string.NovaSignatureMismatchTitle))
                                .setMessage(LocaleController.getString(R.string.NovaSignatureMismatchMessage))
                                .setPositiveButton(LocaleController.getString(R.string.OK), null)
                                .show();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_INSTALL_COMMIT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(installReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            appContext.registerReceiver(installReceiver, filter);
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isApkSignatureCompatible(Context context, File apkFile) {
        try {
            PackageManager pm = context.getPackageManager();
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? PackageManager.GET_SIGNING_CERTIFICATES
                    : PackageManager.GET_SIGNATURES;
            PackageInfo installed = pm.getPackageInfo(context.getPackageName(), flags);
            PackageInfo archive = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), flags);
            if (installed == null || archive == null) {
                return true;
            }
            if (!TextUtils.isEmpty(archive.packageName) && !TextUtils.equals(installed.packageName, archive.packageName)) {
                return false;
            }
            Signature[] instSigs = getSignaturesFromInfo(installed);
            Signature[] archSigs = getSignaturesFromInfo(archive);
            if (instSigs == null || archSigs == null || instSigs.length == 0 || archSigs.length == 0) {
                return true;
            }
            return Arrays.equals(instSigs[0].toByteArray(), archSigs[0].toByteArray());
        } catch (Throwable t) {
            FileLog.e("Signature compatibility check skipped", t);
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    private static Signature[] getSignaturesFromInfo(PackageInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
            Signature[] hist = info.signingInfo.getSigningCertificateHistory();
            if (hist != null && hist.length > 0) return hist;
            return info.signingInfo.getApkContentsSigners();
        }
        return info.signatures;
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
