package com.example.myapplication.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

public class Register extends BaseActivity {

    private static final String TAG = "Register";

    private EditText etFname, etLname, etMail, etPhone, etPassword;
    private Button btnSubmit, btnGoLogin;
    private ProgressBar progressBar;

    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService = DatabaseService.getInstance();

        etFname    = findViewById(R.id.etFname);
        etLname    = findViewById(R.id.etLname);
        etMail     = findViewById(R.id.etEmail);
        etPhone    = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPass);
        btnSubmit  = findViewById(R.id.btnRegister);
        btnGoLogin = findViewById(R.id.btnGoLogin);
        progressBar = findViewById(R.id.progressBar);

        btnSubmit.setOnClickListener(v -> attemptRegister());

        btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(Register.this, Login.class));
            finish();
        });
    }

    private void attemptRegister() {
        String fName    = etFname.getText().toString().trim();
        String lName    = etLname.getText().toString().trim();
        String email    = etMail.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // ולידציה
        if (TextUtils.isEmpty(fName)) {
            etFname.setError("יש להזין שם פרטי");
            etFname.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(lName)) {
            etLname.setError("יש להזין שם משפחה");
            etLname.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etMail.setError("יש להזין אימייל");
            etMail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("יש להזין מספר טלפון");
            etPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("סיסמה חייבת להכיל לפחות 6 תווים");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        // כל משתמש חדש שנרשם הוא עובד רגיל (isAdmin = false)
        Worker worker = new Worker("", fName, lName, phone, email, password, "עובד", false);

        databaseService.createNewWorker(worker, new DatabaseService.DatabaseCallback<String>() {
            @Override
            public void onCompleted(String uid) {
                Log.d(TAG, "עובד נרשם בהצלחה, uid = " + uid);
                worker.setId(uid);
                setLoading(false);

                Toast.makeText(Register.this, "ברוך הבא, " + fName + "!", Toast.LENGTH_SHORT).show();

                // לאחר הרשמה → מסך עובד ישירות
                Intent intent = new Intent(Register.this, EmployeeDashboardActivity.class);
                intent.putExtra("worker", worker);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onFailed(Exception e) {
                setLoading(false);
                Log.e(TAG, "שגיאה בהרשמה", e);
                String msg = e.getMessage() != null && e.getMessage().contains("already in use")
                        ? "אימייל זה כבר רשום במערכת"
                        : "שגיאה בהרשמה, נסה שנית";
                Toast.makeText(Register.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}
