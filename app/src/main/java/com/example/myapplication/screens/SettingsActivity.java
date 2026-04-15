package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.AppPreferences;
import com.example.myapplication.R;

/**
 * מסך הגדרות — מאפשר למשתמש לשנות:
 *  • ערכת צבעים (כחול / ירוק / סגול / אדום)
 *  • גודל טקסט (קטן / רגיל / גדול / גדול מאוד)
 *
 * השינויים נשמרים ב-SharedPreferences דרך AppPreferences.
 * BaseActivity.onResume() מזהה את השינוי ומפעיל recreate() על כל מסך.
 */
public class SettingsActivity extends BaseActivity {

    // ── Font scale options ───────────────────────────────────────────────────
    private static final float[] SCALES = {0.85f, 1.0f, 1.15f, 1.3f};

    // ── Views ────────────────────────────────────────────────────────────────
    private TextView tvFontPreview;

    // theme rows
    private View rowBlue, rowGreen, rowPurple, rowRed;
    // checkmarks
    private TextView checkBlue, checkGreen, checkPurple, checkRed;
    // font pills
    private TextView pillSmall, pillNormal, pillLarge, pillXLarge;

    private String currentTheme;
    private float  currentScale;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);          // BaseActivity applies theme + font
        setContentView(R.layout.activity_settings);

        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Read current prefs
        currentTheme = AppPreferences.getColorTheme(this);
        currentScale = AppPreferences.getFontScale(this);

        bindViews();
        refreshThemeCheckmarks(currentTheme);
        refreshPills(currentScale);
        setClickListeners();
    }

    // ── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvFontPreview = findViewById(R.id.tvFontPreview);

        rowBlue   = findViewById(R.id.rowThemeBlue);
        rowGreen  = findViewById(R.id.rowThemeGreen);
        rowPurple = findViewById(R.id.rowThemePurple);
        rowRed    = findViewById(R.id.rowThemeRed);

        checkBlue   = findViewById(R.id.checkBlue);
        checkGreen  = findViewById(R.id.checkGreen);
        checkPurple = findViewById(R.id.checkPurple);
        checkRed    = findViewById(R.id.checkRed);

        pillSmall  = findViewById(R.id.pillSmall);
        pillNormal = findViewById(R.id.pillNormal);
        pillLarge  = findViewById(R.id.pillLarge);
        pillXLarge = findViewById(R.id.pillXLarge);
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setClickListeners() {
        rowBlue.setOnClickListener(v   -> selectTheme("blue"));
        rowGreen.setOnClickListener(v  -> selectTheme("green"));
        rowPurple.setOnClickListener(v -> selectTheme("purple"));
        rowRed.setOnClickListener(v    -> selectTheme("red"));

        pillSmall.setOnClickListener(v  -> selectScale(SCALES[0]));
        pillNormal.setOnClickListener(v -> selectScale(SCALES[1]));
        pillLarge.setOnClickListener(v  -> selectScale(SCALES[2]));
        pillXLarge.setOnClickListener(v -> selectScale(SCALES[3]));
    }

    // ── Theme selection ───────────────────────────────────────────────────────

    private void selectTheme(String theme) {
        AppPreferences.setColorTheme(this, theme);
        currentTheme = theme;
        refreshThemeCheckmarks(theme);
        // Tint toolbar immediately so the user sees the change without waiting for recreate
        tintToolbar();
    }

    private void refreshThemeCheckmarks(String active) {
        checkBlue.setVisibility(  "blue".equals(active)   ? View.VISIBLE : View.GONE);
        checkGreen.setVisibility( "green".equals(active)  ? View.VISIBLE : View.GONE);
        checkPurple.setVisibility("purple".equals(active) ? View.VISIBLE : View.GONE);
        checkRed.setVisibility(   "red".equals(active)    ? View.VISIBLE : View.GONE);
    }

    // ── Font scale selection ──────────────────────────────────────────────────

    private void selectScale(float scale) {
        AppPreferences.setFontScale(this, scale);
        currentScale = scale;
        refreshPills(scale);
        // Update preview text size immediately (sp-unit, not scaled, just illustrative)
        tvFontPreview.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16 * scale);
    }

    private void refreshPills(float active) {
        int idx = findScaleIndex(active);
        pillSmall .setBackground(getDrawable(idx == 0 ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected));
        pillNormal.setBackground(getDrawable(idx == 1 ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected));
        pillLarge .setBackground(getDrawable(idx == 2 ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected));
        pillXLarge.setBackground(getDrawable(idx == 3 ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected));

        pillSmall .setTextColor(idx == 0 ? 0xFFFFFFFF : 0xFF666666);
        pillNormal.setTextColor(idx == 1 ? 0xFFFFFFFF : 0xFF666666);
        pillLarge .setTextColor(idx == 2 ? 0xFFFFFFFF : 0xFF666666);
        pillXLarge.setTextColor(idx == 3 ? 0xFFFFFFFF : 0xFF666666);
    }

    private int findScaleIndex(float scale) {
        int best = 1;
        float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < SCALES.length; i++) {
            float diff = Math.abs(SCALES[i] - scale);
            if (diff < bestDiff) { bestDiff = diff; best = i; }
        }
        return best;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
