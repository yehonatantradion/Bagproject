package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.NoShowAlertReceiver;
import com.google.firebase.auth.FirebaseUser;

public class adminpage extends BaseActivity {

    private Button btnManageWorkers;
    private Button btnManageShifts;
    private Button btnAddShift;
    private Button btnInviteWorkers;
    private Button btnTodayAttendance;
    private Button btnSettings;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adminpage);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        initListeners();
        // ערוץ התראות אי-הגעה — נוצר כאן כדי שיהיה מוכן לפני שהמנהל יקבל התראות
        NoShowAlertReceiver.createChannel(this);
    }

    private void initViews() {
        btnManageWorkers   = findViewById(R.id.btnManageWorkers);
        btnManageShifts    = findViewById(R.id.btnManageShifts);
        btnAddShift        = findViewById(R.id.btnAddShift);
        btnInviteWorkers   = findViewById(R.id.btnInviteWorkers);
        btnTodayAttendance = findViewById(R.id.btnTodayAttendance);
        btnSettings        = findViewById(R.id.btnSettings);
        btnLogout          = findViewById(R.id.btnLogout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // בדיקת הרשאה: רק מנהל מורשה להיכנס לכאן
        FirebaseUser user = DatabaseService.getInstance().getAuth().getCurrentUser();
        if (user == null) {
            redirectToLogin();
            return;
        }
        DatabaseService.getInstance().getWorker(user.getUid(), new DatabaseService.DatabaseCallback<Worker>() {
            @Override
            public void onCompleted(Worker worker) {
                if (worker == null || !worker.isAdmin()) {
                    Toast.makeText(adminpage.this, "אין הרשאת גישה למסך זה", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                }
            }
            @Override
            public void onFailed(Exception e) {
                Log.e("adminpage", "שגיאה בבדיקת הרשאות", e);
                redirectToLogin();
            }
        });
    }

    private void redirectToLogin() {
        DatabaseService.getInstance().getAuth().signOut();
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initListeners() {
        btnManageWorkers.setOnClickListener(v ->
                startActivity(new Intent(this, ManageWorkersActivity.class)));

        btnManageShifts.setOnClickListener(v ->
                startActivity(new Intent(this, CurrentShiftActivity.class)));

        btnAddShift.setOnClickListener(v ->
                startActivity(new Intent(this, AddShift.class)));

        // הזמנת עובדים למשמרת
        btnInviteWorkers.setOnClickListener(v ->
                startActivity(new Intent(this, InviteWorkersActivity.class)));

        btnTodayAttendance.setOnClickListener(v ->
                startActivity(new Intent(this, TodayAttendanceActivity.class)));

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            DatabaseService.getInstance().getAuth().signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
