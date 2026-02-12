package com.example.myapplication.screens;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Shift;
import com.example.myapplication.services.DatabaseService;

public class ManageShiftsActivity extends AppCompatActivity {

    private TextView tvDisplayDate;
    private Spinner spinnerShiftType;
    private EditText etShiftNotes;
    private Button btnSaveShift;
    private String dateFromIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_shifts);

        tvDisplayDate = findViewById(R.id.tvDisplayDate);
        spinnerShiftType = findViewById(R.id.spinnerShiftType);
        etShiftNotes = findViewById(R.id.etShiftNotes);
        btnSaveShift = findViewById(R.id.btnSaveShift);

        // קבלת התאריך מהמסך הקודם
        dateFromIntent = getIntent().getStringExtra("SELECTED_DATE");
        if (dateFromIntent != null) {
            tvDisplayDate.setText("תאריך משמרת: " + dateFromIntent);
        }

        // הגדרת אפשרויות לספינר
        String[] shiftTypes = {"בוקר", "ערב", "לילה"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shiftTypes);
        spinnerShiftType.setAdapter(adapter);

        btnSaveShift.setOnClickListener(v -> saveShiftToDatabase());
    }

    private void saveShiftToDatabase() {
        String selectedType = spinnerShiftType.getSelectedItem().toString();
        String notes = etShiftNotes.getText().toString();

        // יצירת אובייקט משמרת ושימוש במתודות הקיימות
        Shift shift = new Shift();
        shift.setDate(dateFromIntent); // התיקון לשגיאת ה-setShiftTime
        shift.setType(selectedType);
        shift.setNotes(notes);

        DatabaseService dbService = DatabaseService.getInstance();

        // שמירה ל-Firebase תחת הנתיב Shifts
        dbService.getDbReference().child("Shifts").push().setValue(shift)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "המשמרת נשמרה בהצלחה!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "שגיאה בשמירת המשמרת", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}