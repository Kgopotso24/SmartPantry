package com.example.smartpantrysystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Home screen: greeting, pantry summary, items to use soon, and a recipe you can cook now. */
public class HomeActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextView tvDate, tvGreeting, tvCount, tvCountLabel;
    private TextView tvSoonBadge, tvSoonList, tvReadyBadge, tvFeatureName, tvFeatureInfo, btnFeature;
    private View cardSoon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));

        tvDate = findViewById(R.id.tvDate);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvCount = findViewById(R.id.tvCount);
        tvCountLabel = findViewById(R.id.tvCountLabel);
        cardSoon = findViewById(R.id.cardSoon);
        tvSoonBadge = findViewById(R.id.tvSoonBadge);
        tvSoonList = findViewById(R.id.tvSoonList);
        tvReadyBadge = findViewById(R.id.tvReadyBadge);
        tvFeatureName = findViewById(R.id.tvFeatureName);
        tvFeatureInfo = findViewById(R.id.tvFeatureInfo);
        btnFeature = findViewById(R.id.btnFeature);

        // Intents to the other screens
        findViewById(R.id.cardPantry).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MainActivity.class)));
        findViewById(R.id.tvFindRecipes).setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SuggestedActivity.class)));

        NavHelper.setupBottomNav(this, R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        // ----- greeting -----
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        String name = prefs.getString(SettingsActivity.KEY_NAME, "").trim();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good morning" : (hour < 18 ? "Good afternoon" : "Good evening");
        tvGreeting.setText(name.isEmpty() ? greeting : greeting + ", " + name);
        tvDate.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                .format(new Date()).toUpperCase(Locale.getDefault()));

        // ----- pantry summary -----
        final List<PantryItem> items = db.getAllItems();
        tvCount.setText(String.valueOf(items.size()));
        tvCountLabel.setText(items.size() == 1 ? "ingredient at home" : "ingredients at home");

        // ----- items to use soon (expired or expiring within 3 days, soonest first) -----
        List<PantryItem> soon = new ArrayList<>();
        for (PantryItem item : items) {
            Long days = item.daysUntilExpiry();
            if (days != null && days <= 3) {
                soon.add(item);
            }
        }
        Collections.sort(soon, (a, b) -> Long.compare(a.daysUntilExpiry(), b.daysUntilExpiry()));

        if (soon.isEmpty()) {
            tvSoonBadge.setVisibility(View.GONE);
            tvSoonList.setText("Nothing is about to expire. Nice work!");
        } else {
            tvSoonBadge.setVisibility(View.VISIBLE);
            tvSoonBadge.setText(soon.size() + " to use soon");
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < soon.size() && i < 3; i++) {
                PantryItem item = soon.get(i);
                lines.append(item.getName()).append(" (")
                        .append(IngredientMatcher.formatQty(item.getQuantity())).append(" ")
                        .append(item.getUnit()).append(") \u2022 ")
                        .append(dueText(item.daysUntilExpiry())).append("\n");
            }
            if (soon.size() > 3) {
                lines.append("+").append(soon.size() - 3).append(" more");
            }
            tvSoonList.setText(lines.toString().trim());
        }

        // ----- a recipe you can cook right now (strict matching) -----
        List<Recipe> makeable = new ArrayList<>();
        for (Recipe recipe : db.getAllRecipes()) {
            if (IngredientMatcher.canMake(recipe, items)) {
                makeable.add(recipe);
            }
        }

        if (makeable.isEmpty()) {
            tvReadyBadge.setText("0 recipes");
            tvFeatureName.setText("Nothing to cook yet");
            tvFeatureInfo.setText("Add more ingredients to your pantry to unlock recipes.");
            btnFeature.setText("Go to pantry");
            btnFeature.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, MainActivity.class)));
        } else {
            final Recipe featured = makeable.get(0);
            tvReadyBadge.setText(makeable.size() + (makeable.size() == 1 ? " recipe" : " recipes"));
            tvFeatureName.setText(featured.getName());
            tvFeatureInfo.setText(featured.getIngredients().size()
                    + " ingredients \u2022 everything is already at home");
            btnFeature.setText("View recipe");
            btnFeature.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, RecipeDetailActivity.class);
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, featured.getId());
                startActivity(intent);
            });
        }
    }

    private String dueText(long days) {
        if (days < 0) return "Expired";
        if (days == 0) return "Use today";
        if (days == 1) return "Use tomorrow";
        return "In " + days + " days";
    }
}
