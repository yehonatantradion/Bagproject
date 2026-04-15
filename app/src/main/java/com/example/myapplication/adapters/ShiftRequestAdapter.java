package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ShiftRequest;

import java.util.List;

public class ShiftRequestAdapter extends RecyclerView.Adapter<ShiftRequestAdapter.VH> {

    public interface OnRequestActionListener {
        void onApprove(ShiftRequest request, int position);
        void onReject(ShiftRequest request, int position);
    }

    private final List<ShiftRequest> data;
    private final OnRequestActionListener listener;

    public ShiftRequestAdapter(List<ShiftRequest> data, OnRequestActionListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shift_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ShiftRequest req = data.get(position);

        holder.tvWorkerName.setText(req.getWorkerName() != null ? req.getWorkerName() : "");

        // פורמט: "21.4 בוקר"
        String dateDisplay = formatDate(req.getDate());
        String shiftType   = req.getShiftType();
        if (shiftType == null || shiftType.isEmpty()) {
            String start0 = req.getStartTime() != null ? req.getStartTime() : "";
            shiftType = start0.startsWith("06") ? "בוקר" : start0.startsWith("14") ? "ערב" : "";
        }
        holder.tvDate.setText(dateDisplay + (shiftType.isEmpty() ? "" : "  " + shiftType));

        String start = req.getStartTime() != null ? req.getStartTime() : "--:--";
        String end   = req.getEndTime()   != null ? req.getEndTime()   : "--:--";
        holder.tvTime.setText(start + " – " + end);

        String notes = req.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("הערות: " + notes);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(req, holder.getAdapterPosition());
        });
        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(req, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            String[] parts = raw.split("/");
            int day   = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return day + "." + month;
        } catch (Exception e) {
            return raw;
        }
    }

    public void removeItem(int position) {
        if (position >= 0 && position < data.size()) {
            data.remove(position);
            notifyItemRemoved(position);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvWorkerName, tvDate, tvTime, tvNotes;
        Button btnApprove, btnReject;

        VH(@NonNull View itemView) {
            super(itemView);
            tvWorkerName = itemView.findViewById(R.id.tvWorkerName);
            tvDate       = itemView.findViewById(R.id.tvRequestDate);
            tvTime       = itemView.findViewById(R.id.tvRequestTime);
            tvNotes      = itemView.findViewById(R.id.tvRequestNotes);
            btnApprove   = itemView.findViewById(R.id.btnApprove);
            btnReject    = itemView.findViewById(R.id.btnReject);
        }
    }
}
