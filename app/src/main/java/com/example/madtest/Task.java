package com.example.madtest;

public class Task {
    private int id;
    private String title;
    private String status;

    public Task (int id, String title, String status){
        this.id = id;
        this.title = title;
        this.status = status;
    }

    public int getID() {
        return id;
    }

    public String getTitle(){
        return title;
    }

    public String getStatus(){
        return status;
    }

}
