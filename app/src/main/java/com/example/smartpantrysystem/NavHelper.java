package com.example.smartpantrysystem;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Small helper shared by all screens: bottom navigation bar + status-bar spacing. */
public class NavHelper {

    /** Wires up the bottom navigation bar (Home / Pantry / Recipes / Settings) using Intents. */
    public static void setupBottomNav(final Activity activity, final int selectedId) {
        BottomNavigationView nav = activity.findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(selectedId);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedId) {
                return true; // already on this screen
            }

            Intent intent;
            if (id == R.id.nav_home) {
                // go back to the existing Home screen instead of stacking a new one
                intent = new Intent(activity, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            } else if (id == R.id.nav_pantry) {
                intent = new Intent(activity, MainActivity.class);
            } else if (id == R.id.nav_recipes) {
                intent = new Intent(activity, SuggestedActivity.class);
            } else {
                intent = new Intent(activity, SettingsActivity.class);
            }

            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            if (selectedId != R.id.nav_home) {
                activity.finish(); // Home stays underneath as the "root" screen
            }
            return true;
        });
    }

    /** Pushes the green header down so it does not sit under the phone's status bar. */
    public static void applyHeaderInsets(View header) {
        final int originalTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(header, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), originalTop + bars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
    }
}
