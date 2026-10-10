package com.example.smartpantrysystem;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 4: full ingredient list and method for one recipe. */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "recipe_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        NavHelper.applyHeaderInsets(findViewById(R.id.header));
        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        int recipeId = getIntent().getIntExtra(EXTRA_RECIPE_ID, -1);
        Recipe recipe = new DatabaseHelper(this).getRecipe(recipeId);

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
    }
}
