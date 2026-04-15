package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.ShiftRequest;

import java.util.List;

public class WorkerShiftAdapter extends RecyclerView.Adapter<WorkerShiftAdapter.VH> {

    private final List<ShiftRequest> data;

    public WorkerShiftAdapter(List<ShiftRequest> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker_shift, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ShiftRequest shift = data.get(position);

        // פורמט תאריך: dd/MM/yyyy → d.M (ללא שנה, ללא אפסים מובילים)
        String dateDisplay = formatDate(shift.getDate());

        // סוג משמרת: "בוקר" / "ערב"
        String shiftType = shift.getShiftType();
        if (shiftType == null || shiftType.isEmpty()) {
            // fallback לישן: אם אין shiftType, נסיק לפי startTime
            String start = shift.getStartTime() != null ? shift.getStartTime() : "";
            shiftType = start.startsWith("06") ? "בוקר" : start.startsWith("14") ? "ערב" : "משמרת";
        }

        // שורה ראשית: "21.4  בוקר"
        holder.tvDate.setText(dateDisplay + "  " + shiftType);

        // שורת שעות: "06:00 – 14:00"
        String start = shift.getStartTime() != null ? shift.getStartTime() : "--:--";
        String end   = shift.getEndTime()   != null ? shift.getEndTime()   : "--:--";
        holder.tvTime.setText(start + " – " + end);

        // הערות (אם יש)
        String notes = shift.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("הערות: " + notes);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        String status = shift.getStatus();
        if ("rejected".equals(status)) {
            holder.tvStatus.setText("נדחה ✗");
            holder.tvStatus.setTextColor(0xFFE24B4A);          // error red
            holder.tvStatus.setBackgroundColor(0xFFFDEAEA);    // error_light
        } else if ("pending".equals(status)) {
            holder.tvStatus.setText("ממתין לשיבוץ");
            holder.tvStatus.setTextColor(0xFFBA7517);          // warning
            holder.tvStatus.setBackgroundColor(0xFFFDF3E3);    // warning_light
        } else {
            holder.tvStatus.setText("שובץ ✓");
            holder.tvStatus.setTextColor(0xFF1D9E75);          // success
            holder.tvStatus.setBackgroundColor(0xFFE6F6F1);    // success_light
        }
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    /**
     * ממיר "dd/MM/yyyy" → "d.M"  (לדוגמה "21/04/2026" → "21.4")
     */
    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            String[] parts = raw.split("/");
            int day   = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            return day + "." + month;
        } catch (Exception e) {
            return raw; // fallback: החזר כפי שהוא
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvNotes, tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate   = itemView.findViewById(R.id.tvShiftDate);
            tvTime   = itemView.findViewById(R.id.tvShiftTime);
            tvNotes  = itemView.findViewById(R.id.tvShiftNotes);
            tvStatus = itemView.findViewById(R.id.tvShiftStatus);
        }
    }
}
