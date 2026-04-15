package com.example.myapplication.screens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.adapters.UsersAdapter;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class ManageWorkersActivity extends BaseActivity {

    private static final int REQUEST_WORKER_DETAILS = 1001;

    private RecyclerView rvWorkers;
    private UsersAdapter adapter;
    private ArrayList<Worker> workersList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_workers);

        View main = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // RTL
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvWorkers = findViewById(R.id.rvWorkers); // וודא שיש לך ID כזה ב-XML, או שתעדכן בהתאם
        // אם ב-XML שלך אין עדיין RecyclerView, תצטרך להוסיף אותו ל-XML של activity_manage_workers

        // הגדרת ה-Adapter עבור דף ניהול עובדים:
        // 1. showAddButton = false (אנחנו לא מוסיפים למשמרת כאן)
        // 2. Listener שפותח את דף העריכה (UpdateWorkerActivity)
        adapter = new UsersAdapter(workersList, false, new UsersAdapter.OnUserClickListener() {
            @Override
            public void onWorkerLongClick(Worker worker) {
                Intent intent = new Intent(ManageWorkersActivity.this, DialogWorkerDetails.class);
                // Pass the entire Worker object (implements Serializable)
                intent.putExtra(DialogWorkerDetails.EXTRA_WORKER, worker);
                startActivityForResult(intent, REQUEST_WORKER_DETAILS);
            }

            @Override
            public void onAddToShiftClick(Worker worker) {
                // לא רלוונטי
            }
        });

        rvWorkers.setLayoutManager(new LinearLayoutManager(this));
        rvWorkers.setAdapter(adapter);

        loadWorkers();
    }

    private void loadWorkers() {
        DatabaseService.getInstance().getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> workers) {
                workersList.clear();
                workersList.addAll(workers);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(Exception e) {
                // טיפול בשגיאה
            }
        });
    }

    /** Reload list when returning from DialogWorkerDetails after a delete */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WORKER_DETAILS && resultCode == Activity.RESULT_OK) {
            loadWorkers();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}