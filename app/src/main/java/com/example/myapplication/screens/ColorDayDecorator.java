package com.example.myapplication.screens;

import android.graphics.drawable.GradientDrawable;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Set;

/**
 * Decorates calendar days with a colored circle background + matching text color.
 *
 * Used in AddShift to indicate shift staffing status:
 *  - Yellow  → at least one shift on that day has ≥ 3 assigned workers
 *  - Green   → BOTH morning and evening shifts have ≥ 3 assigned workers
 */
public class ColorDayDecorator implements DayViewDecorator {

    private final Set<CalendarDay> dates;
    private final int bgColor;    // circle fill  (e.g. semi-transparent yellow/green)
    private final int textColor;  // date number color (darker shade for readability)

    public ColorDayDecorator(Set<CalendarDay> dates, int bgColor, int textColor) {
        this.dates     = dates;
        this.bgColor   = bgColor;
        this.textColor = textColor;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates != null && dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // Colored oval background
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(bgColor);
        view.setBackgroundDrawable(circle);

        // Matching dark text so it's readable on the colored circle
        view.addSpan(new ForegroundColorSpan(textColor));
    }
}
