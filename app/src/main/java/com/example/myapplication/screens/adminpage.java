package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;

public class adminpage extends AppCompatActivity {

    private Button btnManageWorkers;
    private Button btnManageShifts;
    private Button btnTodayAttendance;
    private Button btnReports;
    private Button btnSettings;
    private Button btnUsersList;

    // הכפתור החדש שהוספנו
    private Button btnAddShift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adminpage);

        View main = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // הגדרת כיוון מימין לשמאל (RTL)
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        initViews();
        initListeners();

        // יישור אוטומטי של כל הטקסטים לימין
        main.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        alignAllTextViewsToRight(main);
                        main.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
        );
    }

    private void initViews() {
        btnManageWorkers = findViewById(R.id.btnManageWorkers);
        btnManageShifts = findViewById(R.id.btnManageShifts);
        btnTodayAttendance = findViewById(R.id.btnTodayAttendance);
        btnReports = findViewById(R.id.btnReports);
        btnSettings = findViewById(R.id.btnSettings);
        btnUsersList = findViewById(R.id.btnUsersList);

        // קישור הכפתור החדש מה-XML
        btnAddShift = findViewById(R.id.btnAddShift);
    }

    private void initListeners() {
        // ניהול עובדים
        btnManageWorkers.setOnClickListener(v -> {
            Intent intent = new Intent(adminpage.this, ManageWorkersActivity.class);
            startActivity(intent);
        });

        // ניהול משמרות (דף קיים)
        btnManageShifts.setOnClickListener(v -> {
            Intent intent = new Intent(adminpage.this, ManageShiftsActivity.class);
            startActivity(intent);
        });

        // --- הכפתור החדש: הוספת משמרת ---
        if (btnAddShift != null) {
            btnAddShift.setOnClickListener(v -> {
                Intent intent = new Intent(adminpage.this, AddShift.class);
                startActivity(intent);
            });
        }

        // נוכחות היום
        btnTodayAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(adminpage.this, TodayAttendanceActivity.class);
            startActivity(intent);
        });

        // דוחות
        btnReports.setOnClickListener(v -> {
            Intent intent = new Intent(adminpage.this, ReportsActivity.class);
            startActivity(intent);
        });

        // רשימת משתמשים
        btnUsersList.setOnClickListener(v -> {
            Intent intent = new Intent(adminpage.this, UsersList.class);
            startActivity(intent);
        });

        // הגדרות
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(adminpage.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * פונקציית עזר ליישור כל ה-TextViews והכפתורים לימין
     */
    private void alignAllTextViewsToRight(View root) {
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                alignAllTextViewsToRight(group.getChildAt(i));
            }
        }
    }
}