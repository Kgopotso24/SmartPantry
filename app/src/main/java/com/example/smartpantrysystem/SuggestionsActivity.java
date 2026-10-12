package com.example.smartpantrysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Suggestions screen:
 *  1. "Cook with what you have": recipes the pantry fully covers (strict rule), plus an
 *     "Almost there" list of recipes only one or two ingredients short.
 *  2. "What do you want to cook?": the user types a meal (e.g. "pizza"); we find the recipe,
 *     show what they are missing and suggest swaps (preferring ones already in the pantry).
 */
public class SuggestionsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private LayoutInflater inflater;
    private EditText etRequest;
    private LinearLayout llResults;
    private View tvClear;
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);

        db = new DatabaseHelper(this);
        inflater = LayoutInflater.from(this);
        NavHelper.applyHeaderInsets(findViewById(R.id.header));

        etRequest = findViewById(R.id.etRequest);
        llResults = findViewById(R.id.llResults);
        tvClear = findViewById(R.id.tvClear);

        findViewById(R.id.btnAsk).setOnClickListener(v -> ask());
        etRequest.setOnEditorActionListener((v, actionId, event) -> {
            ask();
            return true;
        });
        tvClear.setOnClickListener(v -> {
            etRequest.setText("");
            query = "";
            render();
        });
        findViewById(R.id.tvBrowse).setOnClickListener(v ->
                startActivity(new Intent(SuggestionsActivity.this, SuggestedActivity.class)));
        findViewById(R.id.tvNewRecipe).setOnClickListener(v ->
                startActivity(new Intent(SuggestionsActivity.this, AddRecipeActivity.class)));

        NavHelper.setupBottomNav(this, R.id.nav_recipes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render(); // pantry or recipes may have changed since we were last here
    }

    private void ask() {
        String typed = etRequest.getText().toString().trim();
        if (typed.isEmpty()) {
            etRequest.setError("Type a meal, e.g. pizza");
            return;
        }
        query = typed;

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etRequest.getWindowToken(), 0);
        }
        render();
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void render() {
        llResults.removeAllViews();
        tvClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

        List<PantryItem> pantry = db.getAllItems();
        List<Recipe> all = db.getAllRecipes();

        if (query.isEmpty()) {
            renderDefault(pantry, all);
        } else {
            renderRequest(pantry, all);
        }
    }

    /** Default view: what can I cook right now? */
    private void renderDefault(List<PantryItem> pantry, List<Recipe> all) {
        List<SuggestionEngine.Plan> ready = new ArrayList<>();
        List<SuggestionEngine.Plan> almost = new ArrayList<>();
        for (Recipe recipe : all) {
            SuggestionEngine.Plan plan = SuggestionEngine.analyse(recipe, pantry);
            if (plan.ready()) {
                ready.add(plan);                       // strict: nothing missing
            } else if (plan.gaps.size() <= 2) {
                almost.add(plan);                      // close: only 1-2 ingredients short
            }
        }
        sortByCloseness(almost);

        if (pantry.isEmpty()) {
            addNote("Your pantry is empty. Add ingredients, or load the sample pantry in Settings, "
                    + "to get suggestions.");
        }

        addSection("COOK WITH WHAT YOU HAVE", ready.size() + " ready", true);
        if (ready.isEmpty()) {
            addNote("Nothing is fully covered yet. Add more ingredients, or ask for a meal above.");
        }
        for (int i = 0; i < ready.size() && i < 4; i++) {
            addPlanCard(ready.get(i));
        }
        if (ready.size() > 4) {
            addNote("+" + (ready.size() - 4) + " more - tap Browse all recipes.");
        }

        if (!almost.isEmpty()) {
            addSection("ALMOST THERE", almost.size() + " close", false);
            for (int i = 0; i < almost.size() && i < 3; i++) {
                addPlanCard(almost.get(i));
            }
        }
    }

    /** Request view: the user asked for a specific meal. */
    private void renderRequest(List<PantryItem> pantry, List<Recipe> all) {
        List<Recipe> found = SuggestionEngine.search(query, all);

        if (found.isEmpty()) {
            addSection("RESULTS FOR \"" + query.toUpperCase() + "\"", "no match", false);
            addNote("There is no recipe for \"" + query + "\" in the app yet. You can write it "
                    + "yourself and the app will check it against your pantry.");

            TextView create = new TextView(this);
            create.setText("+ Create this recipe");
            create.setTextColor(0xFFFFFFFF);
            create.setTextSize(16);
            create.setTypeface(null, android.graphics.Typeface.BOLD);
            create.setGravity(android.view.Gravity.CENTER);
            create.setBackgroundResource(R.drawable.pm_btn_gradient);
            int pad = dp(14);
            create.setPadding(pad, pad, pad, pad);
            create.setOnClickListener(v ->
                    startActivity(new Intent(SuggestionsActivity.this, AddRecipeActivity.class)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(16);
            llResults.addView(create, lp);

            // ...and still offer something they can cook right now
            List<SuggestionEngine.Plan> ready = new ArrayList<>();
            for (Recipe recipe : all) {
                SuggestionEngine.Plan plan = SuggestionEngine.analyse(recipe, pantry);
                if (plan.ready()) {
                    ready.add(plan);
                }
            }
            if (!ready.isEmpty()) {
                addSection("INSTEAD, YOU COULD COOK", ready.size() + " ready", true);
                for (int i = 0; i < ready.size() && i < 3; i++) {
                    addPlanCard(ready.get(i));
                }
            }
            return;
        }

        List<SuggestionEngine.Plan> plans = new ArrayList<>();
        for (Recipe recipe : found) {
            plans.add(SuggestionEngine.analyse(recipe, pantry));
        }
        sortByCloseness(plans);

        addSection("RESULTS FOR \"" + query.toUpperCase() + "\"", plans.size() + " found", true);
        for (SuggestionEngine.Plan plan : plans) {
            addPlanCard(plan);
        }
    }

    /** Ready first, then "with swaps", then the fewest missing ingredients. */
    private void sortByCloseness(List<SuggestionEngine.Plan> plans) {
        Collections.sort(plans, (a, b) -> {
            int rankA = a.ready() ? 0 : (a.readyWithSwaps() ? 1 : 2);
            int rankB = b.ready() ? 0 : (b.readyWithSwaps() ? 1 : 2);
            if (rankA != rankB) return rankA - rankB;
            if (a.gaps.size() != b.gaps.size()) return a.gaps.size() - b.gaps.size();
            return a.recipe.getName().compareToIgnoreCase(b.recipe.getName());
        });
    }

    // ------------------------------------------------------------------
    // View builders
    // ------------------------------------------------------------------

    private void addSection(String title, String badge, boolean green) {
        View row = inflater.inflate(R.layout.item_section, llResults, false);
        ((TextView) row.findViewById(R.id.tvSectionTitle)).setText(title);
        TextView tvBadge = row.findViewById(R.id.tvSectionBadge);
        tvBadge.setText(badge);
        tvBadge.setBackgroundResource(green ? R.drawable.pm_chip_mint : R.drawable.pm_chip_amber);
        tvBadge.setTextColor(ContextCompat.getColor(this, green ? R.color.pm_green : R.color.pm_amber));
        llResults.addView(row);
    }

    private void addNote(String text) {
        TextView note = new TextView(this);
        note.setText(text);
        note.setTextColor(ContextCompat.getColor(this, R.color.pm_grey));
        note.setTextSize(15);
        note.setLineSpacing(0, 1.2f);
        note.setPadding(dp(4), 0, dp(4), dp(14));
        llResults.addView(note);
    }

    /** One card per recipe: status, what is missing, and swap ideas. */
    private void addPlanCard(final SuggestionEngine.Plan plan) {
        View card = inflater.inflate(R.layout.item_suggestion, llResults, false);
        TextView tvName = card.findViewById(R.id.tvSugName);
        TextView tvBadge = card.findViewById(R.id.tvSugBadge);
        TextView tvStatus = card.findViewById(R.id.tvSugStatus);
        TextView tvDetails = card.findViewById(R.id.tvSugDetails);

        int green = ContextCompat.getColor(this, R.color.pm_green);
        int amber = ContextCompat.getColor(this, R.color.pm_amber);

        tvName.setText(plan.recipe.getName());

        if (plan.ready()) {
            tvBadge.setText("Ready");
            tvBadge.setBackgroundResource(R.drawable.pm_chip_mint);
            tvBadge.setTextColor(green);
            tvStatus.setText("You have everything - ready to cook!");
            tvStatus.setTextColor(green);
            tvDetails.setVisibility(View.GONE);
        } else {
            boolean swaps = plan.readyWithSwaps();
            tvBadge.setText(swaps ? "With swaps" : "Missing " + plan.gaps.size());
            tvBadge.setBackgroundResource(swaps ? R.drawable.pm_chip_mint : R.drawable.pm_chip_amber);
            tvBadge.setTextColor(swaps ? green : amber);
            tvStatus.setText(swaps
                    ? "You can make this by swapping what you have."
                    : "Missing " + plan.gaps.size() + (plan.gaps.size() == 1 ? " ingredient" : " ingredients"));
            tvStatus.setTextColor(swaps ? green : amber);
            tvDetails.setText(describeGaps(plan));
            tvDetails.setVisibility(View.VISIBLE);
        }

        card.findViewById(R.id.btnSugOpen).setOnClickListener(v -> {
            // Intent carrying the recipe id to the detail screen
            Intent intent = new Intent(SuggestionsActivity.this, RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, plan.recipe.getId());
            startActivity(intent);
        });
        llResults.addView(card);
    }

    /** Turns each gap into readable lines: what is missing and what could replace it. */
    private String describeGaps(SuggestionEngine.Plan plan) {
        StringBuilder sb = new StringBuilder();
        for (SuggestionEngine.Gap gap : plan.gaps) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("\u2022 ").append(gap.ingredient);
            sb.append(gap.haveText != null ? " - not enough (you have " + gap.haveText + ")" : " - missing");

            if (gap.pantryAlt != null) {
                sb.append("\n   \u2714 You have ").append(gap.pantryAlt.name).append(": ")
                        .append(gap.pantryAlt.note);
            } else if (!gap.options.isEmpty()) {
                sb.append("\n   Swap idea: ");
                for (int i = 0; i < gap.options.size() && i < 2; i++) {
                    if (i > 0) sb.append("  or  ");
                    sb.append(gap.options.get(i).name).append(" (").append(gap.options.get(i).note).append(")");
                }
            } else {
                sb.append("\n   No swap known - pick it up from a shop.");
            }
        }
        return sb.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
