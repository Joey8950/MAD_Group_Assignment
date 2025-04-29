package com.example.tasktodo;

public class RecentItem {
    public static final int TYPE_SECTION = 1;
    public static final int TYPE_TASK = 2;
    
    private String title;
    private int type;
    private String description;
    
    public RecentItem(String title, int type) {
        this.title = title;
        this.type = type;
        this.description = "";
    }
    
    public RecentItem(String title, String description, int type) {
        this.title = title;
        this.type = type;
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getType() {
        return type;
    }
    
    public String getDescription() {
        return description;
    }
}
