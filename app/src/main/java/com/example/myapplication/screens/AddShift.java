package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Shift;
import com.example.myapplication.services.DatabaseService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class AddShift extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate, tvAssignedWorkers;
    private Button btnGoToManageWorkers;
    private CalendarDay selectedDay = CalendarDay.today();
    private DatabaseService dbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_add_shift);

        dbService = DatabaseService.getInstance();
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvAssignedWorkers = findViewById(R.id.tvAssignedWorkers);
        btnGoToManageWorkers = findViewById(R.id.btnGoToManageWorkers);

        calendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();

        fetchShiftData(selectedDay);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDay = date;
            fetchShiftData(selectedDay);
        });

        btnGoToManageWorkers.setOnClickListener(v -> {
            String dateString = formatDate(selectedDay);
            Intent intent = new Intent(AddShift.this, ManageShiftsActivity.class);
            intent.putExtra("SELECTED_DATE", dateString);
            startActivity(intent);
        });
    }

    private void fetchShiftData(CalendarDay day) {
        String dateString = formatDate(day);
        tvSelectedDate.setText("תאריך: " + dateString);
        tvAssignedWorkers.setText("בודק משמרות...");

        // התיקון כאן: שימוש ב-getDbReference() וב-child("Shifts")
        dbService.getDbReference().child("Shifts")
                .orderByChild("date").equalTo(dateString)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            StringBuilder workersList = new StringBuilder("פרטי משמרת:\n");
                            for (DataSnapshot shiftSnapshot : snapshot.getChildren()) {
                                Shift shift = shiftSnapshot.getValue(Shift.class);
                                if (shift != null) {
                                    workersList.append("סוג: ").append(shift.getType()).append("\n");
                                    if (shift.getNotes() != null) {
                                        workersList.append("עובדים: ").append(shift.getNotes()).append("\n");
                                    }
                                }
                            }
                            tvAssignedWorkers.setText(workersList.toString());
                        } else {
                            tvAssignedWorkers.setText("אין משמרת רשומה לתאריך זה.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvAssignedWorkers.setText("שגיאה בטעינת נתונים.");
                    }
                });
    }

    private String formatDate(CalendarDay day) {
        return String.format("%02d/%02d/%d", day.getDay(), day.getMonth(), day.getYear());
    }
}