package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.Worker;

import java.util.ArrayList;


    public  class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

        private final ArrayList<Worker> data;

     public     UsersAdapter(ArrayList<Worker> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Worker worker = data.get(position);
            holder.line1.setText(worker.getfName());

            String role = worker.getIsAdmin() ? "admin" : "worker";
            String email = (worker.getEmail() == null || worker.getEmail().isEmpty()) ? "אין מייל" : worker.getEmail();
            String phone = (worker.getPhone() == null || worker.getPhone().isEmpty()) ? "אין טלפון" : worker.getPhone();

            holder.line2.setText("ROLE: " + role + " | " + email + " | " + phone);
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView line1, line2;

            VH(@NonNull View itemView) {
                super(itemView);
                line1 = itemView.findViewById(android.R.id.text1);
                line2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

