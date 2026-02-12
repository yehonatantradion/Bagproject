package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.UsersAdapter;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

import java.util.ArrayList;
import java.util.List;

public class UsersList extends AppCompatActivity {

    private static final String TAG = "UsersList";

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private final ArrayList<Worker> workerArrayList = new ArrayList<>();
    private UsersAdapter adapter;

    // תנסה את כל הנתיבים האלו אחד אחד עד שמוצא נתונים



    DatabaseService  databaseService;
    private int pathIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_users_list2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseService=DatabaseService.getInstance();

        rvUsers = findViewById(R.id.rvUsers);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        adapter = new UsersAdapter(workerArrayList);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);



        loadFromNextPath();
    }

    private void loadFromNextPath() {
       databaseService.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
           @Override
           public void onCompleted(List<Worker> workers) {

               workerArrayList.addAll(workers);

               adapter.notifyDataSetChanged();

           }

           @Override
           public void onFailed(Exception e) {

           }
       });
    }






    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            rvUsers.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

}

