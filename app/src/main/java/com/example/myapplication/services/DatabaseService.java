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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;


/// a service to interact with the Firebase Realtime Database.
/// this class is a singleton, use getInstance() to get an instance of this class
/// @see #getInstance()
/// @see FirebaseDatabase
public class DatabaseService {

    /// tag for logging
    /// @see Log
    private static final String TAG = "DatabaseService";

    /// paths for different data types in the database
    /// @see DatabaseService#readData(String)
    private static final String WORKER_PATH = "worker",
                                FOODS_PATH = "foods",
                                CARTS_PATH = "carts";

    /// callback interface for database operations
    /// @param <T> the type of the object to return
    /// @see DatabaseCallback#onCompleted(Object)
    /// @see DatabaseCallback#onFailed(Exception)
    public interface DatabaseCallback<T> {
        /// called when the operation is completed successfully
        public void onCompleted(T object);

        /// called when the operation fails with an exception
        public void onFailed(Exception e);
    }

    /// the instance of this class
    /// @see #getInstance()
    private static DatabaseService instance;

    /// the reference to the database
    /// @see DatabaseReference
    /// @see FirebaseDatabase#getReference()
    private final DatabaseReference databaseReference;

    /// use getInstance() to get an instance of this class
    /// @see DatabaseService#getInstance()
    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    /// get an instance of this class
    /// @return an instance of this class
    /// @see DatabaseService
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }


    // region private generic methods
    // to write and read data from the database

    /// write data to the database at a specific path
    /// @param path the path to write the data to
    /// @param data the data to write (can be any object, but must be serializable, i.e. must have a default constructor and all fields must have getters and setters)
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
        }
    });
    }

    /// remove data from the database at a specific path
    /// @param path the path to remove the data from
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
        }
    });
    }

    /// read data from the database at a specific path
    /// @param path the path to read the data from
    /// @return a DatabaseReference object to read the data from
    /// @see DatabaseReference

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }


    /// get data from the database at a specific path
    /// @param path the path to get the data from
    /// @param clazz the class of the object to return
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseCallback
    /// @see Class
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

    /// get a list of data from the database at a specific path
    /// @param path the path to get the data from
    /// @param clazz the class of the objects to return
    /// @param callback the callback to call when the operation is completed
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

    /// generate a new id for a new object in the database
    /// @param path the path to generate the id for
    /// @return a new id for the object
    /// @see String
    /// @see DatabaseReference#push()

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }


    /// run a transaction on the data at a specific path </br>
    /// good for incrementing a value or modifying an object in the database
    /// @param path the path to run the transaction on
    /// @param clazz the class of the object to return
    /// @param function the function to apply to the current value of the data
    /// @param callback the callback to call when the operation is completed
    /// @see DatabaseReference#runTransaction(Transaction.Handler)
    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                T currentValue = currentData.getValue(clazz);
                if (currentValue == null) {
                    currentValue = function.apply(null);
                } else {
                    currentValue = function.apply(currentValue);
                }
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

    // endregion of private methods for reading and writing data

    // public methods to interact with the database

    // region Worker Section



    /// create a new worker in the database
    /// @param worker the worker object to create (without the id, null)
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive new worker id
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see Worker
    public void createNewWorker(@NotNull final Worker worker,
                              @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(worker.getEmail(), worker.getPass())
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("TAG", "createWorkerWithEmail:success");
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
                } else {
                    Log.w("TAG", "createWorkerWithEmail:failure", task.getException());
                    if (callback != null)
                        callback.onFailed(task.getException());
                }
            });
    }


/// Login with email and password
    /// @param email , password
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive String (worker id)
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see FirebaseAuth

    public void LoginWorker(@NotNull final String email,final String password,
                              @Nullable final DatabaseCallback<String> callback) {

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.signInWithEmailAndPassword(email,password)

                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Log.d("TAG", "createWorkerWithEmail:success");

                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        callback.onCompleted(uid);
                      
                    } else {
                        Log.w("TAG", "createWorkerWithEmail:failure", task.getException());

                        if (callback != null)
                            callback.onFailed(task.getException());
                    }
                });
    }





    /// get a worker from the database
    /// @param uid the id of the worker to get
    /// @param callback the callback to call when the operation is completed
    ///               the callback will receive the worker object
    ///             if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see Worker
    public void getWorker(@NotNull final String uid, @NotNull final DatabaseCallback<Worker> callback) {
        getData(WORKER_PATH + "/" + uid, Worker.class, callback);
    }

    /// get all the worker from the database
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive a list of worker objects
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see Worker
    public void getWorkerList(@NotNull final DatabaseCallback<List<Worker>> callback) {
        getDataList(WORKER_PATH, Worker.class, callback);
    }

    /// delete a worker from the database
    /// @param uid the worker id to delete
    /// @param callback the callback to call when the operation is completed
    public void deleteWorker(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(WORKER_PATH + "/" + uid, callback);
    }

    /// get a worker by email and password
    /// @param email the email of the worker
    /// @param password the password of the worker
    /// @param callback the callback to call when the operation is completed
    ///            the callback will receive the worker object
    ///          if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see Worker
    public void getWorkerByEmailAndPassword(@NotNull final String email, @NotNull final String password, @NotNull final DatabaseCallback<Worker> callback) {
        readData(WORKER_PATH).orderByChild("email").equalTo(email).get()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Error getting data", task.getException());
                    callback.onFailed(task.getException());
                    return;
                }
                if (task.getResult().getChildrenCount() == 0) {
                    callback.onFailed(new Exception("Worker not found"));
                    return;
                }
                for (DataSnapshot dataSnapshot : task.getResult().getChildren()) {
                    Worker worker = dataSnapshot.getValue(Worker.class);
                    if (worker == null || !Objects.equals(worker.getPass(), password)) {
                        callback.onFailed(new Exception("Invalid email or password"));
                        return;
                    }

                    callback.onCompleted(worker);
                    return;

                }
            });
    }

    /// check if an email already exists in the database
    /// @param email the email to check
    /// @param callback the callback to call when the operation is completed
    public void checkIfEmailExists(@NotNull final String email, @NotNull final DatabaseCallback<Boolean> callback) {
        readData(WORKER_PATH).orderByChild("email").equalTo(email).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Error getting data", task.getException());
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
                if (callback != null) {
                    callback.onCompleted(null);
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) {
                    callback.onFailed(e);
                }
            }
        });
    }


    // endregion Worker Section

    // region food section

    /// create a new food in the database
    /// @param food the food object to create
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive void
    ///             if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see Food
    public void createNewFood(@NotNull final Food food, @Nullable final DatabaseCallback<Void> callback) {
        writeData(FOODS_PATH + "/" + food.toString(), food, callback);
    }

    /// get a food from the database
    /// @param foodId the id of the food to get
    /// @param callback the callback to call when the operation is completed
    ///               the callback will receive the food object
    ///              if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see Food
    public void getFood(@NotNull final String foodId, @NotNull final DatabaseCallback<Food> callback) {
        getData(FOODS_PATH + "/" + foodId, Food.class, callback);
    }

    /// get all the foods from the database
    /// @param callback the callback to call when the operation is completed
    ///              the callback will receive a list of food objects
    ///            if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see List
    /// @see Food
    public void getFoodList(@NotNull final DatabaseCallback<List<Food>> callback) {
        getDataList(FOODS_PATH, Food.class, callback);
    }

    /// generate a new id for a new food in the database
    /// @return a new id for the food
    /// @see #generateNewId(String)
    /// @see Food
    public String generateFoodId() {
        return generateNewId(FOODS_PATH);
    }

    /// delete a food from the database
    /// @param foodId the id of the food to delete
    /// @param callback the callback to call when the operation is completed
    public void deleteFood(@NotNull final String foodId, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(FOODS_PATH + "/" + foodId, callback);
    }

    // endregion food section

    // region cart section

    /// create a new cart in the database
    /// @param cart the cart object to create
    /// @param callback the callback to call when the operation is completed
    ///               the callback will receive void
    ///              if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see Shift
    public void createNewCart(@NotNull final Shift cart, @Nullable final DatabaseCallback<Void> callback) {
        writeData(CARTS_PATH + "/" + cart.toString(), cart, callback);
    }

    /// get a cart from the database
    /// @param cartId the id of the cart to get
    /// @param callback the callback to call when the operation is completed
    ///                the callback will receive the cart object
    ///               if the operation fails, the callback will receive an exception
    /// @see DatabaseCallback
    /// @see Shift
    public void getCart(@NotNull final String cartId, @NotNull final DatabaseCallback<Shift> callback) {
        getData(CARTS_PATH + "/" + cartId, Shift.class, callback);
    }

    /// get all the carts from the database
    /// @param callback the callback to call when the operation is completed
    ///               the callback will receive a list of cart objects
    ///
    public void getCartList(@NotNull final DatabaseCallback<List<Shift>> callback) {
        getDataList(CARTS_PATH, Shift.class, callback);
    }

    /// get all the carts of a specific worker from the database
    /// @param uid the id of the worker to get the carts for
    /// @param callback the callback to call when the operation is completed
    public void getWorkerCartList(@NotNull String uid, @NotNull final DatabaseCallback<List<Shift>> callback) {
        getCartList(new DatabaseCallback<>() {
            @Override
            public void onCompleted(List<Shift> carts) {
                carts.removeIf(cart -> !Objects.equals(cart.toString(), uid));
                callback.onCompleted(carts);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }


    /// generate a new id for a new cart in the database
    /// @return a new id for the cart
    /// @see #generateNewId(String)
    /// @see Shift
    public String generateCartId() {
        return generateNewId(CARTS_PATH);
    }

    /// delete a cart from the database
    /// @param cartId the id of the cart to delete
    /// @param callback the callback to call when the operation is completed
    public void deleteCart(@NotNull final String cartId, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(CARTS_PATH + "/" + cartId, callback);
    }

    // endregion cart section

}

