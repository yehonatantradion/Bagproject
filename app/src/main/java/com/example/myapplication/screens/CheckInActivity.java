package com.example.myapplication.screens;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;
import com.example.myapplication.model.Attendance;
import com.example.myapplication.model.ShiftRequest;
import com.example.myapplication.model.Worker;
import com.example.myapplication.services.DatabaseService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CheckInActivity extends BaseActivity {

    // ===================================================================
    // שנה את הקואורדינטות לפי מיקום בית העסק האמיתי
    // Change these to your actual workplace GPS coordinates
    private static final double WORKPLACE_LATITUDE  = 31.7683;  // ירושלים - דוגמה
    private static final double WORKPLACE_LONGITUDE = 35.2137;  // ירושלים - דוגמה
    private static final float  ALLOWED_RADIUS_METERS = 300f;   // רדיוס מותר (מטרים)
    // ===================================================================

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    private TextView tvShiftInfo, tvLocationStatus;
    private ProgressBar progressBarLocation;
    private Button btnConfirmArrival, btnBack;

    private Worker currentWorker;
    private ShiftRequest todayShift = null;
    private LocationManager locationManager;
    private DatabaseService db;
    private LocationListener locationListener;

    private final String todayDate =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = DatabaseService.getInstance();
        currentWorker = (Worker) getIntent().getSerializableExtra("worker");

        if (currentWorker == null) {
            Toast.makeText(this, "שגיאה: נתוני משתמש חסרים", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadTodayShift();
    }

    private void initViews() {
        tvShiftInfo          = findViewById(R.id.tvShiftInfo);
        tvLocationStatus     = findViewById(R.id.tvLocationStatus);
        progressBarLocation  = findViewById(R.id.progressBarLocation);
        btnConfirmArrival    = findViewById(R.id.btnConfirmArrival);
        btnBack              = findViewById(R.id.btnBackCheckIn);

        btnBack.setOnClickListener(v -> finish());
        btnConfirmArrival.setOnClickListener(v -> confirmAttendance());
    }

    private void loadTodayShift() {
        tvShiftInfo.setText("מחפש משמרת להיום...");

        db.getWorkerShifts(currentWorker.getId(), new DatabaseService.DatabaseCallback<List<ShiftRequest>>() {
            @Override
            public void onCompleted(List<ShiftRequest> shifts) {
                todayShift = null;
                if (shifts != null) {
                    for (ShiftRequest s : shifts) {
                        if (todayDate.equals(s.getDate())) {
                            todayShift = s;
                            break;
                        }
                    }
                }

                if (todayShift != null) {
                    String timeInfo = (todayShift.getStartTime() != null)
                            ? todayShift.getStartTime() + " - " + todayShift.getEndTime()
                            : "";
                    tvShiftInfo.setText("משמרת היום: " + timeInfo);
                    checkAlreadyConfirmed();
                } else {
                    tvShiftInfo.setText("אין לך משמרת מאושרת להיום");
                    tvLocationStatus.setText("לא ניתן לאשר נוכחות ללא משמרת");
                    progressBarLocation.setVisibility(View.GONE);
                    btnConfirmArrival.setEnabled(false);
                }
            }

            @Override
            public void onFailed(Exception e) {
                tvShiftInfo.setText("שגיאה בטעינת משמרת");
                progressBarLocation.setVisibility(View.GONE);
            }
        });
    }

    private void checkAlreadyConfirmed() {
        db.getWorkerAttendance(currentWorker.getId(), todayDate,
                new DatabaseService.DatabaseCallback<Attendance>() {
                    @Override
                    public void onCompleted(Attendance att) {
                        if (att != null && att.isConfirmed()) {
                            tvLocationStatus.setText("נוכחות אושרה כבר היום ✓");
                            progressBarLocation.setVisibility(View.GONE);
                            btnConfirmArrival.setEnabled(false);
                            btnConfirmArrival.setText("כבר אושרת הגעה");
                        } else {
                            requestLocationPermission();
                        }
                    }
                    @Override
                    public void onFailed(Exception e) { requestLocationPermission(); }
                });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                tvLocationStatus.setText("לא ניתנה הרשאת מיקום. לא ניתן לאשר נוכחות.");
                progressBarLocation.setVisibility(View.GONE);
            }
        }
    }

    private void startLocationUpdates() {
        tvLocationStatus.setText("מאתר מיקום, אנא המתן...");
        progressBarLocation.setVisibility(View.VISIBLE);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            tvLocationStatus.setText("שגיאה: מנהל המיקום לא זמין");
            progressBarLocation.setVisibility(View.GONE);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        // ניסיון עם מיקום אחרון ידוע
        Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (last == null) last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (last != null) { onLocationReceived(last); return; }

        // בקשת עדכון מיקום בזמן אמת
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                locationManager.removeUpdates(this);
                onLocationReceived(location);
            }
        };

        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gps) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else if (net) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            tvLocationStatus.setText("GPS כבוי. יש להפעיל מיקום בהגדרות המכשיר.");
            progressBarLocation.setVisibility(View.GONE);
        }
    }

    private void onLocationReceived(Location location) {
        progressBarLocation.setVisibility(View.GONE);

        float[] results = new float[1];
        Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                WORKPLACE_LATITUDE, WORKPLACE_LONGITUDE, results);
        float dist = results[0];

        if (dist <= ALLOWED_RADIUS_METERS) {
            tvLocationStatus.setText("מיקום אומת – אתה נמצא במקום העבודה ✓\n(מרחק: "
                    + Math.round(dist) + " מ')");
            tvLocationStatus.setTextColor(0xFF2ECC71);
            btnConfirmArrival.setEnabled(true);
        } else {
            tvLocationStatus.setText("אינך נמצא במקום העבודה ✗\n(מרחק: "
                    + Math.round(dist) + " מ', נדרש עד "
                    + Math.round(ALLOWED_RADIUS_METERS) + " מ')");
            tvLocationStatus.setTextColor(0xFFE53935);
            btnConfirmArrival.setEnabled(false);
        }
    }

    private void confirmAttendance() {
        if (todayShift == null) return;
        btnConfirmArrival.setEnabled(false);

        String fullName = currentWorker.getfName() + " " + currentWorker.getlName();
        Attendance att = new Attendance(
                currentWorker.getId() + "_" + todayDate.replace("/", ""),
                currentWorker.getId(), fullName, todayDate,
                System.currentTimeMillis(), true);

        db.saveAttendance(att, new DatabaseService.DatabaseCallback<Void>() {
            @Override
            public void onCompleted(Void unused) {
                Toast.makeText(CheckInActivity.this, "נוכחות אושרה בהצלחה!", Toast.LENGTH_LONG).show();
                tvLocationStatus.setText("הגעה אושרה בהצלחה ✓");
                tvLocationStatus.setTextColor(0xFF2ECC71);
                btnConfirmArrival.setText("כבר אושרת הגעה");
            }
            @Override
            public void onFailed(Exception e) {
                Toast.makeText(CheckInActivity.this, "שגיאה בשמירת הנוכחות", Toast.LENGTH_SHORT).show();
                btnConfirmArrival.setEnabled(true);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}
