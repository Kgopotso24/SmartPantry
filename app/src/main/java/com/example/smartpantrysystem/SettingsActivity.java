package com.example.smartpantrysystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/** Screen 5: settings (expiring-soon alerts toggle + default unit). Saved in SharedPreferences. */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "pantry_prefs";
    public static final String KEY_ALERTS = "expiry_alerts";
    public static final String KEY_UNIT = "default_unit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        NavHelper.applyHeaderInsets(findViewById(R.id.header));
        final SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

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
