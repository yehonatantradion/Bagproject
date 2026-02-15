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
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.Shift;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ManageShiftsActivity extends AppCompatActivity {

    private TextView tvSelectedDateDisplay, tvNames;
    private RecyclerView rvEmployeesList;
    private Button btnSave;
    private DatabaseService databaseService;
    private List<Worker> workerList = new ArrayList<>();
    private List<Worker> workersToShift = new ArrayList<>();
    private WorkerAdapter adapter;
    private String names = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.workers_shift);

        databaseService = DatabaseService.getInstance();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        tvNames = findViewById(R.id.tvWorkersShift);
        rvEmployeesList = findViewById(R.id.rvEmployeesList);
        tvSelectedDateDisplay = findViewById(R.id.tvSelectedDateDisplay);
        btnSave = findViewById(R.id.btnSaveShift);

        rvEmployeesList.setLayoutManager(new LinearLayoutManager(this));

        String dateString = getIntent().getStringExtra("SELECTED_DATE");
        if (dateString != null) {
            tvSelectedDateDisplay.setText("ניהול משמרת לתאריך: " + dateString);
        }

        setupRecyclerView();

        btnSave.setOnClickListener(v -> {
            if (workersToShift.isEmpty()) {
                Toast.makeText(this, "אנא בחר לפחות עובד אחד", Toast.LENGTH_SHORT).show();
                return;
            }
            String shiftId = databaseService.generateShiftId();
            Shift newShift = new Shift(shiftId, "1", new Date(), null, new ArrayList<>(workersToShift), "Planned");
            databaseService.createNewShift(newShift, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    Toast.makeText(ManageShiftsActivity.this, "המשמרת נשמרה!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailed(Exception e) {
                    Toast.makeText(ManageShiftsActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupRecyclerView() {
        adapter = new WorkerAdapter(workerList, (worker, position) -> {
            workersToShift.add(worker);
            names += (names.isEmpty() ? "" : ", ") + worker.getfName();
            tvNames.setText("עובדים: " + names);
            workerList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, workerList.size());
        });
        rvEmployeesList.setAdapter(adapter);

        databaseService.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> allWorkers) {
                workerList.clear();
                if (allWorkers != null) workerList.addAll(allWorkers);
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailed(Exception e) { }
        });
    }
}