package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class AddShift extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate;
    private Button btnGoToManageWorkers;
    private CalendarDay selectedDay = CalendarDay.today();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_shift);

        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnGoToManageWorkers = findViewById(R.id.btnGoToManageWorkers);

        // עדכון ראשוני של התאריך
        updateDateText(selectedDay);

        // האזנה לשינוי תאריך בלוח
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDay = date;
            updateDateText(selectedDay);
        });

        // לחיצה על הכפתור למעבר למסך ניהול עובדים
        btnGoToManageWorkers.setOnClickListener(v -> {
            String dateString = selectedDay.getDay() + "/" + selectedDay.getMonth() + "/" + selectedDay.getYear();

            Intent intent = new Intent(AddShift.this, ManageShiftsActivity.class);
            intent.putExtra("SELECTED_DATE", dateString);
            startActivity(intent);
        });
    }

    private void updateDateText(CalendarDay day) {
        tvSelectedDate.setText(String.format("נבחר: %02d/%02d/%d", day.getDay(), day.getMonth(), day.getYear()));
    }
}