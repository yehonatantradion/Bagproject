package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.Shift;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class AddShift extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate;
    private Button btnGoToManageWorkers;
    private CalendarDay selectedDay;
    private DatabaseService dbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_shift);

        dbService = DatabaseService.getInstance();
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnGoToManageWorkers = findViewById(R.id.btnGoToManageWorkers);

        // אתחול לתאריך של היום
        selectedDay = CalendarDay.today();
        checkForExistingShift(selectedDay);

        // מאזין לשינוי תאריך
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
    }

    // פונקציית עזר לפרמוט התאריך בצורה אחידה
    private String getFormattedDate(CalendarDay day) {
        // חשוב: וודא שזה תואם לאיך שזה נשמר ב-DB.
        // כרגע זה מייצר פורמט: 15/02/2026
        return String.format("%02d/%02d/%d", day.getDay(), day.getMonth(), day.getYear());
    }

    private void checkForExistingShift(CalendarDay day) {
        String dateToCheck = getFormattedDate(day);

        // עדכון זמני למשתמש
        tvSelectedDate.setText("בודק נתונים לתאריך: " + dateToCheck + "...");

        dbService.getShiftByDate(dateToCheck, new DatabaseService.DatabaseCallback<Shift>() {
            @Override
            public void onCompleted(Shift shift) {
                // חובה להריץ עדכוני UI בתוך runOnUiThread
                runOnUiThread(() -> {
                    if (shift != null) {
                        // === מקרה 1: משמרת נמצאה ===
                        Log.d("AddShift", "Shift found for date: " + dateToCheck);

                        int count = (shift.getWorkerList() != null) ? shift.getWorkerList().size() : 0;
                        tvSelectedDate.setText("✅ קיימת משמרת בתאריך " + dateToCheck + "\n(" + count + " עובדים משובצים)");

                        btnGoToManageWorkers.setText("ערוך משמרת קיימת");
                        // שינוי צבע לכפתור כדי שיהיה בולט (כתום לעריכה)
                        btnGoToManageWorkers.setBackgroundColor(0xFFFFA726);
                    } else {
                        // === מקרה 2: לא נמצאה משמרת ===
                        Log.d("AddShift", "No shift found for date: " + dateToCheck);

                        tvSelectedDate.setText("📅 תאריך נבחר: " + dateToCheck + "\n(לא קיימת משמרת)");

                        btnGoToManageWorkers.setText("צור משמרת חדשה");
                        // שינוי צבע לכפתור (כחול/ברירת מחדל ליצירה)
                        btnGoToManageWorkers.setBackgroundColor(0xFF2196F3);
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                runOnUiThread(() -> {
                    tvSelectedDate.setText("שגיאה בבדיקת הנתונים");
                    Toast.makeText(AddShift.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // רענון במידה וחוזרים ממסך העריכה
        if (selectedDay != null) {
            checkForExistingShift(selectedDay);
        }
    }
}