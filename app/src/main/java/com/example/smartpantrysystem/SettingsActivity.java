package com.example.smartpantrysystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/** Settings screen: your name, expiring-soon alerts toggle, default unit (SharedPreferences). */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "pantry_prefs";
    public static final String KEY_ALERTS = "expiry_alerts";
    public static final String KEY_UNIT = "default_unit";
    public static final String KEY_NAME = "user_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        NavHelper.applyHeaderInsets(findViewById(R.id.header));
        final SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Name shown in the Home greeting (saved as you type)
        EditText etUserName = findViewById(R.id.etUserName);
        etUserName.setText(prefs.getString(KEY_NAME, ""));
        etUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                prefs.edit().putString(KEY_NAME, s.toString().trim()).apply();
            }
        });

        // Switch: expiring-soon alerts
        SwitchCompat swAlerts = findViewById(R.id.swAlerts);
        swAlerts.setChecked(prefs.getBoolean(KEY_ALERTS, true));
        swAlerts.setOnCheckedChangeListener((button, isChecked) ->
                prefs.edit().putBoolean(KEY_ALERTS, isChecked).apply());

        // Spinner: default unit used when adding a new ingredient
        Spinner spUnit = findViewById(R.id.spDefaultUnit);
        spUnit.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, AddEditActivity.UNITS));

        String saved = prefs.getString(KEY_UNIT, "g");
        for (int i = 0; i < AddEditActivity.UNITS.length; i++) {
            if (AddEditActivity.UNITS[i].equals(saved)) {
                spUnit.setSelection(i);
            }
        }
        spUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(KEY_UNIT, AddEditActivity.UNITS[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        NavHelper.setupBottomNav(this, R.id.nav_settings);
    }
}
