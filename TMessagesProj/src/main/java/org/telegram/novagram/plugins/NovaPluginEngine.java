package org.telegram.novagram.plugins;

import android.text.TextUtils;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.telegram.messenger.FileLog;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NovaPluginEngine {

    private static final Pattern META_TAG = Pattern.compile("^\\s*(?://|\\*)\\s*@(\\w+)\\s+(.+)$", Pattern.MULTILINE);

    private static final ClassShutter SANDBOX_SHUTTER = fullClassName -> {
        if (fullClassName == null) return false;
        if (fullClassName.startsWith("org.telegram.novagram.plugins.NovaPluginApi$")) {
            return true;
        }
        return fullClassName.equals("java.lang.String")
                || fullClassName.equals("java.lang.Boolean")
                || fullClassName.equals("java.lang.Integer")
                || fullClassName.equals("java.lang.Double")
                || fullClassName.equals("java.lang.Object");
    };

    private static Context enterSandboxedContext() {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1); // Required on Android ART
        cx.setLanguageVersion(Context.VERSION_ES6);
        try {
            cx.setClassShutter(SANDBOX_SHUTTER);
        } catch (SecurityException ignored) {
            // ClassShutter already set on this Context
        }
        return cx;
    }

    public static void parseMetadata(NovaPlugin plugin, String fallbackId) {
        plugin.id = fallbackId;
        plugin.name = fallbackId;
        plugin.version = "1.0.0";
        plugin.author = "Unknown";
        plugin.description = "";
        plugin.permissions.clear();

        if (TextUtils.isEmpty(plugin.sourceCode)) {
            return;
        }

        Matcher m = META_TAG.matcher(plugin.sourceCode);
        while (m.find()) {
            String tag = m.group(1).trim().toLowerCase();
            String val = m.group(2).trim();
            switch (tag) {
                case "id":
                    plugin.id = val;
                    break;
                case "name":
                    plugin.name = val;
                    break;
                case "version":
                    plugin.version = val;
                    break;
                case "author":
                    plugin.author = val;
                    break;
                case "description":
                    plugin.description = val;
                    break;
                case "permissions":
                    String[] parts = val.split("[,\\s]+");
                    for (String part : parts) {
                        if (!part.isEmpty()) {
                            plugin.permissions.add(part.trim());
                        }
                    }
                    break;
            }
        }
        if (plugin.permissions.isEmpty()) {
            plugin.permissions.addAll(Arrays.asList("ui", "storage", "commands"));
        }
    }

    public static boolean loadPlugin(NovaPlugin plugin) {
        plugin.resetRuntimeState();
        plugin.lastError = null;
        if (!plugin.enabled || TextUtils.isEmpty(plugin.sourceCode)) {
            return true;
        }

        Context cx = enterSandboxedContext();
        try {
            Scriptable scope = cx.initSafeStandardObjects();
            NovaPluginApi api = new NovaPluginApi(plugin);
            Scriptable novaObj = api.buildNovaObject(cx, scope);
            ScriptableObject.putProperty(scope, "nova", novaObj);

            cx.evaluateString(scope, plugin.sourceCode, plugin.id + ".nvplugin", 1, null);
            plugin.scope = scope;
            return true;
        } catch (Throwable t) {
            plugin.lastError = t.getMessage() != null ? t.getMessage() : t.toString();
            FileLog.e("NovaPlugin load error [" + plugin.id + "]", t);
            plugin.resetRuntimeState();
            return false;
        } finally {
            Context.exit();
        }
    }

    public static Object invokeFunction(NovaPlugin plugin, Function fn, Object... args) {
        if (plugin == null || fn == null || plugin.scope == null) {
            return null;
        }
        Context cx = enterSandboxedContext();
        try {
            return fn.call(cx, plugin.scope, plugin.scope, args);
        } catch (Throwable t) {
            plugin.lastError = t.getMessage() != null ? t.getMessage() : t.toString();
            FileLog.e("NovaPlugin runtime error [" + plugin.id + "]", t);
            return null;
        } finally {
            Context.exit();
        }
    }
}
