package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * גישה מרכזית ל-SharedPreferences של הגדרות האפליקציה.
 * כל פעולת קריאה/כתיבה נעשית דרך כאן.
 */
public class AppPreferences {

    private static final String PREFS_NAME   = "app_settings";
    private static final String KEY_THEME    = "colorTheme";
    private static final String KEY_SCALE    = "fontScale";

    // ── Color theme ──────────────────────────────────────────────────────────

    /** מחזיר את שם ערכת הצבעים הנוכחית: "blue" | "green" | "purple" | "red" */
    public static String getColorTheme(Context ctx) {
        return prefs(ctx).getString(KEY_THEME, "blue");
    }

    public static void setColorTheme(Context ctx, String theme) {
        prefs(ctx).edit().putString(KEY_THEME, theme).apply();
    }

    // ── Font scale ────────────────────────────────────────────────────────────

    /** מחזיר את גודל הגופן היחסי: 0.85 | 1.0 | 1.15 | 1.3 */
    public static float getFontScale(Context ctx) {
        return prefs(ctx).getFloat(KEY_SCALE, 1.0f);
    }

    public static void setFontScale(Context ctx, float scale) {
        prefs(ctx).edit().putFloat(KEY_SCALE, scale).apply();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
