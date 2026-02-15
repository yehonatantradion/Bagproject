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

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.ViewHolder> {

    private List<Worker> workerList;
    private OnWorkerClickListener listener;

    public interface OnWorkerClickListener {
        void onAddClick(Worker worker, int position);
    }

    public WorkerAdapter(List<Worker> workerList, OnWorkerClickListener listener) {
        this.workerList = workerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // שימוש בשם הקובץ שציינת: item_user
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Worker worker = workerList.get(position);

        // הגדרת הטקסט לפי המודל וה-IDs ב-XML
        holder.tvName.setText(worker.getfName());
        holder.tvEmail.setText(worker.getEmail());

        holder.btnAdd.setOnClickListener(v -> {
            if (listener != null) {
                // שליחת העובד והמיקום שלו כדי שנוכל להסיר אותו מהרשימה ב-Activity
                listener.onAddClick(worker, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return workerList != null ? workerList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        Button btnAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור ל-IDs הקיימים ב-item_user.xml
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnAdd = itemView.findViewById(R.id.btnAddToShift);
        }
    }
}