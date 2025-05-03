package com.example.tasktodo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

public class ReminderHelper {
    private static final String TAG = "ReminderHelper";

    public static void scheduleReminder(Context context, Task task) {
        try {
            Log.d(TAG, "Attempting to schedule reminder for task: " + task.getTitle());

            // Check if we have permission to schedule exact alarms on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    // We don't have permission, show a toast and return
                    Toast.makeText(context, "Alarm permission not granted", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Cannot schedule exact alarms - permission not granted");
                    return;
                }
            }

            // Continue with scheduling the alarm
            // Cancel any existing reminder for this task
            cancelReminder(context, task);

            // If no due date or reminder is not set, don't schedule
            if (task.getDueDate() == null || !task.isReminderSet()) {
                Log.d(TAG, "Not scheduling reminder: " +
                        (task.getDueDate() == null ? "due date is null" : "reminder not set"));
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("title", task.getTitle());
            intent.putExtra("description", task.getDescription());
            intent.putExtra("taskId", task.getId());

            // Create a unique request code based on task ID
            int requestCode = (int) task.getId();

            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
            } else {
                pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
            }

            // Get the reminder time
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(task.getDueDate());

            long triggerTime = calendar.getTimeInMillis();
            Log.d(TAG, "Scheduling alarm for: " + new Date(triggerTime).toString());

            // Schedule the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Alarm scheduled with setExactAndAllowWhileIdle");
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Alarm scheduled with setExact");
            }

            Toast.makeText(context, "Reminder set for: " + new Date(triggerTime).toString(),
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error scheduling reminder: " + e.getMessage());
            Toast.makeText(context, "Could not set reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void cancelReminder(Context context, Task task) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);

            // Create the same request code used when scheduling
            int requestCode = (int) task.getId();

            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
            } else {
                pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
            }

            // Cancel the alarm
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled reminder for task: " + task.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error cancelling reminder: " + e.getMessage());
        }
    }
}