package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.adapters.AttendanceAdapter;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.ShiftRequest;
import com.example.myapplication.services.DatabaseService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodayAttendanceActivity extends BaseActivity {

    private RecyclerView rvAttendance;
    private ProgressBar progressBar;
    private TextView tvNoShifts, tvTodayDate, tvArrivedCount, tvMissingCount, tvTotalCount;
    private AttendanceAdapter adapter;
    private final ArrayList<AttendanceAdapter.AttendanceItem> attendanceList = new ArrayList<>();
    private DatabaseService db;

    private final String todayDate =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_attendance);
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
        initViews();
        loadAttendance();
    }

    private void initViews() {
        rvAttendance   = findViewById(R.id.rvAttendance);
        progressBar    = findViewById(R.id.progressBar);
        tvNoShifts     = findViewById(R.id.tvNoShifts);
        tvTodayDate    = findViewById(R.id.tvTodayDate);
        tvArrivedCount = findViewById(R.id.tvArrivedCount);
        tvMissingCount = findViewById(R.id.tvMissingCount);
        tvTotalCount   = findViewById(R.id.tvTotalCount);

        tvTodayDate.setText("תאריך: " + todayDate);

        adapter = new AttendanceAdapter(attendanceList);
        rvAttendance.setLayoutManager(new LinearLayoutManager(this));
        rvAttendance.setAdapter(adapter);
    }

    private void loadAttendance() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoShifts.setVisibility(View.GONE);

        db.getShiftsForDate(todayDate, new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> expected) {
                if (expected == null || expected.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    tvNoShifts.setVisibility(View.VISIBLE);
                    updateSummary(0, 0);
                    return;
                }

                db.getAttendanceByDate(todayDate, new DatabaseService.DatabaseCallback<List<Attendance>>() {
                    @Override
                    public void onCompleted(List<Attendance> arrivals) {
                        progressBar.setVisibility(View.GONE);

                        Map<String, Attendance> arrivedMap = new HashMap<>();
                        if (arrivals != null) {
                            for (Attendance a : arrivals) arrivedMap.put(a.getWorkerId(), a);
                        }

                        attendanceList.clear();
                        int arrivedCount = 0;

                        for (ShiftRequest shift : expected) {
                            Attendance att = arrivedMap.get(shift.getWorkerId());
                            boolean arrived = (att != null && att.isConfirmed());
                            long checkInTime = arrived ? att.getCheckInTime() : 0;
                            String name = shift.getWorkerName() != null
                                    ? shift.getWorkerName() : shift.getWorkerId();
                            attendanceList.add(new AttendanceAdapter.AttendanceItem(name, arrived, checkInTime));
                            if (arrived) arrivedCount++;
                        }

                        updateSummary(arrivedCount, expected.size() - arrivedCount);
                        adapter.notifyDataSetChanged();
                        if (attendanceList.isEmpty()) tvNoShifts.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onFailed(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TodayAttendanceActivity.this,
                                "שגיאה בטעינת נוכחות", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TodayAttendanceActivity.this,
                        "שגיאה בטעינת משמרות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummary(int arrived, int missing) {
        tvArrivedCount.setText(String.valueOf(arrived));
        tvMissingCount.setText(String.valueOf(missing));
        tvTotalCount.setText(String.valueOf(arrived + missing));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAttendance();
    }
}
