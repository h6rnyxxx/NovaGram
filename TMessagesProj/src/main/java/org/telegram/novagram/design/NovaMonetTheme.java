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
        public final int accentPrimaryContainer;
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
                int accentPrimaryContainer,
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
            this.accentPrimaryContainer = accentPrimaryContainer;
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
        int preset = NovaConfig.getMonetPalette();
        if (preset == NovaConfig.PALETTE_LAVENDER) {
            return createLavenderPalette(isDark);
        } else if (preset == NovaConfig.PALETTE_EMERALD) {
            return createEmeraldPalette(isDark);
        } else if (preset == NovaConfig.PALETTE_SUNSET) {
            return createSunsetPalette(isDark);
        } else if (preset == NovaConfig.PALETTE_OCEAN) {
            return createOceanPalette(isDark);
        }

        Context context = ApplicationLoader.applicationContext;
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) {
                int accentPrimary = getSystemColor(context, android.R.color.system_accent1_200, 0xFFB0C4FF);
                int accentPrimaryContainer = getSystemColor(context, android.R.color.system_accent1_700, 0xFF2D3C63);
                int accentPrimaryDark = getSystemColor(context, android.R.color.system_accent1_800, 0xFF1F2B4B);
                int accentSecondary = getSystemColor(context, android.R.color.system_accent2_200, 0xFFBDC5DC);
                int accentTertiary = getSystemColor(context, android.R.color.system_accent3_200, 0xFFDFBCE0);
                int surfaceBase = getSystemColor(context, android.R.color.system_neutral1_900, 0xFF13151B);
                int surfaceContainer = getSystemColor(context, android.R.color.system_neutral1_800, 0xFF1E212B);
                int surfaceContainerHigh = getSystemColor(context, android.R.color.system_neutral2_800, 0xFF282C38);
                int onSurface = getSystemColor(context, android.R.color.system_neutral1_50, 0xFFF2F3F9);
                int onSurfaceMuted = getSystemColor(context, android.R.color.system_neutral2_200, 0xFFAAB2C7);
                int onAccent = getSystemColor(context, android.R.color.system_accent1_900, 0xFF142142);
                int divider = ColorUtils.setAlphaComponent(getSystemColor(context, android.R.color.system_neutral2_600, 0xFF3E4456), 95);
                return new Palette(accentPrimary, accentPrimaryContainer, accentPrimaryDark, accentSecondary, accentTertiary,
                        surfaceBase, surfaceContainer, surfaceContainerHigh, onSurface, onSurfaceMuted, onAccent, divider);
            } else {
                int accentPrimary = getSystemColor(context, android.R.color.system_accent1_600, 0xFF3F5AA9);
                int accentPrimaryContainer = getSystemColor(context, android.R.color.system_accent1_100, 0xFFD9E2FF);
                int accentPrimaryDark = getSystemColor(context, android.R.color.system_accent1_700, 0xFF2B428A);
                int accentSecondary = getSystemColor(context, android.R.color.system_accent2_600, 0xFF565E71);
                int accentTertiary = getSystemColor(context, android.R.color.system_accent3_600, 0xFF725573);
                int surfaceBase = getSystemColor(context, android.R.color.system_neutral1_10, 0xFFFAF9FF);
                int surfaceContainer = getSystemColor(context, android.R.color.system_neutral1_50, 0xFFEFF1F9);
                int surfaceContainerHigh = getSystemColor(context, android.R.color.system_neutral2_100, 0xFFE1E5F2);
                int onSurface = getSystemColor(context, android.R.color.system_neutral1_900, 0xFF191B23);
                int onSurfaceMuted = getSystemColor(context, android.R.color.system_neutral2_500, 0xFF636A7E);
                int onAccent = 0xFFFFFFFF;
                int divider = ColorUtils.setAlphaComponent(getSystemColor(context, android.R.color.system_neutral2_300, 0xFFC2C7D6), 115);
                return new Palette(accentPrimary, accentPrimaryContainer, accentPrimaryDark, accentSecondary, accentTertiary,
                        surfaceBase, surfaceContainer, surfaceContainerHigh, onSurface, onSurfaceMuted, onAccent, divider);
            }
        }
        return createLavenderPalette(isDark);
    }

    private static Palette createLavenderPalette(boolean isDark) {
        if (isDark) {
            return new Palette(
                    0xFF9EB0E0, 0xFF344166, 0xFF25304D, 0xFFB8C3DE, 0xFFD7BDE2,
                    0xFF14161D, 0xFF1E212B, 0xFF292D3A,
                    0xFFF0F2FA, 0xFFA2ABC2, 0xFF151E34, 0x5E474E63
            );
        }
        return new Palette(
                0xFF475D92, 0xFFDCE3F9, 0xFF314575, 0xFF5A647E, 0xFF745577,
                0xFFF9FAFF, 0xFFEEF1FA, 0xFFE1E6F3,
                0xFF181C26, 0xFF61687D, 0xFFFFFFFF, 0x6EC4CADB
        );
    }

    private static Palette createEmeraldPalette(boolean isDark) {
        if (isDark) {
            return new Palette(
                    0xFF7EDBB0, 0xFF1B4F3A, 0xFF133B2A, 0xFFA9CBB9, 0xFF9ED0E8,
                    0xFF0F1613, 0xFF18221E, 0xFF22302A,
                    0xFFEAF5EE, 0xFF9AB4A6, 0xFF092A1C, 0x5E385245
            );
        }
        return new Palette(
                0xFF1B6D4E, 0xFFC8F3DC, 0xFF0E5339, 0xFF486557, 0xFF32657D,
                0xFFF6FBF8, 0xFFE8F2EC, 0xFFD8E8DF,
                0xFF131D18, 0xFF566B60, 0xFFFFFFFF, 0x6EB7CCC0
        );
    }

    private static Palette createSunsetPalette(boolean isDark) {
        if (isDark) {
            return new Palette(
                    0xFFFFB59D, 0xFF673526, 0xFF4C251A, 0xFFE4BDB1, 0xFFD9C18D,
                    0xFF1A1312, 0xFF261D1B, 0xFF332624,
                    0xFFFAEEEB, 0xFFC7ADA7, 0xFF41160A, 0x5E5B423D
            );
        }
        return new Palette(
                0xFFB34427, 0xFFFFDBD1, 0xFF8E2F15, 0xFF77574E, 0xFF6C5D2F,
                0xFFFFF8F6, 0xFFFCECE7, 0xFFF3DDD6,
                0xFF231917, 0xFF785F59, 0xFFFFFFFF, 0x6EDDC2BA
        );
    }

    private static Palette createOceanPalette(boolean isDark) {
        if (isDark) {
            return new Palette(
                    0xFF7CCDF8, 0xFF0B4A6B, 0xFF063752, 0xFFB0C9DA, 0xFFC2C1EC,
                    0xFF0E141A, 0xFF17202A, 0xFF212D3B,
                    0xFFECF4FA, 0xFF9EB3C4, 0xFF002F47, 0x5E384B5C
            );
        }
        return new Palette(
                0xFF006494, 0xFFCBE6FF, 0xFF004B70, 0xFF4E6170, 0xFF5D5B8A,
                0xFFF6FAFF, 0xFFE8F1FA, 0xFFD8E6F4,
                0xFF141C24, 0xFF576775, 0xFFFFFFFF, 0x6EB8CBE0
        );
    }

    public static void applyToColorTable(SparseIntArray colors, boolean isDark) {
        if (colors == null) {
            return;
        }
        boolean materialYou = NovaConfig.isMaterialYouActive();
        boolean liquidGlass = NovaConfig.isLiquidGlassActive();

        if (!materialYou && !liquidGlass) {
            return;
        }

        if (materialYou) {
            Palette p = extractPalette(isDark);
            int alphaPercent = NovaConfig.getLiquidGlassAlpha();
            int glassBgAlpha = Math.round(255f * (100 - alphaPercent * 0.55f) / 100f);
            int panelBg = liquidGlass ? ColorUtils.setAlphaComponent(p.surfaceContainer, glassBgAlpha) : p.surfaceContainer;
            int actionBarBg = isDark ? p.surfaceBase : p.surfaceBase;

            // ActionBar & Headers
            colors.put(Theme.key_actionBarDefault, actionBarBg);
            colors.put(Theme.key_actionBarDefaultIcon, p.onSurface);
            colors.put(Theme.key_actionBarDefaultTitle, p.onSurface);
            colors.put(Theme.key_actionBarDefaultSubtitle, p.onSurfaceMuted);
            colors.put(Theme.key_actionBarDefaultSelector, ColorUtils.setAlphaComponent(p.accentPrimary, 42));
            colors.put(Theme.key_actionBarDefaultArchived, p.surfaceContainer);
            colors.put(Theme.key_actionBarDefaultArchivedIcon, p.onSurface);
            colors.put(Theme.key_actionBarDefaultArchivedTitle, p.onSurface);
            colors.put(Theme.key_actionBarDefaultSearch, p.onSurface);
            colors.put(Theme.key_actionBarDefaultSearchPlaceholder, p.onSurfaceMuted);
            colors.put(Theme.key_actionBarActionModeDefault, p.surfaceContainerHigh);
            colors.put(Theme.key_actionBarActionModeDefaultTop, p.surfaceContainerHigh);
            colors.put(Theme.key_actionBarActionModeDefaultIcon, p.accentPrimary);
            colors.put(Theme.key_actionBarTabActiveText, p.accentPrimary);
            colors.put(Theme.key_actionBarTabUnactiveText, p.onSurfaceMuted);
            colors.put(Theme.key_actionBarTabLine, p.accentPrimary);
            colors.put(Theme.key_actionBarDefaultSubmenuBackground, panelBg);
            colors.put(Theme.key_actionBarDefaultSubmenuItem, p.onSurface);
            colors.put(Theme.key_actionBarDefaultSubmenuItemIcon, p.accentPrimary);
            colors.put(Theme.key_actionBarDefaultSubmenuSeparator, p.divider);
            colors.put(Theme.key_actionBarBrowser, p.surfaceContainer);

            // Surfaces & Settings Cards
            colors.put(Theme.key_windowBackgroundWhite, p.surfaceBase);
            colors.put(Theme.key_windowBackgroundGray, p.surfaceContainer);
            colors.put(Theme.key_windowBackgroundGrayShadow, p.divider);
            colors.put(Theme.key_graySection, p.surfaceContainer);
            colors.put(Theme.key_graySectionText, p.accentPrimary);
            colors.put(Theme.key_divider, p.divider);

            // Texts & Values & Icons
            colors.put(Theme.key_windowBackgroundWhiteBlackText, p.onSurface);
            colors.put(Theme.key_windowBackgroundWhiteGrayText, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteGrayText2, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteGrayText3, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteGrayText4, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteGrayText6, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteGrayText8, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteHintText, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteGrayIcon, p.onSurfaceMuted);
            colors.put(Theme.key_windowBackgroundWhiteBlueText, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueText2, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueText3, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueText4, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueText6, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueText7, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueButton, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueIcon, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteBlueHeader, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteValueText, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteLinkText, p.accentPrimary);
            colors.put(Theme.key_windowBackgroundWhiteLinkSelection, ColorUtils.setAlphaComponent(p.accentPrimary, 40));
            colors.put(Theme.key_windowBackgroundWhiteInputField, p.divider);
            colors.put(Theme.key_windowBackgroundWhiteInputFieldActivated, p.accentPrimary);

            // Glass / Bottom Navigation Tabs / Top Pills (Material You look)
            int tabBarBg = liquidGlass
                    ? ColorUtils.setAlphaComponent(p.surfaceContainerHigh, isDark ? 190 : 205)
                    : p.surfaceContainer;
            int tabSelectedPill = liquidGlass
                    ? ColorUtils.setAlphaComponent(p.accentPrimary, isDark ? 88 : 65)
                    : p.accentPrimaryContainer;
            colors.put(Theme.key_glass_targetMainTabs, tabBarBg);
            colors.put(Theme.key_glass_targetMainTopPanel, tabBarBg);
            colors.put(Theme.key_glass_tabSelected, tabSelectedPill);
            colors.put(Theme.key_glass_tabSelectedText, isDark ? p.onSurface : p.accentPrimaryDark);
            colors.put(Theme.key_glass_tabUnselected, p.onSurfaceMuted);
            colors.put(Theme.key_glass_defaultIcon, p.onSurface);
            colors.put(Theme.key_glass_defaultText, p.onSurface);

            // Switches, Checkboxes, Radios, Buttons & FAB
            colors.put(Theme.key_switchTrack, isDark ? 0xFF454959 : 0xFFC2C8D6);
            colors.put(Theme.key_switchTrackChecked, ColorUtils.setAlphaComponent(p.accentPrimary, 170));
            colors.put(Theme.key_switch2Track, isDark ? 0xFF454959 : 0xFFC2C8D6);
            colors.put(Theme.key_switch2TrackChecked, p.accentPrimary);
            colors.put(Theme.key_checkboxCheck, p.onAccent);
            colors.put(Theme.key_checkbox, p.accentPrimary);
            colors.put(Theme.key_radioBackground, p.onSurfaceMuted);
            colors.put(Theme.key_radioBackgroundChecked, p.accentPrimary);
            colors.put(Theme.key_chats_actionBackground, p.accentPrimaryContainer);
            colors.put(Theme.key_chats_actionPressedBackground, p.accentPrimary);
            colors.put(Theme.key_chats_actionIcon, isDark ? p.onSurface : p.accentPrimaryDark);
            colors.put(Theme.key_featuredStickers_addButton, p.accentPrimary);
            colors.put(Theme.key_featuredStickers_addButtonPressed, p.accentPrimaryDark);
            colors.put(Theme.key_featuredStickers_buttonText, p.onAccent);

            // Dialogs & BottomSheets
            colors.put(Theme.key_dialogBackground, panelBg);
            colors.put(Theme.key_dialogBackgroundGray, p.surfaceBase);
            colors.put(Theme.key_dialogTextBlack, p.onSurface);
            colors.put(Theme.key_dialogTextGray, p.onSurfaceMuted);
            colors.put(Theme.key_dialogTextGray2, p.onSurfaceMuted);
            colors.put(Theme.key_dialogTextGray3, p.onSurfaceMuted);
            colors.put(Theme.key_dialogTextBlue, p.accentPrimary);
            colors.put(Theme.key_dialogTextBlue2, p.accentPrimary);
            colors.put(Theme.key_dialogTextLink, p.accentPrimary);
            colors.put(Theme.key_dialogButton, p.accentPrimary);
            colors.put(Theme.key_dialogButtonSelector, ColorUtils.setAlphaComponent(p.accentPrimary, 36));
            colors.put(Theme.key_dialogIcon, p.accentPrimary);
            colors.put(Theme.key_dialogFloatingButton, p.accentPrimary);
            colors.put(Theme.key_dialogFloatingIcon, p.onAccent);
            colors.put(Theme.key_dialogSearchBackground, p.surfaceContainerHigh);
            colors.put(Theme.key_dialogSearchText, p.onSurface);
            colors.put(Theme.key_dialogSearchHint, p.onSurfaceMuted);
            colors.put(Theme.key_dialogSearchIcon, p.onSurfaceMuted);

            // Chats List & Drawer
            colors.put(Theme.key_chats_name, p.onSurface);
            colors.put(Theme.key_chats_nameArchived, p.onSurface);
            colors.put(Theme.key_chats_message, p.onSurfaceMuted);
            colors.put(Theme.key_chats_messageArchived, p.onSurfaceMuted);
            colors.put(Theme.key_chats_message_threeLines, p.onSurfaceMuted);
            colors.put(Theme.key_chats_nameMessage, p.accentPrimary);
            colors.put(Theme.key_chats_nameMessage_threeLines, p.accentPrimary);
            colors.put(Theme.key_chats_attachMessage, p.accentPrimary);
            colors.put(Theme.key_chats_actionMessage, p.accentPrimary);
            colors.put(Theme.key_chats_date, p.onSurfaceMuted);
            colors.put(Theme.key_chats_date_bold, p.accentPrimary);
            colors.put(Theme.key_chats_unreadCounter, p.accentPrimary);
            colors.put(Theme.key_chats_unreadCounterText, p.onAccent);
            colors.put(Theme.key_chats_unreadCounterMuted, p.surfaceContainerHigh);
            colors.put(Theme.key_chats_pinnedOverlay, ColorUtils.setAlphaComponent(p.accentPrimary, isDark ? 22 : 16));
            colors.put(Theme.key_chats_pinnedIcon, p.accentPrimary);
            colors.put(Theme.key_chats_onlineCircle, p.accentPrimary);
            colors.put(Theme.key_chats_sentCheck, p.accentPrimary);
            colors.put(Theme.key_chats_sentReadCheck, p.accentPrimary);
            colors.put(Theme.key_chats_sentClock, p.onSurfaceMuted);
            colors.put(Theme.key_chats_verifiedBackground, p.accentPrimary);
            colors.put(Theme.key_chats_verifiedCheck, p.onAccent);
            colors.put(Theme.key_chats_muteIcon, p.onSurfaceMuted);
            colors.put(Theme.key_chats_mentionIcon, p.accentPrimary);
            colors.put(Theme.key_chats_tabUnreadActiveBackground, p.accentPrimary);
            colors.put(Theme.key_chats_tabUnreadUnactiveBackground, p.surfaceContainerHigh);
            colors.put(Theme.key_chats_menuBackground, p.surfaceBase);
            colors.put(Theme.key_chats_menuTopBackground, p.surfaceContainer);
            colors.put(Theme.key_chats_menuItemText, p.onSurface);
            colors.put(Theme.key_chats_menuItemIcon, p.accentPrimary);
            colors.put(Theme.key_chats_menuName, p.onSurface);
            colors.put(Theme.key_chats_menuPhone, p.onSurfaceMuted);
            colors.put(Theme.key_listSelector, ColorUtils.setAlphaComponent(p.accentPrimary, 28));

            // Profile
            colors.put(Theme.key_profile_title, p.onSurface);
            colors.put(Theme.key_profile_status, p.onSurfaceMuted);
            colors.put(Theme.key_profile_actionBackground, p.accentPrimaryContainer);
            colors.put(Theme.key_profile_actionPressedBackground, p.accentPrimary);
            colors.put(Theme.key_profile_actionIcon, isDark ? p.onSurface : p.accentPrimaryDark);
            colors.put(Theme.key_profile_tabText, p.onSurfaceMuted);
            colors.put(Theme.key_profile_tabSelectedText, p.accentPrimary);
            colors.put(Theme.key_profile_tabSelectedLine, p.accentPrimary);
            colors.put(Theme.key_profile_verifiedBackground, p.accentPrimary);
            colors.put(Theme.key_profile_verifiedCheck, p.onAccent);

            // Chat Panel
            colors.put(Theme.key_chat_messagePanelBackground, panelBg);
            colors.put(Theme.key_chat_messagePanelIcons, p.onSurfaceMuted);
            colors.put(Theme.key_chat_messagePanelSend, p.accentPrimary);
            colors.put(Theme.key_chat_messagePanelText, p.onSurface);
            colors.put(Theme.key_chat_messagePanelHint, p.onSurfaceMuted);

            if (NovaConfig.isMonetTintBubbles()) {
                int inBubble = isDark ? p.surfaceContainer : p.surfaceBase;
                int outBubble = isDark
                        ? p.accentPrimaryContainer
                        : ColorUtils.blendARGB(p.accentPrimary, 0xFFFFFFFF, 0.78f);
                int outBubbleSelected = ColorUtils.blendARGB(outBubble, p.accentPrimary, 0.22f);
                colors.put(Theme.key_chat_inBubble, inBubble);
                colors.put(Theme.key_chat_inBubbleSelected, ColorUtils.blendARGB(inBubble, p.accentPrimary, 0.14f));
                colors.put(Theme.key_chat_outBubble, outBubble);
                colors.put(Theme.key_chat_outBubbleSelected, outBubbleSelected);
                colors.put(Theme.key_chat_outTimeText, isDark ? p.onSurfaceMuted : p.accentPrimaryDark);
                colors.put(Theme.key_chat_outSentCheck, p.accentPrimary);
                colors.put(Theme.key_chat_outSentCheckSelected, p.accentPrimary);
                colors.put(Theme.key_chat_outSentClock, p.onSurfaceMuted);
                colors.put(Theme.key_chat_outReplyLine, p.accentPrimary);
                colors.put(Theme.key_chat_outReplyNameText, p.accentPrimary);
                colors.put(Theme.key_chat_inReplyLine, p.accentSecondary);
                colors.put(Theme.key_chat_inReplyNameText, p.accentPrimary);
            }
            return;
        }

        // Pure Liquid Glass mode (without Material You overriding the theme palette):
        // Apply crisp crystal translucent tints to glass targets so Liquid Glass refraction pops dramatically.
        int glassPanel = isDark ? 0xB8101828 : 0xC8EAF4FF;
        int glassSelected = isDark ? 0x6638BDF8 : 0x440284C7;
        colors.put(Theme.key_glass_targetMainTabs, glassPanel);
        colors.put(Theme.key_glass_targetMainTopPanel, glassPanel);
        colors.put(Theme.key_glass_tabSelected, glassSelected);
        colors.put(Theme.key_chat_messagePanelBackground, isDark ? 0xCC121B2D : 0xD8F2F8FF);
        colors.put(Theme.key_dialogBackground, isDark ? 0xE6141C2B : 0xEEFFFFFF);
        colors.put(Theme.key_actionBarDefaultSubmenuBackground, isDark ? 0xE0162033 : 0xEBFFFFFF);
    }
}
