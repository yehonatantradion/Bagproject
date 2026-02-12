package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class ManageShiftsActivity extends AppCompatActivity {

    private TextView tvSelectedDateDisplay;
    private RecyclerView rvEmployeesList;
    private String dateFromIntent;
    private WorkerAdapter adapter;
    DatabaseService databaseService;
    private List<Worker> workerList = new ArrayList<>();
    private List<Worker> workersToShift = new ArrayList<>();

    String names = "";
    TextView tvNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.workers_shift);

        tvNames = findViewById(R.id.tvWorkersShift);
        databaseService = DatabaseService.getInstance();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // אתחול הרכיבים
        rvEmployeesList = findViewById(R.id.rvEmployeesList);
        rvEmployeesList.setLayoutManager(new LinearLayoutManager(this));
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay);

        dateFromIntent = getIntent().getStringExtra("SELECTED_DATE");
        if (dateFromIntent != null) {
            tvSelectedDateDisplay.setText("ניהול משמרת לתאריך: " + dateFromIntent);
        }

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        // יצירת האדפטר עם ה-Listener
        adapter = new WorkerAdapter(workerList, (worker, position) -> {
            // קריאה לפונקציית ההוספה עם המיקום ברשימה
            addEmployeeToShift(worker, position);
        });

        rvEmployeesList.setAdapter(adapter);

        // משיכת עובדים מה-Database
        databaseService.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> allWorkers) {
                workerList.clear();
                workerList.addAll(allWorkers);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageShiftsActivity.this, "שגיאה בטעינת עובדים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addEmployeeToShift(Worker worker, int position) {
        // 1. הוספה לרשימת המשמרת
        workersToShift.add(worker);

        // 2. עדכון הטקסט למעלה
        if (names.isEmpty()) names = worker.getfName();
        else names += ", " + worker.getfName();
        tvNames.setText(names);

        // 3. הסרה מהרשימה המוצגת (כדי שלא יופיע יותר)
        workerList.remove(position);
        adapter.notifyItemRemoved(position);
        // תיקון טווח האינדקסים לאדפטר
        adapter.notifyItemRangeChanged(position, workerList.size());

        Toast.makeText(this, worker.getfName() + " נוסף למשמרת", Toast.LENGTH_SHORT).show();
    }
}