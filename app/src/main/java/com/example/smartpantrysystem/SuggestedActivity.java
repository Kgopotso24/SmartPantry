package com.example.smartpantrysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Screen 3: lists ONLY the recipes the user can make right now (strict matching). */
public class SuggestedActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private RecipeAdapter adapter;
    private TextView tvSummary, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested);

        db = new DatabaseHelper(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));

        tvSummary = findViewById(R.id.tvSummary);
        tvEmpty = findViewById(R.id.tvEmpty);

        RecyclerView list = findViewById(R.id.rvRecipes);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(recipe -> {
            // Intent carrying the id of the chosen recipe to the detail screen
            Intent intent = new Intent(SuggestedActivity.this, RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
            startActivity(intent);
        });
        list.setAdapter(adapter);

        NavHelper.setupBottomNav(this, R.id.nav_recipes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<PantryItem> pantry = db.getAllItems();
        List<Recipe> allRecipes = db.getAllRecipes();

        // Strict matching: keep a recipe only if the pantry covers EVERY ingredient
        List<Recipe> makeable = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            if (IngredientMatcher.canMake(recipe, pantry)) {
                makeable.add(recipe);
            }
        }

        adapter.setRecipes(makeable);
        tvSummary.setText("You can make " + makeable.size() + " of " + allRecipes.size() + " recipes");

        // Friendly message instead of a blank screen
        tvEmpty.setVisibility(makeable.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
