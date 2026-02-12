package com.example.myapplication.screens;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoRegister;

    private SharedPreferences sharedPreferences;
    public static final String myPref = "myPref";

    private String savedEmail;
    private String savedPassword;

    private DatabaseService databaseService;
    private boolean isAdmin=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);

        // Database service
        databaseService = DatabaseService.getInstance();

        // SharedPreferences
        sharedPreferences = getSharedPreferences(myPref, Context.MODE_PRIVATE);

        // Load saved credentials
        savedEmail = sharedPreferences.getString("email", "");
        savedPassword = sharedPreferences.getString("password", "");
        isAdmin= sharedPreferences.getBoolean("isAdmin", false);

        // Auto-fill if saved
        if (!savedEmail.isEmpty()) {
            etEmail.setText(savedEmail);
        }
        if (!savedPassword.isEmpty()) {
            etPassword.setText(savedPassword);
        }

        // Login button click
        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }

            // *** התחברות דרך Firebase (DatabaseService) ***
            databaseService.LoginWorker(email, password, new DatabaseService.DatabaseCallback<String>() {
                @Override
                public void onCompleted(String uid) {
                    // לוג למעקב
                    Log.d("Login", "LoginWorker success, uid = " + uid);

                    // אחרי התחברות – נטען את ה-Worker מה־DB
                    databaseService.getWorker(uid, new DatabaseService.DatabaseCallback<Worker>() {
                        @Override
                        public void onCompleted(Worker worker) {
                            if (worker == null) {
                                Toast.makeText(Login.this, "User not found", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // נשמור אימייל+סיסמה לאוטו-פיל
                            sharedPreferences.edit()
                                    .putString("email", email)
                                    .putString("password", password)
                                    .putBoolean("isAdmin", isAdmin)
                                    .apply();

                            // בדיקת אדמין – כאן הבעיה שהייתה לך
                            boolean isAdmin = worker.isAdmin();

                            Log.d("Login", "worker email = " + worker.getEmail());
                            Log.d("Login", "worker isAdmin from DB = " + isAdmin);

                            // טוסט כדי שתראה בעין מה חוזר:
                            Toast.makeText(Login.this, "isAdmin = " + isAdmin, Toast.LENGTH_SHORT).show();

                            if (isAdmin) {
                                // 🟢 אדמין → דף ניהול
                                // תחליף AdminActivity בשם האקטיביטי של דף הניהול שלך
                                Intent intent = new Intent(Login.this, adminpage.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // 🔵 רגיל → דף ראשי
                                Intent intent = new Intent(Login.this, SettingsActivity.class);
                                finish();
                            }
                        }

                        @Override
                        public void onFailed(Exception e) {
                            Log.e("Login", "Failed to load worker", e);
                            Toast.makeText(Login.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailed(Exception e) {
                    Log.e("Login", "LoginWorker failed", e);
                    Toast.makeText(Login.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Register button click
        btnGoRegister.setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });
    }
}
