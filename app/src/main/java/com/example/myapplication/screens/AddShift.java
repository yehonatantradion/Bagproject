package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.services.DatabaseService;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.Calendar;
import java.util.TimeZone;

public class AddShift extends BaseActivity {

    private static final String TYPE_MORNING = "בוקר";
    private static final String TYPE_EVENING = "ערב";

    private MaterialCalendarView calendarView;
    private TextView tvSelectedDate, tvAvailableCount;
    private LinearLayout cardMorning, cardEvening;
    private Button btnGoToManageWorkers, btnBack;

    private CalendarDay selectedDay;
    private String selectedShiftType = null;

    private DatabaseService dbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_shift);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbService = DatabaseService.getInstance();
        dbService.deletePastShifts();

        calendarView          = findViewById(R.id.calendarView);
        tvSelectedDate        = findViewById(R.id.tvSelectedDate);
        tvAvailableCount      = findViewById(R.id.tvAvailableCount);
        cardMorning           = findViewById(R.id.cardMorning);
        cardEvening           = findViewById(R.id.cardEvening);
        btnGoToManageWorkers  = findViewById(R.id.btnGoToManageWorkers);
        btnBack               = findViewById(R.id.btnBack);

        // חסום תאריכים עברים בלוח השנה
        calendarView.state().edit().setMinimumDate(CalendarDay.today()).commit();

        selectedDay = CalendarDay.today();
        updateDateDisplay();

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDay = date;
            updateDateDisplay();
            refreshAvailableCount();
        });

        cardMorning.setOnClickListener(v -> selectType(TYPE_MORNING));
        cardEvening.setOnClickListener(v -> selectType(TYPE_EVENING));

        btnGoToManageWorkers.setOnClickListener(v -> openManageShifts());
        btnBack.setOnClickListener(v -> finish());

        updateSubmitButton();
    }

    private void updateDateDisplay() {
        if (selectedDay == null) return;
        String fmt = String.format("%02d/%02d/%d",
                selectedDay.getDay(), selectedDay.getMonth(), selectedDay.getYear());
        tvSelectedDate.setText("📅 " + fmt);
    }

    private void selectType(String type) {
        selectedShiftType = type;
        if (TYPE_MORNING.equals(type)) {
            cardMorning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_selected));
            cardEvening.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        } else {
            cardEvening.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_selected));
            cardMorning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        }
        refreshAvailableCount();
        updateSubmitButton();
    }

    private void refreshAvailableCount() {
        if (selectedDay == null || selectedShiftType == null) {
            tvAvailableCount.setText("");
            return;
        }
        final String dateStr = String.format("%02d/%02d/%d",
                selectedDay.getDay(), selectedDay.getMonth(), selectedDay.getYear());
        tvAvailableCount.setText("טוען...");

        // שלב 1: כמה ביקשו
        dbService.getShiftRequestsByDateAndType(dateStr, selectedShiftType,
                new DatabaseService.DatabaseCallback<java.util.List<com.example.myapplication.model.ShiftRequest>>() {
                    @Override
                    public void onCompleted(java.util.List<com.example.myapplication.model.ShiftRequest> list) {
                        final int requestCount = list == null ? 0 : list.size();

                        // שלב 2: כמה כבר בסידור
                        dbService.getTodayShifts(dateStr,
                                new DatabaseService.DatabaseCallback<java.util.List<com.example.myapplication.services.Shift>>() {
                                    @Override
                                    public void onCompleted(java.util.List<com.example.myapplication.services.Shift> shifts) {
                                        int assignedCount = 0;
                                        if (shifts != null) {
                                            for (com.example.myapplication.services.Shift s : shifts) {
                                                if (selectedShiftType.equals(s.getShiftType())
                                                        && s.getWorkerList() != null) {
                                                    assignedCount = s.getWorkerList().size();
                                                    break;
                                                }
                                            }
                                        }
                                        String reqEmoji      = requestCount > 0 ? "✅" : "⚠️";
                                        String assignedPart  = assignedCount > 0
                                                ? "  |  👥 " + assignedCount + " בסידור"
                                                : "";
                                        tvAvailableCount.setText(
                                                reqEmoji + " " + requestCount + " ביקשו"
                                                + assignedPart);
                                    }
                                    @Override
                                    public void onFailed(Exception e) {
                                        // הצג לפחות את מספר המבקשים
                                        String emoji = requestCount > 0 ? "✅" : "⚠️";
                                        tvAvailableCount.setText(emoji + " " + requestCount + " ביקשו");
                                    }
                                });
                    }
                    @Override
                    public void onFailed(Exception e) {
                        tvAvailableCount.setText("שגיאה בטעינה");
                    }
                });
    }

    private boolean isShiftExpired() {
        if (selectedDay == null || selectedShiftType == null) return false;
        CalendarDay today = CalendarDay.today();
        // תאריך עבר — תמיד פג תוקף
        if (selectedDay.isBefore(today)) return true;
        // היום — בדוק לפי שעה (שעון ישראל)
        if (selectedDay.equals(today)) {
            int hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"))
                    .get(Calendar.HOUR_OF_DAY);
            if (TYPE_MORNING.equals(selectedShiftType) && hour >= 14) return true;
            if (TYPE_EVENING.equals(selectedShiftType) && hour >= 22) return true;
        }
        return false;
    }

    private void updateSubmitButton() {
        boolean ready = selectedDay != null && selectedShiftType != null && !isShiftExpired();
        btnGoToManageWorkers.setEnabled(ready);
        btnGoToManageWorkers.setAlpha(ready ? 1f : 0.45f);
    }

    private void openManageShifts() {
        if (selectedDay == null || selectedShiftType == null) {
            Toast.makeText(this, "יש לבחור תאריך וסוג משמרת", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isShiftExpired()) {
            Toast.makeText(this, "לא ניתן ליצור משמרת שפג תוקפה", Toast.LENGTH_SHORT).show();
            return;
        }
        String dateStr = String.format("%02d/%02d/%d",
                selectedDay.getDay(), selectedDay.getMonth(), selectedDay.getYear());

        Intent intent = new Intent(this, ManageShiftsActivity.class);
        intent.putExtra("SELECTED_DATE", dateStr);
        intent.putExtra("SELECTED_SHIFT_TYPE", selectedShiftType);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAvailableCount();
    }
}
