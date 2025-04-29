package com.example.tasktodo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_TASKS = "tasks";
    public static final String TABLE_LABELS = "labels";
    public static final String TABLE_TASK_LABELS = "task_labels";

    // Common column names
    public static final String COLUMN_ID = "id";
    
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
    private static final String CREATE_TABLE_TASKS = "CREATE TABLE " + TABLE_TASKS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT NOT NULL,"
            + COLUMN_DESCRIPTION + " TEXT,"
            + COLUMN_COMPLETED + " INTEGER DEFAULT 0,"
            + COLUMN_DUE_DATE + " TEXT"
            + ")";

    private static final String CREATE_TABLE_LABELS = "CREATE TABLE " + TABLE_LABELS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_LABEL_NAME + " TEXT NOT NULL UNIQUE"
            + ")";

    private static final String CREATE_TABLE_TASK_LABELS = "CREATE TABLE " + TABLE_TASK_LABELS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TASK_ID + " INTEGER,"
            + COLUMN_LABEL_ID + " INTEGER,"
            + "FOREIGN KEY(" + COLUMN_TASK_ID + ") REFERENCES " + TABLE_TASKS + "(" + COLUMN_ID + ") ON DELETE CASCADE,"
            + "FOREIGN KEY(" + COLUMN_LABEL_ID + ") REFERENCES " + TABLE_LABELS + "(" + COLUMN_ID + ") ON DELETE CASCADE"
            + ")";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TASKS);
        db.execSQL(CREATE_TABLE_LABELS);
        db.execSQL(CREATE_TABLE_TASK_LABELS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK_LABELS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LABELS);
        
        // Create tables again
        onCreate(db);
    }

    // Helper method to convert Date to String for database storage
    public static String dateToString(Date date) {
        if (date == null) return null;
        return DATE_FORMAT.format(date);
    }

    // Helper method to convert String from database to Date
    public static Date stringToDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        try {
            return DATE_FORMAT.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Insert a new task
    public long insertTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        
        if (task.getDueDate() != null) {
            values.put(COLUMN_DUE_DATE, dateToString(task.getDueDate()));
        }
        
        // Insert task
        long taskId = db.insert(TABLE_TASKS, null, values);
        
        // Insert task labels
        if (taskId != -1 && task.getLabels() != null && !task.getLabels().isEmpty()) {
            for (String labelName : task.getLabels()) {
                long labelId = getOrCreateLabel(labelName);
                if (labelId != -1) {
                    addTaskLabel(taskId, labelId);
                }
            }
        }
        
        return taskId;
    }

    // Update an existing task
    public int updateTask(Task task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_TITLE, task.getTitle());
        values.put(COLUMN_DESCRIPTION, task.getDescription());
        values.put(COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        
        if (task.getDueDate() != null) {
            values.put(COLUMN_DUE_DATE, dateToString(task.getDueDate()));
        } else {
            values.putNull(COLUMN_DUE_DATE);
        }
        
        // Update task
        int rowsAffected = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?", 
                new String[]{String.valueOf(task.getId())});
        
        // Update task labels
        if (rowsAffected > 0) {
            // First remove all existing labels for this task
            db.delete(TABLE_TASK_LABELS, COLUMN_TASK_ID + " = ?", 
                    new String[]{String.valueOf(task.getId())});
            
            // Then add the current labels
            if (task.getLabels() != null && !task.getLabels().isEmpty()) {
                for (String labelName : task.getLabels()) {
                    long labelId = getOrCreateLabel(labelName);
                    if (labelId != -1) {
                        addTaskLabel(task.getId(), labelId);
                    }
                }
            }
        }
        
        return rowsAffected;
    }

    // Delete a task
    public void deleteTask(long taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASK_LABELS, COLUMN_TASK_ID + " = ?", new String[]{String.valueOf(taskId)});
        db.delete(TABLE_TASKS, COLUMN_ID + " = ?", new String[]{String.valueOf(taskId)});
    }

    // Get or create a label
    private long getOrCreateLabel(String labelName) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // First try to find the label
        Cursor cursor = db.query(TABLE_LABELS, new String[]{COLUMN_ID}, 
                COLUMN_LABEL_NAME + " = ?", new String[]{labelName}, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            long labelId = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
            cursor.close();
            return labelId;
        }
        
        // If not found, create a new label
        ContentValues values = new ContentValues();
        values.put(COLUMN_LABEL_NAME, labelName);
        long labelId = db.insert(TABLE_LABELS, null, values);
        
        if (cursor != null) {
            cursor.close();
        }
        
        return labelId;
    }

    // Add a task-label relationship
    private void addTaskLabel(long taskId, long labelId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TASK_ID, taskId);
        values.put(COLUMN_LABEL_ID, labelId);
        db.insert(TABLE_TASK_LABELS, null, values);
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TASKS;
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return tasks;
    }

    // Search tasks by title, description, or label
    public List<Task> searchTasks(String query) {
        List<Task> tasks = new ArrayList<>();
        query = "%" + query.toLowerCase() + "%";
        
        String selectQuery = "SELECT DISTINCT t.* FROM " + TABLE_TASKS + " t " +
                "LEFT JOIN " + TABLE_TASK_LABELS + " tl ON t." + COLUMN_ID + " = tl." + COLUMN_TASK_ID + " " +
                "LEFT JOIN " + TABLE_LABELS + " l ON tl." + COLUMN_LABEL_ID + " = l." + COLUMN_ID + " " +
                "WHERE LOWER(t." + COLUMN_TITLE + ") LIKE ? OR " +
                "LOWER(t." + COLUMN_DESCRIPTION + ") LIKE ? OR " +
                "LOWER(l." + COLUMN_LABEL_NAME + ") LIKE ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{query, query, query});
        
        if (cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                tasks.add(task);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return tasks;
    }

    // Get a single task by ID
    public Task getTask(long taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TASKS, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)}, null, null, null);
        
        Task task = null;
        if (cursor != null && cursor.moveToFirst()) {
            task = cursorToTask(cursor);
            cursor.close();
        }
        
        return task;
    }

    // Convert cursor to Task object
    private Task cursorToTask(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndex(COLUMN_TITLE));
        String description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION));
        boolean completed = cursor.getInt(cursor.getColumnIndex(COLUMN_COMPLETED)) == 1;
        
        String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_DUE_DATE));
        Date dueDate = stringToDate(dateString);
        
        Task task = new Task(title, description, completed, dueDate);
        task.setId(id);
        
        // Get labels for this task
        List<String> labels = getTaskLabels(id);
        for (String label : labels) {
            task.addLabel(label);
        }
        
        return task;
    }

    // Get all labels for a task
    private List<String> getTaskLabels(long taskId) {
        List<String> labels = new ArrayList<>();
        
        String selectQuery = "SELECT l." + COLUMN_LABEL_NAME + " FROM " + TABLE_LABELS + " l " +
                "INNER JOIN " + TABLE_TASK_LABELS + " tl ON l." + COLUMN_ID + " = tl." + COLUMN_LABEL_ID + " " +
                "WHERE tl." + COLUMN_TASK_ID + " = ?";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(taskId)});
        
        if (cursor.moveToFirst()) {
            do {
                labels.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return labels;
    }
}