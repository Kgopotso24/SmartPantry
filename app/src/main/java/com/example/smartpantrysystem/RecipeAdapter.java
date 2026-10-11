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
import java.util.Map;

/** Custom adapter: shows each Recipe as a tappable card. */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    public interface OnRecipeClick {
        void onClick(Recipe recipe);
    }

    private List<Recipe> recipes = new ArrayList<>();
    private Map<Integer, List<String>> missing; // null = "Can make now" view (no status needed)
    private final OnRecipeClick listener;

    public RecipeAdapter(OnRecipeClick listener) {
        this.listener = listener;
    }

    /**
     * @param newRecipes recipes to show
     * @param missingMap recipe id -> missing ingredient names, or null to show the plain
     *                   "n ingredients" line (used by the strict "Can make now" list)
     */
    public void setRecipes(List<Recipe> newRecipes, Map<Integer, List<String>> missingMap) {
        this.recipes = newRecipes;
        this.missing = missingMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Recipe recipe = recipes.get(position);
        holder.tvName.setText(recipe.getName());
        holder.tvInitial.setText(recipe.getName().substring(0, 1).toUpperCase());

        int grey = ContextCompat.getColor(holder.itemView.getContext(), R.color.pm_grey);
        int green = ContextCompat.getColor(holder.itemView.getContext(), R.color.pm_green);
        int amber = ContextCompat.getColor(holder.itemView.getContext(), R.color.pm_amber);

        String info;
        int color = grey;
        if (missing == null) {
            info = recipe.getIngredients().size() + " ingredients - tap to see the method";
        } else {
            List<String> gaps = missing.get(recipe.getId());
            if (gaps == null || gaps.isEmpty()) {
                info = "Ready to cook";
                color = green;
            } else {
                info = "Missing: " + gaps.get(0)
                        + (gaps.size() > 1 ? " +" + (gaps.size() - 1) + " more" : "");
                color = amber;
            }
        }
        if (recipe.isCustom()) {
            info += "  \u2022  Your recipe";
        }
        holder.tvInfo.setText(info);
        holder.tvInfo.setTextColor(color);

        holder.itemView.setOnClickListener(v -> listener.onClick(recipe));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvInitial, tvName, tvInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvInitial);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvInfo = itemView.findViewById(R.id.tvRecipeInfo);
        }
    }
}
