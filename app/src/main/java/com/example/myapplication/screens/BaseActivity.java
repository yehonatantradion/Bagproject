package com.example.myapplication.screens;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.AppPreferences;
import com.example.myapplication.R;

/**
 * Activity בסיסית שמחילה:
 *  1. גודל גופן (fontScale) מ-SharedPreferences לפני כל setContentView
 *  2. ערכת צבעים (colorTheme) לפני כל setContentView
 *  3. צביעה של ה-Toolbar לפי ה-theme הנוכחי (כי ה-XML משתמש ב-@color/primary קשיח)
 *  4. בדיקה ב-onResume: אם ההגדרות השתנו → recreate() להחלה מיידית
 *
 * כל Activity בפרויקט מרחיב את BaseActivity במקום AppCompatActivity.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** הגדרות כפי שהוחלו ב-onCreate הנוכחי — לצורך זיהוי שינוי */
    private String  appliedTheme;
    private float   appliedFontScale;

    // ─────────────────────────────────────────────────────────────────────────
    // Font scale — applies before any activity logic
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void attachBaseContext(Context newBase) {
        float scale = AppPreferences.getFontScale(newBase);
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.fontScale = scale;
        Context scaled = newBase.createConfigurationContext(config);
        super.attachBaseContext(scaled);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Color theme — set before super.onCreate so it takes effect in setContentView
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        appliedTheme     = AppPreferences.getColorTheme(this);
        appliedFontScale = AppPreferences.getFontScale(this);
        applyColorTheme(appliedTheme);
        super.onCreate(savedInstanceState);
    }

    private void applyColorTheme(String theme) {
        switch (theme) {
            case "green":  setTheme(R.style.Theme_MyApplication_Green);   break;
            case "purple": setTheme(R.style.Theme_MyApplication_Purple);  break;
            case "red":    setTheme(R.style.Theme_MyApplication_Red);     break;
            default:       setTheme(R.style.Theme_MyApplication);         break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toolbar tinting — XML uses @color/primary (static), so we override in code
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        tintToolbar();
    }

    /** מוצא Toolbar בפריסה הנוכחית וצובע אותו בצבע הראשי של ה-theme */
    protected void tintToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setBackgroundColor(resolveThemeColor());
            }
        } catch (Exception ignored) {}
    }

    /** מחזיר את colorPrimary של ה-theme הנוכחי כ-int */
    protected int resolveThemeColor() {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(
                android.R.attr.colorPrimary, tv, true);
        return tv.data;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Settings change detection — recreate if theme or font changed
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        String currentTheme = AppPreferences.getColorTheme(this);
        float  currentScale = AppPreferences.getFontScale(this);

        boolean themeChanged = !currentTheme.equals(appliedTheme);
        boolean scaleChanged = Math.abs(currentScale - appliedFontScale) > 0.01f;

        if (themeChanged || scaleChanged) {
            recreate(); // applies new theme+scale cleanly via the full onCreate cycle
        }
    }
}
