package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {

    public static class AttendanceItem {
        public String workerName;
        public boolean arrived;
        public long checkInTime;

        public AttendanceItem(String workerName, boolean arrived, long checkInTime) {
            this.workerName  = workerName;
            this.arrived     = arrived;
            this.checkInTime = checkInTime;
        }
    }

    private final List<AttendanceItem> data;

    public AttendanceAdapter(List<AttendanceItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AttendanceItem item = data.get(position);
        holder.tvName.setText(item.workerName);

        if (item.arrived) {
            holder.tvStatus.setText("הגיע");
            holder.tvStatus.setTextColor(0xFF2ECC71);
            if (item.checkInTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                holder.tvCheckInTime.setText("שעת הגעה: " + sdf.format(new Date(item.checkInTime)));
                holder.tvCheckInTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvCheckInTime.setVisibility(View.GONE);
            }
        } else {
            holder.tvStatus.setText("לא הגיע");
            holder.tvStatus.setTextColor(0xFFE53935);
            holder.tvCheckInTime.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvCheckInTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName       = itemView.findViewById(R.id.tvAttendanceName);
            tvStatus     = itemView.findViewById(R.id.tvAttendanceStatus);
            tvCheckInTime = itemView.findViewById(R.id.tvCheckInTime);
        }
    }
}
