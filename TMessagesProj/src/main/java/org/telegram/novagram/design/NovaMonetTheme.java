package org.telegram.novagram.design;

import android.content.Context;
import android.os.Build;
import android.util.SparseIntArray;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.novagram.NovaConfig;
import org.telegram.ui.ActionBar.Theme;

public class NovaMonetTheme {

    public static class Palette {
        public final int accentPrimary;
        public final int accentPrimaryDark;
        public final int accentSecondary;
        public final int accentTertiary;
        public final int surfaceBase;
        public final int surfaceContainer;
        public final int surfaceContainerHigh;
        public final int onSurface;
        public final int onSurfaceMuted;
        public final int onAccent;
        public final int divider;

        public Palette(
                int accentPrimary,
                int accentPrimaryDark,
                int accentSecondary,
                int accentTertiary,
                int surfaceBase,
                int surfaceContainer,
                int surfaceContainerHigh,
                int onSurface,
                int onSurfaceMuted,
                int onAccent,
                int divider
        ) {
            this.accentPrimary = accentPrimary;
            this.accentPrimaryDark = accentPrimaryDark;
            this.accentSecondary = accentSecondary;
            this.accentTertiary = accentTertiary;
            this.surfaceBase = surfaceBase;
            this.surfaceContainer = surfaceContainer;
            this.surfaceContainerHigh = surfaceContainerHigh;
            this.onSurface = onSurface;
            this.onSurfaceMuted = onSurfaceMuted;
            this.onAccent = onAccent;
            this.divider = divider;
        }
    }

    private static int getSystemColor(Context context, int resId, int fallback) {
        try {
            return ContextCompat.getColor(context, resId) | 0xFF000000;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static Palette extractPalette(boolean isDark) {
        Context context = ApplicationLoader.applicationContext;
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) {
                int accentPrimary = getSystemColor(context, android.R.color.system_accent1_200, 0xFF9ECAFF);
                int accentPrimaryDark = getSystemColor(context, android.R.color.system_accent1_700, 0xFF00497D);
                int accentSecondary = getSystemColor(context, android.R.color.system_accent2_200, 0xFFBBC7DB);
                int accentTertiary = getSystemColor(context, android.R.color.system_accent3_200, 0xFFD6BEE4);
                int surfaceBase = getSystemColor(context, android.R.color.system_neutral1_900, 0xFF121418);
                int surfaceContainer = getSystemColor(context, android.R.color.system_neutral1_800, 0xFF1D2026);
                int surfaceContainerHigh = getSystemColor(context, android.R.color.system_neutral2_800, 0xFF252932);
                int onSurface = getSystemColor(context, android.R.color.system_neutral1_50, 0xFFF0F2F7);
                int onSurfaceMuted = getSystemColor(context, android.R.color.system_neutral2_200, 0xFFB0B8C5);
                int onAccent = getSystemColor(context, android.R.color.system_accent1_900, 0xFF003258);
                int divider = ColorUtils.setAlphaComponent(getSystemColor(context, android.R.color.system_neutral2_600, 0xFF424750), 90);
                return new Palette(accentPrimary, accentPrimaryDark, accentSecondary, accentTertiary,
                        surfaceBase, surfaceContainer, surfaceContainerHigh, onSurface, onSurfaceMuted, onAccent, divider);
            } else {
                int accentPrimary = getSystemColor(context, android.R.color.system_accent1_600, 0xFF1967D2);
                int accentPrimaryDark = getSystemColor(context, android.R.color.system_accent1_700, 0xFF0B57D0);
                int accentSecondary = getSystemColor(context, android.R.color.system_accent2_600, 0xFF535F70);
                int accentTertiary = getSystemColor(context, android.R.color.system_accent3_600, 0xFF6B5778);
                int surfaceBase = getSystemColor(context, android.R.color.system_neutral1_10, 0xFFFDFBFF);
                int surfaceContainer = getSystemColor(context, android.R.color.system_neutral1_50, 0xFFF1F4F9);
                int surfaceContainerHigh = getSystemColor(context, android.R.color.system_neutral2_100, 0xFFE3E8F0);
                int onSurface = getSystemColor(context, android.R.color.system_neutral1_900, 0xFF191C20);
                int onSurfaceMuted = getSystemColor(context, android.R.color.system_neutral2_500, 0xFF6E7582);
                int onAccent = 0xFFFFFFFF;
                int divider = ColorUtils.setAlphaComponent(getSystemColor(context, android.R.color.system_neutral2_300, 0xFFC2C7CF), 110);
                return new Palette(accentPrimary, accentPrimaryDark, accentSecondary, accentTertiary,
                        surfaceBase, surfaceContainer, surfaceContainerHigh, onSurface, onSurfaceMuted, onAccent, divider);
            }
        }

        // Fallback Material You palette for Android < 12
        if (isDark) {
            return new Palette(
                    0xFF9ECAFF, 0xFF00497D, 0xFFBBC7DB, 0xFFD6BEE4,
                    0xFF121418, 0xFF1D2026, 0xFF252932,
                    0xFFF0F2F7, 0xFFB0B8C5, 0xFF003258, 0x5A424750
            );
        }
        return new Palette(
                0xFF1967D2, 0xFF0B57D0, 0xFF535F70, 0xFF6B5778,
                0xFFFDFBFF, 0xFFF1F4F9, 0xFFE3E8F0,
                0xFF191C20, 0xFF6E7582, 0xFFFFFFFF, 0x6EC2C7CF
        );
    }

    public static void applyToColorTable(SparseIntArray colors, boolean isDark) {
        if (colors == null || !NovaConfig.isMaterialYouActive()) {
            return;
        }

        Palette p = extractPalette(isDark);
        boolean liquidGlass = NovaConfig.isLiquidGlassActive();

        // ActionBar & Header
        int actionBarBg = isDark ? p.surfaceContainer : p.surfaceBase;
        int actionBarText = p.onSurface;
        colors.put(Theme.key_actionBarDefault, actionBarBg);
        colors.put(Theme.key_actionBarDefaultIcon, actionBarText);
        colors.put(Theme.key_actionBarDefaultTitle, actionBarText);
        colors.put(Theme.key_actionBarDefaultSubtitle, p.onSurfaceMuted);
        colors.put(Theme.key_actionBarDefaultSelector, ColorUtils.setAlphaComponent(p.accentPrimary, 36));
        colors.put(Theme.key_actionBarDefaultArchived, p.surfaceContainer);
        colors.put(Theme.key_actionBarActionModeDefault, p.surfaceContainerHigh);
        colors.put(Theme.key_actionBarActionModeDefaultIcon, p.accentPrimary);

        // Main window surfaces
        colors.put(Theme.key_windowBackgroundWhite, p.surfaceBase);
        colors.put(Theme.key_windowBackgroundGray, p.surfaceContainer);
        colors.put(Theme.key_windowBackgroundGrayShadow, p.divider);
        colors.put(Theme.key_divider, p.divider);

        // Typography & Accent controls
        colors.put(Theme.key_windowBackgroundWhiteBlackText, p.onSurface);
        colors.put(Theme.key_windowBackgroundWhiteGrayText, p.onSurfaceMuted);
        colors.put(Theme.key_windowBackgroundWhiteGrayText2, p.onSurfaceMuted);
        colors.put(Theme.key_windowBackgroundWhiteGrayText3, p.onSurfaceMuted);
        colors.put(Theme.key_windowBackgroundWhiteBlueText, p.accentPrimary);
        colors.put(Theme.key_windowBackgroundWhiteBlueText2, p.accentPrimary);
        colors.put(Theme.key_windowBackgroundWhiteBlueText4, p.accentPrimary);
        colors.put(Theme.key_windowBackgroundWhiteBlueHeader, p.accentPrimary);

        // Switches, Checkboxes, Floating Action Buttons
        colors.put(Theme.key_switchTrack, isDark ? 0xFF444954 : 0xFFC5CCD6);
        colors.put(Theme.key_switchTrackChecked, ColorUtils.setAlphaComponent(p.accentPrimary, 175));
        colors.put(Theme.key_switch2Track, isDark ? 0xFF444954 : 0xFFC5CCD6);
        colors.put(Theme.key_switch2TrackChecked, p.accentPrimary);
        colors.put(Theme.key_checkboxCheck, p.onAccent);
        colors.put(Theme.key_checkbox, p.accentPrimary);
        colors.put(Theme.key_radioBackground, p.onSurfaceMuted);
        colors.put(Theme.key_radioBackgroundChecked, p.accentPrimary);
        colors.put(Theme.key_chats_actionBackground, p.accentPrimary);
        colors.put(Theme.key_chats_actionPressedBackground, p.accentPrimaryDark);
        colors.put(Theme.key_chats_actionIcon, p.onAccent);
        colors.put(Theme.key_featuredStickers_addButton, p.accentPrimary);
        colors.put(Theme.key_featuredStickers_addButtonPressed, p.accentPrimaryDark);
        colors.put(Theme.key_featuredStickers_buttonText, p.onAccent);

        // Dialogs & BottomSheets (subtle translucency when Liquid Glass is paired)
        int dialogBg = liquidGlass
                ? ColorUtils.setAlphaComponent(p.surfaceContainer, isDark ? 236 : 242)
                : p.surfaceContainer;
        colors.put(Theme.key_dialogBackground, dialogBg);
        colors.put(Theme.key_dialogBackgroundGray, p.surfaceBase);
        colors.put(Theme.key_dialogTextBlack, p.onSurface);
        colors.put(Theme.key_dialogTextGray, p.onSurfaceMuted);
        colors.put(Theme.key_dialogTextGray2, p.onSurfaceMuted);
        colors.put(Theme.key_dialogTextGray3, p.onSurfaceMuted);
        colors.put(Theme.key_dialogTextBlue, p.accentPrimary);
        colors.put(Theme.key_dialogTextBlue2, p.accentPrimary);
        colors.put(Theme.key_dialogButton, p.accentPrimary);
        colors.put(Theme.key_dialogButtonSelector, ColorUtils.setAlphaComponent(p.accentPrimary, 32));

        // Chat list & Badges
        colors.put(Theme.key_chats_unreadCounter, p.accentPrimary);
        colors.put(Theme.key_chats_unreadCounterText, p.onAccent);
        colors.put(Theme.key_chats_unreadCounterMuted, p.surfaceContainerHigh);
        colors.put(Theme.key_chats_pinnedOverlay, ColorUtils.setAlphaComponent(p.accentSecondary, isDark ? 24 : 18));
        colors.put(Theme.key_listSelector, ColorUtils.setAlphaComponent(p.accentPrimary, 26));
        colors.put(Theme.key_chat_messagePanelBackground, p.surfaceContainer);
        colors.put(Theme.key_chat_messagePanelIcons, p.onSurfaceMuted);
        colors.put(Theme.key_chat_messagePanelSend, p.accentPrimary);
        colors.put(Theme.key_chat_messagePanelText, p.onSurface);
        colors.put(Theme.key_chat_messagePanelHint, p.onSurfaceMuted);

        if (NovaConfig.isMonetTintBubbles()) {
            int inBubble = isDark ? p.surfaceContainer : p.surfaceBase;
            int outBubble = isDark
                    ? ColorUtils.blendARGB(p.accentPrimaryDark, p.surfaceContainerHigh, 0.25f)
                    : ColorUtils.blendARGB(p.accentPrimary, 0xFFFFFFFF, 0.78f);
            int outBubbleSelected = ColorUtils.blendARGB(outBubble, p.accentPrimary, 0.2f);
            colors.put(Theme.key_chat_inBubble, inBubble);
            colors.put(Theme.key_chat_inBubbleSelected, ColorUtils.blendARGB(inBubble, p.accentPrimary, 0.14f));
            colors.put(Theme.key_chat_outBubble, outBubble);
            colors.put(Theme.key_chat_outBubbleSelected, outBubbleSelected);
            colors.put(Theme.key_chat_outTimeText, isDark ? ColorUtils.setAlphaComponent(p.onSurface, 185) : p.accentPrimaryDark);
            colors.put(Theme.key_chat_outSentCheck, p.accentPrimary);
            colors.put(Theme.key_chat_outSentCheckSelected, p.accentPrimary);
            colors.put(Theme.key_chat_outSentClock, p.onSurfaceMuted);
            colors.put(Theme.key_chat_outReplyLine, p.accentPrimary);
            colors.put(Theme.key_chat_outReplyNameText, p.accentPrimary);
            colors.put(Theme.key_chat_inReplyLine, p.accentSecondary);
            colors.put(Theme.key_chat_inReplyNameText, p.accentPrimary);
        }
    }
}
