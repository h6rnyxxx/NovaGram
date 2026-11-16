package org.telegram.novagram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.novagram.NovaConfig;
import org.telegram.novagram.design.NovaDesignManager;
import org.telegram.novagram.plugins.NovaPluginManager;
import org.telegram.novagram.updater.NovaUpdateController;
import org.telegram.novagram.updater.NovaUpdateDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

public class NovaSettingsActivity extends UniversalFragment {

    private static final int ID_STYLE_CLASSIC = 1;
    private static final int ID_STYLE_MATERIAL_YOU = 2;
    private static final int ID_STYLE_LIQUID_GLASS = 3;
    private static final int ID_STYLE_HYBRID = 4;
    private static final int ID_TINT_BUBBLES = 5;

    private static final int ID_PALETTE_SYSTEM = 30;
    private static final int ID_PALETTE_LAVENDER = 31;
    private static final int ID_PALETTE_EMERALD = 32;
    private static final int ID_PALETTE_SUNSET = 33;
    private static final int ID_PALETTE_OCEAN = 34;

    private static final int ID_FONTS = 40;

    private static final int ID_PLUGINS_MANAGE = 10;
    private static final int ID_PLUGINS_ENABLED = 11;

    private static final int ID_UPDATE_AUTO = 20;
    private static final int ID_UPDATE_CHECK = 21;
    private static final int ID_OPEN_GITHUB = 22;

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
        return getString(R.string.NovaSettingsTitle);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int currentMode = NovaConfig.getDesignMode();

        items.add(UItem.asHeader(getString(R.string.NovaDesignHeader)));
        items.add(UItem.asRadio(ID_STYLE_HYBRID, getString(R.string.NovaDesignHybrid), getString(R.string.NovaDesignHybridSub))
                .setChecked(currentMode == NovaConfig.DESIGN_HYBRID));
        items.add(UItem.asRadio(ID_STYLE_MATERIAL_YOU, getString(R.string.NovaDesignMaterialYou), getString(R.string.NovaDesignMaterialYouSub))
                .setChecked(currentMode == NovaConfig.DESIGN_MATERIAL_YOU));
        items.add(UItem.asRadio(ID_STYLE_LIQUID_GLASS, getString(R.string.NovaDesignLiquidGlass), getString(R.string.NovaDesignLiquidGlassSub))
                .setChecked(currentMode == NovaConfig.DESIGN_LIQUID_GLASS));
        items.add(UItem.asRadio(ID_STYLE_CLASSIC, getString(R.string.NovaDesignClassic), getString(R.string.NovaDesignClassicSub))
                .setChecked(currentMode == NovaConfig.DESIGN_CLASSIC));

        if (NovaConfig.isMaterialYouActive()) {
            items.add(UItem.asCheck(ID_TINT_BUBBLES, getString(R.string.NovaMonetTintBubbles))
                    .setChecked(NovaConfig.isMonetTintBubbles()));
        }

        String designInfo = Build.VERSION.SDK_INT >= 33
                ? getString(R.string.NovaDesignInfoApi33)
                : getString(R.string.NovaDesignInfoLegacy);
        items.add(UItem.asShadow(designInfo));

        if (NovaConfig.isMaterialYouActive()) {
            int palette = NovaConfig.getMonetPalette();
            items.add(UItem.asHeader(getString(R.string.NovaPaletteHeader)));
            items.add(UItem.asRadio(ID_PALETTE_SYSTEM, getString(R.string.NovaPaletteSystem))
                    .setChecked(palette == NovaConfig.PALETTE_SYSTEM));
            items.add(UItem.asRadio(ID_PALETTE_LAVENDER, getString(R.string.NovaPaletteLavender))
                    .setChecked(palette == NovaConfig.PALETTE_LAVENDER));
            items.add(UItem.asRadio(ID_PALETTE_EMERALD, getString(R.string.NovaPaletteEmerald))
                    .setChecked(palette == NovaConfig.PALETTE_EMERALD));
            items.add(UItem.asRadio(ID_PALETTE_SUNSET, getString(R.string.NovaPaletteSunset))
                    .setChecked(palette == NovaConfig.PALETTE_SUNSET));
            items.add(UItem.asRadio(ID_PALETTE_OCEAN, getString(R.string.NovaPaletteOcean))
                    .setChecked(palette == NovaConfig.PALETTE_OCEAN));
            items.add(UItem.asShadow(getString(R.string.NovaPaletteInfo)));
        }

        items.add(UItem.asHeader(getString(R.string.NovaFontsTitle)));
        items.add(UItem.asButton(ID_FONTS, R.drawable.msg_theme, getString(R.string.NovaFontsRow), getCurrentFontLabel()));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(getString(R.string.NovaPluginsHeader)));
        int activeCount = NovaPluginManager.getInstance().getActivePluginsCount();
        int totalCount = NovaPluginManager.getInstance().getPlugins().size();
        items.add(UItem.asButton(ID_PLUGINS_MANAGE, R.drawable.msg_bot, getString(R.string.NovaPluginsManage), activeCount + " / " + totalCount));
        items.add(UItem.asCheck(ID_PLUGINS_ENABLED, getString(R.string.NovaPluginsEnableEngine))
                .setChecked(NovaConfig.isPluginsEnabled()));
        items.add(UItem.asShadow(getString(R.string.NovaPluginsInfo)));

        items.add(UItem.asHeader(getString(R.string.NovaUpdaterHeader)));
        items.add(UItem.asCheck(ID_UPDATE_AUTO, getString(R.string.NovaUpdateAutoCheck))
                .setChecked(NovaConfig.isAutoCheckUpdates()));

        NovaUpdateController updater = NovaUpdateController.getInstance();
        NovaUpdateController.ReleaseInfo pending = updater.getPendingRelease();
        String updateValue;
        if (updater.isChecking()) {
            updateValue = getString(R.string.NovaUpdateChecking);
        } else if (pending != null) {
            updateValue = "v" + pending.version + "!";
        } else {
            updateValue = "v" + BuildConfig.NOVA_VERSION_NAME;
        }
        items.add(UItem.asButton(ID_UPDATE_CHECK, R.drawable.msg_download, getString(R.string.NovaUpdateCheckNow), updateValue));
        items.add(UItem.asButton(ID_OPEN_GITHUB, R.drawable.msg_openin, getString(R.string.NovaOpenGitHub), NovaConfig.GITHUB_REPO));
        items.add(UItem.asShadow(LocaleController.formatString(
                R.string.NovaAboutFooter,
                BuildConfig.NOVA_VERSION_NAME,
                BuildConfig.BUILD_VERSION_STRING,
                Build.VERSION.RELEASE
        )));
    }

    private String getCurrentFontLabel() {
        switch (NovaConfig.getFontPreset()) {
            case NovaConfig.FONT_SANS_MEDIUM:
                return getString(R.string.NovaFontSansMedium);
            case NovaConfig.FONT_CONDENSED:
                return getString(R.string.NovaFontCondensed);
            case NovaConfig.FONT_SERIF:
                return getString(R.string.NovaFontSerif);
            case NovaConfig.FONT_MONO:
                return getString(R.string.NovaFontMono);
            case NovaConfig.FONT_CUSTOM:
                return !TextUtils.isEmpty(NovaConfig.getCustomFontName())
                        ? NovaConfig.getCustomFontName()
                        : getString(R.string.NovaFontCustomHeader);
            case NovaConfig.FONT_SYSTEM:
            default:
                return getString(R.string.NovaFontSystem);
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_STYLE_CLASSIC:
                NovaDesignManager.setDesignMode(NovaConfig.DESIGN_CLASSIC);
                break;
            case ID_STYLE_MATERIAL_YOU:
                NovaDesignManager.setDesignMode(NovaConfig.DESIGN_MATERIAL_YOU);
                break;
            case ID_STYLE_LIQUID_GLASS:
                NovaDesignManager.setDesignMode(NovaConfig.DESIGN_LIQUID_GLASS);
                break;
            case ID_STYLE_HYBRID:
                NovaDesignManager.setDesignMode(NovaConfig.DESIGN_HYBRID);
                break;
            case ID_TINT_BUBBLES:
                NovaConfig.setMonetTintBubbles(!NovaConfig.isMonetTintBubbles());
                NovaDesignManager.refreshTheme();
                break;
            case ID_PALETTE_SYSTEM:
                NovaConfig.setMonetPalette(NovaConfig.PALETTE_SYSTEM);
                NovaDesignManager.refreshTheme();
                break;
            case ID_PALETTE_LAVENDER:
                NovaConfig.setMonetPalette(NovaConfig.PALETTE_LAVENDER);
                NovaDesignManager.refreshTheme();
                break;
            case ID_PALETTE_EMERALD:
                NovaConfig.setMonetPalette(NovaConfig.PALETTE_EMERALD);
                NovaDesignManager.refreshTheme();
                break;
            case ID_PALETTE_SUNSET:
                NovaConfig.setMonetPalette(NovaConfig.PALETTE_SUNSET);
                NovaDesignManager.refreshTheme();
                break;
            case ID_PALETTE_OCEAN:
                NovaConfig.setMonetPalette(NovaConfig.PALETTE_OCEAN);
                NovaDesignManager.refreshTheme();
                break;
            case ID_FONTS:
                presentFragment(new NovaFontsActivity());
                return;
            case ID_PLUGINS_MANAGE:
                presentFragment(new NovaPluginsActivity());
                return;
            case ID_PLUGINS_ENABLED:
                NovaConfig.setPluginsEnabled(!NovaConfig.isPluginsEnabled());
                NovaPluginManager.getInstance().reloadAll();
                break;
            case ID_UPDATE_AUTO:
                NovaConfig.setAutoCheckUpdates(!NovaConfig.isAutoCheckUpdates());
                break;
            case ID_UPDATE_CHECK:
                checkUpdatesFromUi();
                return;
            case ID_OPEN_GITHUB:
                Browser.openUrl(getContext(), NovaConfig.GITHUB_REPO_URL);
                return;
            default:
                return;
        }
        listView.adapter.update(true);
    }

    private void checkUpdatesFromUi() {
        NovaUpdateController updater = NovaUpdateController.getInstance();
        if (updater.isChecking()) {
            return;
        }
        listView.adapter.update(true);
        updater.checkForUpdates(true, () -> {
            if (getParentActivity() == null) return;
            listView.adapter.update(true);
            NovaUpdateController.ReleaseInfo pending = updater.getPendingRelease();
            if (pending != null) {
                NovaUpdateDialog.show(getParentActivity(), pending);
            } else if (updater.getLastError() != null) {
                BulletinFactory.of(this).createErrorBulletin(updater.getLastError()).show();
            } else {
                BulletinFactory.of(this).createSimpleBulletin(
                        R.raw.chats_infotip,
                        LocaleController.formatString(R.string.NovaUpdateLatestToast, BuildConfig.NOVA_VERSION_NAME)
                ).show();
            }
        });
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
