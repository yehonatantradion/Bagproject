package com.example.myapplication.screens;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.model.ShiftInvitation;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InviteWorkersActivity extends BaseActivity {

    /** Optional intent extras — when provided the date/type pickers are hidden */
    public static final String EXTRA_DATE       = "invite_date";
    public static final String EXTRA_SHIFT_TYPE = "invite_shift_type";

    private static final String TYPE_MORNING = "בוקר";
    private static final String TYPE_EVENING = "ערב";

    private Button btnPickDate, btnSendInvite, btnBack, btnSelectAll, btnDeselectAll;
    private LinearLayout cardMorning, cardEvening;
    private TextView tvPickedDate, tvOpenInvitationsTitle, tvWorkerSelectionHint, tvLockedShift;
    private View layoutShiftTypePicker;   // the LinearLayout wrapping the two type cards
    private RecyclerView rvInvitations, rvWorkerSelect;
    private ProgressBar progressBar, progressWorkers;

    private String selectedDate      = null;
    private String selectedShiftType = null;

    private DatabaseService db;
    private final List<ShiftInvitation> invitationList  = new ArrayList<>();
    private final List<Worker>          workerList       = new ArrayList<>();
    private final Set<String>           selectedWorkerIds = new HashSet<>();
    private InvitationAdapter invAdapter;
    private WorkerSelectAdapter workerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invite_workers);
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

        btnPickDate            = findViewById(R.id.btnPickDate);
        btnSendInvite          = findViewById(R.id.btnSendInvite);
        btnBack                = findViewById(R.id.btnBack);
        btnSelectAll           = findViewById(R.id.btnSelectAll);
        btnDeselectAll         = findViewById(R.id.btnDeselectAll);
        cardMorning            = findViewById(R.id.cardMorning);
        cardEvening            = findViewById(R.id.cardEvening);
        tvPickedDate           = findViewById(R.id.tvPickedDate);
        tvOpenInvitationsTitle = findViewById(R.id.tvOpenInvitationsTitle);
        tvWorkerSelectionHint  = findViewById(R.id.tvWorkerSelectionHint);
        tvLockedShift          = findViewById(R.id.tvLockedShift);
        layoutShiftTypePicker  = findViewById(R.id.layoutShiftTypePicker);
        rvInvitations          = findViewById(R.id.rvInvitations);
        rvWorkerSelect         = findViewById(R.id.rvWorkerSelect);
        progressBar            = findViewById(R.id.progressBar);
        progressWorkers        = findViewById(R.id.progressWorkers);

        // הזמנות פתוחות
        invAdapter = new InvitationAdapter(invitationList, this::deleteInvitation);
        rvInvitations.setLayoutManager(new LinearLayoutManager(this));
        rvInvitations.setAdapter(invAdapter);

        // רשימת עובדים לבחירה
        workerAdapter = new WorkerSelectAdapter(workerList, selectedWorkerIds, this::onWorkerToggled);
        rvWorkerSelect.setLayoutManager(new LinearLayoutManager(this));
        rvWorkerSelect.setAdapter(workerAdapter);

        btnPickDate.setOnClickListener(v -> openDatePicker());
        cardMorning.setOnClickListener(v -> selectType(TYPE_MORNING));
        cardEvening.setOnClickListener(v -> selectType(TYPE_EVENING));
        btnSendInvite.setOnClickListener(v -> sendInvitation());
        btnBack.setOnClickListener(v -> finish());

        btnSelectAll.setOnClickListener(v -> {
            for (Worker w : workerList) {
                if (w.getId() != null) selectedWorkerIds.add(w.getId());
            }
            workerAdapter.notifyDataSetChanged();
            updateSelectionHint();
        });

        btnDeselectAll.setOnClickListener(v -> {
            selectedWorkerIds.clear();
            workerAdapter.notifyDataSetChanged();
            updateSelectionHint();
        });

        // If launched from a specific shift, pre-fill and lock date + type
        String extraDate = getIntent().getStringExtra(EXTRA_DATE);
        String extraType = getIntent().getStringExtra(EXTRA_SHIFT_TYPE);
        if (extraDate != null && extraType != null) {
            applyLockedShift(extraDate, extraType);
        }

        updateSendButton();
        loadInvitations();
        loadWorkers();
    }

    // ─── נעילת משמרת מ-intent ──────────────────────────────────────────────────
    /**
     * When the invite screen is opened from ManageShiftsActivity, the date and
     * shift type are already known. Hide the manual pickers and show a pill with
     * the locked shift info instead.
     */
    private void applyLockedShift(String date, String type) {
        selectedDate      = date;
        selectedShiftType = type;

        // Hide the manual date picker + type cards
        btnPickDate.setVisibility(View.GONE);
        tvPickedDate.setVisibility(View.GONE);
        if (layoutShiftTypePicker != null) layoutShiftTypePicker.setVisibility(View.GONE);

        // Show locked pill
        if (tvLockedShift != null) {
            String label = formatDate(date) + "  —  " + type;
            tvLockedShift.setText("משמרת: " + label);
            tvLockedShift.setVisibility(View.VISIBLE);
        }
    }

    // ─── טעינת עובדים ──────────────────────────────────────────────────────────
    private void loadWorkers() {
        progressWorkers.setVisibility(View.VISIBLE);
        db.getWorkerList(new DatabaseService.DatabaseCallback<List<Worker>>() {
            @Override
            public void onCompleted(List<Worker> list) {
                progressWorkers.setVisibility(View.GONE);
                workerList.clear();
                if (list != null) {
                    for (Worker w : list) {
                        if (!w.isAdmin()) workerList.add(w); // הצג רק עובדים (לא מנהלים)
                    }
                }
                workerAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailed(Exception e) {
                progressWorkers.setVisibility(View.GONE);
            }
        });
    }

    private void onWorkerToggled(String workerId, boolean checked) {
        if (checked) {
            selectedWorkerIds.add(workerId);
        } else {
            selectedWorkerIds.remove(workerId);
        }
        updateSelectionHint();
    }

    private void updateSelectionHint() {
        int count = selectedWorkerIds.size();
        if (count == 0) {
            tvWorkerSelectionHint.setText("(לא נבחר = כל העובדים)");
        } else {
            tvWorkerSelectionHint.setText("נבחרו " + count + " עובדים");
        }
    }

    // ─── בוחר תאריך ────────────────────────────────────────────────────────────
    private void openDatePicker() {
        Calendar today = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(this, (picker, y, m, d) -> {
            selectedDate = String.format("%02d/%02d/%04d", d, m + 1, y);
            btnPickDate.setText(d + "." + (m + 1) + "." + y);
            tvPickedDate.setText("📅 " + d + "." + (m + 1) + "." + y);
            updateSendButton();
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMinDate(today.getTimeInMillis());
        dlg.show();
    }

    // ─── בחירת סוג משמרת ───────────────────────────────────────────────────────
    private void selectType(String type) {
        selectedShiftType = type;
        if (TYPE_MORNING.equals(type)) {
            cardMorning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_selected));
            cardEvening.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        } else {
            cardEvening.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_selected));
            cardMorning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        }
        updateSendButton();
    }

    private void updateSendButton() {
        boolean ready = selectedDate != null && selectedShiftType != null;
        btnSendInvite.setEnabled(ready);
        btnSendInvite.setAlpha(ready ? 1f : 0.45f);
    }

    // ─── שליחת הזמנה ───────────────────────────────────────────────────────────
    private void sendInvitation() {
        btnSendInvite.setEnabled(false);
        ShiftInvitation inv = new ShiftInvitation(selectedDate, selectedShiftType);

        // הוסף מזהי עובדים שנבחרו (אם לא נבחר אף אחד = כל העובדים)
        if (!selectedWorkerIds.isEmpty()) {
            inv.setTargetWorkerIds(new ArrayList<>(selectedWorkerIds));
        }

        db.saveShiftInvitation(inv, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void v) {
                String label = formatDate(selectedDate) + " " + selectedShiftType;
                String extra = selectedWorkerIds.isEmpty()
                        ? " (כל העובדים)"
                        : " (" + selectedWorkerIds.size() + " עובדים)";
                Toast.makeText(InviteWorkersActivity.this,
                        "✅ הזמנה נשלחה: " + label + extra, Toast.LENGTH_SHORT).show();
                resetForm();
                loadInvitations();
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(InviteWorkersActivity.this,
                        "שגיאה בשליחת ההזמנה", Toast.LENGTH_SHORT).show();
                updateSendButton();
            }
        });
    }

    private void resetForm() {
        selectedDate      = null;
        selectedShiftType = null;
        selectedWorkerIds.clear();
        btnPickDate.setText("בחר תאריך");
        tvPickedDate.setText("");
        cardMorning.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        cardEvening.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_shift_unselected));
        workerAdapter.notifyDataSetChanged(); // uncheck all
        updateSelectionHint();
        updateSendButton();
    }

    // ─── טעינת הזמנות פתוחות ───────────────────────────────────────────────────
    private void loadInvitations() {
        progressBar.setVisibility(View.VISIBLE);
        db.getOpenInvitations(new DatabaseService.DatabaseCallback<List<ShiftInvitation>>() {
            @Override
            public void onCompleted(List<ShiftInvitation> list) {
                progressBar.setVisibility(View.GONE);
                invitationList.clear();
                if (list != null) invitationList.addAll(list);
                invAdapter.notifyDataSetChanged();
                tvOpenInvitationsTitle.setVisibility(
                        invitationList.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onFailed(Exception e) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void deleteInvitation(ShiftInvitation inv, int position) {
        db.deleteShiftInvitation(inv.getId(), new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void v) {
                if (position >= 0 && position < invitationList.size()) {
                    invitationList.remove(position);
                    invAdapter.notifyItemRemoved(position);
                    if (invitationList.isEmpty())
                        tvOpenInvitationsTitle.setVisibility(View.GONE);
                }
            }
            @Override public void onFailed(Exception e) {
                Toast.makeText(InviteWorkersActivity.this,
                        "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(String raw) {
        try {
            String[] p = raw.split("/");
            return Integer.parseInt(p[0]) + "." + Integer.parseInt(p[1]);
        } catch (Exception e) { return raw; }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // אדפטר בחירת עובדים
    // ═══════════════════════════════════════════════════════════════════════════
    static class WorkerSelectAdapter
            extends RecyclerView.Adapter<WorkerSelectAdapter.VH> {

        interface OnToggle { void onToggle(String workerId, boolean checked); }

        private final List<Worker>  data;
        private final Set<String>   selected;
        private final OnToggle      listener;

        WorkerSelectAdapter(List<Worker> data, Set<String> selected, OnToggle listener) {
            this.data     = data;
            this.selected = selected;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_worker_checkbox, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Worker w = data.get(position);
            String fullName = (w.getfName() != null ? w.getfName() : "")
                    + " " + (w.getlName() != null ? w.getlName() : "");
            h.tvName.setText(fullName.trim());
            h.cbWorker.setChecked(w.getId() != null && selected.contains(w.getId()));
            h.itemView.setOnClickListener(v -> {
                boolean nowChecked = !h.cbWorker.isChecked();
                h.cbWorker.setChecked(nowChecked);
                if (listener != null && w.getId() != null)
                    listener.onToggle(w.getId(), nowChecked);
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            CheckBox cbWorker;
            VH(android.view.View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvWorkerName);
                cbWorker = v.findViewById(R.id.cbWorker);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // אדפטר הזמנות פתוחות
    // ═══════════════════════════════════════════════════════════════════════════
    static class InvitationAdapter
            extends RecyclerView.Adapter<InvitationAdapter.VH> {

        interface OnDelete { void onDelete(ShiftInvitation inv, int position); }

        private final List<ShiftInvitation> data;
        private final OnDelete listener;

        InvitationAdapter(List<ShiftInvitation> data, OnDelete listener) {
            this.data     = data;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_invitation, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            ShiftInvitation inv = data.get(position);
            String[] p = inv.getDate() != null ? inv.getDate().split("/") : new String[]{"", ""};
            String label = (p.length >= 2
                    ? (Integer.parseInt(p[0]) + "." + Integer.parseInt(p[1]))
                    : inv.getDate()) + "  " + inv.getShiftType();
            // הצג כמה עובדים מוזמנים
            List<String> targets = inv.getTargetWorkerIds();
            if (targets != null && !targets.isEmpty()) {
                label += "  (" + targets.size() + " עובדים)";
            }
            h.tvLabel.setText(label);
            h.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(inv, h.getAdapterPosition());
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvLabel;
            Button   btnDelete;
            VH(android.view.View v) {
                super(v);
                tvLabel   = v.findViewById(R.id.tvInvitationLabel);
                btnDelete = v.findViewById(R.id.btnDeleteInvitation);
            }
        }
    }
}
