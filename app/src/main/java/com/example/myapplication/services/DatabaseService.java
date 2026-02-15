package com.example.myapplication.services;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.Shift;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseService {
    private static final String TAG = "DatabaseService";
    private static final String WORKER_PATH = "worker",
            SHIFTS_PATH = "Shifts";

    private static DatabaseService instance;
    private final DatabaseReference databaseReference;

    private DatabaseService() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public static DatabaseService getInstance() {
        if (instance == null) instance = new DatabaseService();
        return instance;
    }

    public DatabaseReference getDbReference() {
        return databaseReference;
    }

    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }

    // --- Worker Section ---

    // מתודה לרישום עובד חדש (עבור Register.java)
    public void createNewWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(worker.getEmail(), worker.getPass())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        worker.setId(uid);
                        databaseReference.child(WORKER_PATH).child(uid).setValue(worker, (error, ref) -> {
                            if (error != null) {
                                if (callback != null) callback.onFailed(error.toException());
                            } else {
                                if (callback != null) callback.onCompleted(uid);
                            }
                        });
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
    }

    // מתודה להתחברות עובד (עבור Login.java)
    public void LoginWorker(@NotNull final String email, final String password, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                if (callback != null) callback.onCompleted(uid);
            } else if (callback != null) {
                callback.onFailed(task.getException());
            }
        });
    }

    public void getWorker(@NotNull final String uid, @NotNull final DatabaseCallback<Worker> callback) {
        databaseReference.child(WORKER_PATH).child(uid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            callback.onCompleted(task.getResult().getValue(Worker.class));
        });
    }

    public void getWorkerList(@NotNull final DatabaseCallback<List<Worker>> callback) {
        databaseReference.child(WORKER_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            List<Worker> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                list.add(ds.getValue(Worker.class));
            }
            callback.onCompleted(list);
        });
    }

    // --- Shift Section ---

    public String generateShiftId() {
        return databaseReference.child(SHIFTS_PATH).push().getKey();
    }

    public void createNewShift(@NotNull final Shift shift, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(SHIFTS_PATH).child(shift.getId()).setValue(shift, (error, ref) -> {
            if (error != null && callback != null) callback.onFailed(error.toException());
            else if (callback != null) callback.onCompleted(null);
        });
    }
}