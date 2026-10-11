package com.example.smartpantrysystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user create their own recipe: a name, the method, and any number of
 * ingredient rows (name + quantity + unit). The recipe is then matched against the
 * pantry exactly like the built-in recipes.
 */
public class AddRecipeActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private EditText etName, etSteps;
    private LinearLayout llIngredients;
    private List<String> suggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        db = new DatabaseHelper(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));

        etName = findViewById(R.id.etRecipeName);
        etSteps = findViewById(R.id.etSteps);
        llIngredients = findViewById(R.id.llIngredients);
        suggestions = db.getIngredientNames(); // names already used, so spelling matches

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvAddRow).setOnClickListener(v -> addRow());
        findViewById(R.id.btnSaveRecipe).setOnClickListener(v -> save());

        addRow(); // start with two empty ingredient rows
        addRow();
    }

    /** Adds one ingredient row (name + quantity + unit + remove button) to the form. */
    private void addRow() {
        final View row = getLayoutInflater().inflate(R.layout.item_ingredient_row, llIngredients, false);

        AutoCompleteTextView etIngName = row.findViewById(R.id.etIngName);
        etIngName.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, suggestions));

        Spinner spUnit = row.findViewById(R.id.spIngUnit);
        spUnit.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, AddEditActivity.UNITS));
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        String defaultUnit = prefs.getString(SettingsActivity.KEY_UNIT, "g");
        for (int i = 0; i < AddEditActivity.UNITS.length; i++) {
            if (AddEditActivity.UNITS[i].equals(defaultUnit)) {
                spUnit.setSelection(i);
            }
        }

        row.findViewById(R.id.tvRemoveRow).setOnClickListener(v -> llIngredients.removeView(row));
        llIngredients.addView(row);
    }

    /** Validates the whole form, then saves the recipe and its ingredients. */
    private void save() {
        String name = etName.getText().toString().trim();
        String steps = etSteps.getText().toString().trim();
        boolean valid = true;

        if (name.isEmpty()) {
            etName.setError("Please enter a recipe name");
            valid = false;
        }
        if (steps.isEmpty()) {
            etSteps.setError("Please write the method");
            valid = false;
        }

        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < llIngredients.getChildCount(); i++) {
            View row = llIngredients.getChildAt(i);
            AutoCompleteTextView etIngName = row.findViewById(R.id.etIngName);
            EditText etQty = row.findViewById(R.id.etIngQty);
            Spinner spUnit = row.findViewById(R.id.spIngUnit);

            String ingName = etIngName.getText().toString().trim();
            String qtyText = etQty.getText().toString().trim();
            if (ingName.isEmpty() && qtyText.isEmpty()) {
                continue; // a completely blank row is simply ignored
            }

            if (ingName.isEmpty()) {
                etIngName.setError("Enter the ingredient");
                valid = false;
            } else if (ingName.matches(".*\\d.*")) {
                etIngName.setError("No numbers in the name");
                valid = false;
            }

            double quantity = 0;
            if (qtyText.isEmpty()) {
                etQty.setError("Enter a quantity");
                valid = false;
            } else {
                try {
                    quantity = Double.parseDouble(qtyText);
                    if (quantity <= 0) {
                        etQty.setError("Must be more than 0");
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    etQty.setError("Enter a valid number");
                    valid = false;
                }
            }

            if (valid) {
                ingredients.add(new RecipeIngredient(ingName, quantity,
                        spUnit.getSelectedItem().toString()));
            }
        }

        if (valid && ingredients.isEmpty()) {
            Toast.makeText(this, "Add at least one ingredient", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!valid) {
            return; // stay on the form so the user can fix the errors
        }

        db.addCustomRecipe(name, steps, ingredients);
        Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
