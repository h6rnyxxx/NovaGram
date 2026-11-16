package org.telegram.novagram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.novagram.plugins.NovaPlugin;
import org.telegram.novagram.plugins.NovaPluginApi;
import org.telegram.novagram.plugins.NovaPluginEngine;
import org.telegram.novagram.plugins.NovaPluginManager;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class NovaPluginDetailActivity extends UniversalFragment {

    private static final int ID_TOGGLE = 1;
    private static final int ID_UNINSTALL = 2;
    private static final int ID_SETTING_BASE = 200;

    private final NovaPlugin plugin;

    public NovaPluginDetailActivity(NovaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        fragmentView = super.createView(context);
        listView.setSections();
        listView.setSectionsDrawBackground(true);
        return fragmentView;
    }

    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    protected CharSequence getTitle() {
        return plugin != null ? plugin.name : "NovaPlugin";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (plugin == null) return;

        items.add(UItem.asHeader(getString(R.string.NovaPluginOverview)));
        items.add(UItem.asCheck(ID_TOGGLE, getString(R.string.NovaPluginEnabledSwitch)).setChecked(plugin.enabled));
        items.add(UItem.asButton(0, getString(R.string.NovaPluginVersionLabel), plugin.version));
        items.add(UItem.asButton(0, getString(R.string.NovaPluginAuthorLabel), plugin.author));
        items.add(UItem.asButton(0, getString(R.string.NovaPluginPermissionsLabel), TextUtils.join(", ", plugin.getPermissionsReadOnly())));

        StringBuilder summary = new StringBuilder();
        if (!TextUtils.isEmpty(plugin.description)) {
            summary.append(plugin.description);
        }
        if (!TextUtils.isEmpty(plugin.lastError)) {
            if (summary.length() > 0) summary.append("\n\n");
            summary.append("Error: ").append(plugin.lastError);
        }
        items.add(UItem.asShadow(summary.length() > 0 ? summary.toString() : null));

        if (!plugin.settings.isEmpty()) {
            for (int i = 0; i < plugin.settings.size(); i++) {
                NovaPlugin.SettingItem s = plugin.settings.get(i);
                int itemId = ID_SETTING_BASE + i;
                NovaPluginApi.StorageBridge storage = new NovaPluginApi.StorageBridge(plugin.id);
                if (s.type == NovaPlugin.SettingItem.TYPE_HEADER) {
                    items.add(UItem.asHeader(s.title));
                } else if (s.type == NovaPlugin.SettingItem.TYPE_SWITCH) {
                    boolean def = s.defaultValue instanceof Boolean && (Boolean) s.defaultValue;
                    boolean checked = storage.getBoolean(s.key, def);
                    items.add(UItem.asCheck(itemId, s.title).setChecked(checked));
                } else if (s.type == NovaPlugin.SettingItem.TYPE_TEXT) {
                    String def = s.defaultValue != null ? String.valueOf(s.defaultValue) : "";
                    String val = storage.getString(s.key, def);
                    items.add(UItem.asButton(itemId, s.title, val));
                } else if (s.type == NovaPlugin.SettingItem.TYPE_BUTTON) {
                    items.add(UItem.asButton(itemId, s.title).accent());
                }
            }
            items.add(UItem.asShadow(null));
        }

        if (!plugin.commands.isEmpty()) {
            items.add(UItem.asHeader(getString(R.string.NovaPluginCommandsHeader)));
            for (NovaPlugin.CommandEntry cmd : plugin.commands.values()) {
                items.add(UItem.asButton(0, cmd.name, cmd.description));
            }
            items.add(UItem.asShadow(null));
        }

        items.add(UItem.asButton(ID_UNINSTALL, R.drawable.msg_delete, getString(R.string.NovaPluginUninstall)).red());
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (plugin == null) return;
        if (item.id == ID_TOGGLE) {
            NovaPluginManager.getInstance().setPluginEnabled(plugin, !plugin.enabled);
            listView.adapter.update(true);
            return;
        }
        if (item.id == ID_UNINSTALL) {
            new AlertDialog.Builder(getContext())
                    .setTitle(plugin.name)
                    .setMessage(getString(R.string.NovaPluginUninstallConfirm))
                    .setPositiveButton(getString(R.string.Delete), (d, w) -> {
                        NovaPluginManager.getInstance().uninstallPlugin(plugin);
                        finishFragment();
                    })
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .show();
            return;
        }

        int settingIdx = item.id - ID_SETTING_BASE;
        if (settingIdx >= 0 && settingIdx < plugin.settings.size()) {
            NovaPlugin.SettingItem s = plugin.settings.get(settingIdx);
            NovaPluginApi.StorageBridge storage = new NovaPluginApi.StorageBridge(plugin.id);
            if (s.type == NovaPlugin.SettingItem.TYPE_SWITCH) {
                boolean def = s.defaultValue instanceof Boolean && (Boolean) s.defaultValue;
                storage.set(s.key, !storage.getBoolean(s.key, def));
                listView.adapter.update(true);
            } else if (s.type == NovaPlugin.SettingItem.TYPE_TEXT) {
                showTextEditor(s, storage);
            } else if (s.type == NovaPlugin.SettingItem.TYPE_BUTTON && s.onClick != null) {
                NovaPluginEngine.invokeFunction(plugin, s.onClick);
                if (!TextUtils.isEmpty(plugin.lastError)) {
                    BulletinFactory.of(this).createErrorBulletin(plugin.lastError).show();
                }
            }
        }
    }

    private void showTextEditor(NovaPlugin.SettingItem setting, NovaPluginApi.StorageBridge storage) {
        Context ctx = getContext();
        if (ctx == null) return;
        String def = setting.defaultValue != null ? String.valueOf(setting.defaultValue) : "";
        String current = storage.getString(setting.key, def);

        FrameLayout container = new FrameLayout(ctx);
        EditText editText = new EditText(ctx);
        editText.setText(current);
        editText.setSelection(current.length());
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        container.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 24, 8, 24, 8));

        new AlertDialog.Builder(ctx)
                .setTitle(setting.title)
                .setView(container)
                .setPositiveButton(getString(R.string.Save), (d, w) -> {
                    storage.set(setting.key, editText.getText().toString());
                    listView.adapter.update(true);
                })
                .setNegativeButton(getString(R.string.Cancel), null)
                .show();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
