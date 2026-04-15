package com.example.myapplication.screens;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.Shift;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CurrentShiftActivity extends BaseActivity {

    private TextView tvClock, tvTodayDate, tvShiftType, tvShiftHours, tvWorkersTitle;
    private ProgressBar progressBar;
    private RecyclerView rvWorkers;
    private Button btnRefresh;

    private DatabaseService db;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final List<WorkerStatusItem> workerItems = new ArrayList<>();
    private WorkerStatusAdapter statusAdapter;

    private final String todayDate = buildTodayDate();

    private static String buildTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"));
        return sdf.format(new Date());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_shift);
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

        tvClock         = findViewById(R.id.tvClock);
        tvTodayDate     = findViewById(R.id.tvTodayDate);
        tvShiftType     = findViewById(R.id.tvShiftType);
        tvShiftHours    = findViewById(R.id.tvShiftHours);
        tvWorkersTitle  = findViewById(R.id.tvWorkersTitle);
        progressBar     = findViewById(R.id.progressBar);
        rvWorkers       = findViewById(R.id.rvWorkers);
        btnRefresh      = findViewById(R.id.btnRefresh);

        statusAdapter = new WorkerStatusAdapter(workerItems);
        rvWorkers.setLayoutManager(new LinearLayoutManager(this));
        rvWorkers.setAdapter(statusAdapter);

        tvTodayDate.setText(todayDate);

        startClock();
        updateShiftInfo();
        loadCurrentShift();

        btnRefresh.setOnClickListener(v -> loadCurrentShift());
    }

    private void startClock() {
        final SimpleDateFormat clockFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        clockFmt.setTimeZone(TimeZone.getTimeZone("Asia/Jerusalem"));
        clockHandler.post(new Runnable() {
            @Override
            public void run() {
                String time = clockFmt.format(new Date());
                if (tvClock != null) tvClock.setText(time);
                clockHandler.postDelayed(this, 1000);
            }
        });
    }

    private String getCurrentShiftType() {
        int hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jerusalem"))
                .get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 14)  return "בוקר";
        if (hour >= 14 && hour < 22) return "ערב";
        return null;
    }

    private void updateShiftInfo() {
        String type = getCurrentShiftType();
        if (type == null) {
            tvShiftType.setText("אין משמרת פעילה");
            tvShiftHours.setText("שעות פעילות: 06:00-14:00  |  14:00-22:00");
        } else if ("בוקר".equals(type)) {
            tvShiftType.setText("🌅 משמרת בוקר");
            tvShiftHours.setText("06:00 – 14:00");
        } else {
            tvShiftType.setText("🌙 משמרת ערב");
            tvShiftHours.setText("14:00 – 22:00");
        }
    }

    private void loadCurrentShift() {
        progressBar.setVisibility(View.VISIBLE);
        tvWorkersTitle.setVisibility(View.GONE);
        workerItems.clear();
        statusAdapter.notifyDataSetChanged();

        String shiftType = getCurrentShiftType();
        if (shiftType == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        final String currentType = shiftType;

        db.getTodayShifts(todayDate, new DatabaseService.DatabaseCallback<List<Shift>>() {
            @Override
            public void onCompleted(List<Shift> shifts) {
                // מצא את המשמרת המתאימה לסוג הנוכחי
                Shift active = null;
                if (shifts != null) {
                    for (Shift s : shifts) {
                        if (currentType.equals(s.getShiftType())) {
                            active = s;
                            break;
                        }
                        // fallback: אם שדה shiftType לא מולא, קח את הראשונה
                        if (active == null) active = s;
                    }
                }

                if (active == null || active.getWorkerList() == null || active.getWorkerList().isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    tvShiftType.setText("אין משמרת מתוזמנת להיום");
                    tvShiftHours.setText("");
                    return;
                }

                final List<Worker> workers = active.getWorkerList();
                tvWorkersTitle.setVisibility(View.VISIBLE);

                // הכנס עובדים ואחזר נוכחות לכל אחד
                workerItems.clear();
                for (Worker w : workers) {
                    workerItems.add(new WorkerStatusItem(w, null)); // null = טרם נטען
                }
                statusAdapter.notifyDataSetChanged();

                // טען נוכחות
                db.getAttendanceByDate(todayDate, new DatabaseService.DatabaseCallback<List<Attendance>>() {
                    @Override
                    public void onCompleted(List<Attendance> attendances) {
                        progressBar.setVisibility(View.GONE);
                        for (WorkerStatusItem item : workerItems) {
                            item.arrived = false;
                            if (attendances != null) {
                                for (Attendance att : attendances) {
                                    if (item.worker.getId() != null
                                            && item.worker.getId().equals(att.getWorkerId())
                                            && att.isConfirmed()) {
                                        item.arrived = true;
                                        break;
                                    }
                                }
                            }
                        }
                        statusAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onFailed(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        statusAdapter.notifyDataSetChanged();
                    }
                });
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacksAndMessages(null);
    }

    // ─── Data class ────────────────────────────
    static class WorkerStatusItem {
        Worker worker;
        Boolean arrived; // null=loading, true=arrived, false=not arrived

        WorkerStatusItem(Worker worker, Boolean arrived) {
            this.worker  = worker;
            this.arrived = arrived;
        }
    }

    // ─── Adapter ───────────────────────────────
    static class WorkerStatusAdapter
            extends RecyclerView.Adapter<WorkerStatusAdapter.VH> {

        private final List<WorkerStatusItem> data;

        WorkerStatusAdapter(List<WorkerStatusItem> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_shift_worker_status, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            WorkerStatusItem item = data.get(position);
            String fullName = (item.worker.getfName() != null ? item.worker.getfName() : "")
                    + " " + (item.worker.getlName() != null ? item.worker.getlName() : "");
            h.tvName.setText(fullName.trim());

            if (item.arrived == null) {
                h.tvStatus.setText("טוען...");
                h.tvStatus.setTextColor(0xFF888888);
                h.tvStatus.setBackgroundColor(0xFFF5F5F5);
            } else if (item.arrived) {
                h.tvStatus.setText("הגיע ✓");
                h.tvStatus.setTextColor(0xFF2ECC71);
                h.tvStatus.setBackgroundColor(0xFFE8F5E9);
            } else {
                h.tvStatus.setText("לא הגיע");
                h.tvStatus.setTextColor(0xFFE53935);
                h.tvStatus.setBackgroundColor(0xFFFFEBEE);
            }
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvWorkerName);
                tvStatus = v.findViewById(R.id.tvAttendanceStatus);
            }
        }
    }
}
