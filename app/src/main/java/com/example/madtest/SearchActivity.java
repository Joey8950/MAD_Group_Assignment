package com.example.tasktodo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private RecyclerView rvRecentlyViewed;
    private TextView tvNoResults;
    private TaskRepository taskRepository;
    private TaskAdapter searchResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize views
        etSearch = findViewById(R.id.etSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvNoResults = findViewById(R.id.tvNoResults);
        rvRecentlyViewed = findViewById(R.id.rvRecentlyViewed);

        // Initialize task repository
        taskRepository = TaskRepository.getInstance(this);

        // Set up search results recycler view
        searchResultsAdapter = new TaskAdapter(new ArrayList<>());
        searchResultsAdapter.setOnTaskClickListener(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rvSearchResults.setAdapter(searchResultsAdapter);

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up recently viewed items
        setupRecentlyViewedItems();

        // Set up search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    // If search query is empty, hide results and show recently viewed
                    rvSearchResults.setVisibility(View.GONE);
                    tvNoResults.setVisibility(View.GONE);
                    rvRecentlyViewed.setVisibility(View.VISIBLE);
                } else {
                    // Search tasks
                    List<Task> searchResults = taskRepository.searchTasks(query);
                    
                    if (searchResults.isEmpty()) {
                        // No results found
                        rvRecentlyViewed.setVisibility(View.GONE);
                        rvSearchResults.setVisibility(View.GONE);
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("No tasks found for \"" + query + "\"");
                    } else {
                        // Display search results
                        rvRecentlyViewed.setVisibility(View.GONE);
                        tvNoResults.setVisibility(View.GONE);
                        rvSearchResults.setVisibility(View.VISIBLE);
                        searchResultsAdapter.updateTasks(searchResults);
                    }
                }
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            // Set the selected item to search
            bottomNavigationView.setSelectedItemId(R.id.nav_search);
            
            // Set up the navigation listener
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_today) {
                    // Navigate to Today view (MainActivity)
                    navigateToToday();
                    return true;
                } else if (itemId == R.id.nav_upcoming) {
                    // Navigate to Upcoming view
                    // You can implement this later
                    return true;
                } else if (itemId == R.id.nav_search) {
                    // Already on search, do nothing
                    return true;
                } else if (itemId == R.id.nav_browse) {
                    // Navigate to Browse view
                    // You can implement this later
                    return true;
                }
                
                return false;
            });
        }
    }

    private void navigateToToday() {
        // Go back to MainActivity (Today view)
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // Close this activity
    }

    private void setupRecentlyViewedItems() {
        // This would typically show recently viewed tasks or categories
        // For now, we'll just show a static list of "Today" and "Upcoming"
        List<RecentItem> recentItems = new ArrayList<>();
        recentItems.add(new RecentItem("Today", RecentItem.TYPE_SECTION));
        recentItems.add(new RecentItem("Upcoming", RecentItem.TYPE_SECTION));
        
        RecentItemAdapter recentAdapter = new RecentItemAdapter(recentItems);
        rvRecentlyViewed.setLayoutManager(new LinearLayoutManager(this));
        rvRecentlyViewed.setAdapter(recentAdapter);
        
        recentAdapter.setOnItemClickListener(item -> {
            // Handle clicks on recently viewed items
            if ("Today".equals(item.getTitle())) {
                navigateToToday(); // Navigate to Today view
            } else if ("Upcoming".equals(item.getTitle())) {
                // Navigate to upcoming tasks view
                // You can implement this later
            }
        });
    }

    @Override
    public void onBackPressed() {
        // When back button is pressed, go to Today view
        navigateToToday();
    }

    @Override
    public void onTaskClick(Task task, int position) {
        // Show task detail dialog
        showTaskDetailDialog(task);
    }

    private void showTaskDetailDialog(Task task) {
        TaskDetailDialog dialog = new TaskDetailDialog(this, task);
        dialog.setOnTaskUpdatedListener(updatedTask -> {
            // Update the task in the repository
            taskRepository.updateTask(updatedTask);
            
            // Refresh the search results
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                List<Task> searchResults = taskRepository.searchTasks(query);
                searchResultsAdapter.updateTasks(searchResults);
            }
        });
        dialog.show();
    }
}