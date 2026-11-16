package org.telegram.novagram.design;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.novagram.NovaConfig;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public class NovaFontManager {

    private static Typeface cachedRegular;
    private static Typeface cachedMedium;
    private static Typeface cachedBold;
    private static Typeface cachedItalic;
    private static int cachedPreset = -1;
    private static String cachedCustomPath = "";

    public static void invalidateCache() {
        cachedRegular = null;
        cachedMedium = null;
        cachedBold = null;
        cachedItalic = null;
        cachedPreset = -1;
        cachedCustomPath = "";
        AndroidUtilities.mediumTypeface = null;
        AndroidUtilities.clearTypefaceCache();
    }

    private static void ensureLoaded() {
        int preset = NovaConfig.getFontPreset();
        String customPath = NovaConfig.getCustomFontPath();
        if (cachedPreset == preset && TextUtils.equals(cachedCustomPath, customPath)) {
            return;
        }
        cachedPreset = preset;
        cachedCustomPath = customPath;
        cachedRegular = null;
        cachedMedium = null;
        cachedBold = null;
        cachedItalic = null;

        try {
            switch (preset) {
                case NovaConfig.FONT_SANS_MEDIUM:
                    cachedRegular = Typeface.create("sans-serif", Typeface.NORMAL);
                    cachedMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                    cachedBold = Typeface.create("sans-serif", Typeface.BOLD);
                    cachedItalic = Typeface.create("sans-serif", Typeface.ITALIC);
                    break;
                case NovaConfig.FONT_CONDENSED:
                    cachedRegular = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
                    cachedMedium = loadAssetDirect("fonts/rcondensedbold.ttf", Typeface.create("sans-serif-condensed", Typeface.BOLD));
                    cachedBold = cachedMedium;
                    cachedItalic = Typeface.create("sans-serif-condensed", Typeface.ITALIC);
                    break;
                case NovaConfig.FONT_SERIF:
                    cachedRegular = Typeface.SERIF;
                    cachedMedium = loadAssetDirect("fonts/mw_bold.ttf", Typeface.create(Typeface.SERIF, Typeface.BOLD));
                    cachedBold = cachedMedium;
                    cachedItalic = Typeface.create(Typeface.SERIF, Typeface.ITALIC);
                    break;
                case NovaConfig.FONT_MONO:
                    cachedRegular = loadAssetDirect("fonts/rmono.ttf", Typeface.MONOSPACE);
                    cachedMedium = Typeface.create(cachedRegular, Typeface.BOLD);
                    cachedBold = cachedMedium;
                    cachedItalic = Typeface.create(cachedRegular, Typeface.ITALIC);
                    break;
                case NovaConfig.FONT_CUSTOM:
                    if (!TextUtils.isEmpty(customPath) && new File(customPath).exists()) {
                        Typeface base = Typeface.createFromFile(customPath);
                        cachedRegular = base;
                        cachedMedium = Typeface.create(base, Typeface.BOLD);
                        cachedBold = cachedMedium;
                        cachedItalic = Typeface.create(base, Typeface.ITALIC);
                    }
                    break;
                case NovaConfig.FONT_SYSTEM:
                default:
                    break;
            }
        } catch (Throwable t) {
            FileLog.e("NovaFontManager load error", t);
        }
    }

    private static Typeface loadAssetDirect(String assetPath, Typeface fallback) {
        try {
            return Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), assetPath);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static Typeface getRegularTypeface() {
        ensureLoaded();
        return cachedRegular;
    }

    public static Typeface resolveTypeface(String assetPath) {
        ensureLoaded();
        if (cachedPreset == NovaConfig.FONT_SYSTEM || cachedRegular == null) {
            return null;
        }
        if (assetPath == null) {
            return cachedRegular;
        }
        if (assetPath.contains("rmono")) {
            return cachedPreset == NovaConfig.FONT_MONO ? cachedRegular : null;
        }
        if (assetPath.contains("italic")) {
            return cachedItalic != null ? cachedItalic : cachedRegular;
        }
        if (assetPath.contains("rextrabold") || assetPath.contains("rbold")) {
            return cachedBold != null ? cachedBold : cachedMedium;
        }
        if (assetPath.contains("medium") || assetPath.contains("condensedbold") || assetPath.contains("mw_bold")) {
            return cachedMedium != null ? cachedMedium : cachedRegular;
        }
        return cachedRegular;
    }

    public static void applyToThemePaints() {
        Typeface regular = getRegularTypeface();
        Typeface bold = cachedBold != null ? cachedBold : (cachedMedium != null ? cachedMedium : AndroidUtilities.bold());

        if (Theme.dialogs_namePaint != null) {
            for (int a = 0; a < Theme.dialogs_namePaint.length; a++) {
                if (Theme.dialogs_namePaint[a] != null) Theme.dialogs_namePaint[a].setTypeface(bold);
                if (Theme.dialogs_nameEncryptedPaint != null && Theme.dialogs_nameEncryptedPaint[a] != null) {
                    Theme.dialogs_nameEncryptedPaint[a].setTypeface(bold);
                }
                if (Theme.dialogs_messagePaint != null && Theme.dialogs_messagePaint[a] != null) {
                    Theme.dialogs_messagePaint[a].setTypeface(regular);
                }
                if (Theme.dialogs_messagePrintingPaint != null && Theme.dialogs_messagePrintingPaint[a] != null) {
                    Theme.dialogs_messagePrintingPaint[a].setTypeface(regular);
                }
            }
        }
        if (Theme.dialogs_timePaint != null) Theme.dialogs_timePaint.setTypeface(regular);
        if (Theme.dialogs_onlinePaint != null) Theme.dialogs_onlinePaint.setTypeface(regular);
        if (Theme.dialogs_offlinePaint != null) Theme.dialogs_offlinePaint.setTypeface(regular);
        if (Theme.dialogs_searchNamePaint != null) Theme.dialogs_searchNamePaint.setTypeface(bold);
        if (Theme.dialogs_messageNamePaint != null) Theme.dialogs_messageNamePaint.setTypeface(bold);

        if (Theme.chat_msgTextPaint != null) Theme.chat_msgTextPaint.setTypeface(regular);
        if (Theme.chat_msgGameTextPaint != null) Theme.chat_msgGameTextPaint.setTypeface(regular);
        if (Theme.chat_replyTextPaint != null) Theme.chat_replyTextPaint.setTypeface(regular);
        if (Theme.chat_quoteTextPaint != null) Theme.chat_quoteTextPaint.setTypeface(regular);
        if (Theme.chat_timePaint != null) Theme.chat_timePaint.setTypeface(regular);
        if (Theme.chat_namePaint != null) Theme.chat_namePaint.setTypeface(bold);
        if (Theme.chat_replyNamePaint != null) Theme.chat_replyNamePaint.setTypeface(bold);
        if (Theme.chat_msgBotButtonPaint != null) Theme.chat_msgBotButtonPaint.setTypeface(bold);
    }

    public static void selectPreset(int preset) {
        NovaConfig.setFontPreset(preset);
        invalidateCache();
        NovaDesignManager.refreshTheme();
    }

    public static boolean installFontFromFile(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists() || ApplicationLoader.applicationContext == null) {
            return false;
        }
        try {
            // Verify valid font before saving
            Typeface test = Typeface.createFromFile(sourceFile);
            if (test == null) return false;

            File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "novagram/fonts");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, sourceFile.getName());
            try (InputStream is = new java.io.FileInputStream(sourceFile);
                 FileOutputStream fos = new FileOutputStream(dest)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
            }
            NovaConfig.setCustomFont(dest.getAbsolutePath(), sourceFile.getName());
            invalidateCache();
            NovaDesignManager.refreshTheme();
            return true;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    public static String installFontFromUri(Activity activity, Uri uri) {
        if (activity == null || uri == null) return null;
        try {
            File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "novagram/fonts");
            if (!dir.exists()) dir.mkdirs();
            String name = queryFileName(activity, uri);
            if (TextUtils.isEmpty(name)) {
                name = "custom_font_" + System.currentTimeMillis() + ".ttf";
            }
            File dest = new File(dir, name);
            try (InputStream is = activity.getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(dest)) {
                if (is == null) return null;
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }
            }
            Typeface test = Typeface.createFromFile(dest);
            if (test == null) {
                dest.delete();
                return null;
            }
            NovaConfig.setCustomFont(dest.getAbsolutePath(), name);
            invalidateCache();
            NovaDesignManager.refreshTheme();
            return name;
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    public static boolean isFontFileName(String name) {
        if (TextUtils.isEmpty(name)) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".ttf") || lower.endsWith(".otf");
    }

    private static String queryFileName(Activity activity, Uri uri) {
        try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Throwable ignored) {
        }
        return uri.getLastPathSegment();
    }
}
