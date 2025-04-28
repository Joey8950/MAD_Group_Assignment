package com.example.madtest;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText etTitle;
    Spinner spinnerStatus;
    Button btnAddTask;
    RecyclerView recyclerView;
    ProgressBar progressBar;
    TextView txtProgress;

    TaskAdapter taskAdapter;
    List<Task> taskList;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTitle = findViewById(R.id.etTitle);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnAddTask = findViewById(R.id.btnAddTask);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        txtProgress = findViewById(R.id.txtProgress);

        dbHelper = new DatabaseHelper(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.task_statuses, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String status = spinnerStatus.getSelectedItem().toString();

                if (!title.isEmpty()) {
                    boolean inserted = dbHelper.addTask(title, status);
                    if (inserted) {
                        Toast.makeText(MainActivity.this, "Task added!", Toast.LENGTH_SHORT).show();
                        etTitle.setText("");
                        spinnerStatus.setSelection(0);
                        loadTasks();
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to add task.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Title cannot be empty.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadTasks();
    }

    private void loadTasks() {
        taskList = dbHelper.getAllTasks();
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onUpdate(Task task) {
                showUpdateDialog(task);
            }

            @Override
            public void onDelete(Task task) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Confirm Delete")
                        .setMessage("Do you really want to DELETE this??")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            dbHelper.deleteTask(task.getID());
                            loadTasks(); // Refresh list
                            Toast.makeText(MainActivity.this, "Task deleted successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        recyclerView.setAdapter(taskAdapter);
        updateProgress(); // Update progress bar after loading tasks
    }

    private void showUpdateDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Task");

        View view = getLayoutInflater().inflate(R.layout.dialog_update_task, null);
        EditText editTitle = view.findViewById(R.id.etUpdateTitle);
        Spinner spinner = view.findViewById(R.id.spinnerUpdateStatus);

        editTitle.setText(task.getTitle());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.task_statuses, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int index = adapter.getPosition(task.getStatus());
        spinner.setSelection(index);

        builder.setView(view);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String updatedTitle = editTitle.getText().toString().trim();
            String updatedStatus = spinner.getSelectedItem().toString();

            if (!updatedTitle.isEmpty()) {
                dbHelper.updateTask(task.getID(), updatedTitle, updatedStatus);
                Toast.makeText(MainActivity.this, "Task updated successfully!", Toast.LENGTH_SHORT).show();
                loadTasks();
            } else {
                Toast.makeText(MainActivity.this, "Title cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateProgress() {
        int total = taskList.size();
        int completed = 0;

        for (Task task : taskList) {
            if (task.getStatus().equals("Completed")) completed++;
        }

        int percent = total == 0 ? 0 : (completed * 100 / total);
        progressBar.setProgress(percent);
        txtProgress.setText(percent + "% Completed");
    }
}
