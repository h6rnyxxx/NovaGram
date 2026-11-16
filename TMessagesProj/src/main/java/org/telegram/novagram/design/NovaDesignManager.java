package org.telegram.novagram.design;

import android.os.Build;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.novagram.NovaConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.BlurSettingsBottomSheet;
import org.telegram.ui.LaunchActivity;
import org.telegram.utils.glass.GlassEngine;

public class NovaDesignManager {

    public static void syncOnStartup() {
        applyDesignModeInternal(NovaConfig.getDesignMode(), false);
    }

    public static void setDesignMode(int mode) {
        NovaConfig.setDesignMode(mode);
        applyDesignModeInternal(mode, true);
    }

    public static void refreshTheme() {
        AndroidUtilities.runOnUIThread(() -> {
            Theme.refreshThemeColors(true, true);
            NovaFontManager.applyToThemePaints();
            Theme.applyCommonTheme();
            Theme.applyDialogsTheme();
            Theme.applyChatTheme(false, true);
            if (LaunchActivity.instance != null) {
                if (LaunchActivity.instance.actionBarLayout != null) {
                    LaunchActivity.instance.actionBarLayout.rebuildAllFragmentViews(true, true);
                }
                if (LaunchActivity.instance.rightActionBarLayout != null) {
                    LaunchActivity.instance.rightActionBarLayout.rebuildAllFragmentViews(true, true);
                }
            }
        });
    }

    private static void applyDesignModeInternal(int mode, boolean refreshUi) {
        boolean wantLiquidGlass = (mode == NovaConfig.DESIGN_LIQUID_GLASS || mode == NovaConfig.DESIGN_HYBRID)
                && Build.VERSION.SDK_INT >= 33;
        boolean wantBlur = mode == NovaConfig.DESIGN_LIQUID_GLASS || mode == NovaConfig.DESIGN_HYBRID;

        int flags = LiteMode.getValue(true);
        if (wantBlur) {
            flags |= LiteMode.FLAG_CHAT_BLUR;
            BlurSettingsBottomSheet.blurRadius = 1.0f;
            BlurSettingsBottomSheet.blurAlpha = NovaConfig.getLiquidGlassAlpha() / 100f;
        } else {
            flags &= ~LiteMode.FLAG_LIQUID_GLASS;
        }

        if (wantLiquidGlass) {
            flags |= LiteMode.FLAG_LIQUID_GLASS;
        } else {
            flags &= ~LiteMode.FLAG_LIQUID_GLASS;
        }

        LiteMode.setAllFlags(flags);
        GlassEngine.getCurrentGlassSettings();

        if (refreshUi) {
            refreshTheme();
        }
    }

    public static String getDesignModeTitle(int mode) {
        switch (mode) {
            case NovaConfig.DESIGN_MATERIAL_YOU:
                return "Material You";
            case NovaConfig.DESIGN_LIQUID_GLASS:
                return "Liquid Glass";
            case NovaConfig.DESIGN_HYBRID:
                return "Material You + Liquid Glass";
            case NovaConfig.DESIGN_CLASSIC:
            default:
                return "Telegram Classic";
        }
    }
}
