package com.example.myapplication.screens;

import android.text.style.ForegroundColorSpan;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.Set;

public class ColorDayDecorator implements DayViewDecorator {
    private final int color;
    private final Set<CalendarDay> dates;

    public ColorDayDecorator(Set<CalendarDay> dates, int color) {
        this.color = color;
        this.dates = dates;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // צובע את המספר של היום בלוח השנה
        view.addSpan(new ForegroundColorSpan(color));
    }
}