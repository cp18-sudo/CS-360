package com.example.weighttracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Displays weight entries in a RecyclerView using {@link ListAdapter} backed
 * by {@link DiffUtil}. DiffUtil computes the minimal set of changes between
 * the old and new list, so only affected rows animate and rebind instead of
 * the entire list refreshing on every database change.
 *
 * Click listeners use {@code getAdapterPosition()} to avoid a stale-position
 * bug that existed in the original adapter.
 */
public class WeightAdapter extends ListAdapter<WeightEntry, WeightAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(WeightEntry entry);
    }

    public interface OnEditListener {
        void onEdit(WeightEntry entry);
    }

    private static final DiffUtil.ItemCallback<WeightEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull WeightEntry oldItem,
                                               @NonNull WeightEntry newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull WeightEntry oldItem,
                                                  @NonNull WeightEntry newItem) {
                    return oldItem.getId() == newItem.getId()
                            && oldItem.getWeight() == newItem.getWeight()
                            && oldItem.getDate().equals(newItem.getDate());
                }
            };

    private final OnDeleteListener deleteListener;
    private final OnEditListener editListener;

    public WeightAdapter(OnDeleteListener deleteListener, OnEditListener editListener) {
        super(DIFF_CALLBACK);
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weight_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeightEntry entry = getItem(position);
        holder.dateText.setText(entry.getDate());
        holder.weightText.setText(String.valueOf(entry.getWeight()));

        holder.editButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && editListener != null) {
                editListener.onEdit(getItem(pos));
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && deleteListener != null) {
                deleteListener.onDelete(getItem(pos));
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView dateText;
        final TextView weightText;
        final Button editButton;
        final Button deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            weightText = itemView.findViewById(R.id.weightText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
