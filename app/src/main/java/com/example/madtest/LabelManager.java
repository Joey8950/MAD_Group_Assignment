package com.example.tasktodo;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LabelManager {
    private static final String PREF_NAME = "label_preferences";
    private static final String KEY_LABELS = "saved_labels";

    private static LabelManager instance;
    private final SharedPreferences preferences;
    private final List<String> defaultLabels = Arrays.asList("Work", "Personal", "Shopping", "Health", "Education");

    private LabelManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized LabelManager getInstance(Context context) {
        if (instance == null) {
            instance = new LabelManager(context.getApplicationContext());
        }
        return instance;
    }

    public List<String> getAllLabels() {
        Set<String> savedLabels = preferences.getStringSet(KEY_LABELS, new HashSet<>(defaultLabels));
        return new ArrayList<>(savedLabels);
    }

    public void addLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return;
        }

        Set<String> savedLabels = new HashSet<>(getAllLabels());
        savedLabels.add(label.trim());

        preferences.edit().putStringSet(KEY_LABELS, savedLabels).apply();
    }

    public void removeLabel(String label) {
        Set<String> savedLabels = new HashSet<>(getAllLabels());
        savedLabels.remove(label);

        preferences.edit().putStringSet(KEY_LABELS, savedLabels).apply();
    }
}