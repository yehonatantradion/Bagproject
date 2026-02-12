package com.example.myapplication.services;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.model.Worker;
import com.example.myapplication.model.Shift; // ודא שזה מצביע למודל הנכון
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * שירות לאינטראקציה עם Firebase Realtime Database.
 * מחלקה זו היא Singleton, השתמש ב-getInstance() כדי לקבל מופע שלה.
 */
public class DatabaseService {

    private static final String TAG = "DatabaseService";

    // נתיבים בבסיס הנתונים
    private static final String WORKER_PATH = "worker",
            FOODS_PATH = "foods",
            CARTS_PATH = "carts",
            SHIFTS_PATH = "Shifts"; // נתיב חדש למשמרות

    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }

    private static DatabaseService instance;
    private final DatabaseReference databaseReference;

    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    /**
     * מחזירה רפרנס ישיר לשורש מסד הנתונים.
     */
    public DatabaseReference getDbReference() {
        return databaseReference;
    }

    // region private generic methods

    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            T data = task.getResult().getValue(clazz);
            callback.onCompleted(data);
        });
    }

    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                tList.add(t);
            });
            callback.onCompleted(tList);
        });
    }

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                T currentValue = currentData.getValue(clazz);
                currentValue = function.apply(currentValue);
                currentData.setValue(currentValue);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction failed", error.toException());
                    callback.onFailed(error.toException());
                    return;
                }
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });
    }

    // endregion

    // region Worker Section

    public void createNewWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(worker.getEmail(), worker.getPass())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        worker.setId(uid);
                        writeData(WORKER_PATH + "/" + uid, worker, new DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void v) {
                                if (callback != null) callback.onCompleted(uid);
                            }
                            @Override
                            public void onFailed(Exception e) {
                                if (callback != null) callback.onFailed(e);
                            }
                        });
                    } else if (callback != null) {
                        callback.onFailed(task.getException());
                    }
                });
    }

    public void LoginWorker(@NotNull final String email, final String password, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        if (callback != null) callback.onCompleted(uid);
                    } else if (callback != null) {
                        callback.onFailed(task.getException());
                    }
                });
    }

    public void getWorker(@NotNull final String uid, @NotNull final DatabaseCallback<Worker> callback) {
        getData(WORKER_PATH + "/" + uid, Worker.class, callback);
    }

    public void getWorkerList(@NotNull final DatabaseCallback<List<Worker>> callback) {
        getDataList(WORKER_PATH, Worker.class, callback);
    }

    public void deleteWorker(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(WORKER_PATH + "/" + uid, callback);
    }

    public void checkIfEmailExists(@NotNull final String email, @NotNull final DatabaseCallback<Boolean> callback) {
        readData(WORKER_PATH).orderByChild("email").equalTo(email).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onFailed(task.getException());
                        return;
                    }
                    boolean exists = task.getResult().getChildrenCount() > 0;
                    callback.onCompleted(exists);
                });
    }

    public void updateWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<Void> callback) {
        runTransaction(WORKER_PATH + "/" + worker.getId(), Worker.class, currentWorker -> worker, new DatabaseCallback<Worker>() {
            @Override
            public void onCompleted(Worker object) {
                if (callback != null) callback.onCompleted(null);
            }
            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    // endregion

    // region food section (דוגמה קיימת בפרויקט שלך)
    public void getFoodList(@NotNull final DatabaseCallback<List<Food>> callback) {
        getDataList(FOODS_PATH, Food.class, callback);
    }
    // endregion

    // region Shift section
    public void createNewShift(@NotNull final Shift shift, @Nullable final DatabaseCallback<Void> callback) {
        writeData(SHIFTS_PATH + "/" + generateNewId(SHIFTS_PATH), shift, callback);
    }
    // endregion
}