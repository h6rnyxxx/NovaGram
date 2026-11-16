package org.telegram.novagram.plugins;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.novagram.NovaConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NovaPluginManager {

    public static final int OPTION_INSTALL_PLUGIN_FILE = 9500;
    public static final int OPTION_INSTALL_FONT_FILE = 9501;
    public static final int OPTION_PLUGIN_MENU_BASE = 9600;

    public static class OutgoingResult {
        public boolean cancelled;
        public String text;
    }

    private static volatile NovaPluginManager instance;

    public static NovaPluginManager getInstance() {
        if (instance == null) {
            synchronized (NovaPluginManager.class) {
                if (instance == null) {
                    instance = new NovaPluginManager();
                }
            }
        }
        return instance;
    }

    private final List<NovaPlugin> plugins = new ArrayList<>();
    private final Map<Integer, MenuDispatchTarget> activeMenuTargets = new LinkedHashMap<>();
    private boolean initialized = false;

    private static class MenuDispatchTarget {
        final NovaPlugin plugin;
        final NovaPlugin.MenuActionEntry action;

        MenuDispatchTarget(NovaPlugin plugin, NovaPlugin.MenuActionEntry action) {
            this.plugin = plugin;
            this.action = action;
        }
    }

    public File getPluginsDir() {
        File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "novagram/plugins");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public synchronized void init() {
        if (initialized || ApplicationLoader.applicationContext == null) {
            return;
        }
        initialized = true;
        if (!NovaConfig.isSeededDefaultPlugins()) {
            seedBuiltInPlugins();
            NovaConfig.setSeededDefaultPlugins(true);
        }
        reloadAll();
    }

    private void seedBuiltInPlugins() {
        writePluginFile("nova_essentials.nvplugin",
                "// @id nova_essentials\n"
                        + "// @name Nova Essentials\n"
                        + "// @version 1.0.0\n"
                        + "// @author NovaGram Team\n"
                        + "// @description Built-in utility commands (.ping, .shrug, .reverse, .mock) and message word counter.\n"
                        + "// @permissions ui, storage, commands, messages\n\n"
                        + "nova.settings.define([\n"
                        + "  { type: 'header', title: 'Chat Commands' },\n"
                        + "  { type: 'switch', key: 'enable_commands', title: 'Enable .ping / .shrug / .reverse / .mock', default: true },\n"
                        + "  { type: 'switch', key: 'enable_counter', title: 'Show Word Counter in Message Menu', default: true }\n"
                        + "]);\n\n"
                        + "function isCommandsEnabled() {\n"
                        + "  return nova.storage.getBoolean('enable_commands', true);\n"
                        + "}\n\n"
                        + "nova.commands.register('ping', 'Check NovaGram plugin latency', function(ctx) {\n"
                        + "  if (!isCommandsEnabled()) return null;\n"
                        + "  nova.ui.toast('Pong! NovaPlugins JS engine active (' + nova.device.getNovaVersion() + ')');\n"
                        + "  return { cancel: true };\n"
                        + "});\n\n"
                        + "nova.commands.register('shrug', 'Append shrug emoticon', function(ctx) {\n"
                        + "  if (!isCommandsEnabled()) return null;\n"
                        + "  var base = ctx.args ? (ctx.args + ' ') : '';\n"
                        + "  return { text: base + '¯\\\\_(ツ)_/¯' };\n"
                        + "});\n\n"
                        + "nova.commands.register('reverse', 'Reverse text', function(ctx) {\n"
                        + "  if (!isCommandsEnabled() || !ctx.args) return null;\n"
                        + "  return { text: ctx.args.split('').reverse().join('') };\n"
                        + "});\n\n"
                        + "nova.commands.register('mock', 'Alternating case text', function(ctx) {\n"
                        + "  if (!isCommandsEnabled() || !ctx.args) return null;\n"
                        + "  var out = '';\n"
                        + "  for (var i = 0; i < ctx.args.length; i++) {\n"
                        + "    var ch = ctx.args.charAt(i);\n"
                        + "    out += (i % 2 === 0) ? ch.toLowerCase() : ch.toUpperCase();\n"
                        + "  }\n"
                        + "  return { text: out };\n"
                        + "});\n\n"
                        + "nova.menu.registerMessageAction('word_count', 'Word & Char Count', function(msg) {\n"
                        + "  if (!nova.storage.getBoolean('enable_counter', true)) return;\n"
                        + "  var txt = (msg.text || '').trim();\n"
                        + "  var chars = txt.length;\n"
                        + "  var words = chars === 0 ? 0 : txt.split(/\\s+/).length;\n"
                        + "  nova.ui.alert('Message Stats', 'Words: ' + words + '\\nCharacters: ' + chars + '\\nMessage ID: ' + msg.id);\n"
                        + "});\n"
        );

        writePluginFile("nova_signature.nvplugin",
                "// @id nova_signature\n"
                        + "// @name Quick Signature\n"
                        + "// @version 1.0.0\n"
                        + "// @author NovaGram Team\n"
                        + "// @description Optionally append a custom signature to outgoing messages.\n"
                        + "// @permissions ui, storage, messages\n\n"
                        + "nova.settings.define([\n"
                        + "  { type: 'header', title: 'Signature Settings' },\n"
                        + "  { type: 'switch', key: 'enabled', title: 'Append Signature to Outgoing Messages', default: false },\n"
                        + "  { type: 'text', key: 'suffix', title: 'Signature Text', subtitle: 'Appended to each message', default: '— via NovaGram' }\n"
                        + "]);\n\n"
                        + "nova.hooks.onSendMessage(function(ctx) {\n"
                        + "  if (!nova.storage.getBoolean('enabled', false)) return null;\n"
                        + "  var suffix = nova.storage.getString('suffix', '— via NovaGram');\n"
                        + "  if (!suffix || !ctx.text || ctx.text.charAt(0) === '/' || ctx.text.charAt(0) === '.') return null;\n"
                        + "  return { text: ctx.text + '\\n' + suffix };\n"
                        + "});\n"
        );
    }

    private void writePluginFile(String fileName, String content) {
        try {
            File target = new File(getPluginsDir(), fileName);
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public synchronized void reloadAll() {
        plugins.clear();
        File dir = getPluginsDir();
        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".nvplugin") || lower.endsWith(".nv.js") || lower.endsWith(".js");
        });
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        android.content.SharedPreferences statePrefs = ApplicationLoader.applicationContext
                .getSharedPreferences("novaplugins_state", android.content.Context.MODE_PRIVATE);

        for (File file : files) {
            try {
                String source = readFileUtf8(file);
                String fallbackId = file.getName().replaceFirst("\\.(nvplugin|nv\\.js|js)$", "");
                NovaPlugin plugin = new NovaPlugin();
                plugin.filePath = file.getAbsolutePath();
                plugin.sourceCode = source;
                NovaPluginEngine.parseMetadata(plugin, fallbackId);
                plugin.enabled = statePrefs.getBoolean("enabled_" + plugin.id, true);

                if (NovaConfig.isPluginsEnabled() && plugin.enabled) {
                    NovaPluginEngine.loadPlugin(plugin);
                }
                plugins.add(plugin);
            } catch (Throwable t) {
                FileLog.e("Failed loading plugin file: " + file.getName(), t);
            }
        }
    }

    public synchronized List<NovaPlugin> getPlugins() {
        init();
        return Collections.unmodifiableList(new ArrayList<>(plugins));
    }

    public synchronized int getActivePluginsCount() {
        init();
        if (!NovaConfig.isPluginsEnabled()) return 0;
        int count = 0;
        for (NovaPlugin p : plugins) {
            if (p.enabled && p.lastError == null) count++;
        }
        return count;
    }

    public synchronized void setPluginEnabled(NovaPlugin plugin, boolean enabled) {
        if (plugin == null) return;
        plugin.enabled = enabled;
        ApplicationLoader.applicationContext
                .getSharedPreferences("novaplugins_state", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("enabled_" + plugin.id, enabled)
                .apply();
        if (enabled && NovaConfig.isPluginsEnabled()) {
            NovaPluginEngine.loadPlugin(plugin);
        } else {
            plugin.resetRuntimeState();
        }
    }

    public synchronized boolean uninstallPlugin(NovaPlugin plugin) {
        if (plugin == null || TextUtils.isEmpty(plugin.filePath)) return false;
        plugin.resetRuntimeState();
        File file = new File(plugin.filePath);
        boolean deleted = !file.exists() || file.delete();
        plugins.remove(plugin);
        return deleted;
    }

    public synchronized NovaPlugin installPluginFromFile(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) return null;
        try {
            String content = readFileUtf8(sourceFile);
            String name = sourceFile.getName();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".nvplugin") && !name.toLowerCase(Locale.ROOT).endsWith(".js")) {
                name = name + ".nvplugin";
            }
            File dest = new File(getPluginsDir(), name);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            }
            reloadAll();
            for (NovaPlugin p : plugins) {
                if (dest.getAbsolutePath().equals(p.filePath)) {
                    return p;
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return null;
    }

    public synchronized NovaPlugin installPluginFromUri(Activity activity, Uri uri) {
        if (activity == null || uri == null) return null;
        try (InputStream is = activity.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            byte[] data = readAllBytes(is);
            String fileName = queryFileName(activity, uri);
            if (TextUtils.isEmpty(fileName)) {
                fileName = "plugin_" + System.currentTimeMillis() + ".nvplugin";
            }
            if (!fileName.toLowerCase(Locale.ROOT).endsWith(".nvplugin") && !fileName.toLowerCase(Locale.ROOT).endsWith(".js")) {
                fileName += ".nvplugin";
            }
            File dest = new File(getPluginsDir(), fileName);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(data);
            }
            reloadAll();
            for (NovaPlugin p : plugins) {
                if (dest.getAbsolutePath().equals(p.filePath)) {
                    return p;
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return null;
    }

    public synchronized OutgoingResult processOutgoingText(long dialogId, String text) {
        OutgoingResult out = new OutgoingResult();
        out.text = text;
        if (!NovaConfig.isPluginsEnabled() || TextUtils.isEmpty(text)) {
            return out;
        }
        init();

        String trimmed = text.trim();
        if (trimmed.startsWith(".")) {
            int spaceIdx = trimmed.indexOf(' ');
            String cmd = (spaceIdx > 0 ? trimmed.substring(0, spaceIdx) : trimmed).toLowerCase(Locale.ROOT);
            String args = spaceIdx > 0 ? trimmed.substring(spaceIdx + 1).trim() : "";
            for (NovaPlugin p : plugins) {
                if (!p.enabled || p.scope == null) continue;
                NovaPlugin.CommandEntry entry = p.commands.get(cmd);
                if (entry != null) {
                    Scriptable ctxObj = createContextObj(p, dialogId, out.text, args);
                    Object res = NovaPluginEngine.invokeFunction(p, entry.handler, ctxObj);
                    if (applyJsMessageResult(res, out)) {
                        return out;
                    }
                }
            }
        }

        for (NovaPlugin p : plugins) {
            if (!p.enabled || p.scope == null || p.onSendHooks.isEmpty()) continue;
            for (org.mozilla.javascript.Function fn : p.onSendHooks) {
                Scriptable ctxObj = createContextObj(p, dialogId, out.text, "");
                Object res = NovaPluginEngine.invokeFunction(p, fn, ctxObj);
                applyJsMessageResult(res, out);
                if (out.cancelled) {
                    return out;
                }
            }
        }
        return out;
    }

    public synchronized void fillMessageMenu(
            MessageObject message,
            ArrayList<Integer> icons,
            ArrayList<CharSequence> items,
            ArrayList<Integer> options
    ) {
        activeMenuTargets.clear();
        if (message == null) return;

        if (isPluginDocument(message)) {
            items.add(LocaleController.getString(R.string.NovaInstallPlugin));
            options.add(OPTION_INSTALL_PLUGIN_FILE);
            icons.add(R.drawable.msg_add);
        } else if (isFontDocument(message)) {
            items.add(LocaleController.getString(R.string.NovaInstallFont));
            options.add(OPTION_INSTALL_FONT_FILE);
            icons.add(R.drawable.msg_theme);
        }

        if (!NovaConfig.isPluginsEnabled()) return;
        init();

        int nextOption = OPTION_PLUGIN_MENU_BASE;
        for (NovaPlugin p : plugins) {
            if (!p.enabled || p.scope == null || p.menuActions.isEmpty()) continue;
            for (NovaPlugin.MenuActionEntry action : p.menuActions) {
                action.assignedOptionId = nextOption;
                activeMenuTargets.put(nextOption, new MenuDispatchTarget(p, action));
                items.add(action.title);
                options.add(nextOption);
                icons.add(R.drawable.msg_bot);
                nextOption++;
            }
        }
    }

    public synchronized boolean handleMessageMenuOption(ChatActivity chatActivity, int option, MessageObject message) {
        if (option == OPTION_INSTALL_FONT_FILE && message != null) {
            TLRPC.Document doc = message.getDocument();
            if (doc != null) {
                File file = FileLoader.getInstance(chatActivity.getCurrentAccount()).getPathToAttach(doc, true);
                if (file != null && file.exists()) {
                    boolean ok = org.telegram.novagram.design.NovaFontManager.installFontFromFile(file);
                    if (ok) {
                        BulletinFactory.of(chatActivity).createSimpleBulletin(
                                R.raw.chats_infotip,
                                LocaleController.formatString(R.string.NovaFontInstalled, file.getName())
                        ).show();
                    } else {
                        BulletinFactory.of(chatActivity).createErrorBulletin(LocaleController.getString(R.string.NovaFontInstallFailed)).show();
                    }
                } else {
                    new AlertDialog.Builder(chatActivity.getContext())
                            .setTitle("NovaGram")
                            .setMessage(LocaleController.getString(R.string.NovaPluginDownloadFirst))
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
            return true;
        }

        if (option == OPTION_INSTALL_PLUGIN_FILE && message != null) {
            TLRPC.Document doc = message.getDocument();
            if (doc != null) {
                File file = FileLoader.getInstance(chatActivity.getCurrentAccount()).getPathToAttach(doc, true);
                if (file != null && file.exists()) {
                    NovaPlugin installed = installPluginFromFile(file);
                    if (installed != null) {
                        BulletinFactory.of(chatActivity).createSimpleBulletin(
                                R.raw.chats_infotip,
                                LocaleController.formatString(R.string.NovaPluginInstalled, installed.name)
                        ).show();
                    } else {
                        BulletinFactory.of(chatActivity).createErrorBulletin(LocaleController.getString(R.string.NovaPluginInstallFailed)).show();
                    }
                } else {
                    new AlertDialog.Builder(chatActivity.getContext())
                            .setTitle("NovaPlugins")
                            .setMessage(LocaleController.getString(R.string.NovaPluginDownloadFirst))
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
            return true;
        }

        MenuDispatchTarget target = activeMenuTargets.get(option);
        if (target == null || message == null) {
            return false;
        }
        Scriptable msgObj = createMessageJsObject(target.plugin, message);
        NovaPluginEngine.invokeFunction(target.plugin, target.action.handler, msgObj);
        return true;
    }

    public static boolean isFontDocument(MessageObject message) {
        if (message == null) return false;
        TLRPC.Document doc = message.getDocument();
        if (doc == null) return false;
        String fileName = FileLoader.getDocumentFileName(doc);
        return org.telegram.novagram.design.NovaFontManager.isFontFileName(fileName);
    }

    public static boolean isPluginDocument(MessageObject message) {
        if (message == null) return false;
        TLRPC.Document doc = message.getDocument();
        if (doc == null) return false;
        String fileName = FileLoader.getDocumentFileName(doc);
        if (TextUtils.isEmpty(fileName)) return false;
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".nvplugin") || lower.endsWith(".nv.js");
    }

    private Scriptable createContextObj(NovaPlugin plugin, long dialogId, String text, String args) {
        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
        cx.setOptimizationLevel(-1);
        try {
            Scriptable obj = cx.newObject(plugin.scope);
            ScriptableObject.putProperty(obj, "dialogId", String.valueOf(dialogId));
            ScriptableObject.putProperty(obj, "text", text != null ? text : "");
            ScriptableObject.putProperty(obj, "args", args != null ? args : "");
            return obj;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    private Scriptable createMessageJsObject(NovaPlugin plugin, MessageObject message) {
        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
        cx.setOptimizationLevel(-1);
        try {
            Scriptable obj = cx.newObject(plugin.scope);
            CharSequence rawText = !TextUtils.isEmpty(message.caption) ? message.caption : message.messageText;
            ScriptableObject.putProperty(obj, "id", message.getId());
            ScriptableObject.putProperty(obj, "dialogId", String.valueOf(message.getDialogId()));
            ScriptableObject.putProperty(obj, "text", rawText != null ? rawText.toString() : "");
            ScriptableObject.putProperty(obj, "isOut", message.isOut());
            ScriptableObject.putProperty(obj, "date", message.messageOwner != null ? message.messageOwner.date : 0);
            return obj;
        } finally {
            org.mozilla.javascript.Context.exit();
        }
    }

    private boolean applyJsMessageResult(Object res, OutgoingResult out) {
        if (res instanceof NativeObject) {
            NativeObject obj = (NativeObject) res;
            Object cancel = obj.get("cancel", obj);
            if (cancel instanceof Boolean && (Boolean) cancel) {
                out.cancelled = true;
                return true;
            }
            Object newText = obj.get("text", obj);
            if (newText != null && newText != Scriptable.NOT_FOUND) {
                out.text = String.valueOf(newText);
            }
        } else if (res instanceof String) {
            out.text = (String) res;
        }
        return false;
    }

    private static String readFileUtf8(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            return new String(readAllBytes(fis), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static String queryFileName(Activity activity, Uri uri) {
        try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    return cursor.getString(idx);
                }
            }
        } catch (Throwable ignored) {
        }
        return uri.getLastPathSegment();
    }
}
