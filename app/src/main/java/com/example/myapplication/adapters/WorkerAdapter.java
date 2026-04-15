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
import java.util.List;
import java.util.Set;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.ViewHolder> {

    private final List<Worker> workerList;
    private final OnWorkerClickListener listener;
    // מזהי עובדים שביקשו את המשמרת — מועבר בהפניה, מתעדכן אוטומטית
    private Set<String> requestingIds = null;

    public interface OnWorkerClickListener {
        void onAddClick(Worker worker, int position);
    }

    public WorkerAdapter(List<Worker> workerList, OnWorkerClickListener listener) {
        this.workerList = workerList;
        this.listener   = listener;
    }

    /** העבר את קבוצת המבקשים — ישמש לסימון "ביקש ✓" */
    public void setRequestingIds(Set<String> requestingIds) {
        this.requestingIds = requestingIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Worker worker = workerList.get(position);

        // שם מלא (פרטי + משפחה)
        String fullName = (worker.getfName() != null ? worker.getfName() : "")
                + " " + (worker.getlName() != null ? worker.getlName() : "");
        holder.tvName.setText(fullName.trim());
        holder.tvEmail.setText(worker.getEmail() != null ? worker.getEmail() : "");
        holder.tvPhone.setVisibility(View.GONE); // לא רלוונטי במסך זה

        // סמן עובדים שביקשו את המשמרת
        boolean requested = requestingIds != null
                && worker.getId() != null
                && requestingIds.contains(worker.getId());
        if (requested) {
            holder.tvRole.setText("ביקש ✓");
            holder.tvRole.setTextColor(0xFF2ECC71); // ירוק
            holder.tvRole.setVisibility(View.VISIBLE);
        } else {
            holder.tvRole.setText("לא ביקש");
            holder.tvRole.setTextColor(0xFF9E9E9E); // אפור
            holder.tvRole.setVisibility(View.VISIBLE);
        }

        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddClick(worker, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return workerList != null ? workerList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, tvRole;
        Button btnAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName  = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvRole  = itemView.findViewById(R.id.tvRole);
            btnAdd  = itemView.findViewById(R.id.btnAddToShift);
        }
    }
}
