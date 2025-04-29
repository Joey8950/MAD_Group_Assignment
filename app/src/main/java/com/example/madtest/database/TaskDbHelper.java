package com.example.tasktodo.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_TASKS = "tasks";
    public static final String TABLE_LABELS = "labels";
    public static final String TABLE_TASK_LABELS = "task_labels";

    // Common column names
    public static final String COLUMN_ID = "_id";
    
    // Tasks table columns
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_COMPLETED = "completed";
    public static final String COLUMN_DUE_DATE = "due_date";
    
    // Labels table columns
    public static final String COLUMN_LABEL_NAME = "name";
    
    // Task_Labels table columns
    public static final String COLUMN_TASK_ID = "task_id";
    public static final String COLUMN_LABEL_ID = "label_id";

    // Create table statements
    private static final String CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_TASKS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TITLE + " TEXT NOT NULL, " +
            COLUMN_DESCRIPTION + " TEXT, " +
            COLUMN_COMPLETED + " INTEGER DEFAULT 0, " +
            COLUMN_DUE_DATE + " INTEGER" +
            ")";

    private static final String CREATE_LABELS_TABLE = "CREATE TABLE " + TABLE_LABELS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_LABEL_NAME + " TEXT UNIQUE NOT NULL" +
            ")";

    private static final String CREATE_TASK_LABELS_TABLE = "CREATE TABLE " + TABLE_TASK_LABELS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TASK_ID + " INTEGER NOT NULL, " +
            COLUMN_LABEL_ID + " INTEGER NOT NULL, " +
            "FOREIGN KEY (" + COLUMN_TASK_ID + ") REFERENCES " + TABLE_TASKS + "(" + COLUMN_ID + ") ON DELETE CASCADE, " +
            "FOREIGN KEY (" + COLUMN_LABEL_ID + ") REFERENCES " + TABLE_LABELS + "(" + COLUMN_ID + ") ON DELETE CASCADE, " +
            "UNIQUE (" + COLUMN_TASK_ID + ", " + COLUMN_LABEL_ID + ")" +
            ")";

    public TaskDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TASKS_TABLE);
        db.execSQL(CREATE_LABELS_TABLE);
        db.execSQL(CREATE_TASK_LABELS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For simplicity, drop and recreate tables on upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK_LABELS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LABELS);
        onCreate(db);
    }
}