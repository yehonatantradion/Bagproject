package com.example.myapplication.services;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.myapplication.model.Worker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
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
    // נתיבים ב-Firebase
    private static final String WORKER_PATH = "worker";
    private static final String SHIFTS_PATH = "Shifts";

    private static DatabaseService instance;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth mAuth;

    private DatabaseService() {
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        this.mAuth = FirebaseAuth.getInstance();
    }

    public static DatabaseService getInstance() {
        if (instance == null) instance = new DatabaseService();
        return instance;
    }

    public DatabaseReference getDbReference() {
        return databaseReference;
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }

    // ==========================================
    // region Worker Section (ניהול עובדים)
    // ==========================================

    // יצירת עובד חדש (כולל Auth ו-Database)
    public void createNewWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<String> callback) {
        mAuth.createUserWithEmailAndPassword(worker.getEmail(), worker.getPass())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        worker.setId(uid);
                        // שמירת פרטי העובד בדאטאבייס
                        databaseReference.child(WORKER_PATH).child(uid).setValue(worker, (error, ref) -> {
                            if (error != null && callback != null) callback.onFailed(error.toException());
                            else if (callback != null) callback.onCompleted(uid);
                        });
                    } else if (callback != null) callback.onFailed(task.getException());
                });
    }

    // התחברות עובד קיים
    public void LoginWorker(@NotNull final String email, final String password, @Nullable final DatabaseCallback<String> callback) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                if (callback != null) callback.onCompleted(uid);
            } else if (callback != null) callback.onFailed(task.getException());
        });
    }

    // קבלת עובד ספצי לפי ID
    public void getWorker(@NotNull final String uid, @NotNull final DatabaseCallback<Worker> callback) {
        databaseReference.child(WORKER_PATH).child(uid).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) callback.onFailed(task.getException());
            else callback.onCompleted(task.getResult().getValue(Worker.class));
        });
    }

    // קבלת רשימת כל העובדים
    public void getWorkerList(@NotNull final DatabaseCallback<List<Worker>> callback) {
        databaseReference.child(WORKER_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) callback.onFailed(task.getException());
            else {
                List<Worker> list = new ArrayList<>();
                for (DataSnapshot ds : task.getResult().getChildren()) {
                    Worker w = ds.getValue(Worker.class);
                    if (w != null) list.add(w);
                }
                callback.onCompleted(list);
            }
        });
    }

    // עדכון פרטי עובד (כולל הפיכה למנהל)
    public void updateWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<Void> callback) {
        if (worker.getId() == null) {
            if (callback != null) callback.onFailed(new Exception("Worker ID is null"));
            return;
        }
        databaseReference.child(WORKER_PATH).child(worker.getId()).setValue(worker)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onCompleted(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailed(e);
                });
    }
    // endregion

    // ==========================================
    // region Shift Section (ניהול משמרות)
    // ==========================================

    // יצירת ID ייחודי למשמרת
    public String generateShiftId() {
        return databaseReference.child(SHIFTS_PATH).push().getKey();
    }

    // יצירה או עדכון של משמרת
    public void createNewShift(@NotNull final Shift shift, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(SHIFTS_PATH).child(shift.getId()).setValue(shift, (error, ref) -> {
            if (error != null && callback != null) callback.onFailed(error.toException());
            else if (callback != null) callback.onCompleted(null);
        });
    }

    // קבלת משמרת לפי תאריך (ללא צורך באינדקסים)
    public void getShiftByDate(@NotNull final String dateStr, @NotNull final DatabaseCallback<Shift> callback) {
        databaseReference.child(SHIFTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }

            Shift foundShift = null;
            // מעבר על כל המשמרות ומציאת התאריך המתאים
            for (DataSnapshot ds : task.getResult().getChildren()) {
                Shift shift = ds.getValue(Shift.class);
                // בדיקת התאמה (dayOfWeek0 מחזיק את מחרוזת התאריך)
                if (shift != null && Objects.equals(shift.getDayOfWeek0(), dateStr)) {
                    foundShift = shift;
                    break;
                }
            }
            callback.onCompleted(foundShift);
        });
    }

    // מחיקת משמרות שעבר זמנן
    public void deletePastShifts() {
        databaseReference.child(SHIFTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            // הגדרת "היום" בחצות הלילה (כדי להשוות רק תאריכים ללא שעות)
            Calendar cal = Calendar.getInstance();
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
                        // אם תאריך המשמרת קטן מהיום -> מחיקה
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
    // endregion
}