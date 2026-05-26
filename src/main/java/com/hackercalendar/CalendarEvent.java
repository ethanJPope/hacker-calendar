package com.hackercalendar;

import java.time.LocalDate;
import java.time.LocalTime;

public class CalendarEvent {
    private String title;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String category;
    private String color;

    public CalendarEvent(String title, LocalDate date, LocalTime startTime, LocalTime endTime, String category, String color) {
        this.title = title;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.color = color;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

}
