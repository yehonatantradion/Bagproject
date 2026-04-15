package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.adapters.ShiftRequestAdapter;
import com.example.myapplication.model.ShiftRequest;
import com.example.myapplication.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestsActivity extends BaseActivity {

    private RecyclerView rvRequests;
    private ProgressBar progressBar;
    private TextView tvNoRequests;
    private ShiftRequestAdapter adapter;
    private final ArrayList<ShiftRequest> requestList = new ArrayList<>();
    private DatabaseService db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = DatabaseService.getInstance();
        initViews();
        loadRequests();
    }

    private void initViews() {
        rvRequests   = findViewById(R.id.rvRequests);
        progressBar  = findViewById(R.id.progressBar);
        tvNoRequests = findViewById(R.id.tvNoRequests);

        adapter = new ShiftRequestAdapter(requestList, new ShiftRequestAdapter.OnRequestActionListener() {
            @Override
            public void onApprove(ShiftRequest request, int position) {
                approveRequest(request, position);
            }
            @Override
            public void onReject(ShiftRequest request, int position) {
                rejectRequest(request, position);
            }
        });

        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);
    }

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoRequests.setVisibility(View.GONE);

        db.getPendingShiftRequests(new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> requests) {
                progressBar.setVisibility(View.GONE);
                requestList.clear();
                if (requests != null && !requests.isEmpty()) {
                    requestList.addAll(requests);
                } else {
                    tvNoRequests.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PendingRequestsActivity.this, "שגיאה בטעינת הבקשות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveRequest(ShiftRequest request, int position) {
        request.setStatus("approved");

        db.updateShiftRequest(request, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                db.saveWorkerShift(request.getWorkerId(), request, new DatabaseService.DatabaseCallback<Void>() {
                    @Override
                    public void onCompleted(Void unused2) {
                        db.saveShiftForDate(request.getDate(), request, new DatabaseService.DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void unused3) {
                                Toast.makeText(PendingRequestsActivity.this, "הבקשה אושרה", Toast.LENGTH_SHORT).show();
                                adapter.removeItem(position);
                                if (requestList.isEmpty()) tvNoRequests.setVisibility(View.VISIBLE);
                            }
                            @Override
                            public void onFailed(Exception e) { showError(); }
                        });
                    }
                    @Override
                    public void onFailed(Exception e) { showError(); }
                });
            }
            @Override
            public void onFailed(Exception e) {
                request.setStatus("pending");
                showError();
            }
        });
    }

    private void rejectRequest(ShiftRequest request, int position) {
        request.setStatus("rejected");
        db.updateShiftRequest(request, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(PendingRequestsActivity.this, "הבקשה נדחתה", Toast.LENGTH_SHORT).show();
                adapter.removeItem(position);
                if (requestList.isEmpty()) tvNoRequests.setVisibility(View.VISIBLE);
            }
            @Override
            public void onFailed(Exception e) {
                request.setStatus("pending");
                Toast.makeText(PendingRequestsActivity.this, "שגיאה בדחיית הבקשה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showError() {
        Toast.makeText(this, "שגיאה בעיבוד הבקשה", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }
}
