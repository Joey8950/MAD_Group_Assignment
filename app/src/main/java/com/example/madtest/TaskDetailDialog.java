public class TaskDetailDialog {
    
}
package com.example.tasktodo;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaskDetailDialog {
    private Context context;
    private Task task;
    private AlertDialog dialog;
    private OnTaskUpdatedListener listener;
    private OnTaskDeletedListener deleteListener;

    public interface OnTaskUpdatedListener {
        void onTaskUpdated(Task task);
    }

    public interface OnTaskDeletedListener {
        void onTaskDeleted(Task task);
    }

    public TaskDetailDialog(Context context, Task task) {
        this.context = context;
        this.task = task;
    }

    public void setOnTaskUpdatedListener(OnTaskUpdatedListener listener) {
        this.listener = listener;
    }

    public void setOnTaskDeletedListener(OnTaskDeletedListener listener) {
        this.deleteListener = listener;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_task_detail, null);
        builder.setView(dialogView);

        // Initialize views
        TextView tvTaskTitleDetail = dialogView.findViewById(R.id.tvTaskTitleDetail);
        TextView tvTaskDescriptionDetail = dialogView.findViewById(R.id.tvTaskDescriptionDetail);
        TextView tvDueDateDetail = dialogView.findViewById(R.id.tvDueDateDetail);
        CheckBox checkboxTaskDetail = dialogView.findViewById(R.id.checkboxTaskDetail);
        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Set current task values
        tvTaskTitleDetail.setText(task.getTitle());
        tvTaskDescriptionDetail.setText(task.getDescription());
        checkboxTaskDetail.setChecked(task.isCompleted());

        // Set due date if available
        if (task.getDueDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            tvDueDateDetail.setText(dateFormat.format(task.getDueDate()));
            tvDueDateDetail.setVisibility(View.VISIBLE);
        } else {
            tvDueDateDetail.setVisibility(View.GONE);
        }

        // Handle checkbox changes
        checkboxTaskDetail.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            TaskRepository repository = TaskRepository.getInstance(context);
            repository.updateTask(task);

            if (listener != null) {
                listener.onTaskUpdated(task);
            }
        });

        // Handle edit button click
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showEditDialog();
        });

        // Handle delete button click
        btnDelete.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Delete", (dialogInterface, i) -> {
                        // Get the TaskRepository instance and delete the task
                        TaskRepository repository = TaskRepository.getInstance(context);
                        repository.deleteTask(task);

                        if (deleteListener != null) {
                            deleteListener.onTaskDeleted(task);
                        }

                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText etTaskTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etTaskDescription = dialogView.findViewById(R.id.etTaskDescription);
        Button btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        Button btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);
        TextView tvSelectedDateTime = dialogView.findViewById(R.id.tvSelectedDateTime);
        Button btnSelectLabels = dialogView.findViewById(R.id.btnSelectLabels);
        ChipGroup chipGroupLabels = dialogView.findViewById(R.id.chipGroupLabels);
        Button btnSetReminder = dialogView.findViewById(R.id.btnSetReminder);

        // Set current task values
        etTaskTitle.setText(task.getTitle());
        etTaskDescription.setText(task.getDescription());

        final Calendar calendar = Calendar.getInstance();
        final boolean[] dateSelected = {false};
        final boolean[] timeSelected = {false};
        final boolean[] reminderSet = {task.isReminderSet()};  // Initialize from task

        if (task.getDueDate() != null) {
            calendar.setTime(task.getDueDate());
            dateSelected[0] = true;
            timeSelected[0] = true;
            updateSelectedDateTime(tvSelectedDateTime, calendar, true, true);
        }

        // Update reminder button state based on existing task
        if (reminderSet[0]) {
            btnSetReminder.setText("Reminder Set");
            btnSetReminder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        final Set<String> selectedLabels = new HashSet<>(task.getLabels());
        updateChipGroup(chipGroupLabels, new ArrayList<>(selectedLabels));

        btnSetReminder.setOnClickListener(v -> {
            if (!dateSelected[0] || !timeSelected[0]) {
                Toast.makeText(context, "Please select date and time first", Toast.LENGTH_SHORT).show();
                return;
            }

            reminderSet[0] = true;
            btnSetReminder.setText("Reminder Set");
            btnSetReminder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            Toast.makeText(context, "Reminder set for selected time", Toast.LENGTH_SHORT).show();
        });

        btnSelectDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
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
                            Toast.makeText(context, "Cannot select past dates", Toast.LENGTH_SHORT).show();
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
                    context,
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


        // Update reminder button
        if (reminderSet[0]) {
            btnSetReminder.setText("Reminder Set");
            btnSetReminder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }

        btnSetReminder.setOnClickListener(v -> {
            if (!dateSelected[0] || !timeSelected[0]) {
                Toast.makeText(context, "Please select date and time first", Toast.LENGTH_SHORT).show();
                return;
            }

            reminderSet[0] = true;
            btnSetReminder.setText("Reminder Set");
            btnSetReminder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            Toast.makeText(context, "Reminder set for selected time", Toast.LENGTH_SHORT).show();
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            String title = etTaskTitle.getText().toString().trim();
            String description = etTaskDescription.getText().toString().trim();

            if (!title.isEmpty()) {
                task.setTitle(title);
                task.setDescription(description);

                if (dateSelected[0] || timeSelected[0]) {
                    task.setDueDate(calendar.getTime());
                } else {
                    task.setDueDate(null);
                }

                // Update reminder status
                task.setReminderSet(reminderSet[0]);

                // Update labels
                task.getLabels().clear();
                for (String label : selectedLabels) {
                    task.addLabel(label);
                }

                // Update task in repository
                TaskRepository repository = TaskRepository.getInstance(context);
                repository.updateTask(task);

                // Schedule or cancel reminder with error handling
                try {
                    if (reminderSet[0] && task.getDueDate() != null) {
                        ReminderHelper.scheduleReminder(context, task);
                    } else {
                        ReminderHelper.cancelReminder(context, task);
                    }
                } catch (Exception e) {
                    // Log the error but don't crash
                    e.printStackTrace();
                    Toast.makeText(context, "Could not set reminder: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Make sure to update the task's reminder status if it failed
                    task.setReminderSet(false);
                    repository.updateTask(task);
                }

                if (listener != null) {
                    listener.onTaskUpdated(task);
                }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_labels, null);
        builder.setView(dialogView);

        RecyclerView rvLabels = dialogView.findViewById(R.id.rvLabels);
        EditText etNewLabel = dialogView.findViewById(R.id.etNewLabel);
        Button btnAddNewLabel = dialogView.findViewById(R.id.btnAddNewLabel);
        Button btnSaveLabels = dialogView.findViewById(R.id.btnSaveLabels);
        ChipGroup chipGroupSelectedLabels = dialogView.findViewById(R.id.chipGroupSelectedLabels);

        LabelManager labelManager = LabelManager.getInstance(context);
        List<String> allLabels = labelManager.getAllLabels();
        List<String> currentSelectedLabels = new ArrayList<>(selectedLabels);

        LabelAdapter labelAdapter = new LabelAdapter(allLabels, currentSelectedLabels);
        rvLabels.setLayoutManager(new LinearLayoutManager(context));
        rvLabels.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
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
            Chip chip = new Chip(context);
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
}