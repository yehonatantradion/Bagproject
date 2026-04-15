package com.example.myapplication.screens;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class Login extends BaseActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoRegister;
    private ProgressBar progressBar;

    private SharedPreferences sharedPreferences;
    public static final String myPref = "myPref";

    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPass);
        btnLogin     = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        // progressBar הוא אופציונלי – אם קיים ב-XML נשתמש בו
        progressBar  = findViewById(R.id.progressBar);

        databaseService   = DatabaseService.getInstance();
        sharedPreferences = getSharedPreferences(myPref, Context.MODE_PRIVATE);

        // מילוי אוטומטי
        String savedEmail = sharedPreferences.getString("email", "");
        String savedPass  = sharedPreferences.getString("password", "");
        if (!savedEmail.isEmpty()) etEmail.setText(savedEmail);
        if (!savedPass.isEmpty())  etPassword.setText(savedPass);

        btnLogin.setOnClickListener(view -> attemptLogin());

        btnGoRegister.setOnClickListener(view ->
                startActivity(new Intent(Login.this, Register.class)));
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("יש להזין אימייל");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("יש להזין סיסמה");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        databaseService.LoginWorker(email, password, new DatabaseService.DatabaseCallback<String>() {
            @Override
            public void onCompleted(String uid) {
                Log.d("Login", "התחברות הצליחה, uid = " + uid);

                databaseService.getWorker(uid, new DatabaseService.DatabaseCallback<Worker>() {
                    @Override
                    public void onCompleted(Worker worker) {
                        setLoading(false);

                        if (worker == null) {
                            Toast.makeText(Login.this, "משתמש לא נמצא במערכת", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // שמירת פרטים
                        sharedPreferences.edit()
                                .putString("email", email)
                                .putString("password", password)
                                .putBoolean("isAdmin", worker.isAdmin())
                                .apply();

                        Log.d("Login", "isAdmin = " + worker.isAdmin());

                        if (worker.isAdmin()) {
                            // מנהל → מסך ניהול
                            startActivity(new Intent(Login.this, adminpage.class));
                        } else {
                            // עובד → מסך עובד
                            Intent intent = new Intent(Login.this, EmployeeDashboardActivity.class);
                            intent.putExtra("worker", worker);
                            startActivity(intent);
                        }
                        finish();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        setLoading(false);
                        Log.e("Login", "שגיאה בטעינת נתוני עובד", e);
                        Toast.makeText(Login.this, "שגיאה בטעינת נתוני המשתמש", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailed(Exception e) {
                setLoading(false);
                Log.e("Login", "שגיאת התחברות", e);
                Toast.makeText(Login.this, "אימייל או סיסמה שגויים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}
