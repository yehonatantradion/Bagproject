package com.example.myapplication.screens;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.adapters.WorkerShiftAdapter;
import com.example.myapplication.model.ShiftInvitation;
import com.example.myapplication.model.ShiftRequest;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;
import com.example.myapplication.services.ShiftReminderReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeDashboardActivity extends BaseActivity {

    private TextView tvWelcome, tvNoShifts, tvInvitationsTitle;
    private Button btnRequestShift, btnCheckIn, btnLogout;
    private RecyclerView rvShifts, rvInvitations;
    private ProgressBar progressBar;
    private WorkerShiftAdapter adapter;
    private final ArrayList<ShiftRequest>    shiftList      = new ArrayList<>();
    private final ArrayList<ShiftRequest>    pendingList    = new ArrayList<>();
    private final ArrayList<ShiftInvitation> invitationList = new ArrayList<>();
    private Worker currentWorker;
    private DatabaseService db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_dashboard);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        db = DatabaseService.getInstance();
        currentWorker = (Worker) getIntent().getSerializableExtra("worker");

        if (currentWorker == null) {
            Toast.makeText(this, "שגיאה: נתוני משתמש חסרים", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        createNotificationChannel();
        initViews();
        loadShifts(); // loadInvitations() נקרא בסוף loadAssignedShifts
    }

    private void loadInvitations() {
        db.getOpenInvitations(new DatabaseService.DatabaseCallback<List<ShiftInvitation>>() {
            @Override
            public void onCompleted(List<ShiftInvitation> list) {
                invitationList.clear();
                if (list != null) {
                    String myId = currentWorker != null ? currentWorker.getId() : null;
                    for (ShiftInvitation inv : list) {
                        // בדוק שההזמנה מיועדת לעובד זה
                        java.util.List<String> targets = inv.getTargetWorkerIds();
                        if (targets != null && !targets.isEmpty()
                                && (myId == null || !targets.contains(myId))) {
                            continue; // לא מיועדת לעובד זה
                        }
                        // בדוק שהעובד עדיין לא ביקש/שובץ למשמרת זו
                        if (alreadyRequested(inv.getDate(), inv.getShiftType())) {
                            continue; // כבר ביקש — הסתר את ההזמנה
                        }
                        invitationList.add(inv);
                    }
                }
                if (rvInvitations != null) {
                    rvInvitations.getAdapter().notifyDataSetChanged();
                }
                if (tvInvitationsTitle != null) {
                    tvInvitationsTitle.setVisibility(
                            invitationList.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
            @Override public void onFailed(Exception e) {}
        });
    }

    /** בדיקה אם העובד כבר שלח בקשה (ממתינה או מאושרת) לאותו תאריך+סוג */
    private boolean alreadyRequested(String date, String shiftType) {
        for (ShiftRequest s : shiftList) {
            if (date != null && date.equals(s.getDate())
                    && shiftType != null && shiftType.equals(s.getShiftType())) {
                return true;
            }
        }
        return false;
    }

    private void quickRequestShift(ShiftInvitation inv) {
        if (currentWorker == null) return;
        String start = "בוקר".equals(inv.getShiftType()) ? "06:00" : "14:00";
        String end   = "בוקר".equals(inv.getShiftType()) ? "14:00" : "22:00";
        ShiftRequest req = new ShiftRequest(
                currentWorker.getId(),
                currentWorker.getfName() + " " + currentWorker.getlName(),
                inv.getDate(), start, end, "");
        req.setShiftType(inv.getShiftType());
        db.addShiftRequest(req, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void v) {
                android.widget.Toast.makeText(EmployeeDashboardActivity.this,
                        "✅ הבקשה נשלחה!", android.widget.Toast.LENGTH_SHORT).show();
                // רענן — ההזמנה תיעלם אוטומטית לאחר הגשת הבקשה
                loadShifts();
            }
            @Override
            public void onFailed(Exception e) {
                android.widget.Toast.makeText(EmployeeDashboardActivity.this,
                        "שגיאה בשליחת הבקשה", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    // מיני-אדפטר להזמנות מנהל
    static class InvitationQuickAdapter
            extends RecyclerView.Adapter<InvitationQuickAdapter.VH> {
        interface OnRequest { void onRequest(ShiftInvitation inv); }

        private final List<ShiftInvitation> data;
        private final OnRequest listener;

        InvitationQuickAdapter(List<ShiftInvitation> data, OnRequest listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_invitation_worker, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ShiftInvitation inv = data.get(position);
            try {
                String[] p = inv.getDate().split("/");
                String label = Integer.parseInt(p[0]) + "." + Integer.parseInt(p[1])
                        + "  " + inv.getShiftType();
                h.tvLabel.setText(label);
            } catch (Exception e) {
                h.tvLabel.setText(inv.getDate() + " " + inv.getShiftType());
            }
            h.btnRequest.setOnClickListener(v -> {
                if (listener != null) listener.onRequest(inv);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvLabel;
            android.widget.Button   btnRequest;
            VH(android.view.View v) {
                super(v);
                tvLabel    = v.findViewById(R.id.tvInvitationLabel);
                btnRequest = v.findViewById(R.id.btnRequestInvitation);
            }
        }
    }

    private void initViews() {
        tvWelcome          = findViewById(R.id.tvWelcome);
        tvNoShifts         = findViewById(R.id.tvNoShifts);
        tvInvitationsTitle = findViewById(R.id.tvInvitationsTitle);
        btnRequestShift    = findViewById(R.id.btnRequestShift);
        btnCheckIn         = findViewById(R.id.btnCheckIn);
        btnLogout          = findViewById(R.id.btnLogout);
        rvShifts           = findViewById(R.id.rvShifts);
        rvInvitations      = findViewById(R.id.rvInvitations);
        progressBar        = findViewById(R.id.progressBar);

        tvWelcome.setText("שלום, " + currentWorker.getfName() + " " + currentWorker.getlName());

        adapter = new WorkerShiftAdapter(shiftList);
        rvShifts.setLayoutManager(new LinearLayoutManager(this));
        rvShifts.setAdapter(adapter);

        // הזמנות מנהל
        if (rvInvitations != null) {
            rvInvitations.setLayoutManager(new LinearLayoutManager(this));
            rvInvitations.setAdapter(new InvitationQuickAdapter(
                    invitationList, this::quickRequestShift));
        }

        btnRequestShift.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkerRequestShift.class);
            intent.putExtra("worker", currentWorker);
            startActivity(intent);
        });

        btnCheckIn.setOnClickListener(v -> {
            Intent intent = new Intent(this, CheckInActivity.class);
            intent.putExtra("worker", currentWorker);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            db.getAuth().signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadShifts() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoShifts.setVisibility(View.GONE);

        // טען בקשות ממתינות
        db.getWorkerPendingRequests(currentWorker.getId(),
                new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> pending) {
                pendingList.clear();
                if (pending != null) pendingList.addAll(pending);
                // לאחר טעינת ממתינות, טען גם את המשמרות המאושרות
                loadAssignedShifts();
            }
            @Override
            public void onFailed(Exception e) {
                loadAssignedShifts();
            }
        });
    }

    private void loadAssignedShifts() {
        db.getWorkerShifts(currentWorker.getId(),
                new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> shifts) {
                progressBar.setVisibility(View.GONE);
                shiftList.clear();
                // הוסף ממתינות קודם
                shiftList.addAll(pendingList);
                // הוסף מאושרות (בלי כפילות לפי תאריך+סוג)
                if (shifts != null) {
                    for (ShiftRequest s : shifts) {
                        boolean dup = false;
                        for (ShiftRequest p : pendingList) {
                            if (s.getDate() != null && s.getDate().equals(p.getDate())
                                    && s.getShiftType() != null
                                    && s.getShiftType().equals(p.getShiftType())) {
                                dup = true; break;
                            }
                        }
                        if (!dup) shiftList.add(s);
                    }
                    if (!shifts.isEmpty()) scheduleReminders(shifts);
                }
                adapter.notifyDataSetChanged();
                // שלב 3: טען בקשות שנדחו — לפני הזמנות
                loadRejectedRequests();
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EmployeeDashboardActivity.this,
                        "שגיאה בטעינת משמרות", Toast.LENGTH_SHORT).show();
                loadRejectedRequests();
            }
        });
    }

    /** שלב 3: טוען בקשות שנדחו ומוסיף אותן לרשימה (כדי שהעובד יראה את הסטטוס) */
    private void loadRejectedRequests() {
        db.getWorkerRejectedRequests(currentWorker.getId(),
                new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> rejected) {
                progressBar.setVisibility(View.GONE);
                if (rejected != null && !rejected.isEmpty()) {
                    for (ShiftRequest r : rejected) {
                        // הוסף רק אם אין כבר פריט לאותו תאריך+סוג ברשימה
                        boolean dup = false;
                        for (ShiftRequest existing : shiftList) {
                            if (r.getDate() != null && r.getDate().equals(existing.getDate())
                                    && r.getShiftType() != null
                                    && r.getShiftType().equals(existing.getShiftType())) {
                                dup = true;
                                break;
                            }
                        }
                        if (!dup) shiftList.add(r);
                    }
                    adapter.notifyDataSetChanged();
                }
                if (shiftList.isEmpty()) {
                    tvNoShifts.setVisibility(View.VISIBLE);
                }
                // שלב 4: טען הזמנות — אחרי שיש לנו את רשימת המשמרות המלאה לסינון
                loadInvitations();
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
                if (shiftList.isEmpty()) tvNoShifts.setVisibility(View.VISIBLE);
                loadInvitations();
            }
        });
    }

    private void scheduleReminders(List<ShiftRequest> shifts) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (ShiftRequest shift : shifts) {
            if (shift.getDate() == null || shift.getStartTime() == null) continue;
            try {
                Date shiftDate = sdf.parse(shift.getDate() + " " + shift.getStartTime());
                if (shiftDate == null) continue;
                long reminderTime = shiftDate.getTime() - (30 * 60 * 1000L);
                if (reminderTime <= System.currentTimeMillis()) continue;

                Intent intent = new Intent(this, ShiftReminderReceiver.class);
                intent.putExtra("workerName", currentWorker.getfName());
                intent.putExtra("shiftDate", shift.getDate());
                intent.putExtra("startTime", shift.getStartTime());

                int code = (currentWorker.getId() + shift.getRequestId()).hashCode();
                PendingIntent pi = PendingIntent.getBroadcast(this, code, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pi);
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pi);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pi);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pi);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ShiftReminderReceiver.CHANNEL_ID,
                    "תזכורות משמרות",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("תזכורת 30 דקות לפני תחילת משמרת");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // בדיקת הרשאה: רק עובד מחובר מורשה להיכנס לכאן
        if (db.getAuth().getCurrentUser() == null) {
            Intent intent = new Intent(this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        loadShifts(); // loadInvitations() נקרא בסוף loadAssignedShifts
    }
}
