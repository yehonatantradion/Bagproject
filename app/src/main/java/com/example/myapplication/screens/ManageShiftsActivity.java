package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.adapters.SelectedWorkerAdapter;
import com.example.myapplication.adapters.WorkerAdapter;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.Shift;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ManageShiftsActivity extends AppCompatActivity {

    private TextView tvSelectedDateDisplay;
    private RecyclerView rvEmployeesList; // רשימת זמינים
    private RecyclerView rvSelectedWorkers; // רשימת נבחרים (חדש)
    private Button btnSave;
    private DatabaseService databaseService;

    // רשימות נתונים
    private List<Worker> allWorkersList = new ArrayList<>();
    private List<Worker> workersToShift = new ArrayList<>();

    // אדפטרים
    private WorkerAdapter availableAdapter;
    private SelectedWorkerAdapter selectedAdapter; // האדפטר החדש

    private String dateFromIntent;
    private Shift existingShift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.workers_shift);

        databaseService = DatabaseService.getInstance();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // אתחול Views
        rvEmployeesList = findViewById(R.id.rvEmployeesList);
        rvSelectedWorkers = findViewById(R.id.rvSelectedWorkers);
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay);
        btnSave = findViewById(R.id.btnSaveShift);

        // הגדרת Layout Managers
        rvEmployeesList.setLayoutManager(new LinearLayoutManager(this));
        // רשימה אופקית לנבחרים
        rvSelectedWorkers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        dateFromIntent = getIntent().getStringExtra("SELECTED_DATE");
        if (dateFromIntent != null) {
            tvSelectedDateDisplay.setText("ניהול משמרת: " + dateFromIntent);
            checkAndLoadExistingShift(dateFromIntent);
        } else {
            setupAdapters();
            loadAllWorkers();
        }

        btnSave.setOnClickListener(v -> saveShift());
    }

    private void setupAdapters() {
        // 1. אדפטר לעובדים זמינים (לחיצה = הוספה למשמרת)
        availableAdapter = new WorkerAdapter(allWorkersList, (worker, position) -> {
            moveWorkerToShift(worker);
        });
        rvEmployeesList.setAdapter(availableAdapter);

        // 2. אדפטר לעובדים נבחרים (לחיצה = הסרה מהמשמרת)
        selectedAdapter = new SelectedWorkerAdapter(workersToShift, (worker, position) -> {
            removeWorkerFromShift(worker);
        });
        rvSelectedWorkers.setAdapter(selectedAdapter);
    }

    // פונקציה להעברת עובד ל"נבחרים"
    private void moveWorkerToShift(Worker worker) {
        // הוספה לרשימת הנבחרים
        workersToShift.add(worker);
        selectedAdapter.notifyItemInserted(workersToShift.size() - 1);
        rvSelectedWorkers.smoothScrollToPosition(workersToShift.size() - 1); // גלילה אוטומטית לסוף

        // הסרה מרשימת הזמינים
        int index = -1;
        for (int i = 0; i < allWorkersList.size(); i++) {
            if (allWorkersList.get(i).getId().equals(worker.getId())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            allWorkersList.remove(index);
            availableAdapter.notifyItemRemoved(index);
        }
    }

    // פונקציה להחזרת עובד ל"זמינים" (מחיקה)
    private void removeWorkerFromShift(Worker worker) {
        // הוספה חזרה לרשימת הזמינים
        allWorkersList.add(worker);
        availableAdapter.notifyItemInserted(allWorkersList.size() - 1);

        // הסרה מרשימת הנבחרים
        int index = -1;
        for (int i = 0; i < workersToShift.size(); i++) {
            if (workersToShift.get(i).getId().equals(worker.getId())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            workersToShift.remove(index);
            selectedAdapter.notifyItemRemoved(index);
        }
    }

    private void checkAndLoadExistingShift(String dateStr) {
        databaseService.getShiftByDate(dateStr, new DatabaseService.DatabaseCallback<Shift>() {
            @Override
            public void onCompleted(Shift shift) {
                existingShift = shift;
                if (existingShift != null) {
                    tvSelectedDateDisplay.setText("עריכת משמרת: " + dateStr);
                    if (existingShift.getWorkerList() != null) {
                        workersToShift.clear();
                        workersToShift.addAll(existingShift.getWorkerList());
                    }
                }

                // אתחול האדפטרים והנתונים רק אחרי שקיבלנו תשובה מהשרת
                setupAdapters();
                loadAllWorkers();
            }

            @Override
            public void onFailed(Exception e) {
                setupAdapters();
                loadAllWorkers();
            }
        });
    }

    private void loadAllWorkers() {
        databaseService.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> workers) {
                allWorkersList.clear();
                if (workers != null) {
                    for (Worker w : workers) {
                        // סינון: הוסף לרשימה הכללית רק אם הוא לא נמצא כבר ברשימת הנבחרים
                        boolean isAlreadySelected = false;
                        for (Worker selected : workersToShift) {
                            if (selected.getId() != null && w.getId() != null && selected.getId().equals(w.getId())) {
                                isAlreadySelected = true;
                                break;
                            }
                        }
                        if (!isAlreadySelected) {
                            allWorkersList.add(w);
                        }
                    }
                }
                availableAdapter.notifyDataSetChanged();
                selectedAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageShiftsActivity.this, "שגיאה בטעינת עובדים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveShift() {
        if (workersToShift.isEmpty()) {
            Toast.makeText(this, "אנא בחר לפחות עובד אחד", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = (existingShift != null) ? existingShift.getId() : databaseService.generateShiftId();
        Shift shift = new Shift(id, dateFromIntent, new Date(), null, new ArrayList<>(workersToShift), "Planned");

        databaseService.createNewShift(shift, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(ManageShiftsActivity.this, "המשמרת נשמרה בהצלחה!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageShiftsActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
            }
        });
    }
}