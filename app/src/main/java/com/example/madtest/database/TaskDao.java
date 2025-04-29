package com.example.tasktodo.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.tasktodo.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskDao {
    private TaskDbHelper dbHelper;
    private SQLiteDatabase database;

    public TaskDao(Context context) {
        dbHelper = new TaskDbHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // Insert a new task
    public long insertTask(Task task) {
        ContentValues values = new ContentValues();
        values.put(TaskDbHelper.COLUMN_TITLE, task.getTitle());
        values.put(TaskDbHelper.COLUMN_DESCRIPTION, task.getDescription());
        values.put(TaskDbHelper.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        
        if (task.getDueDate() != null) {
            values.put(TaskDbHelper.COLUMN_DUE_DATE, task.getDueDate().getTime());
        }

        long taskId = database.insert(TaskDbHelper.TABLE_TASKS, null, values);
        
        // Insert task labels
        if (taskId != -1 && task.getLabels() != null) {
            for (String labelName : task.getLabels()) {
                long labelId = getOrCreateLabel(labelName);
                linkTaskToLabel(taskId, labelId);
            }
        }
        
        return taskId;
    }

    // Update an existing task
    public int updateTask(Task task, long taskId) {
        ContentValues values = new ContentValues();
        values.put(TaskDbHelper.COLUMN_TITLE, task.getTitle());
        values.put(TaskDbHelper.COLUMN_DESCRIPTION, task.getDescription());
        values.put(TaskDbHelper.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        
        if (task.getDueDate() != null) {
            values.put(TaskDbHelper.COLUMN_DUE_DATE, task.getDueDate().getTime());
        } else {
            values.putNull(TaskDbHelper.COLUMN_DUE_DATE);
        }

        // Update task labels
        updateTaskLabels(taskId, task.getLabels());
        
        return database.update(
                TaskDbHelper.TABLE_TASKS,
                values,
                TaskDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)}
        );
    }

    // Delete a task
    public int deleteTask(long taskId) {
        // Due to foreign key constraints, task_labels entries will be deleted automatically
        return database.delete(
                TaskDbHelper.TABLE_TASKS,
                TaskDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)}
        );
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        
        Cursor cursor = database.query(
                TaskDbHelper.TABLE_TASKS,
                null,
                null,
                null,
                null,
                null,
                null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                tasks.add(task);
            } while (cursor.moveToNext());
            
            cursor.close();
        }
        
        return tasks;
    }

    // Search tasks by title or label
    public List<Task> searchTasks(String query) {
        List<Task> tasks = new ArrayList<>();
        
        // Search by title or description
        String selection = TaskDbHelper.COLUMN_TITLE + " LIKE ? OR " + 
                           TaskDbHelper.COLUMN_DESCRIPTION + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        
        Cursor cursor = database.query(
                TaskDbHelper.TABLE_TASKS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                tasks.add(task);
            } while (cursor.moveToNext());
            
            cursor.close();
        }
        
        // Search by label
        String labelQuery = "SELECT t.* FROM " + TaskDbHelper.TABLE_TASKS + " t " +
                "JOIN " + TaskDbHelper.TABLE_TASK_LABELS + " tl ON t." + TaskDbHelper.COLUMN_ID + " = tl." + TaskDbHelper.COLUMN_TASK_ID + " " +
                "JOIN " + TaskDbHelper.TABLE_LABELS + " l ON tl." + TaskDbHelper.COLUMN_LABEL_ID + " = l." + TaskDbHelper.COLUMN_ID + " " +
                "WHERE l." + TaskDbHelper.COLUMN_LABEL_NAME + " LIKE ?";
        
        cursor = database.rawQuery(labelQuery, new String[]{"%" + query + "%"});
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Task task = cursorToTask(cursor);
                // Check if task is already in the list to avoid duplicates
                boolean isDuplicate = false;
                for (Task existingTask : tasks) {
                    if (getTaskId(existingTask) == getTaskId(task)) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    tasks.add(task);
                }
            } while (cursor.moveToNext());
            
            cursor.close();
        }
        
        return tasks;
    }

    // Get a task by ID
    public Task getTaskById(long taskId) {
        Cursor cursor = database.query(
                TaskDbHelper.TABLE_TASKS,
                null,
                TaskDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(taskId)},
                null,
                null,
                null
        );
        
        Task task = null;
        if (cursor != null && cursor.moveToFirst()) {
            task = cursorToTask(cursor);
            cursor.close();
        }
        
        return task;
    }

    // Helper method to convert cursor to Task object
    private Task cursorToTask(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(TaskDbHelper.COLUMN_ID);
        int titleIndex = cursor.getColumnIndex(TaskDbHelper.COLUMN_TITLE);
        int descriptionIndex = cursor.getColumnIndex(TaskDbHelper.COLUMN_DESCRIPTION);
        int completedIndex = cursor.getColumnIndex(TaskDbHelper.COLUMN_COMPLETED);
        int dueDateIndex = cursor.getColumnIndex(TaskDbHelper.COLUMN_DUE_DATE);
        
        long id = cursor.getLong(idIndex);
        String title = cursor.getString(titleIndex);
        String description = cursor.getString(descriptionIndex);
        boolean completed = cursor.getInt(completedIndex) == 1;
        
        Date dueDate = null;
        if (!cursor.isNull(dueDateIndex)) {
            dueDate = new Date(cursor.getLong(dueDateIndex));
        }
        
        Task task = dueDate != null 
                ? new Task(title, description, completed, dueDate) 
                : new Task(title, description, completed);
        
        // Set task ID as a tag for later use
        task.setTag(id);
        
        // Get labels for this task
        List<String> labels = getLabelsForTask(id);
        for (String label : labels) {
            task.addLabel(label);
        }
        
        return task;
    }

    // Helper method to get task ID (stored as a tag)
    public long getTaskId(Task task) {
        return task.getTag() != null ? (long) task.getTag() : -1;
    }

    // Label operations
    private long getOrCreateLabel(String labelName) {
        // First try to find the label
        Cursor cursor = database.query(
                TaskDbHelper.TABLE_LABELS,
                new String[]{TaskDbHelper.COLUMN_ID},
                TaskDbHelper.COLUMN_LABEL_NAME + " = ?",
                new String[]{labelName},
                null,
                null,
                null
        );
        
        long labelId = -1;
        
        if (cursor != null && cursor.moveToFirst()) {
            labelId = cursor.getLong(cursor.getColumnIndex(TaskDbHelper.COLUMN_ID));
            cursor.close();
        } else {
            // Label doesn't exist, create it
            ContentValues values = new ContentValues();
            values.put(TaskDbHelper.COLUMN_LABEL_NAME, labelName);
            labelId = database.insert(TaskDbHelper.TABLE_LABELS, null, values);
        }
        
        return labelId;
    }

    private void linkTaskToLabel(long taskId, long labelId) {
        ContentValues values = new ContentValues();
        values.put(TaskDbHelper.COLUMN_TASK_ID, taskId);
        values.put(TaskDbHelper.COLUMN_LABEL_ID, labelId);
        database.insert(TaskDbHelper.TABLE_TASK_LABELS, null, values);
    }

    private List<String> getLabelsForTask(long taskId) {
        List<String> labels = new ArrayList<>();
        
        String query = "SELECT l." + TaskDbHelper.COLUMN_LABEL_NAME + 
                " FROM " + TaskDbHelper.TABLE_LABELS + " l " +
                "JOIN " + TaskDbHelper.TABLE_TASK_LABELS + " tl ON l." + TaskDbHelper.COLUMN_ID + " = tl." + TaskDbHelper.COLUMN_LABEL_ID + " " +
                "WHERE tl." + TaskDbHelper.COLUMN_TASK_ID + " = ?";
        
        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(taskId)});
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                labels.add(cursor.getString(0));
            } while (cursor.moveToNext());
            
            cursor.close();
        }
        
        return labels;
    }

    private void updateTaskLabels(long taskId, List<String> newLabels) {
        // Remove all existing task-label links
        database.delete(
                TaskDbHelper.TABLE_TASK_LABELS,
                TaskDbHelper.COLUMN_TASK_ID + " = ?",
                new String[]{String.valueOf(taskId)}
        );
        
        // Add new links
        if (newLabels != null) {
            for (String labelName : newLabels) {
                long labelId = getOrCreateLabel(labelName);
                linkTaskToLabel(taskId, labelId);
            }
        }
    }
}