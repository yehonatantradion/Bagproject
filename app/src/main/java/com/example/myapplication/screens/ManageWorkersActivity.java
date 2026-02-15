package com.example.myapplication.screens;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapters.WorkerAdapter;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import java.util.ArrayList;
import java.util.List;

public class ManageWorkersActivity extends AppCompatActivity {

    private RecyclerView rvManagers, rvWorkers;
    private DatabaseService dbService;
    private WorkerAdapter managersAdapter, workersAdapter;
    private List<Worker> managersList = new ArrayList<>();
    private List<Worker> workersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_workers);

        dbService = DatabaseService.getInstance();

        // חיבור ל-XML
        rvManagers = findViewById(R.id.rvManagers);
        rvWorkers = findViewById(R.id.rvWorkers);

        // הגדרת תצוגה
        rvManagers.setLayoutManager(new LinearLayoutManager(this));
        rvWorkers.setLayoutManager(new LinearLayoutManager(this));

        // יצירת האדפטרים
        // הערה: נשתמש ב-Interface של האדפטר כדי לזהות לחיצה
        managersAdapter = new WorkerAdapter(managersList, (worker, position) -> {
            showEditDialog(worker); // לחיצה פותחת עריכה
        });

        workersAdapter = new WorkerAdapter(workersList, (worker, position) -> {
            showEditDialog(worker); // לחיצה פותחת עריכה
        });

        rvManagers.setAdapter(managersAdapter);
        rvWorkers.setAdapter(workersAdapter);

        // טעינת נתונים
        loadWorkersData();
    }

    private void loadWorkersData() {
        dbService.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> allWorkers) {
                managersList.clear();
                workersList.clear();

                if (allWorkers != null) {
                    for (Worker w : allWorkers) {
                        // מיון לפי שדה isAdmin (או המקביל אצלך במודל)
                        if (w.isAdmin()) {
                            managersList.add(w);
                        } else {
                            workersList.add(w);
                        }
                    }
                }
                // רענון המסך
                managersAdapter.notifyDataSetChanged();
                workersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageWorkersActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog(Worker worker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_dialog_worker_details, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // חיבור שדות הדיאלוג
        EditText etName = view.findViewById(R.id.etWorkerName);
        EditText etPhone = view.findViewById(R.id.etWorkerPhone);
        EditText etEmail = view.findViewById(R.id.etWorkerEmail);
        RadioGroup rgRole = view.findViewById(R.id.rgRole);
        RadioButton rbManager = view.findViewById(R.id.rbManager);
        RadioButton rbWorker = view.findViewById(R.id.rbWorker);
        Button btnSave = view.findViewById(R.id.btnSaveChanges);

        // מילוי נתונים קיימים
        etName.setText(worker.getfName());
        etPhone.setText(worker.getPhone());
        etEmail.setText(worker.getEmail());

        if (worker.isAdmin()) {
            rbManager.setChecked(true);
        } else {
            rbWorker.setChecked(true);
        }

        // שמירה
        btnSave.setOnClickListener(v -> {
            // עדכון האובייקט המקומי
            worker.setfName(etName.getText().toString());
            worker.setPhone(etPhone.getText().toString());

            // עדכון סטטוס מנהל
            boolean isAdmin = rbManager.isChecked();
            worker.setAdmin(isAdmin);

            // שליחה ל-Firebase
            dbService.updateWorker(worker, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    Toast.makeText(ManageWorkersActivity.this, "הפרטים עודכנו!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadWorkersData(); // טעינה מחדש כדי לסדר את הרשימות
                }

                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(ManageWorkersActivity.this, "שגיאה בעדכון", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}