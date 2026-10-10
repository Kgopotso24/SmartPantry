package com.example.smartpantrysystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Custom adapter: shows each PantryItem as a card in the pantry list. */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    /** Lets the screen react when the user taps Edit or Delete on a card. */
    public interface OnItemAction {
        void onEdit(PantryItem item);
        void onDelete(PantryItem item);
    }

    private List<PantryItem> items = new ArrayList<>();
    private final OnItemAction listener;

    public PantryAdapter(OnItemAction listener) {
        this.listener = listener;
    }

    public void setItems(List<PantryItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final PantryItem item = items.get(position);

        holder.tvName.setText(item.getName());
        holder.tvQty.setText(IngredientMatcher.formatQty(item.getQuantity()) + " " + item.getUnit());

        // Expiry text: red when expired or expiring within 3 days
        Long days = item.daysUntilExpiry();
        int grey = ContextCompat.getColor(holder.itemView.getContext(), R.color.pm_grey);
        int red = ContextCompat.getColor(holder.itemView.getContext(), R.color.pm_red);
        if (days == null) {
            holder.tvExpiry.setText("No expiry date");
            holder.tvExpiry.setTextColor(grey);
        } else if (days < 0) {
            holder.tvExpiry.setText("Expired on " + item.getExpiry());
            holder.tvExpiry.setTextColor(red);
        } else if (days == 0) {
            holder.tvExpiry.setText("Expires today");
            holder.tvExpiry.setTextColor(red);
        } else {
            holder.tvExpiry.setText("Expires " + item.getExpiry() + " (in " + days + " days)");
            holder.tvExpiry.setTextColor(days <= 3 ? red : grey);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvQty, tvExpiry, btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvExpiry = itemView.findViewById(R.id.tvExpiry);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
