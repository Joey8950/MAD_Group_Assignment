package com.example.tasktodo;

import android.content.Context;
import com.example.tasktodo.database.TaskDao;
import java.util.ArrayList;
import java.util.Calendar; // Add this import
import java.util.List;


public class TaskRepository {
    private static TaskRepository instance;
    private TaskDao taskDao;

    private TaskRepository(Context context) {
        taskDao = new TaskDao(context);
        taskDao.open();

        // Check if we have any tasks, if not add sample tasks
        if (taskDao.getAllTasks().isEmpty()) {
            addSampleTasks();
        }
    }

    public static synchronized TaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TaskRepository(context.getApplicationContext());
        }
        return instance;
    }

    private void addSampleTasks() {
        // Add sample tasks to the database
        Task task1 = new Task("Download Todoist on all your devices and email for", "Multiple devices", false);
        task1.addLabel("Personal");

        Task task2 = new Task("Testing", "Test", false);
        task2.addLabel("Work");

        Task task3 = new Task("Buy groceries", "Milk, eggs, bread", false);
        task3.addLabel("Shopping");

        Task task4 = new Task("Prepare presentation", "For Monday meeting", false);
        task4.addLabel("Work");
        task4.addLabel("Important");

        addTask(task1);
        addTask(task2);
        addTask(task3);
        addTask(task4);
    }

    public List<Task> getAllTasks() {
        return taskDao.getAllTasks();
    }

    public List<Task> getTodayTasks() {
        List<Task> allTasks = getAllTasks();
        List<Task> todayTasks = new ArrayList<>();

        // Get current date without time component
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);

        for (Task task : allTasks) {
            // Include tasks without due date in today's tasks
            if (task.getDueDate() == null) {
                todayTasks.add(task);
                continue;
            }

            // Get task date without time component
            Calendar taskDate = Calendar.getInstance();
            taskDate.setTime(task.getDueDate());
            taskDate.set(Calendar.HOUR_OF_DAY, 0);
            taskDate.set(Calendar.MINUTE, 0);
            taskDate.set(Calendar.SECOND, 0);
            taskDate.set(Calendar.MILLISECOND, 0);

            // Check if task date is today
            if (!taskDate.before(today) && taskDate.before(tomorrow)) {
                todayTasks.add(task);
            }
        }

        return todayTasks;
    }

    public List<Task> getUpcomingTasks() {
        List<Task> allTasks = getAllTasks();
        List<Task> upcomingTasks = new ArrayList<>();

        // Get current date without time component
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (Task task : allTasks) {
            if (task.getDueDate() != null) {
                // Check if the task's due date is in the future
                if (task.getDueDate().after(today.getTime())) {
                    upcomingTasks.add(task);
                }
            }
        }

        return upcomingTasks;
    }

    public List<Task> searchTasks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return taskDao.searchTasks(query);
    }

    public Task getTask(long taskId) {
        return taskDao.getTaskById(taskId);
    }

    public void addTask(Task task) {
        long id = taskDao.insertTask(task);
        task.setId(id);
    }

    public void updateTask(Task task) {
        taskDao.updateTask(task, task.getId());
    }

    public void deleteTask(Task task) {
        taskDao.deleteTask(task.getId());
    }
}
