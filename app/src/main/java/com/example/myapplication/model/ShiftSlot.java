package com.example.myapplication.model;

import androidx.annotation.NonNull;

/**
 * מודל בסיסי לתצוגת משמרת (תאריך, סוג, הערות).
 * שמו שונה מ-Shift ל-ShiftSlot כדי למנוע בלבול עם
 * {@link com.example.myapplication.services.Shift}
 * שהוא המודל המלא המשמש את ה-DatabaseService.
 */
public class ShiftSlot {

    private String date;
    private String type;
    private String notes;

    /** קונסטרקטור ריק עבור Firebase */
    public ShiftSlot() {}

    public ShiftSlot(String date, String type, String notes) {
        this.date  = date;
        this.type  = type;
        this.notes = notes;
    }

    public String getDate()  { return date;  }
    public void   setDate(String date)   { this.date  = date;  }

    public String getType()  { return type;  }
    public void   setType(String type)   { this.type  = type;  }

    public String getNotes() { return notes; }
    public void   setNotes(String notes) { this.notes = notes; }

    @NonNull
    @Override
    public String toString() {
        return "ShiftSlot{date='" + date + "', type='" + type + "', notes='" + notes + "'}";
    }
}
