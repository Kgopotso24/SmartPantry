package com.example.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Browse screen (opened from Suggestions) with two clearly separated views:
 *  - "Can make now": ONLY recipes the pantry fully covers (the strict-matching rule)
 *  - "All recipes": every recipe, with what is missing (ready ones first)
 */
public class SuggestedActivity extends AppCompatActivity {

    public static final String EXTRA_SHOW_ALL = "show_all";

    private DatabaseHelper db;
    private RecipeAdapter adapter;
    private TextView tvSummary, tvEmpty, tabCan, tabAll;
    private boolean showAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested);

        db = new DatabaseHelper(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));
        showAll = getIntent().getBooleanExtra(EXTRA_SHOW_ALL, false);

        tvSummary = findViewById(R.id.tvSummary);
        tvEmpty = findViewById(R.id.tvEmpty);
        tabCan = findViewById(R.id.tabCan);
        tabAll = findViewById(R.id.tabAll);

        RecyclerView list = findViewById(R.id.rvRecipes);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(recipe -> {
            // Intent carrying the id of the chosen recipe to the detail screen
            Intent intent = new Intent(SuggestedActivity.this, RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
            startActivity(intent);
        });
        list.setAdapter(adapter);

        tabCan.setOnClickListener(v -> { showAll = false; refresh(); });
        tabAll.setOnClickListener(v -> { showAll = true; refresh(); });
        findViewById(R.id.btnNewRecipe).setOnClickListener(v ->
                startActivity(new Intent(SuggestedActivity.this, AddRecipeActivity.class)));

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh(); // also picks up recipes just created on the Add Recipe screen
    }

    private void refresh() {
        final List<PantryItem> pantry = db.getAllItems();
        List<Recipe> allRecipes = db.getAllRecipes();

        // Highlight the selected tab
        styleTab(tabCan, !showAll);
        styleTab(tabAll, showAll);

        // Work out what each recipe is missing
        Map<Integer, List<String>> missing = new HashMap<>();
        List<Recipe> makeable = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            List<String> gaps = IngredientMatcher.missing(recipe, pantry);
            missing.put(recipe.getId(), gaps);
            if (IngredientMatcher.canMake(recipe, pantry)) {
                makeable.add(recipe);
            }
        }

        if (!showAll) {
            // STRICT list: only fully covered recipes
            adapter.setRecipes(makeable, null);
            tvSummary.setText("You can make " + makeable.size() + " of " + allRecipes.size() + " recipes");
            tvEmpty.setVisibility(makeable.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            // ALL list: ready recipes first, then the ones closest to ready
            final Map<Integer, List<String>> gapsById = missing;
            List<Recipe> sorted = new ArrayList<>(allRecipes);
            Collections.sort(sorted, (a, b) -> {
                int byMissing = gapsById.get(a.getId()).size() - gapsById.get(b.getId()).size();
                return byMissing != 0 ? byMissing : a.getName().compareToIgnoreCase(b.getName());
            });
            adapter.setRecipes(sorted, missing);
            tvSummary.setText(allRecipes.size() + " recipes - " + makeable.size() + " ready to cook");
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.pm_btn_gradient : R.drawable.pm_chip_mint);
        tab.setTextColor(selected ? 0xFFFFFFFF : ContextCompat.getColor(this, R.color.pm_green));
    }
}
