package com.example.tasktodo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import android.content.res.ColorStateList;
import androidx.recyclerview.widget.DividerItemDecoration;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private TextView tvDate;
    private List<Label> allLabels;
    private LabelManager labelManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        rvTasks = findViewById(R.id.rvTasks);
        tvDate = findViewById(R.id.tvDate);
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Set current date
        setCurrentDate();

        // Initialize LabelManager
        labelManager = LabelManager.getInstance(this);

        // Initialize labels
        initializeLabels();

        // Initialize task list and adapter
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList);
        taskAdapter.setOnTaskClickListener(this);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        // Add sample tasks
        addSampleTasks();

        // Set up bottom navigation
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_today);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_today) {
                    return true;
                } else if (itemId == R.id.nav_upcoming) {
                    return true;
                } else if (itemId == R.id.nav_search) {
                    return true;
                } else if (itemId == R.id.nav_browse) {
                    return true;
                }
                return false;
            });
        }

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
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
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
        SimpleDateFormat weekdayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String day = dayFormat.format(currentDate);
        String weekday = weekdayFormat.format(currentDate);
        tvDate.setText(day + " · " + weekday);
    }

    private void addSampleTasks() {
        taskList.add(new Task("Download Todoist on all your devices and email for", "Multiple devices", false));
        taskList.add(new Task("Testing", "Test", false));
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

        final Calendar calendar = Calendar.getInstance();
        final boolean[] dateSelected = {false};
        final boolean[] timeSelected = {false};
        final Set<String> selectedLabels = new HashSet<>();

        btnSelectDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    MainActivity.this,
                    (view, year, month, dayOfMonth) -> {
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
                Task newTask = (dateSelected[0] || timeSelected[0])
                        ? new Task(title, description, false, calendar.getTime())
                        : new Task(title, description, false);

                for (String label : selectedLabels) {
                    newTask.addLabel(label);
                }

                taskList.add(0, newTask);
                taskAdapter.notifyItemInserted(0);
                rvTasks.scrollToPosition(0);
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
        // Assuming you handle the dialog in the rest of the code
    }
}
