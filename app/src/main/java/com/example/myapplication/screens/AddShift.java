package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
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

        // הגבלת לוח השנה לתאריך המינימלי (היום)
        calendarView.state().edit()
                .setMinimumDate(CalendarDay.today())
                .commit();

        updateDateText(selectedDay);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // הגנה נוספת ליתר ביטחון
            if (date.isBefore(CalendarDay.today())) {
                Toast.makeText(this, "לא ניתן לבחור תאריך שעבר", Toast.LENGTH_SHORT).show();
                calendarView.setSelectedDate(CalendarDay.today());
                selectedDay = CalendarDay.today();
            } else {
                selectedDay = date;
            }
            updateDateText(selectedDay);
        });

        btnGoToManageWorkers.setOnClickListener(v -> {
            // וידוא סופי לפני מעבר למסך הבא
            if (selectedDay.isBefore(CalendarDay.today())) {
                Toast.makeText(this, "אנא בחר תאריך תקין", Toast.LENGTH_SHORT).show();
                return;
            }

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