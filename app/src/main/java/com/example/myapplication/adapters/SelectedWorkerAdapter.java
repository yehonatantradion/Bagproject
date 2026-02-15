package com.example.myapplication.adapters; // או com.example.myapplication.adapters

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Worker;
import java.util.List;

public class SelectedWorkerAdapter extends RecyclerView.Adapter<SelectedWorkerAdapter.ViewHolder> {

    private final List<Worker> selectedWorkers;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Worker worker, int position);
    }

    public SelectedWorkerAdapter(List<Worker> selectedWorkers, OnItemClickListener listener) {
        this.selectedWorkers = selectedWorkers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_selected_worker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Worker worker = selectedWorkers.get(position);
        holder.tvName.setText(worker.getfName()); // וודא שזה getfName או getName לפי המודל שלך

        holder.itemView.setOnClickListener(v -> listener.onItemClick(worker, position));
    }

    @Override
    public int getItemCount() {
        return selectedWorkers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWorkerName);
        }
    }
}