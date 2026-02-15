package com.example.myapplication.services;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.myapplication.model.Worker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.jetbrains.annotations.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    public void createNewWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(worker.getEmail(), worker.getPass())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        worker.setId(uid);
                        databaseReference.child(WORKER_PATH).child(uid).setValue(worker, (error, ref) -> {
                            if (error != null && callback != null) callback.onFailed(error.toException());
                            else if (callback != null) callback.onCompleted(uid);
                        });
                    } else if (callback != null) callback.onFailed(task.getException());
                });
    }

    public void LoginWorker(@NotNull final String email, final String password, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                if (callback != null) callback.onCompleted(uid);
            } else if (callback != null) callback.onFailed(task.getException());
        });
    }

    public void getWorker(@NotNull final String uid, @NotNull final DatabaseCallback<Worker> callback) {
        databaseReference.child(WORKER_PATH).child(uid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) callback.onFailed(task.getException());
            else callback.onCompleted(task.getResult().getValue(Worker.class));
        });
    }

    public void getWorkerList(@NotNull final DatabaseCallback<List<Worker>> callback) {
        databaseReference.child(WORKER_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) callback.onFailed(task.getException());
            else {
                List<Worker> list = new ArrayList<>();
                for (DataSnapshot ds : task.getResult().getChildren()) {
                    list.add(ds.getValue(Worker.class));
                }
                callback.onCompleted(list);
            }
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

    public void getShiftByDate(@NotNull final String dateStr, @NotNull final DatabaseCallback<Shift> callback) {
        databaseReference.child(SHIFTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }

            Shift foundShift = null;
            for (DataSnapshot ds : task.getResult().getChildren()) {
                Shift shift = ds.getValue(Shift.class);
                if (shift != null && Objects.equals(shift.getDayOfWeek0(), dateStr)) {
                    foundShift = shift;
                    break;
                }
            }
            callback.onCompleted(foundShift);
        });
    }

    // --- פונקציה חדשה למחיקת משמרות עבר ---
    public void deletePastShifts() {
        databaseReference.child(SHIFTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date today = new Date();

            // איפוס השעות של "היום" כדי להשוות רק תאריכים
            Calendar cal = Calendar.getInstance();
            cal.setTime(today);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date todayZeroTime = cal.getTime();

            for (DataSnapshot ds : task.getResult().getChildren()) {
                Shift shift = ds.getValue(Shift.class);
                if (shift != null && shift.getDayOfWeek0() != null) {
                    try {
                        Date shiftDate = sdf.parse(shift.getDayOfWeek0());
                        // אם תאריך המשמרת קטן מהיום -> למחוק
                        if (shiftDate != null && shiftDate.before(todayZeroTime)) {
                            Log.d(TAG, "Deleting past shift: " + shift.getDayOfWeek0());
                            ds.getRef().removeValue();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}