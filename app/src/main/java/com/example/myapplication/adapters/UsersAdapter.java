package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Worker;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

    private final ArrayList<Worker> data;
    private final boolean showAddButton; // האם להציג את כפתור ההוספה?
    private final OnUserClickListener listener; // המאזין ללחיצות

    // ממשק שמגדיר אילו פעולות אפשר לעשות (לחיצה ארוכה, לחיצה על כפתור)
    public interface OnUserClickListener {
        void onWorkerLongClick(Worker worker);
        void onAddToShiftClick(Worker worker); // אופציונלי, למקרה שנצטרך
    }

    // הבנאי (Constructor) החדש
    public UsersAdapter(ArrayList<Worker> data, boolean showAddButton, OnUserClickListener listener) {
        this.data = data;
        this.showAddButton = showAddButton;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Worker worker = data.get(position);

        // שליטה על נראות הכפתור לפי הפרמטר שהועבר
        holder.btnAddToShift.setVisibility(showAddButton ? View.VISIBLE : View.GONE);

        holder.tvName.setText(worker.getfName() != null ? worker.getfName() : "ללא שם");

        String email = (worker.getEmail() == null || worker.getEmail().isEmpty()) ? "אין מייל" : worker.getEmail();
        holder.tvEmail.setText(email);

        String phone = (worker.getPhone() == null || worker.getPhone().isEmpty()) ? "אין טלפון" : worker.getPhone();
        holder.tvPhone.setText(phone);

        String role = worker.getIsAdmin() ? "מנהל" : "עובד";
        holder.tvRole.setText("תפקיד: " + role);

        // לחיצה ארוכה - במקום לפתוח מסך, קוראים לפונקציה של ה-Listener
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onWorkerLongClick(worker);
            }
            return true;
        });

        // לחיצה על כפתור הוספה (אם קיים)
        holder.btnAddToShift.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToShiftClick(worker);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvRole;
        Button btnAddToShift;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvRole = itemView.findViewById(R.id.tvRole);
            btnAddToShift = itemView.findViewById(R.id.btnAddToShift);
        }
    }
}