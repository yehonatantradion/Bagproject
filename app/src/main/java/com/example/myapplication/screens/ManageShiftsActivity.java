package com.example.myapplication.screens;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.adapters.SelectedWorkerAdapter;
import com.example.myapplication.adapters.WorkerAdapter;
import com.example.myapplication.model.ShiftRequest;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.NoShowAlertReceiver;
import com.example.myapplication.services.Shift;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageShiftsActivity extends BaseActivity {

    private TextView tvSelectedDateDisplay, tvEmptyMessage;
    private RecyclerView rvEmployeesList;
    private RecyclerView rvSelectedWorkers;
    private Button btnSave, btnBack;
    private DatabaseService databaseService;

    private final List<Worker> availableWorkers = new ArrayList<>();
    private final List<Worker> workersToShift   = new ArrayList<>();

    private WorkerAdapter availableAdapter;
    private SelectedWorkerAdapter selectedAdapter;

    private String dateFromIntent;
    private String shiftTypeFromIntent;
    private Shift  existingShift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.workers_shift);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        databaseService = DatabaseService.getInstance();

        rvEmployeesList       = findViewById(R.id.rvEmployeesList);
        rvSelectedWorkers     = findViewById(R.id.rvSelectedWorkers);
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay);
        tvEmptyMessage        = findViewById(R.id.tvEmptyMessage);
        btnSave               = findViewById(R.id.btnSaveShift);
        btnBack               = findViewById(R.id.btnBack);

        rvEmployeesList.setLayoutManager(new LinearLayoutManager(this));
        rvSelectedWorkers.setLayoutManager(new LinearLayoutManager(this));

        dateFromIntent      = getIntent().getStringExtra("SELECTED_DATE");
        shiftTypeFromIntent = getIntent().getStringExtra("SELECTED_SHIFT_TYPE");

        if (shiftTypeFromIntent == null) shiftTypeFromIntent = "";

        if (dateFromIntent != null) {
            String label = formatDateLabel(dateFromIntent) + "  " + shiftTypeFromIntent;
            tvSelectedDateDisplay.setText("משמרת: " + label);
        }

        setupAdapters();

        // שלב 1: טען עובדים שכבר שובצו, ואחר כך עובדים שביקשו
        loadExistingShiftThenRequests();

        // יצירת ערוץ התראות אי-הגעה (למנהל)
        NoShowAlertReceiver.createChannel(this);

        btnSave.setOnClickListener(v -> saveShift());
        btnBack.setOnClickListener(v -> finish());
    }

    private String formatDateLabel(String raw) {
        try {
            String[] p = raw.split("/");
            return Integer.parseInt(p[0]) + "." + Integer.parseInt(p[1]);
        } catch (Exception e) { return raw; }
    }

    private void setupAdapters() {
        availableAdapter = new WorkerAdapter(availableWorkers, (worker, position) ->
                moveWorkerToShift(worker));
        rvEmployeesList.setAdapter(availableAdapter);

        selectedAdapter = new SelectedWorkerAdapter(workersToShift, (worker, position) ->
                removeWorkerFromShift(worker));
        rvSelectedWorkers.setAdapter(selectedAdapter);
    }

    // ── שלב 1: טען משמרת קיימת → שלב 2: טען מבקשים ─────────────────────────
    private void loadExistingShiftThenRequests() {
        if (dateFromIntent == null) return;
        tvEmptyMessage.setVisibility(View.GONE);

        databaseService.getTodayShifts(dateFromIntent,
                new DatabaseService.DatabaseCallback<List<Shift>>() {
            @Override
            public void onCompleted(List<Shift> shifts) {
                // מצא את המשמרת המתאימה לסוג
                if (shifts != null) {
                    for (Shift s : shifts) {
                        if (shiftTypeFromIntent.equals(s.getShiftType())) {
                            existingShift = s;
                            break;
                        }
                    }
                }

                // אם יש משמרת קיימת — מלא את רשימת השובצים
                workersToShift.clear();
                if (existingShift != null && existingShift.getWorkerList() != null) {
                    workersToShift.addAll(existingShift.getWorkerList());
                }
                selectedAdapter.notifyDataSetChanged();

                // שלב 2: טען את המבקשים
                loadRequestingWorkers();
            }
            @Override
            public void onFailed(Exception e) {
                // גם אם נכשל — המשך לטעינת המבקשים
                loadRequestingWorkers();
            }
        });
    }

    // ── שלב 2: רק עובדים שביקשו את המשמרת ──────────────────────────────────
    private void loadRequestingWorkers() {
        databaseService.getShiftRequestsByDateAndType(dateFromIntent, shiftTypeFromIntent,
                new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> requests) {
                availableWorkers.clear();
                if (requests != null) {
                    for (ShiftRequest req : requests) {
                        // דלג על עובדים שכבר שובצו
                        if (indexById(workersToShift, req.getWorkerId()) != -1) continue;

                        Worker w = new Worker();
                        w.setId(req.getWorkerId());
                        String fullName = req.getWorkerName() != null ? req.getWorkerName() : "";
                        String[] parts  = fullName.split(" ", 2);
                        w.setfName(parts.length > 0 ? parts[0] : fullName);
                        w.setlName(parts.length > 1 ? parts[1] : "");
                        w.setEmail("");
                        availableWorkers.add(w);
                    }
                }
                availableAdapter.notifyDataSetChanged();

                tvEmptyMessage.setVisibility(
                        availableWorkers.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageShiftsActivity.this,
                        "שגיאה בטעינת עובדים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveWorkerToShift(Worker worker) {
        workersToShift.add(worker);
        selectedAdapter.notifyItemInserted(workersToShift.size() - 1);

        int idx = indexById(availableWorkers, worker.getId());
        if (idx != -1) {
            availableWorkers.remove(idx);
            availableAdapter.notifyItemRemoved(idx);
        }
        tvEmptyMessage.setVisibility(availableWorkers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void removeWorkerFromShift(Worker worker) {
        availableWorkers.add(0, worker);
        availableAdapter.notifyItemInserted(0);
        tvEmptyMessage.setVisibility(View.GONE);

        int idx = indexById(workersToShift, worker.getId());
        if (idx != -1) {
            workersToShift.remove(idx);
            selectedAdapter.notifyItemRemoved(idx);
        }
    }

    private int indexById(List<Worker> list, String id) {
        if (id == null) return -1;
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).getId())) return i;
        }
        return -1;
    }

    private void saveShift() {
        if (workersToShift.isEmpty()) {
            Toast.makeText(this, "יש לבחור לפחות עובד אחד", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSave.setEnabled(false);

        String id = (existingShift != null)
                ? existingShift.getId()
                : databaseService.generateShiftId();

        Shift shift = new Shift(id, dateFromIntent, new Date(), null,
                new ArrayList<>(workersToShift), "Planned");
        shift.setShiftType(shiftTypeFromIntent);

        databaseService.createNewShift(shift, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void v) {
                markRequestsAsAssigned();
                saveWorkerShifts();
                Toast.makeText(ManageShiftsActivity.this, "המשמרת נשמרה!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageShiftsActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
            }
        });
    }

    private void saveWorkerShifts() {
        String startTime = "בוקר".equals(shiftTypeFromIntent) ? "06:00" : "14:00";
        String endTime   = "בוקר".equals(shiftTypeFromIntent) ? "14:00" : "22:00";
        for (Worker worker : workersToShift) {
            ShiftRequest sr = new ShiftRequest(
                    worker.getId(),
                    worker.getfName() + " " + worker.getlName(),
                    dateFromIntent, startTime, endTime, "");
            sr.setShiftType(shiftTypeFromIntent);
            sr.setStatus("assigned");
            String reqId = databaseService.generateShiftRequestId();
            sr.setRequestId(reqId);
            databaseService.saveWorkerShift(worker.getId(), sr, null);

            // קבע התראת אי-הגעה למנהל: 30 דקות אחרי תחילת המשמרת
            scheduleNoShowAlarm(worker, dateFromIntent, shiftTypeFromIntent, startTime);
        }
    }

    /**
     * מתזמן התראה למנהל אם עובד לא מאשר הגעה 30 דקות אחרי תחילת המשמרת.
     */
    private void scheduleNoShowAlarm(Worker worker, String date, String shiftType, String startTime) {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date shiftStart = sdf.parse(date + " " + startTime);
            if (shiftStart == null) return;

            // אזעקה 30 דקות אחרי תחילת המשמרת
            long alertTime = shiftStart.getTime() + (30 * 60 * 1000L);
            if (alertTime <= System.currentTimeMillis()) return; // המשמרת כבר עברה

            Intent intent = new Intent(this, NoShowAlertReceiver.class);
            intent.putExtra("workerId",   worker.getId());
            intent.putExtra("workerName", worker.getfName() + " " + worker.getlName());
            intent.putExtra("shiftDate",  date);
            intent.putExtra("shiftType",  shiftType);

            int requestCode = Math.abs((worker.getId() + date + shiftType).hashCode());
            PendingIntent pi = PendingIntent.getBroadcast(
                    this, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTime, pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTime, pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alertTime, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, alertTime, pi);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void markRequestsAsAssigned() {
        databaseService.getShiftRequestsByDateAndType(dateFromIntent, shiftTypeFromIntent,
                new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> requests) {
                if (requests == null) return;
                for (ShiftRequest req : requests) {
                    for (Worker assigned : workersToShift) {
                        if (assigned.getId() != null
                                && assigned.getId().equals(req.getWorkerId())) {
                            req.setStatus("assigned");
                            databaseService.updateShiftRequest(req, null);
                        }
                    }
                }
            }
            @Override public void onFailed(Exception e) {}
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
