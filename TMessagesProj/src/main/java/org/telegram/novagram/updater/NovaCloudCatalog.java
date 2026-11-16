package org.telegram.novagram.updater;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.novagram.NovaConfig;
import org.telegram.novagram.plugins.NovaPlugin;
import org.telegram.novagram.plugins.NovaPluginManager;
import org.telegram.ui.web.HttpGetTask;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cloud Hot-Sync service for NovaGram: fetches live announcements and curated .nvplugin scripts
 * from the NovaGram cloud manifest so users get new plugins and patches without an APK rebuild.
 */
public class NovaCloudCatalog {

    public static class CloudPluginItem {
        public final String id;
        public final String name;
        public final String version;
        public final String author;
        public final String description;
        public final String url;

        public CloudPluginItem(String id, String name, String version, String author, String description, String url) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.author = author;
            this.description = description;
            this.url = url;
        }
    }

    public interface InstallCallback {
        void onComplete(NovaPlugin plugin, String error);
    }

    private static volatile NovaCloudCatalog instance;

    public static NovaCloudCatalog getInstance() {
        if (instance == null) {
            synchronized (NovaCloudCatalog.class) {
                if (instance == null) {
                    instance = new NovaCloudCatalog();
                }
            }
        }
        return instance;
    }

    private static final long SYNC_INTERVAL_MS = 3L * 60L * 60L * 1000L;

    private String announcement = "";
    private final List<CloudPluginItem> cloudPlugins = new ArrayList<>();
    private long lastSync = 0L;
    private boolean syncing = false;

    private NovaCloudCatalog() {
        loadCached();
    }

    private SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("nova_cloud", Context.MODE_PRIVATE);
    }

    private synchronized void loadCached() {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences prefs = getPrefs();
        lastSync = prefs.getLong("last_sync", 0L);
        String raw = prefs.getString("manifest_json", "");
        if (!TextUtils.isEmpty(raw)) {
            parseManifest(raw);
        }
    }

    public synchronized List<CloudPluginItem> getCloudPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(cloudPlugins));
    }

    public synchronized String getAnnouncement() {
        return announcement;
    }

    public void syncAsync(boolean force, Runnable onDone) {
        if (syncing) {
            if (onDone != null) AndroidUtilities.runOnUIThread(onDone);
            return;
        }
        if (!force && System.currentTimeMillis() - lastSync < SYNC_INTERVAL_MS) {
            if (onDone != null) AndroidUtilities.runOnUIThread(onDone);
            return;
        }
        syncing = true;
        new HttpGetTask(res -> {
            syncing = false;
            if (!TextUtils.isEmpty(res)) {
                synchronized (NovaCloudCatalog.this) {
                    if (parseManifest(res)) {
                        lastSync = System.currentTimeMillis();
                        getPrefs().edit()
                                .putString("manifest_json", res)
                                .putLong("last_sync", lastSync)
                                .apply();
                    }
                }
            }
            if (onDone != null) {
                AndroidUtilities.runOnUIThread(onDone);
            }
        })
                .setHeader("User-Agent", "NovaGram-Android/" + BuildConfig.NOVA_VERSION_NAME)
                .setHeader("Accept", "application/json")
                .execute(NovaConfig.CLOUD_MANIFEST_URL);
    }

    private boolean parseManifest(String raw) {
        try {
            JSONObject obj = new JSONObject(raw);
            announcement = obj.optString("announcement", "");
            cloudPlugins.clear();
            JSONArray arr = obj.optJSONArray("plugins");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.optJSONObject(i);
                    if (p == null) continue;
                    String id = p.optString("id", "");
                    String name = p.optString("name", id);
                    String ver = p.optString("version", "1.0.0");
                    String author = p.optString("author", "NovaGram");
                    String desc = p.optString("description", "");
                    String url = p.optString("url", "");
                    if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(url)) {
                        cloudPlugins.add(new CloudPluginItem(id, name, ver, author, desc, url));
                    }
                }
            }
            return true;
        } catch (Throwable t) {
            FileLog.e("NovaCloudCatalog parse failed", t);
            return false;
        }
    }

    public void installCloudPlugin(CloudPluginItem item, InstallCallback callback) {
        if (item == null || TextUtils.isEmpty(item.url)) {
            if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.onComplete(null, "Invalid plugin URL"));
            return;
        }
        new HttpGetTask(code -> AndroidUtilities.runOnUIThread(() -> {
            if (TextUtils.isEmpty(code)) {
                if (callback != null) callback.onComplete(null, "Network error");
                return;
            }
            try {
                File tmp = new File(NovaPluginManager.getInstance().getPluginsDir(), item.id.replaceAll("[^a-zA-Z0-9._-]", "_") + ".nvplugin");
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    fos.write(code.getBytes(StandardCharsets.UTF_8));
                }
                NovaPluginManager.getInstance().reloadAll();
                NovaPlugin installed = null;
                for (NovaPlugin p : NovaPluginManager.getInstance().getPlugins()) {
                    if (TextUtils.equals(p.id, item.id) || tmp.getAbsolutePath().equals(p.filePath)) {
                        installed = p;
                        break;
                    }
                }
                if (callback != null) callback.onComplete(installed, installed == null ? "Parse failed" : null);
            } catch (Throwable t) {
                FileLog.e(t);
                if (callback != null) callback.onComplete(null, t.getMessage());
            }
        }))
                .setHeader("User-Agent", "NovaGram-Android/" + BuildConfig.NOVA_VERSION_NAME)
                .execute(item.url);
    }
}
