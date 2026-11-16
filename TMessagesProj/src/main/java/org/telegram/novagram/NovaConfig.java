package org.telegram.novagram;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;

public class NovaConfig {

    public static final int DESIGN_CLASSIC = 0;
    public static final int DESIGN_MATERIAL_YOU = 1;
    public static final int DESIGN_LIQUID_GLASS = 2;
    public static final int DESIGN_HYBRID = 3;

    public static final int PALETTE_SYSTEM = 0;
    public static final int PALETTE_LAVENDER = 1;
    public static final int PALETTE_EMERALD = 2;
    public static final int PALETTE_SUNSET = 3;
    public static final int PALETTE_OCEAN = 4;

    public static final int FONT_SYSTEM = 0;
    public static final int FONT_SANS_MEDIUM = 1;
    public static final int FONT_CONDENSED = 2;
    public static final int FONT_SERIF = 3;
    public static final int FONT_MONO = 4;
    public static final int FONT_CUSTOM = 5;

    public static final String GITHUB_REPO = BuildConfig.NOVA_GITHUB_REPO;
    public static final String GITHUB_REPO_URL = "https://github.com/" + GITHUB_REPO;
    public static final String GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    public static final String CLOUD_MANIFEST_URL = "https://raw.githubusercontent.com/" + GITHUB_REPO + "/master/cloud/manifest.json";

    private static final String PREFS_NAME = "novaconfig";

    private static int designMode = -1;
    private static int monetPalette = PALETTE_SYSTEM;
    private static boolean monetTintBubbles = true;
    private static int liquidGlassAlpha = 72; // 0..100
    private static int fontPreset = FONT_SYSTEM;
    private static String customFontPath = "";
    private static String customFontName = "";
    private static boolean autoCheckUpdates = true;
    private static boolean includePreReleases = false;
    private static boolean pluginsEnabled = true;
    private static boolean seededDefaultPlugins = false;
    private static boolean profileOrbitNfts = true;
    private static boolean profileAura = true;
    private static boolean profileMetaRow = true;
    private static boolean ghostDontRead = false;
    private static boolean ghostDontType = false;
    private static boolean ghostOffline = false;
    private static boolean antiDeleteMessages = true;
    private static boolean showSecondsInTime = false;
    private static boolean showMessageId = false;
    private static boolean confirmCalls = true;
    private static boolean disableProximity = false;
    private static boolean loaded = false;

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void ensureLoaded() {
        if (loaded || ApplicationLoader.applicationContext == null) {
            return;
        }
        SharedPreferences prefs = getPrefs();
        int defaultMode = Build.VERSION.SDK_INT >= 33 ? DESIGN_HYBRID : DESIGN_MATERIAL_YOU;
        designMode = prefs.getInt("design_mode", defaultMode);
        monetPalette = prefs.getInt("monet_palette", PALETTE_SYSTEM);
        monetTintBubbles = prefs.getBoolean("monet_tint_bubbles", true);
        liquidGlassAlpha = prefs.getInt("liquid_glass_alpha", 72);
        fontPreset = prefs.getInt("font_preset", FONT_SYSTEM);
        customFontPath = prefs.getString("custom_font_path", "");
        customFontName = prefs.getString("custom_font_name", "");
        autoCheckUpdates = prefs.getBoolean("auto_check_updates", true);
        includePreReleases = prefs.getBoolean("include_prereleases", false);
        pluginsEnabled = prefs.getBoolean("plugins_enabled", true);
        seededDefaultPlugins = prefs.getBoolean("seeded_default_plugins_v1", false);
        profileOrbitNfts = prefs.getBoolean("profile_orbit_nfts", true);
        profileAura = prefs.getBoolean("profile_aura", true);
        profileMetaRow = prefs.getBoolean("profile_meta_row", true);
        ghostDontRead = prefs.getBoolean("ghost_dont_read", false);
        ghostDontType = prefs.getBoolean("ghost_dont_type", false);
        ghostOffline = prefs.getBoolean("ghost_offline", false);
        antiDeleteMessages = prefs.getBoolean("anti_delete_messages", true);
        showSecondsInTime = prefs.getBoolean("show_seconds_in_time", false);
        showMessageId = prefs.getBoolean("show_message_id", false);
        confirmCalls = prefs.getBoolean("confirm_calls", true);
        disableProximity = prefs.getBoolean("disable_proximity", false);
        loaded = true;
    }

    public static int getDesignMode() {
        ensureLoaded();
        return designMode;
    }

    public static void setDesignMode(int mode) {
        ensureLoaded();
        if (mode < DESIGN_CLASSIC || mode > DESIGN_HYBRID) {
            mode = DESIGN_HYBRID;
        }
        designMode = mode;
        getPrefs().edit().putInt("design_mode", mode).apply();
    }

    public static int getMonetPalette() {
        ensureLoaded();
        return monetPalette;
    }

    public static void setMonetPalette(int palette) {
        ensureLoaded();
        if (palette < PALETTE_SYSTEM || palette > PALETTE_OCEAN) {
            palette = PALETTE_SYSTEM;
        }
        monetPalette = palette;
        getPrefs().edit().putInt("monet_palette", palette).apply();
    }

    public static boolean isMaterialYouActive() {
        int mode = getDesignMode();
        return mode == DESIGN_MATERIAL_YOU || mode == DESIGN_HYBRID;
    }

    public static boolean isLiquidGlassActive() {
        int mode = getDesignMode();
        return mode == DESIGN_LIQUID_GLASS || mode == DESIGN_HYBRID;
    }

    public static boolean isMonetTintBubbles() {
        ensureLoaded();
        return monetTintBubbles;
    }

    public static void setMonetTintBubbles(boolean value) {
        ensureLoaded();
        monetTintBubbles = value;
        getPrefs().edit().putBoolean("monet_tint_bubbles", value).apply();
    }

    public static int getLiquidGlassAlpha() {
        ensureLoaded();
        return liquidGlassAlpha;
    }

    public static void setLiquidGlassAlpha(int value) {
        ensureLoaded();
        liquidGlassAlpha = Math.max(20, Math.min(95, value));
        getPrefs().edit().putInt("liquid_glass_alpha", liquidGlassAlpha).apply();
    }

    public static int getFontPreset() {
        ensureLoaded();
        return fontPreset;
    }

    public static void setFontPreset(int preset) {
        ensureLoaded();
        fontPreset = preset;
        getPrefs().edit().putInt("font_preset", preset).apply();
    }

    public static String getCustomFontPath() {
        ensureLoaded();
        return customFontPath;
    }

    public static String getCustomFontName() {
        ensureLoaded();
        return customFontName;
    }

    public static void setCustomFont(String path, String name) {
        ensureLoaded();
        customFontPath = path != null ? path : "";
        customFontName = name != null ? name : "";
        fontPreset = FONT_CUSTOM;
        getPrefs().edit()
                .putString("custom_font_path", customFontPath)
                .putString("custom_font_name", customFontName)
                .putInt("font_preset", FONT_CUSTOM)
                .apply();
    }

    public static boolean isAutoCheckUpdates() {
        ensureLoaded();
        return autoCheckUpdates;
    }

    public static void setAutoCheckUpdates(boolean value) {
        ensureLoaded();
        autoCheckUpdates = value;
        getPrefs().edit().putBoolean("auto_check_updates", value).apply();
    }

    public static boolean isIncludePreReleases() {
        ensureLoaded();
        return includePreReleases;
    }

    public static void setIncludePreReleases(boolean value) {
        ensureLoaded();
        includePreReleases = value;
        getPrefs().edit().putBoolean("include_prereleases", value).apply();
    }

    public static boolean isPluginsEnabled() {
        ensureLoaded();
        return pluginsEnabled;
    }

    public static void setPluginsEnabled(boolean value) {
        ensureLoaded();
        pluginsEnabled = value;
        getPrefs().edit().putBoolean("plugins_enabled", value).apply();
    }

    public static boolean isSeededDefaultPlugins() {
        ensureLoaded();
        return seededDefaultPlugins;
    }

    public static void setSeededDefaultPlugins(boolean value) {
        ensureLoaded();
        seededDefaultPlugins = value;
        getPrefs().edit().putBoolean("seeded_default_plugins_v1", value).apply();
    }

    public static boolean isProfileOrbitNfts() {
        ensureLoaded();
        return profileOrbitNfts;
    }

    public static void setProfileOrbitNfts(boolean value) {
        ensureLoaded();
        profileOrbitNfts = value;
        getPrefs().edit().putBoolean("profile_orbit_nfts", value).apply();
    }

    public static boolean isProfileAura() {
        ensureLoaded();
        return profileAura;
    }

    public static void setProfileAura(boolean value) {
        ensureLoaded();
        profileAura = value;
        getPrefs().edit().putBoolean("profile_aura", value).apply();
    }

    public static boolean isProfileMetaRow() {
        ensureLoaded();
        return profileMetaRow;
    }

    public static void setProfileMetaRow(boolean value) {
        ensureLoaded();
        profileMetaRow = value;
        getPrefs().edit().putBoolean("profile_meta_row", value).apply();
    }

    public static boolean isGhostDontRead() {
        ensureLoaded();
        return ghostDontRead;
    }

    public static void setGhostDontRead(boolean value) {
        ensureLoaded();
        ghostDontRead = value;
        getPrefs().edit().putBoolean("ghost_dont_read", value).apply();
    }

    public static boolean isGhostDontType() {
        ensureLoaded();
        return ghostDontType;
    }

    public static void setGhostDontType(boolean value) {
        ensureLoaded();
        ghostDontType = value;
        getPrefs().edit().putBoolean("ghost_dont_type", value).apply();
    }

    public static boolean isGhostOffline() {
        ensureLoaded();
        return ghostOffline;
    }

    public static void setGhostOffline(boolean value) {
        ensureLoaded();
        ghostOffline = value;
        getPrefs().edit().putBoolean("ghost_offline", value).apply();
    }

    public static boolean isGhostAnyActive() {
        ensureLoaded();
        return ghostDontRead || ghostDontType || ghostOffline;
    }

    public static boolean isAntiDeleteMessages() {
        ensureLoaded();
        return antiDeleteMessages;
    }

    public static void setAntiDeleteMessages(boolean value) {
        ensureLoaded();
        antiDeleteMessages = value;
        getPrefs().edit().putBoolean("anti_delete_messages", value).apply();
    }

    public static boolean isShowSecondsInTime() {
        ensureLoaded();
        return showSecondsInTime;
    }

    public static void setShowSecondsInTime(boolean value) {
        ensureLoaded();
        showSecondsInTime = value;
        getPrefs().edit().putBoolean("show_seconds_in_time", value).apply();
    }

    public static boolean isShowMessageId() {
        ensureLoaded();
        return showMessageId;
    }

    public static void setShowMessageId(boolean value) {
        ensureLoaded();
        showMessageId = value;
        getPrefs().edit().putBoolean("show_message_id", value).apply();
    }

    public static boolean isConfirmCalls() {
        ensureLoaded();
        return confirmCalls;
    }

    public static void setConfirmCalls(boolean value) {
        ensureLoaded();
        confirmCalls = value;
        getPrefs().edit().putBoolean("confirm_calls", value).apply();
    }

    public static boolean isDisableProximity() {
        ensureLoaded();
        return disableProximity;
    }

    public static void setDisableProximity(boolean value) {
        ensureLoaded();
        disableProximity = value;
        getPrefs().edit().putBoolean("disable_proximity", value).apply();
    }
}
