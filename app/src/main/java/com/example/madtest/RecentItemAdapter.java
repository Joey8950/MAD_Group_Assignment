package com.example.tasktodo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentItemAdapter extends RecyclerView.Adapter<RecentItemAdapter.ViewHolder> {
    
    private List<RecentItem> items;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(RecentItem item);
    }
    
    public RecentItemAdapter(List<RecentItem> items) {
        this.items = items;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == RecentItem.TYPE_SECTION) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_section, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recent_task, parent, false);
        }
        return new ViewHolder(view, viewType);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        
        if (item.getType() == RecentItem.TYPE_TASK && holder.tvDescription != null) {
            holder.tvDescription.setText(item.getDescription());
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        
        ViewHolder(View itemView, int viewType) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            
            // Only try to find tvDescription for task items, not section items
            if (viewType == RecentItem.TYPE_TASK) {
                tvDescription = itemView.findViewById(R.id.tvDescription);
            }
        }
    }
}