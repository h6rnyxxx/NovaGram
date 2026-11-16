package org.telegram.novagram.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class NovaPluginApi {

    private final NovaPlugin plugin;

    public NovaPluginApi(NovaPlugin plugin) {
        this.plugin = plugin;
    }

    public Scriptable buildNovaObject(org.mozilla.javascript.Context cx, Scriptable scope) {
        Scriptable nova = cx.newObject(scope);

        Scriptable meta = cx.newObject(scope);
        ScriptableObject.putProperty(meta, "id", plugin.id);
        ScriptableObject.putProperty(meta, "name", plugin.name);
        ScriptableObject.putProperty(meta, "version", plugin.version);
        ScriptableObject.putProperty(meta, "author", plugin.author);
        ScriptableObject.putProperty(nova, "plugin", meta);

        ScriptableObject.putProperty(nova, "ui", new UiBridge());
        ScriptableObject.putProperty(nova, "storage", new StorageBridge(plugin.id));
        ScriptableObject.putProperty(nova, "settings", new SettingsBridge(plugin));
        ScriptableObject.putProperty(nova, "commands", new CommandsBridge(plugin));
        ScriptableObject.putProperty(nova, "hooks", new HooksBridge(plugin));
        ScriptableObject.putProperty(nova, "menu", new MenuBridge(plugin));
        ScriptableObject.putProperty(nova, "net", new NetBridge(plugin));
        ScriptableObject.putProperty(nova, "device", new DeviceBridge());

        return nova;
    }

    public static class UiBridge {
        public void toast(String text) {
            if (TextUtils.isEmpty(text)) return;
            AndroidUtilities.runOnUIThread(() -> {
                LaunchActivity activity = LaunchActivity.instance;
                if (activity != null) {
                    BaseFragment fragment = LaunchActivity.getLastFragment();
                    if (fragment != null && fragment.getLayoutContainer() != null) {
                        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.chats_infotip, text).show();
                        return;
                    }
                }
                if (ApplicationLoader.applicationContext != null) {
                    Toast.makeText(ApplicationLoader.applicationContext, text, Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void alert(String title, String message) {
            AndroidUtilities.runOnUIThread(() -> {
                LaunchActivity activity = LaunchActivity.instance;
                if (activity == null) {
                    toast((title != null ? title + ": " : "") + message);
                    return;
                }
                new AlertDialog.Builder(activity)
                        .setTitle(title != null ? title : "NovaPlugin")
                        .setMessage(message != null ? message : "")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }

        public void copy(String text) {
            if (text == null) return;
            AndroidUtilities.runOnUIThread(() -> AndroidUtilities.addToClipboard(text));
        }
    }

    public static class StorageBridge {
        private final SharedPreferences prefs;

        public StorageBridge(String pluginId) {
            String safeId = pluginId.replaceAll("[^a-zA-Z0-9._-]", "_");
            prefs = ApplicationLoader.applicationContext.getSharedPreferences("novaplugin_" + safeId, Context.MODE_PRIVATE);
        }

        public String getString(String key, String def) {
            return prefs.getString(key, def);
        }

        public boolean getBoolean(String key, boolean def) {
            return prefs.getBoolean(key, def);
        }

        public int getInt(String key, int def) {
            return prefs.getInt(key, def);
        }

        public Object get(String key, Object def) {
            if (!prefs.contains(key)) {
                return def;
            }
            Object v = prefs.getAll().get(key);
            return v != null ? v : def;
        }

        public void set(String key, Object value) {
            if (TextUtils.isEmpty(key)) return;
            SharedPreferences.Editor e = prefs.edit();
            if (value == null) {
                e.remove(key);
            } else if (value instanceof Boolean) {
                e.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                e.putInt(key, (Integer) value);
            } else if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (d == Math.rint(d) && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE) {
                    e.putInt(key, (int) d);
                } else {
                    e.putString(key, String.valueOf(value));
                }
            } else {
                e.putString(key, String.valueOf(value));
            }
            e.apply();
        }

        public void remove(String key) {
            prefs.edit().remove(key).apply();
        }
    }

    public static class SettingsBridge {
        private final NovaPlugin plugin;

        public SettingsBridge(NovaPlugin plugin) {
            this.plugin = plugin;
        }

        public void addHeader(String title) {
            plugin.settings.add(new NovaPlugin.SettingItem(
                    NovaPlugin.SettingItem.TYPE_HEADER, null, title, null, null, null
            ));
        }

        public void addSwitch(String key, String title, String subtitle, boolean defaultValue) {
            plugin.settings.add(new NovaPlugin.SettingItem(
                    NovaPlugin.SettingItem.TYPE_SWITCH, key, title, subtitle, defaultValue, null
            ));
        }

        public void addText(String key, String title, String subtitle, String defaultValue) {
            plugin.settings.add(new NovaPlugin.SettingItem(
                    NovaPlugin.SettingItem.TYPE_TEXT, key, title, subtitle, defaultValue != null ? defaultValue : "", null
            ));
        }

        public void addButton(String title, Function onClick) {
            plugin.settings.add(new NovaPlugin.SettingItem(
                    NovaPlugin.SettingItem.TYPE_BUTTON, null, title, null, null, onClick
            ));
        }

        public void define(Object rawItems) {
            if (!(rawItems instanceof NativeArray)) {
                return;
            }
            NativeArray arr = (NativeArray) rawItems;
            for (int i = 0; i < arr.getLength(); i++) {
                Object itemObj = arr.get(i, arr);
                if (!(itemObj instanceof NativeObject)) continue;
                NativeObject obj = (NativeObject) itemObj;
                String type = strProp(obj, "type", "switch").toLowerCase(Locale.ROOT);
                String key = strProp(obj, "key", "");
                String title = strProp(obj, "title", key);
                String subtitle = strProp(obj, "subtitle", null);
                if ("header".equals(type)) {
                    addHeader(title);
                } else if ("text".equals(type)) {
                    addText(key, title, subtitle, strProp(obj, "default", ""));
                } else if ("button".equals(type)) {
                    Object fn = obj.get("onClick", obj);
                    addButton(title, fn instanceof Function ? (Function) fn : null);
                } else {
                    Object def = obj.get("default", obj);
                    boolean boolDef = def instanceof Boolean ? (Boolean) def : false;
                    addSwitch(key, title, subtitle, boolDef);
                }
            }
        }

        private static String strProp(NativeObject obj, String key, String def) {
            Object v = obj.get(key, obj);
            if (v == null || v == Scriptable.NOT_FOUND) return def;
            return String.valueOf(v);
        }
    }

    public static class CommandsBridge {
        private final NovaPlugin plugin;

        public CommandsBridge(NovaPlugin plugin) {
            this.plugin = plugin;
        }

        public void register(String command, String description, Function handler) {
            if (TextUtils.isEmpty(command) || handler == null) return;
            String normalized = command.trim().toLowerCase(Locale.ROOT);
            if (!normalized.startsWith(".")) {
                normalized = "." + normalized;
            }
            plugin.commands.put(normalized, new NovaPlugin.CommandEntry(normalized, description != null ? description : "", handler));
        }
    }

    public static class HooksBridge {
        private final NovaPlugin plugin;

        public HooksBridge(NovaPlugin plugin) {
            this.plugin = plugin;
        }

        public void onSendMessage(Function handler) {
            if (handler != null) {
                plugin.onSendHooks.add(handler);
            }
        }
    }

    public static class MenuBridge {
        private final NovaPlugin plugin;

        public MenuBridge(NovaPlugin plugin) {
            this.plugin = plugin;
        }

        public void registerMessageAction(String id, String title, Function handler) {
            if (TextUtils.isEmpty(title) || handler == null) return;
            String safeId = TextUtils.isEmpty(id) ? title : id;
            plugin.menuActions.add(new NovaPlugin.MenuActionEntry(safeId, title, handler));
        }
    }

    public static class NetBridge {
        private final NovaPlugin plugin;

        public NetBridge(NovaPlugin plugin) {
            this.plugin = plugin;
        }

        public void get(String urlString, Function callback) {
            if (!plugin.hasPermission("network")) {
                if (callback != null) {
                    NovaPluginEngine.invokeFunction(plugin, callback, "Permission 'network' not declared in @permissions", null);
                }
                return;
            }
            Utilities.globalQueue.postRunnable(() -> {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "NovaGram-Android/" + BuildConfig.NOVA_VERSION_NAME);
                    int code = conn.getResponseCode();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream(),
                            StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    reader.close();
                    String body = sb.toString();
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> NovaPluginEngine.invokeFunction(plugin, callback, code >= 200 && code < 400 ? null : ("HTTP " + code), body));
                    }
                } catch (Throwable t) {
                    FileLog.e(t);
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> NovaPluginEngine.invokeFunction(plugin, callback, t.getMessage(), null));
                    }
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            });
        }
    }

    public static class DeviceBridge {
        public String getModel() {
            return Build.MANUFACTURER + " " + Build.MODEL;
        }

        public int getSdkInt() {
            return Build.VERSION.SDK_INT;
        }

        public String getNovaVersion() {
            return BuildConfig.NOVA_VERSION_NAME;
        }
    }
}
