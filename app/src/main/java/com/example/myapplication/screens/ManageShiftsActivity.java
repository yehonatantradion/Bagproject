package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Shift;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import java.util.ArrayList;
import java.util.List;

public class ManageShiftsActivity extends AppCompatActivity {

    private TextView tvSelectedDateDisplay, tvNames;
    private RecyclerView rvEmployeesList;
    private Button btnSave;
    private String dateFromIntent;
    private WorkerAdapter adapter;
    private DatabaseService databaseService;
    private List<Worker> workerList = new ArrayList<>();
    private List<Worker> workersToShift = new ArrayList<>();
    private String names = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.workers_shift);

        databaseService = DatabaseService.getInstance();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        tvNames = findViewById(R.id.tvWorkersShift);
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay);
        rvEmployeesList = findViewById(R.id.rvEmployeesList);
        btnSave = findViewById(R.id.button);

        rvEmployeesList.setLayoutManager(new LinearLayoutManager(this));

        dateFromIntent = getIntent().getStringExtra("SELECTED_DATE");
        if (dateFromIntent != null) {
            tvSelectedDateDisplay.setText("ניהול משמרת לתאריך: " + dateFromIntent);
        }

        setupRecyclerView();

        btnSave.setOnClickListener(v -> saveShift());
    }

    private void setupRecyclerView() {
        adapter = new WorkerAdapter(workerList, (worker, position) -> {
            // הוספת עובד לרשימת הנבחרים
            workersToShift.add(worker);

            // עדכון התצוגה של השמות למעלה
            if (names.isEmpty()) names = worker.getfName();
            else names += ", " + worker.getfName();
            tvNames.setText(names);

            // הסרה מהרשימה הכללית כדי שלא ייבחר פעמיים
            workerList.remove(position);
            adapter.notifyItemRemoved(position);
        });
        rvEmployeesList.setAdapter(adapter);

        // טעינת עובדים מהדאטאבייס
        databaseService.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> allWorkers) {
                workerList.clear();
                workerList.addAll(allWorkers);
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageShiftsActivity.this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveShift() {
        if (workersToShift.isEmpty()) {
            Toast.makeText(this, "בחר לפחות עובד אחד", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת המשמרת ושמירה
        Shift shift = new Shift();
        shift.setShiftTime(dateFromIntent);
        shift.setWorkerList(new ArrayList<>(workersToShift));
        shift.setStatus("Active");

        databaseService.createNewShift(shift, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void object) {
                Toast.makeText(ManageShiftsActivity.this, "המשמרת נשמרה!", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(ManageShiftsActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}