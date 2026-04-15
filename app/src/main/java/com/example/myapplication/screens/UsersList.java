package com.example.myapplication.screens;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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

public class UsersList extends BaseActivity {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private final ArrayList<Worker> workerArrayList = new ArrayList<>();
    private UsersAdapter adapter;
    private DatabaseService databaseService;

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

        // RTL
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        databaseService = DatabaseService.getInstance();

        rvUsers = findViewById(R.id.rvUsers);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        // הגדרת האדפטר:
        adapter = new UsersAdapter(workerArrayList, false, new UsersAdapter.OnUserClickListener() {
            @Override
            public void onWorkerLongClick(Worker worker) {
                // פתיחת הדיאלוג המעוצב החדש
                showEditDialog(worker);
            }

            @Override
            public void onAddToShiftClick(Worker worker) {
                // לא רלוונטי כאן
            }
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        loadWorkers();
    }

    private void showEditDialog(Worker worker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // טעינת דיאלוג עריכה (נפרד מ-DialogWorkerDetails שהוא מסך תצוגה בלבד)
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_worker, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // --- קישור לרכיבים לפי ה-XML החדש ששלחת ---
        EditText etName = view.findViewById(R.id.etWorkerName);
        EditText etPhone = view.findViewById(R.id.etWorkerPhone);
        EditText etEmail = view.findViewById(R.id.etWorkerEmail);
        RadioGroup rgRole = view.findViewById(R.id.rgRole);
        RadioButton rbManager = view.findViewById(R.id.rbManager);
        RadioButton rbWorker = view.findViewById(R.id.rbWorker);
        Button btnSave = view.findViewById(R.id.btnSaveChanges);

        // מילוי הנתונים הקיימים
        etName.setText(worker.getfName());
        etPhone.setText(worker.getPhone());
        etEmail.setText(worker.getEmail()); // מוצג אך לא ניתן לעריכה

        // הגדרת הבחירה הנוכחית ברדיו באטן (מנהל או עובד)
        if (worker.getIsAdmin()) {
            rbManager.setChecked(true);
        } else {
            rbWorker.setChecked(true);
        }

        // האזנה לכפתור השמירה
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            // בדיקה האם נבחר "מנהל"
            boolean newIsAdmin = rbManager.isChecked();

            if (newName.isEmpty()) {
                etName.setError("שדה חובה");
                return;
            }

            // עדכון האובייקט המקומי
            worker.setfName(newName);
            worker.setPhone(newPhone);
            worker.setIsAdmin(newIsAdmin); // עדכון הרשאת ניהול

            // עדכון במסד הנתונים
            showLoading(true);
            databaseService.updateWorker(worker, new DatabaseService.DatabaseCallback<Void>() {
                @Override
                public void onCompleted(Void object) {
                    showLoading(false);
                    Toast.makeText(UsersList.this, "הפרטים עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    dialog.dismiss(); // סגירת הדיאלוג
                    adapter.notifyDataSetChanged(); // רענון הרשימה
                }

                @Override
                public void onFailed(Exception e) {
                    showLoading(false);
                    Toast.makeText(UsersList.this, "שגיאה בעדכון", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // הצגת הדיאלוג עם רקע שקוף (כדי שהפינות העגולות של ה-CardView יראו טוב)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void loadWorkers() {
        showLoading(true);
        databaseService.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> workers) {
                showLoading(false);
                workerArrayList.clear();
                if (workers != null && !workers.isEmpty()) {
                    workerArrayList.addAll(workers);
                    showEmpty(false);
                } else {
                    showEmpty(true);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(Exception e) {
                showLoading(false);
                showEmpty(true);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && workerArrayList.isEmpty()) {
            rvUsers.setVisibility(View.GONE);
        } else {
            rvUsers.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(boolean empty) {
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}