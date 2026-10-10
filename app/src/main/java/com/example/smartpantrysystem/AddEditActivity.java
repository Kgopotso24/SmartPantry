package com.example.smartpantrysystem;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

/** Screen 2: add a new ingredient, or edit an existing one (with input validation). */
public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "item_id";
    public static final String[] UNITS = {"g", "kg", "ml", "l", "cup", "tbsp", "tsp", "pcs"};

    private DatabaseHelper db;
    private AutoCompleteTextView etName;
    private EditText etQty, etExpiry;
    private Spinner spUnit;
    private int itemId = -1; // -1 means "adding a new item"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        db = new DatabaseHelper(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));

        etName = findViewById(R.id.etName);
        etQty = findViewById(R.id.etQty);
        etExpiry = findViewById(R.id.etExpiry);
        spUnit = findViewById(R.id.spUnit);
        TextView tvTitle = findViewById(R.id.tvTitle);

        spUnit.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, UNITS));

        // Suggest ingredient names used by the recipes, so spelling matches
        etName.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, db.getIngredientNames()));

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        // Did the Pantry screen send us an item id? Then we are in EDIT mode.
        itemId = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);
        PantryItem existing = (itemId == -1) ? null : db.getItem(itemId);

        if (existing != null) {
            tvTitle.setText("Edit Ingredient");
            etName.setText(existing.getName(), false);
            etQty.setText(IngredientMatcher.formatQty(existing.getQuantity()));
            etExpiry.setText(existing.getExpiry() == null ? "" : existing.getExpiry());
            spUnit.setSelection(indexOfUnit(existing.getUnit()));
        } else {
            itemId = -1;
            SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
            spUnit.setSelection(indexOfUnit(prefs.getString(SettingsActivity.KEY_UNIT, "g")));
        }

        etExpiry.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.tvClearDate).setOnClickListener(v -> etExpiry.setText(""));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    private int indexOfUnit(String unit) {
        for (int i = 0; i < UNITS.length; i++) {
            if (UNITS[i].equals(unit)) {
                return i;
            }
        }
        return 0;
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                etExpiry.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /** Validates the form; only saves when everything is correct. */
    private void save() {
        String name = etName.getText().toString().trim();
        String qtyText = etQty.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();
        boolean valid = true;

        if (name.isEmpty()) {
            etName.setError("Please enter an ingredient name");
            valid = false;
        } else if (name.matches(".*\\d.*")) {
            etName.setError("The name cannot contain numbers");
            valid = false;
        }

        double quantity = 0;
        if (qtyText.isEmpty()) {
            etQty.setError("Please enter a quantity");
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(qtyText);
                if (quantity <= 0) {
                    etQty.setError("Quantity must be more than 0");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                etQty.setError("Enter a valid number");
                valid = false;
            }
        }

        if (!valid) {
            return; // stay on the form so the user can fix the errors
        }

        String unit = spUnit.getSelectedItem().toString();
        String expiryValue = expiry.isEmpty() ? null : expiry;

        if (itemId == -1) {
            db.addItem(name, quantity, unit, expiryValue);          // CREATE
            Toast.makeText(this, "Ingredient added", Toast.LENGTH_SHORT).show();
        } else {
            db.updateItem(itemId, name, quantity, unit, expiryValue); // UPDATE
            Toast.makeText(this, "Ingredient updated", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
