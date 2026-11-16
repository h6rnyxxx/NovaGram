package org.telegram.novagram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.novagram.NovaConfig;
import org.telegram.novagram.plugins.NovaPlugin;
import org.telegram.novagram.plugins.NovaPluginManager;
import org.telegram.novagram.updater.NovaCloudCatalog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.List;

public class NovaPluginsActivity extends UniversalFragment {

    private static final int REQ_PICK_PLUGIN = 9411;

    private static final int ID_IMPORT = 1;
    private static final int ID_RELOAD = 2;
    private static final int ID_CLOUD_SYNC = 3;
    private static final int ID_PLUGIN_BASE = 100;
    private static final int ID_CLOUD_BASE = 500;

    private List<NovaPlugin> displayedPlugins = new ArrayList<>();
    private List<NovaCloudCatalog.CloudPluginItem> displayedCloud = new ArrayList<>();

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
        return getString(R.string.NovaPluginsHeader);
    }

    @Override
    public void onResume() {
        super.onResume();
        NovaCloudCatalog.getInstance().syncAsync(false, () -> {
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.NovaPluginsActionsHeader)));
        items.add(UItem.asButton(ID_IMPORT, R.drawable.msg_add, getString(R.string.NovaPluginImport)).accent());
        items.add(UItem.asButton(ID_CLOUD_SYNC, R.drawable.msg_download, getString(R.string.NovaCloudSyncPlugins)));
        items.add(UItem.asButton(ID_RELOAD, R.drawable.msg_customize, getString(R.string.NovaPluginReloadAll)));
        items.add(UItem.asShadow(NovaConfig.isPluginsEnabled()
                ? getString(R.string.NovaPluginsImportHint)
                : getString(R.string.NovaPluginsDisabledHint)));

        displayedPlugins = NovaPluginManager.getInstance().getPlugins();
        items.add(UItem.asHeader(getString(R.string.NovaPluginsInstalledHeader)));
        if (displayedPlugins.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.NovaPluginsEmpty)));
        } else {
            for (int i = 0; i < displayedPlugins.size(); i++) {
                NovaPlugin p = displayedPlugins.get(i);
                String status;
                if (!p.enabled) {
                    status = getString(R.string.NovaPluginStatusDisabled);
                } else if (!TextUtils.isEmpty(p.lastError)) {
                    status = getString(R.string.NovaPluginStatusError);
                } else {
                    status = "v" + p.version + " · " + p.author;
                }
                items.add(UItem.asButton(ID_PLUGIN_BASE + i, R.drawable.msg_bot, p.name, status));
            }
            items.add(UItem.asShadow(getString(R.string.NovaPluginsTapHint)));
        }

        displayedCloud = NovaCloudCatalog.getInstance().getCloudPlugins();
        items.add(UItem.asHeader(getString(R.string.NovaCloudPluginsHeader)));
        if (displayedCloud.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.NovaCloudPluginsEmpty)));
        } else {
            for (int i = 0; i < displayedCloud.size(); i++) {
                NovaCloudCatalog.CloudPluginItem cp = displayedCloud.get(i);
                boolean installed = isPluginInstalled(cp.id);
                String badge = installed
                        ? getString(R.string.NovaCloudPluginInstalledBadge)
                        : ("v" + cp.version);
                items.add(UItem.asButton(ID_CLOUD_BASE + i, installed ? R.drawable.checkbig : R.drawable.msg_download, cp.name, badge));
            }
            items.add(UItem.asShadow(getString(R.string.NovaCloudPluginsHint)));
        }
    }

    private boolean isPluginInstalled(String id) {
        for (NovaPlugin p : displayedPlugins) {
            if (TextUtils.equals(p.id, id)) return true;
        }
        return false;
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_IMPORT) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQ_PICK_PLUGIN);
            return;
        }
        if (item.id == ID_CLOUD_SYNC) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, getString(R.string.NovaUpdateChecking)).show();
            NovaCloudCatalog.getInstance().syncAsync(true, () -> {
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            });
            return;
        }
        if (item.id == ID_RELOAD) {
            NovaPluginManager.getInstance().reloadAll();
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, getString(R.string.NovaPluginsReloaded)).show();
            return;
        }
        if (item.id >= ID_CLOUD_BASE) {
            int cIdx = item.id - ID_CLOUD_BASE;
            if (cIdx >= 0 && cIdx < displayedCloud.size()) {
                NovaCloudCatalog.CloudPluginItem cp = displayedCloud.get(cIdx);
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.formatString(R.string.NovaCloudInstalling, cp.name)).show();
                NovaCloudCatalog.getInstance().installCloudPlugin(cp, (plugin, err) -> {
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                    if (plugin != null) {
                        BulletinFactory.of(this).createSimpleBulletin(
                                R.raw.chats_infotip,
                                LocaleController.formatString(R.string.NovaPluginInstalled, plugin.name)
                        ).show();
                    } else {
                        BulletinFactory.of(this).createErrorBulletin(err != null ? err : getString(R.string.NovaPluginInstallFailed)).show();
                    }
                });
            }
            return;
        }
        int idx = item.id - ID_PLUGIN_BASE;
        if (idx >= 0 && idx < displayedPlugins.size()) {
            presentFragment(new NovaPluginDetailActivity(displayedPlugins.get(idx)));
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        int idx = item.id - ID_PLUGIN_BASE;
        if (idx >= 0 && idx < displayedPlugins.size()) {
            NovaPlugin p = displayedPlugins.get(idx);
            NovaPluginManager.getInstance().setPluginEnabled(p, !p.enabled);
            listView.adapter.update(true);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_PLUGIN && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            NovaPlugin installed = NovaPluginManager.getInstance().installPluginFromUri(getParentActivity(), data.getData());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (installed != null) {
                BulletinFactory.of(this).createSimpleBulletin(
                        R.raw.chats_infotip,
                        LocaleController.formatString(R.string.NovaPluginInstalled, installed.name)
                ).show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(getString(R.string.NovaPluginInstallFailed)).show();
            }
        }
    }
}
