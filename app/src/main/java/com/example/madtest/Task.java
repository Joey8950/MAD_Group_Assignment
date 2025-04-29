package com.example.tasktodo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Task {
    private String title;
    private String description;
    private boolean completed;
    private Date dueDate;
    private List<String> labels;

    public Task(String title, String description, boolean completed) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.dueDate = null;
        this.labels = new ArrayList<>();
    }

    public Task(String title, String description, boolean completed, Date dueDate) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.dueDate = dueDate;
        this.labels = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void addLabel(String label) {
        if (!labels.contains(label)) {
            labels.add(label);
        }
    }

    public void removeLabel(String label) {
        labels.remove(label);
    }

    public boolean hasLabel(String label) {
        return labels.contains(label);
    }
}
