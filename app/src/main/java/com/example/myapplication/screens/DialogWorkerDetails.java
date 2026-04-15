package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.google.android.material.button.MaterialButton;

/**
 * מציג את כל פרטי העובד שנבחר בלחיצה ארוכה מ-ManageWorkersActivity.
 * מאפשר מחיקת המשתמש מ-Firebase לאחר אישור.
 *
 * העובד מועבר כ-Serializable דרך ה-Intent (מפתח "worker").
 * לאחר מחיקה מחזיר RESULT_OK כדי ש-ManageWorkersActivity ירענן את הרשימה.
 */
public class DialogWorkerDetails extends BaseActivity {

    public static final String EXTRA_WORKER = "worker";

    private Worker worker;

    // Views
    private TextView tvAvatar, tvFullName, tvRoleBadge;
    private TextView tvEmail, tvPhone, tvJobTitle, tvRole;
    private MaterialButton btnDeleteWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_worker_details);

        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        // Toolbar with back arrow
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Read worker from intent
        worker = (Worker) getIntent().getSerializableExtra(EXTRA_WORKER);
        if (worker == null) {
            Toast.makeText(this, "שגיאה: לא נמצאו פרטי עובד", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        populateData();
        setupDeleteButton();
    }

    // ── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvAvatar     = findViewById(R.id.tvAvatar);
        tvFullName   = findViewById(R.id.tvFullName);
        tvRoleBadge  = findViewById(R.id.tvRoleBadge);
        tvEmail      = findViewById(R.id.tvEmail);
        tvPhone      = findViewById(R.id.tvPhone);
        tvJobTitle   = findViewById(R.id.tvJobTitle);
        tvRole       = findViewById(R.id.tvRole);
        btnDeleteWorker = findViewById(R.id.btnDeleteWorker);
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private void populateData() {
        String fName = nullToEmpty(worker.getfName());
        String lName = nullToEmpty(worker.getlName());
        String fullName = (fName + " " + lName).trim();
        if (fullName.isEmpty()) fullName = "ללא שם";

        // Initials for avatar (up to 2 letters)
        String initials = "";
        if (!fName.isEmpty()) initials += fName.charAt(0);
        if (!lName.isEmpty()) initials += lName.charAt(0);
        if (initials.isEmpty()) initials = "?";

        tvAvatar.setText(initials.toUpperCase());
        tvFullName.setText(fullName);

        boolean isAdmin = worker.getIsAdmin();
        String roleLabel = isAdmin ? "מנהל" : "עובד";
        tvRoleBadge.setText(roleLabel);
        // Tint badge differently for admin vs regular worker
        if (isAdmin) {
            tvRoleBadge.setTextColor(0xFF7B4FBE);
            tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_admin);
        } else {
            tvRoleBadge.setTextColor(0xFF185FA5);
            tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_role);
        }

        tvEmail.setText(nullToFallback(worker.getEmail(), "לא צוין"));
        tvPhone.setText(nullToFallback(worker.getPhone(), "לא צוין"));
        tvJobTitle.setText(nullToFallback(worker.getJobTitle(), "לא צוין"));
        tvRole.setText(roleLabel);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void setupDeleteButton() {
        btnDeleteWorker.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showDeleteConfirmation() {
        String name = nullToEmpty(worker.getfName()) + " " + nullToEmpty(worker.getlName());
        new AlertDialog.Builder(this)
                .setTitle("מחיקת משתמש")
                .setMessage("האם למחוק את " + name.trim() + "?\nפעולה זו אינה ניתנת לביטול.")
                .setPositiveButton("מחק", (dialog, which) -> deleteWorker())
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteWorker() {
        btnDeleteWorker.setEnabled(false);
        btnDeleteWorker.setText("מוחק...");

        DatabaseService.getInstance().deleteWorker(worker.getId(),
                new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void unused) {
                        Toast.makeText(DialogWorkerDetails.this,
                                "המשתמש נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);   // signal ManageWorkersActivity to reload
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        Toast.makeText(DialogWorkerDetails.this,
                                "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnDeleteWorker.setEnabled(true);
                        btnDeleteWorker.setText("מחק משתמש");
                    }
                });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nullToFallback(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }
}
