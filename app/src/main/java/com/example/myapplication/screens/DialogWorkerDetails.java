package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
 * מציג ומאפשר עריכה של כל פרטי העובד שנבחר בלחיצה ארוכה מ-ManageWorkersActivity.
 * ניתן לשנות: שם פרטי, שם משפחה, טלפון, תפקיד, הרשאה (עובד/מנהל).
 * האימייל מוצג בלבד (לא ניתן לשינוי).
 * כפתור מחיקה מסיר את העובד לאחר אישור.
 *
 * מחזיר RESULT_OK לאחר שמירה/מחיקה כדי ש-ManageWorkersActivity ירענן את הרשימה.
 */
public class DialogWorkerDetails extends BaseActivity {

    public static final String EXTRA_WORKER = "worker";

    private Worker worker;

    // Header
    private TextView tvAvatar, tvFullName, tvRoleBadge;

    // Editable fields
    private EditText etFName, etLName, etPhone, etJobTitle, etEmail;
    private RadioGroup rgRole;
    private RadioButton rbWorker, rbAdmin;

    // Action buttons
    private MaterialButton btnSave, btnDeleteWorker;

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        worker = (Worker) getIntent().getSerializableExtra(EXTRA_WORKER);
        if (worker == null) {
            Toast.makeText(this, "שגיאה: לא נמצאו פרטי עובד", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        populateFields();
        setupListeners();
    }

    // ── Bind ─────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvAvatar    = findViewById(R.id.tvAvatar);
        tvFullName  = findViewById(R.id.tvFullName);
        tvRoleBadge = findViewById(R.id.tvRoleBadge);

        etFName    = findViewById(R.id.etFName);
        etLName    = findViewById(R.id.etLName);
        etPhone    = findViewById(R.id.etPhone);
        etJobTitle = findViewById(R.id.etJobTitle);
        etEmail    = findViewById(R.id.etEmail);

        rgRole   = findViewById(R.id.rgRole);
        rbWorker = findViewById(R.id.rbWorker);
        rbAdmin  = findViewById(R.id.rbAdmin);

        btnSave         = findViewById(R.id.btnSave);
        btnDeleteWorker = findViewById(R.id.btnDeleteWorker);
    }

    // ── Populate ──────────────────────────────────────────────────────────────

    private void populateFields() {
        String fName = nullToEmpty(worker.getfName());
        String lName = nullToEmpty(worker.getlName());

        // Avatar initials
        String initials = "";
        if (!fName.isEmpty()) initials += fName.charAt(0);
        if (!lName.isEmpty()) initials += lName.charAt(0);
        tvAvatar.setText(initials.isEmpty() ? "?" : initials.toUpperCase());

        // Header name + badge
        String fullName = (fName + " " + lName).trim();
        tvFullName.setText(fullName.isEmpty() ? "ללא שם" : fullName);
        refreshBadge(worker.getIsAdmin());

        // Fill editable fields
        etFName.setText(fName);
        etLName.setText(lName);
        etPhone.setText(nullToEmpty(worker.getPhone()));
        etJobTitle.setText(nullToEmpty(worker.getJobTitle()));
        etEmail.setText(nullToEmpty(worker.getEmail()));

        // Role radio
        if (worker.getIsAdmin()) {
            rbAdmin.setChecked(true);
        } else {
            rbWorker.setChecked(true);
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setupListeners() {
        // Live-update header as role changes
        rgRole.setOnCheckedChangeListener((group, checkedId) ->
                refreshBadge(checkedId == R.id.rbAdmin));

        btnSave.setOnClickListener(v -> saveChanges());
        btnDeleteWorker.setOnClickListener(v -> showDeleteConfirmation());
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveChanges() {
        String fName    = etFName.getText().toString().trim();
        String lName    = etLName.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String jobTitle = etJobTitle.getText().toString().trim();
        boolean isAdmin = rbAdmin.isChecked();

        if (fName.isEmpty()) {
            etFName.setError("שדה חובה");
            etFName.requestFocus();
            return;
        }

        // Apply changes to worker object
        worker.setfName(fName);
        worker.setlName(lName);
        worker.setPhone(phone);
        worker.setJobTitle(jobTitle);
        worker.setIsAdmin(isAdmin);

        btnSave.setEnabled(false);
        btnSave.setText("שומר...");

        DatabaseService.getInstance().updateWorker(worker, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(DialogWorkerDetails.this,
                        "הפרטים עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                // Refresh header
                String full = (fName + " " + lName).trim();
                tvFullName.setText(full.isEmpty() ? "ללא שם" : full);
                String ini = "";
                if (!fName.isEmpty()) ini += fName.charAt(0);
                if (!lName.isEmpty()) ini += lName.charAt(0);
                tvAvatar.setText(ini.isEmpty() ? "?" : ini.toUpperCase());
                refreshBadge(isAdmin);

                btnSave.setEnabled(true);
                btnSave.setText("שמור שינויים");
                setResult(RESULT_OK);
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(DialogWorkerDetails.this,
                        "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                btnSave.setEnabled(true);
                btnSave.setText("שמור שינויים");
            }
        });
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    private void showDeleteConfirmation() {
        String name = (nullToEmpty(worker.getfName()) + " " + nullToEmpty(worker.getlName())).trim();
        new AlertDialog.Builder(this)
                .setTitle("מחיקת משתמש")
                .setMessage("האם למחוק את " + (name.isEmpty() ? "העובד" : name) + "?\nפעולה זו אינה ניתנת לביטול.")
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
                        setResult(RESULT_OK);
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshBadge(boolean isAdmin) {
        if (isAdmin) {
            tvRoleBadge.setText("מנהל");
            tvRoleBadge.setTextColor(0xFF7B4FBE);
            tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_admin);
        } else {
            tvRoleBadge.setText("עובד");
            tvRoleBadge.setTextColor(0xFF185FA5);
            tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_role);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
