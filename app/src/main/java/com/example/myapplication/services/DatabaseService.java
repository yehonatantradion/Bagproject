package com.example.myapplication.services;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.model.Worker;
import com.example.myapplication.model.Shift;
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

public class DatabaseService {

    private static final String TAG = "DatabaseService";

    // נתיבים נקיים ורלוונטיים בלבד
    private static final String WORKER_PATH = "workers",
            SHIFTS_PATH = "shifts";

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

    // region Private Helpers
    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        databaseReference.child(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        databaseReference.child(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            for (DataSnapshot dataSnapshot : task.getResult().getChildren()) {
                T t = dataSnapshot.getValue(clazz);
                tList.add(t);
            }
            callback.onCompleted(tList);
        });
    }

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }
    // endregion

    // region Worker Section
    public void createNewWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(worker.getEmail(), worker.getPass())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
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
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
    }

    public void getWorkerList(@NotNull final DatabaseCallback<List<Worker>> callback) {
        getDataList(WORKER_PATH, Worker.class, callback);
    }
    // endregion

    // region Shift Section - הכל כאן שונה מ-Cart ל-Shift
    public String generateShiftId() {
        return generateNewId(SHIFTS_PATH);
    }

    public void createNewShift(@NotNull final Shift shift, @Nullable final DatabaseCallback<Void> callback) {
        writeData(SHIFTS_PATH + "/" + shift.getShiftId(), shift, callback);
    }

    public void getShiftList(@NotNull final DatabaseCallback<List<Shift>> callback) {
        getDataList(SHIFTS_PATH, Shift.class, callback);
    }

    public void deleteShift(@NotNull final String shiftId, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(SHIFTS_PATH + "/" + shiftId, callback);
    }
    // endregion
}