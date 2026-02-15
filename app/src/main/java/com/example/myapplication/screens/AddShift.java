package com.example.myapplication.screens;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.Shift;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import java.util.Date;

public class AddShift extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate;
    private TextView tvWorkerSummary;
    private Button btnGoToManageWorkers;
    private Button btnBack; // הכפתור החדש
    private CalendarDay selectedDay;
    private DatabaseService dbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_shift);

        dbService = DatabaseService.getInstance();

        // מחיקת משמרות עבר בכניסה למסך
        dbService.deletePastShifts();

        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvWorkerSummary = findViewById(R.id.tvWorkerSummary);
        btnGoToManageWorkers = findViewById(R.id.btnGoToManageWorkers);
        btnBack = findViewById(R.id.btnBack); // חיבור הכפתור

        selectedDay = CalendarDay.today();
        checkForExistingShift(selectedDay);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDay = date;
            checkForExistingShift(selectedDay);
        });

        btnGoToManageWorkers.setOnClickListener(v -> {
            String dateString = getFormattedDate(selectedDay);
            Intent intent = new Intent(AddShift.this, ManageShiftsActivity.class);
            intent.putExtra("SELECTED_DATE", dateString);
            startActivity(intent);
        });

        // לוגיקה לכפתור חזרה
        btnBack.setOnClickListener(v -> {
            finish(); // סוגר את המסך הנוכחי וחוזר למסך הקודם
        });
    }

    private String getFormattedDate(CalendarDay day) {
        return String.format("%02d/%02d/%d", day.getDay(), day.getMonth(), day.getYear());
    }

    private void checkForExistingShift(CalendarDay day) {
        String dateToCheck = getFormattedDate(day);

        // בדיקת תאריך עבר
        if (day.isBefore(CalendarDay.today())) {
            tvSelectedDate.setText("⛔ תאריך עבר: " + dateToCheck);
            tvWorkerSummary.setText("לא ניתן ליצור או לערוך משמרות שהיו בעבר.");

            btnGoToManageWorkers.setText("חסום");
            btnGoToManageWorkers.setBackgroundColor(Color.GRAY);
            btnGoToManageWorkers.setEnabled(false);
            return;
        }

        btnGoToManageWorkers.setEnabled(true);
        tvSelectedDate.setText("בודק נתונים...");
        tvWorkerSummary.setText("");

        dbService.getShiftByDate(dateToCheck, new DatabaseService.DatabaseCallback<Shift>() {
            @Override
            public void onCompleted(Shift shift) {
                runOnUiThread(() -> {
                    if (shift != null) {
                        // === יש משמרת ===
                        int count = (shift.getWorkerList() != null) ? shift.getWorkerList().size() : 0;
                        tvSelectedDate.setText("✅ קיימת משמרת (" + count + " עובדים)");

                        if (count > 0) {
                            StringBuilder names = new StringBuilder();
                            for (Worker w : shift.getWorkerList()) {
                                String name = (w.getfName() != null) ? w.getfName() : "ללא שם";
                                names.append(name).append(", ");
                            }
                            if (names.length() > 2) names.setLength(names.length() - 2);
                            tvWorkerSummary.setText("משובצים: " + names.toString());
                        } else {
                            tvWorkerSummary.setText("משמרת ריקה (אין עובדים)");
                        }

                        btnGoToManageWorkers.setText("ערוך משמרת");
                        btnGoToManageWorkers.setBackgroundColor(0xFFFFA726); // כתום

                    } else {
                        // === אין משמרת ===
                        tvSelectedDate.setText("📅 תאריך: " + dateToCheck);
                        tvWorkerSummary.setText("לא קיימת משמרת רשומה");

                        btnGoToManageWorkers.setText("צור משמרת חדשה");
                        btnGoToManageWorkers.setBackgroundColor(0xFF2196F3); // כחול
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                runOnUiThread(() -> {
                    tvSelectedDate.setText("שגיאה בטעינה");
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedDay != null) {
            checkForExistingShift(selectedDay);
        }
    }
}