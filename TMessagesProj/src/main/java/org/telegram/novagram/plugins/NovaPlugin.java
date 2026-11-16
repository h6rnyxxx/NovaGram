package org.telegram.novagram.plugins;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NovaPlugin {

    public static class SettingItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_SWITCH = 1;
        public static final int TYPE_TEXT = 2;
        public static final int TYPE_BUTTON = 3;

        public final int type;
        public final String key;
        public final String title;
        public final String subtitle;
        public final Object defaultValue;
        public final Function onClick;

        public SettingItem(int type, String key, String title, String subtitle, Object defaultValue, Function onClick) {
            this.type = type;
            this.key = key;
            this.title = title;
            this.subtitle = subtitle;
            this.defaultValue = defaultValue;
            this.onClick = onClick;
        }
    }

    public static class CommandEntry {
        public final String name;
        public final String description;
        public final Function handler;

        public CommandEntry(String name, String description, Function handler) {
            this.name = name;
            this.description = description;
            this.handler = handler;
        }
    }

    public static class MenuActionEntry {
        public final String id;
        public final String title;
        public final Function handler;
        public int assignedOptionId;

        public MenuActionEntry(String id, String title, Function handler) {
            this.id = id;
            this.title = title;
            this.handler = handler;
        }
    }

    public String id;
    public String name;
    public String version = "1.0.0";
    public String author = "Community";
    public String description = "";
    public List<String> permissions = new ArrayList<>();
    public String filePath;
    public String sourceCode;
    public boolean enabled = true;
    public String lastError;

    public Scriptable scope;
    public final List<SettingItem> settings = new ArrayList<>();
    public final Map<String, CommandEntry> commands = new LinkedHashMap<>();
    public final List<Function> onSendHooks = new ArrayList<>();
    public final List<MenuActionEntry> menuActions = new ArrayList<>();

    public void resetRuntimeState() {
        scope = null;
        settings.clear();
        commands.clear();
        onSendHooks.clear();
        menuActions.clear();
    }

    public boolean hasPermission(String permission) {
        if (permissions == null) {
            return false;
        }
        for (String p : permissions) {
            if (p.equalsIgnoreCase(permission)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getPermissionsReadOnly() {
        return Collections.unmodifiableList(permissions);
    }
}
