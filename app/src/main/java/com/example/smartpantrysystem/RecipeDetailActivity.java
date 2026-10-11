package com.example.smartpantrysystem;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** Full ingredient list and method for one recipe (user-made recipes can be deleted here). */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipe_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        NavHelper.applyHeaderInsets(findViewById(R.id.header));
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        final DatabaseHelper db = new DatabaseHelper(this);
        final int recipeId = getIntent().getIntExtra(EXTRA_RECIPE_ID, -1);
        final Recipe recipe = db.getRecipe(recipeId);

        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTitle)).setText(recipe.getName());

        StringBuilder ingredients = new StringBuilder();
        for (RecipeIngredient ing : recipe.getIngredients()) {
            ingredients.append("\u2022 ")
                    .append(IngredientMatcher.formatQty(ing.getQuantity())).append(" ")
                    .append(ing.getUnit()).append(" ")
                    .append(ing.getName()).append("\n");
        }
        ((TextView) findViewById(R.id.tvIngredients)).setText(ingredients.toString().trim());
        ((TextView) findViewById(R.id.tvSteps)).setText(recipe.getSteps());

        // Only recipes the user created can be deleted; the built-in ones are protected
        View btnDelete = findViewById(R.id.btnDeleteRecipe);
        if (recipe.isCustom()) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Delete recipe")
                    .setMessage("Delete \"" + recipe.getName() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        db.deleteRecipe(recipeId);
                        Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }
    }
}
