package com.example.tasktodo;


import com.example.tasktodo.Task;

import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlarmManager;
import android.content.Context;

import android.content.res.ColorStateList;
import androidx.recyclerview.widget.DividerItemDecoration;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private TextView tvDate;
    private TextView tvTitle;
    private List<Label> allLabels;
    private LabelManager labelManager;
    private TaskRepository taskRepository;

    // UI Components for Progress Tracking
    private ProgressBar progressBar;
    private TextView txtProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }

        // Request alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestAlarmPermission();
        }

        // Create notification channel
        NotificationHelper.createNotificationChannel(this);

        // Initialize views
        rvTasks = findViewById(R.id.rvTasks);
        tvDate = findViewById(R.id.tvDate);
        tvTitle = findViewById(R.id.tvToday);
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize Progress Tracking UI
        progressBar = findViewById(R.id.progressBar);
        txtProgress = findViewById(R.id.txtProgress);

        // Set current date
        setCurrentDate();

        // Initialize LabelManager
        labelManager = LabelManager.getInstance(this);

        // Initialize labels
        initializeLabels();

        // Initialize task repository
        taskRepository = TaskRepository.getInstance(this);

        // Initialize task list and adapter
        taskList = taskRepository.getTodayTasks();
        taskAdapter = new TaskAdapter(taskList);
        taskAdapter.setOnTaskClickListener(this);

        // Set status change listener
        taskAdapter.setOnTaskStatusChangedListener((task, position) -> {
            // Update task in repository
            taskRepository.updateTask(task);

            // Update progress bar
            updateProgressBar();
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        // Update progress bar initially
        updateProgressBar();

        // Set up bottom navigation
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_today);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_today) {
                    // Load today's tasks
                    taskList.clear();
                    taskList.addAll(taskRepository.getTodayTasks());
                    taskAdapter.notifyDataSetChanged();
                    updateProgressBar();
                    // Set title to Today
                    setCurrentDate();
                    return true;
                } else if (itemId == R.id.nav_upcoming) {
                    // Load upcoming tasks
                    taskList.clear();
                    taskList.addAll(taskRepository.getUpcomingTasks());
                    taskAdapter.notifyDataSetChanged();
                    updateProgressBar();
                    // Set title to Upcoming with current month
                    setUpcomingTitle();
                    return true;
                } else if (itemId == R.id.nav_search) {
                    // Navigate to search activity
                    startActivity(new Intent(MainActivity.this, SearchActivity.class));
                    return true;
                } else if (itemId == R.id.nav_browse) {
                    // Navigate to browse activity
                    startActivity(new Intent(MainActivity.this, BrowseActivity.class));
                    return true;
                }
                return false;
            });
        }

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        // Check if we should select a specific tab
        if (getIntent().hasExtra("select_tab")) {
            String tabToSelect = getIntent().getStringExtra("select_tab");
            if (bottomNavigationView != null) {
                if ("upcoming".equals(tabToSelect)) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_upcoming);
                } else if ("today".equals(tabToSelect)) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_today);
                }else if ("browse".equals(tabToSelect)) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_browse);
                }
            }
        }
    }

    private void initializeLabels() {
        allLabels = new ArrayList<>();
        allLabels.add(new Label("Work", Color.parseColor("#F44336")));
        allLabels.add(new Label("Personal", Color.parseColor("#2196F3")));
        allLabels.add(new Label("Shopping", Color.parseColor("#FF9800")));
        allLabels.add(new Label("Health", Color.parseColor("#4CAF50")));
        allLabels.add(new Label("Education", Color.parseColor("#9C27B0")));

        // Also store labels in LabelManager
        for (Label label : allLabels) {
            labelManager.addLabel(label.getName());
        }
    }

    private void setCurrentDate() {
        try {
            // Force a new calendar instance with current time
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date()); // Force update with current time

            // Log raw components for debugging
            Log.d("MainActivity", "Calendar components: " +
                    "Day=" + calendar.get(Calendar.DAY_OF_MONTH) +
                    ", Month=" + (calendar.get(Calendar.MONTH) + 1) +  // +1 because January=0
                    ", Year=" + calendar.get(Calendar.YEAR));

            // Format properly with current locale
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM - EEEE", Locale.getDefault());
            // Force timezone to device default to ensure consistency
            dateFormat.setTimeZone(TimeZone.getDefault());
            String formattedDate = dateFormat.format(new Date()); // Use new Date() directly

            Log.d("MainActivity", "Formatted date: " + formattedDate);

            // Set TextViews
            tvTitle.setText("Today");
            tvDate.setText(formattedDate);
        } catch (Exception e) {
            Log.e("MainActivity", "Error formatting date", e);
            tvDate.setText(new Date().toString());
        }
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText etTaskTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etTaskDescription = dialogView.findViewById(R.id.etTaskDescription);
        Button btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        Button btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);
        TextView tvSelectedDateTime = dialogView.findViewById(R.id.tvSelectedDateTime);
        Button btnSelectLabels = dialogView.findViewById(R.id.btnSelectLabels);
        ChipGroup chipGroupLabels = dialogView.findViewById(R.id.chipGroupLabels);
        Button btnSetReminder = dialogView.findViewById(R.id.btnSetReminder); // Add this line

        final Calendar calendar = Calendar.getInstance();
        final boolean[] dateSelected = {false};
        final boolean[] timeSelected = {false};
        final boolean[] reminderSet = {false}; // Add this line
        final Set<String> selectedLabels = new HashSet<>();

        btnSetReminder.setOnClickListener(v -> {
            if (!dateSelected[0] || !timeSelected[0]) {
                Toast.makeText(MainActivity.this, "Please select date and time first", Toast.LENGTH_SHORT).show();
                return;
            }

            reminderSet[0] = true;
            btnSetReminder.setText("Reminder Set");
            btnSetReminder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            Toast.makeText(MainActivity.this, "Reminder set for selected time", Toast.LENGTH_SHORT).show();
        });

        btnSelectDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);

                        // Ensure selected date is not in the past
                        Calendar today = Calendar.getInstance();
                        today.set(Calendar.HOUR_OF_DAY, 0);
                        today.set(Calendar.MINUTE, 0);
                        today.set(Calendar.SECOND, 0);
                        today.set(Calendar.MILLISECOND, 0);

                        if (selectedDate.before(today)) {
                            Toast.makeText(MainActivity.this, "Cannot select past dates", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateSelected[0] = true;
                        updateSelectedDateTime(tvSelectedDateTime, calendar, dateSelected[0], timeSelected[0]);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        btnSelectTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    MainActivity.this,
                    (view, hourOfDay, minute) -> {
                        // Check if selected time is in the past for today's date
                        Calendar selectedDateTime = (Calendar) calendar.clone();
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDateTime.set(Calendar.MINUTE, minute);

                        Calendar now = Calendar.getInstance();

                        // If selected date is today and time is in the past
                        if (selectedDateTime.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                selectedDateTime.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                selectedDateTime.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH) &&
                                selectedDateTime.before(now)) {

                            Toast.makeText(MainActivity.this, "Cannot select past time", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        timeSelected[0] = true;
                        updateSelectedDateTime(tvSelectedDateTime, calendar, dateSelected[0], timeSelected[0]);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            );
            timePickerDialog.show();
        });

        btnSelectLabels.setOnClickListener(v -> showLabelsDialog(selectedLabels, chipGroupLabels));

        builder.setPositiveButton("Add", (dialog, which) -> {
            String title = etTaskTitle.getText().toString().trim();
            String description = etTaskDescription.getText().toString().trim();

            if (!title.isEmpty()) {
                // Check if date and time are selected
                if (!dateSelected[0] || !timeSelected[0]) {
                    Toast.makeText(MainActivity.this, "Please select both date and time", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create and add the task
                Task newTask = new Task(title, description, false);

                // Date and time are now mandatory
                newTask.setDueDate(calendar.getTime());

                // Add selected labels to the task
                for (String label : selectedLabels) {
                    newTask.addLabel(label);
                }

                // Add task to repository
                taskRepository.addTask(newTask);

                // Force reload tasks from database
                loadTasks();

                // Show confirmation to user
                Toast.makeText(MainActivity.this, "Task added successfully", Toast.LENGTH_SHORT).show();

                // Refresh the task list to show the new task
                taskList.clear();
                taskList.addAll(taskRepository.getTodayTasks());
                taskAdapter.notifyDataSetChanged();

                // Update progress bar
                updateProgressBar();

                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void updateSelectedDateTime(TextView textView, Calendar calendar, boolean hasDate, boolean hasTime) {
        StringBuilder dateTimeText = new StringBuilder();
        if (hasDate) {
            dateTimeText.append(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.getTime()));
        }
        if (hasTime) {
            if (dateTimeText.length() > 0) {
                dateTimeText.append(" ");
            }
            dateTimeText.append(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime()));
        }
        textView.setText(dateTimeText.toString());
    }

    private void showLabelsDialog(Set<String> selectedLabels, ChipGroup chipGroup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_labels, null);
        builder.setView(dialogView);

        RecyclerView rvLabels = dialogView.findViewById(R.id.rvLabels);
        EditText etNewLabel = dialogView.findViewById(R.id.etNewLabel);
        Button btnAddNewLabel = dialogView.findViewById(R.id.btnAddNewLabel);
        Button btnSaveLabels = dialogView.findViewById(R.id.btnSaveLabels);
        ChipGroup chipGroupSelectedLabels = dialogView.findViewById(R.id.chipGroupSelectedLabels);

        List<String> allLabels = labelManager.getAllLabels();
        List<String> currentSelectedLabels = new ArrayList<>(selectedLabels);

        LabelAdapter labelAdapter = new LabelAdapter(allLabels, currentSelectedLabels);
        rvLabels.setLayoutManager(new LinearLayoutManager(this));
        rvLabels.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvLabels.setAdapter(labelAdapter);

        updateChipGroup(chipGroupSelectedLabels, currentSelectedLabels);

        labelAdapter.setOnLabelSelectedListener((label, isSelected) -> {
            if (isSelected) currentSelectedLabels.add(label);
            else currentSelectedLabels.remove(label);
            updateChipGroup(chipGroupSelectedLabels, currentSelectedLabels);
        });

        btnAddNewLabel.setOnClickListener(v -> {
            String newLabel = etNewLabel.getText().toString().trim();
            if (!newLabel.isEmpty()) {
                labelManager.addLabel(newLabel);
                allLabels.add(newLabel);
                labelAdapter.updateLabels(allLabels);

                // Also add to selected labels and update chip group
                currentSelectedLabels.add(newLabel);
                updateChipGroup(chipGroupSelectedLabels, currentSelectedLabels);

                etNewLabel.setText("");
            }
        });

        btnSaveLabels.setOnClickListener(v -> {
            selectedLabels.clear();
            selectedLabels.addAll(currentSelectedLabels);
            updateChipGroup(chipGroup, currentSelectedLabels);
            AlertDialog dialog = (AlertDialog) dialogView.getTag();
            if (dialog != null) dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialogView.setTag(dialog);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void updateChipGroup(ChipGroup chipGroup, List<String> labels) {
        chipGroup.removeAllViews();
        for (String label : labels) {
            Chip chip = new Chip(this);
            chip.setText(label);
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#333333")));
            chip.setTextColor(Color.WHITE);
            chip.setOnCloseIconClickListener(v -> {
                labels.remove(label);
                chipGroup.removeView(chip);
            });
            chipGroup.addView(chip);
        }
    }

    @Override
    public void onTaskClick(Task task, int position) {
        showTaskDetailDialog(task);
    }

    private void showTaskDetailDialog(Task task) {
        TaskDetailDialog dialog = new TaskDetailDialog(this, task);
        // In the showTaskDetailDialog method
        dialog.setOnTaskUpdatedListener(updatedTask -> {
            // Update the task in the repository
            taskRepository.updateTask(updatedTask);

            // Refresh the task list
            taskList.clear();
            taskList.addAll(taskRepository.getTodayTasks());
            taskAdapter.notifyDataSetChanged();

            // Update progress bar
            updateProgressBar();

            // Update navigation selection to match current view
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.nav_today);
            }
        });

        dialog.setOnTaskDeletedListener(deletedTask -> {
            // Refresh the task list after deletion
            taskList.clear();
            taskList.addAll(taskRepository.getTodayTasks());
            taskAdapter.notifyDataSetChanged();

            // Update progress bar
            updateProgressBar();
        });

        dialog.show();
    }

    private void setUpBottomNavigation() {
        // In your MainActivity's onCreate or wherever you set up the bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            // Set the selected item to today
            bottomNavigationView.setSelectedItemId(R.id.nav_today);

            // Set up the navigation listener
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_today) {
                    // Load today's tasks
                    taskList.clear();
                    taskList.addAll(taskRepository.getTodayTasks());
                    taskAdapter.notifyDataSetChanged();
                    updateProgressBar();
                    // Set title to Today
                    setCurrentDate();
                    return true;
                } else if (itemId == R.id.nav_upcoming) {
                    // Load upcoming tasks
                    taskList.clear();
                    taskList.addAll(taskRepository.getUpcomingTasks());
                    taskAdapter.notifyDataSetChanged();
                    updateProgressBar();
                    // Set title to Upcoming with current month
                    setUpcomingTitle();
                    return true;
                } else if (itemId == R.id.nav_search) {
                    // Navigate to search activity
                    startActivity(new Intent(MainActivity.this, SearchActivity.class));
                    return true;
                } else if (itemId == R.id.nav_browse) {
                    return true;
                }
                return false;
            });
        }
    }

    private void navigateToSearch() {
        // Start the SearchActivity
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.hasExtra("select_tab")) {
            String tabToSelect = intent.getStringExtra("select_tab");
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

            new Handler().post(() -> {
                if ("upcoming".equals(tabToSelect)) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_upcoming);
                    switchTab(R.id.nav_upcoming); // Make sure you call your content update logic
                } else if ("today".equals(tabToSelect)) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_today);
                    switchTab(R.id.nav_today);
                }
            });
        }
    }





    @Override
    protected void onResume() {
        super.onResume();
        setCurrentDate();

        // Refresh the date when the app resumes
        if (tvTitle != null && "Today".equals(tvTitle.getText().toString())) {
            setCurrentDate();
        }

        // Also refresh the task list to ensure we're showing the correct tasks
        if (taskList != null && taskAdapter != null) {
            // Reload tasks based on current view
            if (tvTitle != null) {
                if ("Today".equals(tvTitle.getText().toString())) {
                    taskList.clear();
                    taskList.addAll(taskRepository.getTodayTasks());
                } else if (tvTitle.getText().toString().contains("Upcoming")) {
                    taskList.clear();
                    taskList.addAll(taskRepository.getUpcomingTasks());
                }
                taskAdapter.notifyDataSetChanged();
                updateProgressBar();
            }
        }
    }

    // Make sure this method exists to load tasks based on the current tab
    private void loadTasks() {
        if (tvTitle != null) {
            if ("Today".equals(tvTitle.getText().toString())) {
                taskList.clear();
                taskList.addAll(taskRepository.getTodayTasks());
            } else if (tvTitle.getText().toString().contains("Upcoming")) {
                taskList.clear();
                taskList.addAll(taskRepository.getUpcomingTasks());
            }
            taskAdapter.notifyDataSetChanged();
            updateProgressBar();
        }
    }

    // Add method to update progress bar
    private void updateProgressBar() {
        int totalTasks = taskList.size();
        if (totalTasks == 0) {
            progressBar.setProgress(0);
            txtProgress.setText("0% Completed");
            return;
        }

        int completedTasks = 0;
        for (Task task : taskList) {
            if (task.isCompleted()) {
                completedTasks++;
            }
        }

        int progressPercentage = (completedTasks * 100) / totalTasks;
        progressBar.setProgress(progressPercentage);
        txtProgress.setText(progressPercentage + "% Completed");
    }

    private void setUpcomingTitle() {
        // Get current month
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        String month = monthFormat.format(currentDate);
        tvTitle.setText("Upcoming");
        // Set title to "Upcoming" with current month below
        tvDate.setText(month);
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
    // Add this method to test notifications
    private void testNotification() {
        NotificationHelper.showNotification(
                this,
                "Test Notification",
                "This is a test notification to verify the system is working"
        );
    }
    // Add this method to request alarm permission
    private void requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            // Check if permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                // Optionally guide the user to settings to enable notifications
                showNotificationPermissionSettings();
            }
        }
    }

    private void showNotificationPermissionSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notification Permission Required")
                .setMessage("Notifications are required for reminders to work. Please enable notifications in settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handleSelectedTab(String tabToSelect) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        if ("upcoming".equals(tabToSelect)) {
            bottomNavigationView.setSelectedItemId(R.id.nav_upcoming);

            tvTitle.setText("Upcoming");
            setUpcomingTitle();
            taskList.clear();
            taskList.addAll(taskRepository.getUpcomingTasks());
            taskAdapter.notifyDataSetChanged();
            updateProgressBar();

        } else if ("today".equals(tabToSelect)) {
            bottomNavigationView.setSelectedItemId(R.id.nav_today);

            tvTitle.setText("Today");
            setCurrentDate();
            taskList.clear();
            taskList.addAll(taskRepository.getTodayTasks());
            taskAdapter.notifyDataSetChanged();
            updateProgressBar();
        }
    }
    private void switchTab(int itemId) {
        if (itemId == R.id.nav_today) {
            tvTitle.setText("Today");
            setCurrentDate();
            taskList.clear();
            taskList.addAll(taskRepository.getTodayTasks());
            taskAdapter.notifyDataSetChanged();
            updateProgressBar();
        } else if (itemId == R.id.nav_upcoming) {
            tvTitle.setText("Upcoming");
            setUpcomingTitle();
            taskList.clear();
            taskList.addAll(taskRepository.getUpcomingTasks());
            taskAdapter.notifyDataSetChanged();
            updateProgressBar();
        }
    }



}









