package org.telegram.novagram.profile;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.util.List;

/**
 * Helpers for NovaGram's enhanced user/chat profiles:
 * luminous avatar aura, registration date estimation, DC location, and metadata formatting.
 */
public class NovaProfileHelper {

    private static final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Matrix sweepMatrix = new Matrix();

    private static int cachedAccent = 0;
    private static int cachedSecondary = 0;
    private static int cachedTertiary = 0;
    private static SweepGradient cachedSweep;
    private static RadialGradient cachedRadial;
    private static float cachedRadius = 0f;

    static {
        ringPaint.setStyle(Paint.Style.STROKE);
    }

    // Approximate historical Telegram ID -> epoch timestamp anchors (month precision)
    private static final long[] ID_ANCHORS = new long[]{
            1_000_000L,      // Aug 2013
            50_000_000L,     // Apr 2014
            100_000_000L,    // Mar 2015
            200_000_000L,    // Apr 2016
            350_000_000L,    // Jun 2017
            550_000_000L,    // Jun 2018
            850_000_000L,    // Jun 2019
            1_200_000_000L,  // Jun 2020
            1_800_000_000L,  // Jun 2021
            5_100_000_000L,  // Mar 2022
            5_800_000_000L,  // Dec 2022
            6_400_000_000L,  // Aug 2023
            7_000_000_000L,  // Apr 2024
            7_600_000_000L,  // Jan 2025
            8_100_000_000L,  // Dec 2025
            8_600_000_000L   // Nov 2026
    };

    private static final int[] YEAR_MONTH_ANCHORS = new int[]{
            2013 * 12 + 7,   // Aug 2013
            2014 * 12 + 3,   // Apr 2014
            2015 * 12 + 2,   // Mar 2015
            2016 * 12 + 3,   // Apr 2016
            2017 * 12 + 5,   // Jun 2017
            2018 * 12 + 5,   // Jun 2018
            2019 * 12 + 5,   // Jun 2019
            2020 * 12 + 5,   // Jun 2020
            2021 * 12 + 5,   // Jun 2021
            2022 * 12 + 2,   // Mar 2022
            2022 * 12 + 11,  // Dec 2022
            2023 * 12 + 7,   // Aug 2023
            2024 * 12 + 3,   // Apr 2024
            2025 * 12 + 0,   // Jan 2025
            2025 * 12 + 11,  // Dec 2025
            2026 * 12 + 10   // Nov 2026
    };

    private static final String[] MONTHS_RU = {
            "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
            "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
    };

    private static final String[] MONTHS_EN = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    public static void drawAvatarAura(
            Canvas canvas,
            View avatarContainer,
            int primaryColor,
            List<Integer> nftColors,
            float expandProgress,
            float collapseProgress,
            float alpha
    ) {
        if (canvas == null || avatarContainer == null || alpha <= 0.01f || expandProgress >= 0.96f || collapseProgress <= 0.05f) {
            return;
        }
        float w = avatarContainer.getWidth() * avatarContainer.getScaleX();
        float h = avatarContainer.getHeight() * avatarContainer.getScaleY();
        if (w <= 0 || h <= 0) {
            return;
        }

        float cx = avatarContainer.getX() + w / 2f;
        float cy = avatarContainer.getY() + h / 2f;
        float baseR = Math.max(w, h) / 2f;

        int c1 = primaryColor != 0 ? (primaryColor | 0xFF000000) : Theme.getColor(Theme.key_featuredStickers_addButton);
        int c2 = (nftColors != null && !nftColors.isEmpty()) ? (nftColors.get(0) | 0xFF000000) : shiftHue(c1, 42f);
        int c3 = (nftColors != null && nftColors.size() > 1) ? (nftColors.get(1) | 0xFF000000) : shiftHue(c1, -48f);

        float sec = SystemClock.uptimeMillis() / 1000f;
        float pulse = 1f + 0.035f * (float) Math.sin(sec * 2.2f);
        float haloR = baseR * 1.48f * pulse;

        if (cachedRadial == null || Math.abs(cachedRadius - haloR) > dp(2) || cachedAccent != c1 || cachedSecondary != c2) {
            cachedRadius = haloR;
            cachedAccent = c1;
            cachedSecondary = c2;
            cachedTertiary = c3;
            cachedRadial = new RadialGradient(
                    0f, 0f, Math.max(1f, haloR),
                    new int[]{ColorUtils.setAlphaComponent(c2, 110), ColorUtils.setAlphaComponent(c1, 48), 0x00000000},
                    new float[]{0.45f, 0.78f, 1.0f},
                    Shader.TileMode.CLAMP
            );
            cachedSweep = new SweepGradient(
                    0f, 0f,
                    new int[]{c1, c2, c3, c1},
                    new float[]{0f, 0.34f, 0.68f, 1f}
            );
        }

        float vis = (1f - expandProgress) * collapseProgress * alpha;

        canvas.save();
        canvas.translate(cx, cy);

        // Soft glowing radial aura behind avatar
        haloPaint.setShader(cachedRadial);
        haloPaint.setAlpha((int) (255 * vis));
        canvas.drawCircle(0f, 0f, haloR, haloPaint);

        // Iridescent rotating halo ring
        sweepMatrix.setRotate((sec * 38f) % 360f);
        cachedSweep.setLocalMatrix(sweepMatrix);
        ringPaint.setShader(cachedSweep);
        ringPaint.setStrokeWidth(dp(2.4f));
        ringPaint.setAlpha((int) (210 * vis));
        canvas.drawCircle(0f, 0f, baseR + dp(3.8f), ringPaint);

        canvas.restore();
    }

    private static int shiftHue(int color, float deltaDeg) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        hsl[0] = (hsl[0] + deltaDeg + 360f) % 360f;
        hsl[1] = Math.min(1f, Math.max(0.45f, hsl[1]));
        return ColorUtils.HSLToColor(hsl);
    }

    public static String estimateRegistrationDate(long userId) {
        if (userId <= 0) {
            return "";
        }
        int ym;
        if (userId <= ID_ANCHORS[0]) {
            ym = YEAR_MONTH_ANCHORS[0];
        } else if (userId >= ID_ANCHORS[ID_ANCHORS.length - 1]) {
            ym = YEAR_MONTH_ANCHORS[YEAR_MONTH_ANCHORS.length - 1];
        } else {
            ym = YEAR_MONTH_ANCHORS[YEAR_MONTH_ANCHORS.length - 1];
            for (int i = 0; i < ID_ANCHORS.length - 1; i++) {
                if (userId >= ID_ANCHORS[i] && userId <= ID_ANCHORS[i + 1]) {
                    double frac = (double) (userId - ID_ANCHORS[i]) / (double) (ID_ANCHORS[i + 1] - ID_ANCHORS[i]);
                    ym = (int) Math.round(YEAR_MONTH_ANCHORS[i] + frac * (YEAR_MONTH_ANCHORS[i + 1] - YEAR_MONTH_ANCHORS[i]));
                    break;
                }
            }
        }
        int year = ym / 12;
        int monthIdx = ym % 12;
        boolean isRu = isRussianUi();
        String month = isRu ? MONTHS_RU[monthIdx] : MONTHS_EN[monthIdx];
        return "~" + month + " " + year;
    }

    public static String getDcLabel(TLRPC.User user, TLRPC.Chat chat) {
        int dcId = 0;
        if (user != null && user.photo != null) {
            dcId = user.photo.dc_id;
        } else if (chat != null && chat.photo != null) {
            dcId = chat.photo.dc_id;
        }
        if (dcId <= 0) {
            return "";
        }
        switch (dcId) {
            case 1:
            case 3:
                return "DC" + dcId + " (US)";
            case 2:
            case 4:
                return "DC" + dcId + " (NL)";
            case 5:
                return "DC5 (SG)";
            default:
                return "DC" + dcId;
        }
    }

    public static String formatProfileMeta(TLRPC.User user, TLRPC.UserFull userInfo, TLRPC.Chat chat) {
        StringBuilder sb = new StringBuilder();
        if (user != null) {
            sb.append("ID: ").append(user.id);
            if (!user.bot) {
                String reg = estimateRegistrationDate(user.id);
                if (!reg.isEmpty()) {
                    sb.append("  •  ").append(reg);
                }
            }
            String dc = getDcLabel(user, null);
            if (!dc.isEmpty()) {
                sb.append("  •  ").append(dc);
            }
            if (userInfo != null && userInfo.stargifts_count > 0) {
                sb.append("  •  \uD83C\uDF81 ").append(userInfo.stargifts_count);
            }
        } else if (chat != null) {
            sb.append("ID: -100").append(chat.id);
            String dc = getDcLabel(null, chat);
            if (!dc.isEmpty()) {
                sb.append("  •  ").append(dc);
            }
        }
        return sb.toString();
    }

    public static String getProfileMetaLabel(TLRPC.User user) {
        if (user != null) {
            return LocaleController.getString(R.string.NovaProfileMetaLabelUser);
        }
        return LocaleController.getString(R.string.NovaProfileMetaLabelChat);
    }

    private static boolean isRussianUi() {
        try {
            String lang = LocaleController.getInstance().getCurrentLocale().getLanguage();
            return "ru".equalsIgnoreCase(lang) || "uk".equalsIgnoreCase(lang) || "be".equalsIgnoreCase(lang);
        } catch (Throwable ignored) {
            return true;
        }
    }
}
