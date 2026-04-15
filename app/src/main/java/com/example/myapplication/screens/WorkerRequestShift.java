package com.example.myapplication.screens;

import android.app.DatePickerDialog;
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
import com.example.myapplication.model.ShiftRequest;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class WorkerRequestShift extends BaseActivity {

    // שעות קבועות לפי סוג משמרת
    private static final String MORNING_START = "06:00";
    private static final String MORNING_END   = "14:00";
    private static final String EVENING_START = "14:00";
    private static final String EVENING_END   = "22:00";
    private static final String TYPE_MORNING  = "בוקר";
    private static final String TYPE_EVENING  = "ערב";

    private Button btnPickDate, btnRequestShift, btnBack;
    private TextView tvSelectedDate, tvSummary;
    private LinearLayout cardMorning, cardEvening, layoutSummary;

    private DatabaseService db;

    // מצב בחירה
    private int selectedYear  = -1;
    private int selectedMonth = -1; // 1-based
    private int selectedDay   = -1;
    private String selectedShiftType = null; // "בוקר" / "ערב"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_worker_request_shift);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = DatabaseService.getInstance();

        btnPickDate     = findViewById(R.id.btnPickDate);
        btnRequestShift = findViewById(R.id.btnRequestShift);
        btnBack         = findViewById(R.id.btnBack);
        tvSelectedDate  = findViewById(R.id.tvSelectedDate);
        tvSummary       = findViewById(R.id.tvSummary);
        cardMorning     = findViewById(R.id.cardMorning);
        cardEvening     = findViewById(R.id.cardEvening);
        layoutSummary   = findViewById(R.id.layoutSummary);

        btnPickDate.setOnClickListener(v -> openDatePicker());
        cardMorning.setOnClickListener(v -> selectShiftType(TYPE_MORNING));
        cardEvening.setOnClickListener(v -> selectShiftType(TYPE_EVENING));
        btnRequestShift.setOnClickListener(v -> submitRequest());
        btnBack.setOnClickListener(v -> finish());
    }

    // ──────────────────────────────────────
    // פתיחת בוחר תאריך — תאריכים עבריים בלבד
    // ──────────────────────────────────────
    private void openDatePicker() {
        Calendar today = Calendar.getInstance();
        int year  = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH);  // 0-based
        int day   = today.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (picker, y, m, d) -> onDateSelected(y, m + 1, d),
                year, month, day);

        // חסימת תאריכים עברים (לפני היום)
        dialog.getDatePicker().setMinDate(today.getTimeInMillis());
        dialog.show();
    }

    private void onDateSelected(int year, int month, int day) {
        selectedYear  = year;
        selectedMonth = month;
        selectedDay   = day;

        // הצגת התאריך הנבחר בפורמט d.M.yyyy
        String display = day + "." + month + "." + year;
        tvSelectedDate.setText("📅 " + display);
        btnPickDate.setText(display);

        updateSummary();
        updateSubmitButton();
    }

    // ──────────────────────────────────────
    // בחירת סוג משמרת (בוקר / ערב)
    // ──────────────────────────────────────
    private void selectShiftType(String type) {
        selectedShiftType = type;

        // עדכון ויזואל של הכרטיסיות
        if (TYPE_MORNING.equals(type)) {
            cardMorning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_selected));
            cardEvening.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        } else {
            cardEvening.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_selected));
            cardMorning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        }

        updateSummary();
        updateSubmitButton();
    }

    // ──────────────────────────────────────
    // עדכון תצוגת הסיכום
    // ──────────────────────────────────────
    private void updateSummary() {
        if (selectedDay == -1 || selectedShiftType == null) {
            layoutSummary.setVisibility(View.GONE);
            return;
        }
        String dateLabel = selectedDay + "." + selectedMonth + "." + selectedYear;
        String timeRange = TYPE_MORNING.equals(selectedShiftType)
                ? MORNING_START + " – " + MORNING_END
                : EVENING_START + " – " + EVENING_END;

        tvSummary.setText(dateLabel + "  |  " + selectedShiftType + "  |  " + timeRange);
        layoutSummary.setVisibility(View.VISIBLE);
    }

    private void updateSubmitButton() {
        btnRequestShift.setEnabled(selectedDay != -1 && selectedShiftType != null);
    }

    // ──────────────────────────────────────
    // שליחת הבקשה
    // ──────────────────────────────────────
    private void submitRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "יש להתחבר תחילה", Toast.LENGTH_SHORT).show();
            return;
        }
        btnRequestShift.setEnabled(false);

        String dateFb = String.format("%02d/%02d/%04d", selectedDay, selectedMonth, selectedYear);
        String startTime = TYPE_MORNING.equals(selectedShiftType) ? MORNING_START : EVENING_START;
        String endTime   = TYPE_MORNING.equals(selectedShiftType) ? MORNING_END   : EVENING_END;

        db.getWorker(user.getUid(), new DatabaseService.DatabaseCallback<Worker>() {
            @Override
            public void onCompleted(Worker worker) {
                if (worker == null) {
                    Toast.makeText(WorkerRequestShift.this, "שגיאה: משתמש לא נמצא", Toast.LENGTH_SHORT).show();
                    btnRequestShift.setEnabled(true);
                    return;
                }

                // בדיקה שלא קיימת בקשה כבר לאותה משמרת
                db.checkWorkerHasRequest(worker.getId(), dateFb, selectedShiftType,
                        new DatabaseService.DatabaseCallback<Boolean>() {
                    @Override
                    public void onCompleted(Boolean exists) {
                        if (Boolean.TRUE.equals(exists)) {
                            Toast.makeText(WorkerRequestShift.this,
                                    "כבר שלחת בקשה למשמרת זו", Toast.LENGTH_SHORT).show();
                            btnRequestShift.setEnabled(true);
                            return;
                        }
                        // שלח את הבקשה
                        ShiftRequest request = new ShiftRequest(
                                worker.getId(),
                                worker.getfName() + " " + worker.getlName(),
                                dateFb, startTime, endTime, "");
                        request.setShiftType(selectedShiftType);

                        db.addShiftRequest(request, new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void v) {
                                Toast.makeText(WorkerRequestShift.this,
                                        "הבקשה נשלחה! " + selectedDay + "." + selectedMonth
                                                + " " + selectedShiftType,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            @Override
                            public void onFailed(Exception e) {
                                Toast.makeText(WorkerRequestShift.this,
                                        "שגיאה בשליחת הבקשה", Toast.LENGTH_SHORT).show();
                                btnRequestShift.setEnabled(true);
                            }
                        });
                    }
                    @Override
                    public void onFailed(Exception e) {
                        // אם הבדיקה נכשלה, שלח בכל זאת (fail-open)
                        ShiftRequest request = new ShiftRequest(
                                worker.getId(),
                                worker.getfName() + " " + worker.getlName(),
                                dateFb, startTime, endTime, "");
                        request.setShiftType(selectedShiftType);
                        db.addShiftRequest(request, new DatabaseService.DatabaseCallback<Void>() {
                            @Override public void onCompleted(Void v) { finish(); }
                            @Override public void onFailed(Exception ex) {
                                Toast.makeText(WorkerRequestShift.this,
                                        "שגיאה בשליחת הבקשה", Toast.LENGTH_SHORT).show();
                                btnRequestShift.setEnabled(true);
                            }
                        });
                    }
                });
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(WorkerRequestShift.this,
                        "שגיאה בטעינת פרטי המשתמש", Toast.LENGTH_SHORT).show();
                btnRequestShift.setEnabled(true);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
