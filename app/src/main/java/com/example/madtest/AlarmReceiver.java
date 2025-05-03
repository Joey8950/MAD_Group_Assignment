package com.example.tasktodo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        long taskId = intent.getLongExtra("taskId", -1);

        Log.d(TAG, "Showing notification for: " + title);

        // Show notification
        try {
            NotificationHelper.showNotification(context, title, description);
            Toast.makeText(context, "Task reminder: " + title, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
            e.printStackTrace();
        }

        // Update task in database to mark reminder as shown
        if (taskId != -1) {
            try {
                TaskRepository repository = TaskRepository.getInstance(context);
                Task task = repository.getTask(taskId);
                if (task != null) {
                    task.setReminderSet(false);
                    repository.updateTask(task);
                    Log.d(TAG, "Updated task reminder status in database");
                } else {
                    Log.e(TAG, "Task not found with ID: " + taskId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating task: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}