package com.example.myapplication.services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.ShiftInvitation;
import com.example.myapplication.model.ShiftRequest;
import com.example.myapplication.model.Worker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

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
    private static final String WORKER_PATH          = "worker";
    private static final String SHIFTS_PATH          = "Shifts";
    private static final String SHIFT_REQUESTS_PATH  = "ShiftRequests";
    private static final String WORKER_SHIFTS_PATH   = "WorkerShifts";
    private static final String SHIFTS_FOR_DATE_PATH = "ShiftsForDate";
    private static final String ATTENDANCE_PATH         = "Attendance";
    private static final String SHIFT_INVITATIONS_PATH  = "ShiftInvitations";

    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }

    private static DatabaseService instance;
    private final DatabaseReference databaseReference;
    private final FirebaseAuth mAuth;

    private DatabaseService() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
    }

    public static DatabaseService getInstance() {
        if (instance == null) instance = new DatabaseService();
        return instance;
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    public DatabaseReference getDbReference() {
        return databaseReference;
    }

    // region עזרים פרטיים (generic helpers)

    private void writeData(@NotNull final String path, @NotNull final Object data,
                           @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(path).setValue(data, (error, ref) -> {
            if (callback == null) return;
            if (error != null) callback.onFailed(error.toException());
            else callback.onCompleted(null);
        });
    }

    private void deleteData(@NotNull final String path,
                            @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(path).removeValue((error, ref) -> {
            if (callback == null) return;
            if (error != null) callback.onFailed(error.toException());
            else callback.onCompleted(null);
        });
    }

    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz,
                             @NotNull final DatabaseCallback<T> callback) {
        databaseReference.child(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            callback.onCompleted(task.getResult().getValue(clazz));
        });
    }

    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz,
                                 @NotNull final DatabaseCallback<List<T>> callback) {
        databaseReference.child(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailed(task.getException());
                return;
            }
            List<T> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                T item = ds.getValue(clazz);
                if (item != null) list.add(item);
            }
            callback.onCompleted(list);
        });
    }

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz,
                                    @NotNull java.util.function.UnaryOperator<T> function,
                                    @NotNull final DatabaseCallback<T> callback) {
        databaseReference.child(path).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                T val = currentData.getValue(clazz);
                currentData.setValue(function.apply(val));
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot currentData) {
                if (error != null) { callback.onFailed(error.toException()); return; }
                callback.onCompleted(currentData != null ? currentData.getValue(clazz) : null);
            }
        });
    }

    // endregion

    // ==========================================
    // region Workers (עובדים)
    // ==========================================

    public void createNewWorker(@NotNull final Worker worker,
                                @Nullable final DatabaseCallback<String> callback) {
        mAuth.createUserWithEmailAndPassword(worker.getEmail(), worker.getPass())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                        worker.setId(uid);
                        writeData(WORKER_PATH + "/" + uid, worker, new DatabaseCallback<Void>() {
                            @Override public void onCompleted(Void v) { if (callback != null) callback.onCompleted(uid); }
                            @Override public void onFailed(Exception e) { if (callback != null) callback.onFailed(e); }
                        });
                    } else {
                        if (callback != null) callback.onFailed(task.getException());
                    }
                });
    }

    public void LoginWorker(@NotNull final String email, final String password,
                            @Nullable final DatabaseCallback<String> callback) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                if (callback != null) callback.onCompleted(uid);
            } else {
                if (callback != null) callback.onFailed(task.getException());
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

    public void updateWorker(@NotNull final Worker worker, @Nullable final DatabaseCallback<Void> callback) {
        runTransaction(WORKER_PATH + "/" + worker.getId(), Worker.class, current -> worker,
                new DatabaseCallback<Worker>() {
                    @Override public void onCompleted(Worker object) { if (callback != null) callback.onCompleted(null); }
                    @Override public void onFailed(Exception e) { if (callback != null) callback.onFailed(e); }
                });
    }

    public void checkIfEmailExists(@NotNull final String email,
                                   @NotNull final DatabaseCallback<Boolean> callback) {
        databaseReference.child(WORKER_PATH).orderByChild("email").equalTo(email).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
                    callback.onCompleted(task.getResult().getChildrenCount() > 0);
                });
    }

    // endregion

    // ==========================================
    // region Shifts (משמרות)
    // ==========================================

    public String generateShiftId() {
        return generateNewId(SHIFTS_PATH);
    }

    // שמירת משמרת - מפתח הוא ה-ID של המשמרת
    public void createNewShift(@NotNull final Shift shift, @Nullable final DatabaseCallback<Void> callback) {
        writeData(SHIFTS_PATH + "/" + shift.getId(), shift, callback);
    }

    public void getShift(@NotNull final String shiftId, @NotNull final DatabaseCallback<Shift> callback) {
        getData(SHIFTS_PATH + "/" + shiftId, Shift.class, callback);
    }

    public void getShiftList(@NotNull final DatabaseCallback<List<Shift>> callback) {
        getDataList(SHIFTS_PATH, Shift.class, callback);
    }

    public void deleteShift(@NotNull final String shiftId, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(SHIFTS_PATH + "/" + shiftId, callback);
    }

    // מציאת משמרת לפי תאריך (dayOfWeek0)
    public void getShiftByDate(@NotNull final String dateStr,
                               @NotNull final DatabaseCallback<Shift> callback) {
        databaseReference.child(SHIFTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            Shift found = null;
            for (DataSnapshot ds : task.getResult().getChildren()) {
                Shift shift = ds.getValue(Shift.class);
                if (shift != null && Objects.equals(shift.getDayOfWeek0(), dateStr)) {
                    found = shift;
                    break;
                }
            }
            callback.onCompleted(found);
        });
    }

    // מחיקת משמרות שעבר זמנן
    public void deletePastShifts() {
        databaseReference.child(SHIFTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                Shift shift = ds.getValue(Shift.class);
                if (shift == null || shift.getDayOfWeek0() == null) continue;
                try {
                    Date shiftDate = sdf.parse(shift.getDayOfWeek0());
                    if (shiftDate != null && shiftDate.before(today)) {
                        Log.d(TAG, "מוחק משמרת עבר: " + shift.getDayOfWeek0());
                        ds.getRef().removeValue();
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // endregion

    // ==========================================
    // region ShiftRequests (בקשות משמרת)
    // ==========================================

    public String generateShiftRequestId() {
        return generateNewId(SHIFT_REQUESTS_PATH);
    }

    // שמירת בקשת משמרת חדשה (מגדיר requestId אוטומטית)
    public void addShiftRequest(@NotNull final ShiftRequest request,
                                @Nullable final DatabaseCallback<Void> callback) {
        String id = generateNewId(SHIFT_REQUESTS_PATH);
        request.setRequestId(id);
        if (request.getStatus() == null) request.setStatus("pending");
        writeData(SHIFT_REQUESTS_PATH + "/" + id, request, callback);
    }

    // קבלת כל הבקשות הממתינות (למנהל)
    public void getPendingShiftRequests(@NotNull final DatabaseCallback<List<ShiftRequest>> callback) {
        databaseReference.child(SHIFT_REQUESTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            List<ShiftRequest> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                ShiftRequest req = ds.getValue(ShiftRequest.class);
                if (req == null) continue;
                // בקשה ממתינה: status == "pending" או (ישן) isApproved == false ו-status == null
                boolean isPending = "pending".equals(req.getStatus())
                        || (req.getStatus() == null && !req.isApproved());
                if (isPending) list.add(req);
            }
            callback.onCompleted(list);
        });
    }

    // קבלת כל הבקשות לפי שידה
    public void getShiftRequests(@NotNull final DatabaseCallback<List<ShiftRequest>> callback) {
        getDataList(SHIFT_REQUESTS_PATH, ShiftRequest.class, callback);
    }

    // עדכון בקשת משמרת (לאישור/דחייה)
    public void updateShiftRequest(@NotNull final ShiftRequest request,
                                   @Nullable final DatabaseCallback<Void> callback) {
        writeData(SHIFT_REQUESTS_PATH + "/" + request.getRequestId(), request, callback);
    }

    // בקשות משמרת לפי תאריך וסוג (למנהל בעת יצירת משמרת)
    public void getShiftRequestsByDateAndType(@NotNull final String date,
                                              @NotNull final String shiftType,
                                              @NotNull final DatabaseCallback<List<ShiftRequest>> callback) {
        databaseReference.child(SHIFT_REQUESTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            List<ShiftRequest> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                ShiftRequest req = ds.getValue(ShiftRequest.class);
                if (req == null) continue;
                boolean dateMatch = date.equals(req.getDate());
                boolean typeMatch = shiftType.equals(req.getShiftType());
                // הכנס רק בקשות ממתינות (לא שובצו / לא נדחו)
                boolean pending = !"assigned".equals(req.getStatus())
                        && !"rejected".equals(req.getStatus());
                if (dateMatch && typeMatch && pending) list.add(req);
            }
            callback.onCompleted(list);
        });
    }

    // בדיקה האם לעובד כבר יש בקשה לאותה תאריך + סוג (ממתינה או שובצה)
    public void checkWorkerHasRequest(@NotNull final String workerId,
                                       @NotNull final String date,
                                       @NotNull final String shiftType,
                                       @NotNull final DatabaseCallback<Boolean> callback) {
        databaseReference.child(SHIFT_REQUESTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            for (DataSnapshot ds : task.getResult().getChildren()) {
                ShiftRequest req = ds.getValue(ShiftRequest.class);
                if (req != null
                        && workerId.equals(req.getWorkerId())
                        && date.equals(req.getDate())
                        && shiftType.equals(req.getShiftType())
                        && !"rejected".equals(req.getStatus())) {
                    callback.onCompleted(true);
                    return;
                }
            }
            callback.onCompleted(false);
        });
    }

    // קבלת כל הבקשות הממתינות (לא-שובצו) של עובד ספציפי
    public void getWorkerPendingRequests(@NotNull final String workerId,
                                          @NotNull final DatabaseCallback<List<ShiftRequest>> callback) {
        databaseReference.child(SHIFT_REQUESTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            List<ShiftRequest> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                ShiftRequest req = ds.getValue(ShiftRequest.class);
                if (req != null && workerId.equals(req.getWorkerId())
                        && "pending".equals(req.getStatus())) {
                    list.add(req);
                }
            }
            callback.onCompleted(list);
        });
    }

    // קבלת בקשות שנדחו על ידי המנהל עבור עובד ספציפי
    public void getWorkerRejectedRequests(@NotNull final String workerId,
                                           @NotNull final DatabaseCallback<List<ShiftRequest>> callback) {
        databaseReference.child(SHIFT_REQUESTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            List<ShiftRequest> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                ShiftRequest req = ds.getValue(ShiftRequest.class);
                if (req != null && workerId.equals(req.getWorkerId())
                        && "rejected".equals(req.getStatus())) {
                    list.add(req);
                }
            }
            callback.onCompleted(list);
        });
    }

    // קבלת כל המשמרות שנשמרו לתאריך ספציפי (עבור CurrentShiftActivity)
    public void getTodayShifts(@NotNull final String date,
                                @NotNull final DatabaseCallback<List<Shift>> callback) {
        databaseReference.child(SHIFTS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            List<Shift> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                Shift shift = ds.getValue(Shift.class);
                if (shift != null && date.equals(shift.getDayOfWeek0())) {
                    list.add(shift);
                }
            }
            callback.onCompleted(list);
        });
    }

    // endregion

    // ==========================================
    // region ShiftInvitations (הזמנות מנהל לעובדים)
    // ==========================================

    public void saveShiftInvitation(@NotNull final ShiftInvitation invitation,
                                    @Nullable final DatabaseCallback<Void> callback) {
        String id = generateNewId(SHIFT_INVITATIONS_PATH);
        invitation.setId(id);
        writeData(SHIFT_INVITATIONS_PATH + "/" + id, invitation, callback);
    }

    public void getOpenInvitations(@NotNull final DatabaseCallback<List<ShiftInvitation>> callback) {
        databaseReference.child(SHIFT_INVITATIONS_PATH).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
            List<ShiftInvitation> list = new ArrayList<>();
            for (DataSnapshot ds : task.getResult().getChildren()) {
                ShiftInvitation inv = ds.getValue(ShiftInvitation.class);
                if (inv != null && inv.isOpen()) list.add(inv);
            }
            callback.onCompleted(list);
        });
    }

    public void deleteShiftInvitation(@NotNull final String invitationId,
                                      @Nullable final DatabaseCallback<Void> callback) {
        deleteData(SHIFT_INVITATIONS_PATH + "/" + invitationId, callback);
    }

    // endregion

    // ==========================================
    // region WorkerShifts (משמרות מאושרות לעובד)
    // ==========================================

    public void saveWorkerShift(@NotNull final String workerId,
                                @NotNull final ShiftRequest approved,
                                @Nullable final DatabaseCallback<Void> callback) {
        writeData(WORKER_SHIFTS_PATH + "/" + workerId + "/" + approved.getRequestId(),
                approved, callback);
    }

    public void getWorkerShifts(@NotNull final String workerId,
                                @NotNull final DatabaseCallback<List<ShiftRequest>> callback) {
        databaseReference.child(WORKER_SHIFTS_PATH).child(workerId).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
                    List<ShiftRequest> list = new ArrayList<>();
                    for (DataSnapshot ds : task.getResult().getChildren()) {
                        ShiftRequest s = ds.getValue(ShiftRequest.class);
                        if (s != null) list.add(s);
                    }
                    callback.onCompleted(list);
                });
    }

    // endregion

    // ==========================================
    // region ShiftsForDate (אינדקס תאריך → נוכחות יומית)
    // ==========================================

    public void saveShiftForDate(@NotNull final String date,
                                 @NotNull final ShiftRequest approved,
                                 @Nullable final DatabaseCallback<Void> callback) {
        writeData(SHIFTS_FOR_DATE_PATH + "/" + date.replace("/", "-")
                + "/" + approved.getWorkerId(), approved, callback);
    }

    public void getShiftsForDate(@NotNull final String date,
                                 @NotNull final DatabaseCallback<List<ShiftRequest>> callback) {
        databaseReference.child(SHIFTS_FOR_DATE_PATH).child(date.replace("/", "-"))
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
                    List<ShiftRequest> list = new ArrayList<>();
                    for (DataSnapshot ds : task.getResult().getChildren()) {
                        ShiftRequest s = ds.getValue(ShiftRequest.class);
                        if (s != null) list.add(s);
                    }
                    callback.onCompleted(list);
                });
    }

    // endregion

    // ==========================================
    // region Attendance (נוכחות)
    // ==========================================

    public void saveAttendance(@NotNull final Attendance attendance,
                               @Nullable final DatabaseCallback<Void> callback) {
        writeData(ATTENDANCE_PATH + "/" + attendance.getDate().replace("/", "-")
                + "/" + attendance.getWorkerId(), attendance, callback);
    }

    public void getAttendanceByDate(@NotNull final String date,
                                    @NotNull final DatabaseCallback<List<Attendance>> callback) {
        databaseReference.child(ATTENDANCE_PATH).child(date.replace("/", "-"))
                .get().addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) { callback.onFailed(task.getException()); return; }
                    List<Attendance> list = new ArrayList<>();
                    for (DataSnapshot ds : task.getResult().getChildren()) {
                        Attendance a = ds.getValue(Attendance.class);
                        if (a != null) list.add(a);
                    }
                    callback.onCompleted(list);
                });
    }

    public void getWorkerAttendance(@NotNull final String workerId, @NotNull final String date,
                                    @NotNull final DatabaseCallback<Attendance> callback) {
        getData(ATTENDANCE_PATH + "/" + date.replace("/", "-") + "/" + workerId,
                Attendance.class, callback);
    }

    // endregion
}
