package com.example.tasktodo;

import android.graphics.Color;

public class Label {
    private String name;
    private int color;

    public Label(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public Label(String name) {
        this.name = name;
        this.color = Color.parseColor("#4CAF50"); // Default green color
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Label label = (Label) obj;
        return name.equals(label.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}