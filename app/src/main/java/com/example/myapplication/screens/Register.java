package com.example.myapplication.screens;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

public class Register extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Register";

    EditText etFname, etLname, etMail, etPhone, etPassword;
    String fName, lName, email, phone, password;
    Button btnSubmit, btnGoLogin;

    private DatabaseService databaseService;
        boolean isAdmin=false;

    SharedPreferences sharedPreferences;
    public static final String myPref = "myPref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        sharedPreferences = getSharedPreferences(myPref, Context.MODE_PRIVATE);

        databaseService = DatabaseService.getInstance();


        etFname = findViewById(R.id.etFname);
        etLname = findViewById(R.id.etLname);
        etMail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPass);
        btnSubmit = findViewById(R.id.btnRegister);
        btnGoLogin = findViewById(R.id.btnGoLogin);

        btnSubmit.setOnClickListener(this);

        // כפתור "כבר יש לך חשבון? התחבר"
        btnGoLogin.setOnClickListener(view -> {
            Intent intent = new Intent(Register.this, Login.class);
            startActivity(intent);
            finish();
        });

        // ----------------------------
        // טעינת אימייל + סיסמה שנשמרו
        // ----------------------------
       String savedEmail = sharedPreferences.getString("email", "");
      String savedPass = sharedPreferences.getString("password", "");

        if (!savedEmail.isEmpty()) {
            etMail.setText(savedEmail);
        }

        if (!savedPass.isEmpty()) {
            etPassword.setText(savedPass);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == btnSubmit.getId()) {

            Log.d(TAG, "onClick: Register button clicked");

            fName = etFname.getText().toString();
            lName = etLname.getText().toString();
            email = etMail.getText().toString();
            phone = etPhone.getText().toString();
            password = etPassword.getText().toString();

            registerWorker(fName, lName, phone, email, password);
        }
    }

    private void registerWorker(String fname, String lname, String phone, String email, String password) {
        Log.d(TAG, "registerWorker: Registering worker...");
        Worker worker = new Worker("uid", fname, lname, phone, email, password, "worker", false);
        createWorkerInDatabase(worker);
    }

    private void createWorkerInDatabase(Worker worker) {

        databaseService.createNewWorker(worker, new DatabaseService.DatabaseCallback<String>() {

            @Override
            public void onCompleted(String wid) {
                Log.d(TAG, "createWorkerInDatabase: Worker created successfully");

                worker.setId(wid);

                // Save credentials
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("email", email);
                editor.putString("password", password);
                editor.putBoolean("isAdmin", worker.isAdmin());
                editor.apply();

                Log.d(TAG, "createWorkerInDatabase: Redirecting to MainActivity");

                Intent intent = new Intent(Register.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onFailed(Exception e) {
                Log.e(TAG, "createWorkerInDatabase: Failed to create worker", e);
                Toast.makeText(Register.this, "Failed to register worker", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
