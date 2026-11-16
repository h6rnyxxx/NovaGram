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

    public static final String GITHUB_REPO = BuildConfig.NOVA_GITHUB_REPO;
    public static final String GITHUB_REPO_URL = "https://github.com/" + GITHUB_REPO;
    public static final String GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    private static final String PREFS_NAME = "novaconfig";

    private static int designMode = -1;
    private static boolean monetTintBubbles = true;
    private static boolean autoCheckUpdates = true;
    private static boolean includePreReleases = false;
    private static boolean pluginsEnabled = true;
    private static boolean seededDefaultPlugins = false;
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
        monetTintBubbles = prefs.getBoolean("monet_tint_bubbles", true);
        autoCheckUpdates = prefs.getBoolean("auto_check_updates", true);
        includePreReleases = prefs.getBoolean("include_prereleases", false);
        pluginsEnabled = prefs.getBoolean("plugins_enabled", true);
        seededDefaultPlugins = prefs.getBoolean("seeded_default_plugins_v1", false);
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
}
