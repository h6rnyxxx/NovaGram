/*
 * This is the source code of Telegram for Android v. 7.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2020.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.android.billingclient.api.ProductDetails;

import java.util.Objects;

public class BuildVars {

    public static boolean DEBUG_VERSION = BuildConfig.DEBUG_VERSION;
    public static boolean LOGS_ENABLED = BuildConfig.DEBUG_VERSION;
    public static boolean DEBUG_PRIVATE_VERSION = BuildConfig.DEBUG_PRIVATE_VERSION;
    public static boolean USE_CLOUD_STRINGS = true;
    public static boolean CHECK_UPDATES = true;
    public static boolean NO_SCOPED_STORAGE = Build.VERSION.SDK_INT <= 29;
    public static String BUILD_VERSION_STRING = BuildConfig.BUILD_VERSION_STRING;

    public static final int DEFAULT_APP_ID = 4;
    public static final String DEFAULT_APP_HASH = "014b35b6184100b085b0d0572f9b5103";

    public static int APP_ID = BuildConfig.NOVA_APP_ID != 0 ? BuildConfig.NOVA_APP_ID : DEFAULT_APP_ID;
    public static String APP_HASH = !android.text.TextUtils.isEmpty(BuildConfig.NOVA_APP_HASH) ? BuildConfig.NOVA_APP_HASH : DEFAULT_APP_HASH;

    public static boolean hasCustomApiCredentials() {
        try {
            if (ApplicationLoader.applicationContext == null) return false;
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("novaconfig", Context.MODE_PRIVATE);
            return prefs.getInt("custom_api_id", 0) > 0 && !android.text.TextUtils.isEmpty(prefs.getString("custom_api_hash", ""));
        } catch (Throwable ignore) {
            return false;
        }
    }

    public static boolean hasConfiguredApiCredentials() {
        return BuildConfig.NOVA_APP_ID != 0 || hasCustomApiCredentials();
    }

    public static void applySavedApiCredentials() {
        try {
            if (ApplicationLoader.applicationContext == null) return;
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("novaconfig", Context.MODE_PRIVATE);
            int savedId = prefs.getInt("custom_api_id", 0);
            String savedHash = prefs.getString("custom_api_hash", "");
            if (savedId > 0 && !android.text.TextUtils.isEmpty(savedHash)) {
                APP_ID = savedId;
                APP_HASH = savedHash.trim();
            } else {
                APP_ID = BuildConfig.NOVA_APP_ID != 0 ? BuildConfig.NOVA_APP_ID : DEFAULT_APP_ID;
                APP_HASH = !android.text.TextUtils.isEmpty(BuildConfig.NOVA_APP_HASH) ? BuildConfig.NOVA_APP_HASH : DEFAULT_APP_HASH;
            }
        } catch (Throwable ignore) {
        }
    }

    public static void saveCustomApiCredentials(int apiId, String apiHash) {
        if (ApplicationLoader.applicationContext == null) return;
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("novaconfig", Context.MODE_PRIVATE);
        if (apiId > 0 && !android.text.TextUtils.isEmpty(apiHash)) {
            APP_ID = apiId;
            APP_HASH = apiHash.trim();
            prefs.edit().putInt("custom_api_id", APP_ID).putString("custom_api_hash", APP_HASH).apply();
        } else {
            APP_ID = BuildConfig.NOVA_APP_ID != 0 ? BuildConfig.NOVA_APP_ID : DEFAULT_APP_ID;
            APP_HASH = !android.text.TextUtils.isEmpty(BuildConfig.NOVA_APP_HASH) ? BuildConfig.NOVA_APP_HASH : DEFAULT_APP_HASH;
            prefs.edit().remove("custom_api_id").remove("custom_api_hash").apply();
        }
    }

    // SafetyNet key for Google Identity SDK, set it to empty to disable
    public static String SAFETYNET_KEY = "AIzaSyDqt8P-7F7CPCseMkOiVRgb1LY8RN1bvH8";
    public static String PLAYSTORE_APP_URL = "https://github.com/h6rnyxxx/NovaGram/releases";
    public static String HUAWEI_STORE_URL = "https://github.com/h6rnyxxx/NovaGram/releases";
    public static String GOOGLE_AUTH_CLIENT_ID = "760348033671-81kmi3pi84p11ub8hp9a1funsv0rn2p9.apps.googleusercontent.com";

    public static String HUAWEI_APP_ID = "101184875";

    // You can use this flag to disable Google Play Billing (If you're making fork and want it to be in Google Play)
    public static boolean IS_BILLING_UNAVAILABLE = true;

    // works only on official app ids, disable on your forks
    public static boolean SUPPORTS_PASSKEYS = false;
    // the second main tab: News, or Contacts as before them

    public static boolean SUPPORTS_FPNV = false;

    static {
        if (ApplicationLoader.applicationContext != null) {
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = DEBUG_VERSION || sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION);
            if (LOGS_ENABLED) {
                final Thread.UncaughtExceptionHandler pastHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                    FileLog.fatal(exception, false);
                    if (pastHandler != null) {
                        pastHandler.uncaughtException(thread, exception);
                    }
                });
            }
        }
    }

    public static boolean useInvoiceBilling() {
        return BillingController.billingClientEmpty || DEBUG_VERSION && false || ApplicationLoader.isStandaloneBuild() || isBetaApp() && false || isHuaweiStoreApp() || hasDirectCurrency();
    }

    private static boolean hasDirectCurrency() {
        if (!BillingController.getInstance().isReady() || BillingController.PREMIUM_PRODUCT_DETAILS == null) {
            return false;
        }
        for (ProductDetails.SubscriptionOfferDetails offerDetails : BillingController.PREMIUM_PRODUCT_DETAILS.getSubscriptionOfferDetails()) {
            for (ProductDetails.PricingPhase phase : offerDetails.getPricingPhases().getPricingPhaseList()) {
                for (String cur : MessagesController.getInstance(UserConfig.selectedAccount).directPaymentsCurrency) {
                    if (Objects.equals(phase.getPriceCurrencyCode(), cur)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Boolean betaApp;
    public static boolean isBetaApp() {
        if (betaApp == null) {
            betaApp = ApplicationLoader.applicationContext != null && "org.telegram.messenger.beta".equals(ApplicationLoader.applicationContext.getPackageName());
        }
        return betaApp;
    }


    public static boolean isHuaweiStoreApp() {
        return ApplicationLoader.isHuaweiStoreBuild();
    }

    public static String getSmsHash() {
        return ApplicationLoader.isStandaloneBuild() ? "w0lkcmTZkKh" : (DEBUG_VERSION ? "O2P2z+/jBpJ" : "oLeq9AcOZkT");
    }
}
