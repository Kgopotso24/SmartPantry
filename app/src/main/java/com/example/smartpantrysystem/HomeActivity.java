package com.example.smartpantrysystem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Home dashboard: stat tiles, a pantry-freshness bar, quick actions, tonight's pick
 * (favouring recipes that use expiring food) and an "almost there" list of recipes
 * that are exactly one ingredient short.
 */
public class HomeActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextView tvDate, tvGreeting;
    private TextView tvStatIngredients, tvStatSoon, tvStatReady;
    private TextView tvFreshLegend, tvSoonList;
    private TextView tvPickBadge, tvPickName, tvPickInfo, tvPickIngredients, btnPick;
    private TextView tvAlmostBadge, tvAlmostList;
    private View vFresh, vSoon, vExpired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = new DatabaseHelper(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));

        tvDate = findViewById(R.id.tvDate);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvStatIngredients = findViewById(R.id.tvStatIngredients);
        tvStatSoon = findViewById(R.id.tvStatSoon);
        tvStatReady = findViewById(R.id.tvStatReady);
        vFresh = findViewById(R.id.vFresh);
        vSoon = findViewById(R.id.vSoon);
        vExpired = findViewById(R.id.vExpired);
        tvFreshLegend = findViewById(R.id.tvFreshLegend);
        tvSoonList = findViewById(R.id.tvSoonList);
        tvPickBadge = findViewById(R.id.tvPickBadge);
        tvPickName = findViewById(R.id.tvPickName);
        tvPickInfo = findViewById(R.id.tvPickInfo);
        tvPickIngredients = findViewById(R.id.tvPickIngredients);
        btnPick = findViewById(R.id.btnPick);
        tvAlmostBadge = findViewById(R.id.tvAlmostBadge);
        tvAlmostList = findViewById(R.id.tvAlmostList);

        // Intents to the other screens
        findViewById(R.id.tileIngredients).setOnClickListener(v -> open(MainActivity.class));
        findViewById(R.id.tileSoon).setOnClickListener(v -> open(MainActivity.class));
        findViewById(R.id.tileReady).setOnClickListener(v -> open(SuggestionsActivity.class));
        findViewById(R.id.btnQuickIngredient).setOnClickListener(v -> open(AddEditActivity.class));
        findViewById(R.id.btnQuickRecipe).setOnClickListener(v -> open(AddRecipeActivity.class));
        findViewById(R.id.cardAlmost).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SuggestedActivity.class);
            intent.putExtra(SuggestedActivity.EXTRA_SHOW_ALL, true); // open the "All recipes" view
            startActivity(intent);
        });

        NavHelper.setupBottomNav(this, R.id.nav_home);
    }

    private void open(Class<?> screen) {
        startActivity(new Intent(HomeActivity.this, screen));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        // ---------- greeting ----------
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        String name = prefs.getString(SettingsActivity.KEY_NAME, "").trim();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good morning" : (hour < 18 ? "Good afternoon" : "Good evening");
        tvGreeting.setText(name.isEmpty() ? greeting : greeting + ", " + name);
        tvDate.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                .format(new Date()).toUpperCase(Locale.getDefault()));

        // ---------- pantry freshness ----------
        List<PantryItem> items = db.getAllItems();
        List<PantryItem> useSoon = new ArrayList<>();
        int fresh = 0, soon = 0, expired = 0;
        for (PantryItem item : items) {
            Long days = item.daysUntilExpiry();
            if (days == null || days > 3) {
                fresh++;                       // no date, or comfortably in date
            } else if (days < 0) {
                expired++;
                useSoon.add(item);
            } else {
                soon++;
                useSoon.add(item);
            }
        }
        Collections.sort(useSoon, (a, b) -> Long.compare(a.daysUntilExpiry(), b.daysUntilExpiry()));

        setWeight(vFresh, fresh);
        setWeight(vSoon, soon);
        setWeight(vExpired, expired);

        tvStatIngredients.setText(String.valueOf(items.size()));
        tvStatSoon.setText(String.valueOf(soon + expired));

        if (items.isEmpty()) {
            tvFreshLegend.setText("Add ingredients to see how fresh your pantry is");
        } else {
            tvFreshLegend.setText(fresh + " fresh  \u2022  " + soon + " expiring  \u2022  " + expired + " expired");
        }

        if (useSoon.isEmpty()) {
            tvSoonList.setText("Nothing is about to expire. Nice work!");
        } else {
            StringBuilder lines = new StringBuilder();
            for (int i = 0; i < useSoon.size() && i < 3; i++) {
                PantryItem item = useSoon.get(i);
                lines.append(item.getName()).append(" (")
                        .append(IngredientMatcher.formatQty(item.getQuantity())).append(" ")
                        .append(item.getUnit()).append(") \u2022 ")
                        .append(dueText(item.daysUntilExpiry())).append("\n");
            }
            if (useSoon.size() > 3) {
                lines.append("+").append(useSoon.size() - 3).append(" more");
            }
            tvSoonList.setText(lines.toString().trim());
        }

        // ---------- recipes: strict matches and near-misses ----------
        List<Recipe> makeable = new ArrayList<>();
        List<String> almostLines = new ArrayList<>();
        int almostCount = 0;
        for (Recipe recipe : db.getAllRecipes()) {
            List<String> gaps = IngredientMatcher.missing(recipe, items);
            if (IngredientMatcher.canMake(recipe, items)) {
                makeable.add(recipe);
            } else if (gaps.size() == 1) {
                almostCount++;
                if (almostLines.size() < 3) {
                    almostLines.add(recipe.getName() + " \u2022 missing " + gaps.get(0));
                }
            }
        }
        tvStatReady.setText(String.valueOf(makeable.size()));

        // "Almost there" card
        if (almostCount == 0) {
            tvAlmostBadge.setVisibility(View.GONE);
            tvAlmostList.setText("No near-misses right now.");
        } else {
            tvAlmostBadge.setVisibility(View.VISIBLE);
            tvAlmostBadge.setText(almostCount + " nearly there");
            StringBuilder sb = new StringBuilder();
            for (String line : almostLines) {
                sb.append(line).append("\n");
            }
            if (almostCount > 3) {
                sb.append("+").append(almostCount - 3).append(" more");
            }
            tvAlmostList.setText(sb.toString().trim());
        }

        // ---------- tonight's pick: prefer a recipe that uses food about to expire ----------
        Set<String> expiring = new HashSet<>();
        for (PantryItem item : useSoon) {
            expiring.add(IngredientMatcher.normalise(item.getName()));
        }
        Recipe pick = null;
        String expiringUsed = null;
        search:
        for (Recipe recipe : makeable) {
            for (RecipeIngredient ing : recipe.getIngredients()) {
                if (expiring.contains(IngredientMatcher.normalise(ing.getName()))) {
                    pick = recipe;
                    expiringUsed = ing.getName();
                    break search;
                }
            }
        }
        if (pick == null && !makeable.isEmpty()) {
            pick = makeable.get(0);
        }

        if (pick == null) {
            tvPickBadge.setText("0 ready");
            tvPickName.setText("Nothing to cook yet");
            tvPickInfo.setText("Add ingredients, or write your own recipe.");
            tvPickIngredients.setVisibility(View.GONE);
            btnPick.setText("Go to pantry");
            btnPick.setOnClickListener(v -> open(MainActivity.class));
        } else {
            final Recipe chosen = pick;
            tvPickBadge.setText(makeable.size() + " ready");
            tvPickName.setText(chosen.getName());
            tvPickInfo.setText(chosen.getIngredients().size() + " ingredients  \u2022  "
                    + (expiringUsed != null ? "uses " + expiringUsed + " before it expires"
                    : "everything is at home"));

            StringBuilder names = new StringBuilder();
            for (int i = 0; i < chosen.getIngredients().size() && i < 5; i++) {
                if (i > 0) names.append(", ");
                names.append(chosen.getIngredients().get(i).getName());
            }
            tvPickIngredients.setText(names.toString());
            tvPickIngredients.setVisibility(View.VISIBLE);

            btnPick.setText("Open recipe");
            btnPick.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, RecipeDetailActivity.class);
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, chosen.getId());
                startActivity(intent);
            });
        }
    }

    /** Sets the share of the freshness bar a coloured segment takes up. */
    private void setWeight(View segment, float weight) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) segment.getLayoutParams();
        lp.weight = weight;
        segment.setLayoutParams(lp);
    }

    private String dueText(long days) {
        if (days < 0) return "Expired";
        if (days == 0) return "Use today";
        if (days == 1) return "Use tomorrow";
        return "In " + days + " days";
    }
}
