package com.example.smartpantrysystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Screen 1: the Pantry list (READ + entry point for Create / Update / Delete). */
public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private PantryAdapter adapter;
    private TextView tvEmpty, tvAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));

        tvEmpty = findViewById(R.id.tvEmpty);
        tvAlert = findViewById(R.id.tvAlert);

        RecyclerView list = findViewById(R.id.rvPantry);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryAdapter(new PantryAdapter.OnItemAction() {
            @Override
            public void onEdit(PantryItem item) {
                // Intent carrying the id of the item to edit
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra(AddEditActivity.EXTRA_ITEM_ID, item.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(PantryItem item) {
                confirmDelete(item);
            }
        });
        list.setAdapter(adapter);

        findViewById(R.id.btnAdd).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddEditActivity.class)));

        NavHelper.setupBottomNav(this, R.id.nav_pantry);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh(); // reload from the database every time we come back to this screen
    }

    private void refresh() {
        List<PantryItem> items = db.getAllItems();
        adapter.setItems(items);
        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        // Expiring-soon alert banner (can be switched off in Settings)
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        boolean alertsOn = prefs.getBoolean(SettingsActivity.KEY_ALERTS, true);
        int expiringSoon = 0;
        for (PantryItem item : items) {
            Long days = item.daysUntilExpiry();
            if (days != null && days <= 3) {
                expiringSoon++;
            }
        }
        if (alertsOn && expiringSoon > 0) {
            tvAlert.setText("\u26A0 " + expiringSoon + " item(s) expired or expiring within 3 days");
            tvAlert.setVisibility(View.VISIBLE);
        } else {
            tvAlert.setVisibility(View.GONE);
        }
    }

    private void confirmDelete(final PantryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete ingredient")
                .setMessage("Remove " + item.getName() + " from your pantry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deleteItem(item.getId());
                    Toast.makeText(this, "Ingredient deleted", Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
