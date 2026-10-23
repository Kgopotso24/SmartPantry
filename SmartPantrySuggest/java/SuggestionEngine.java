package com.example.smartpantry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The brain of the Suggestions screen (pure Java, no Android code, so it is easy to test):
 *  - analyse(): for one recipe, what is missing and what could replace it?
 *  - search():  finds recipes for a typed request like "how do I make pizza".
 *
 * NOTE: the strict "Can make now" rule is untouched. A recipe is only called "ready" when
 * IngredientMatcher says every ingredient is covered. Swaps are shown separately and
 * labelled "With swaps".
 */
public class SuggestionEngine {

    /** One ingredient that is missing (or not enough) for a recipe. */
    public static class Gap {
        public final String ingredient;
        public final String haveText;                   // e.g. "1 pcs" if the pantry has some, else null
        public final Substitutions.Alt pantryAlt;       // a swap the user ALREADY has, or null
        public final List<Substitutions.Alt> options;   // all known swaps

        Gap(String ingredient, String haveText, Substitutions.Alt pantryAlt,
            List<Substitutions.Alt> options) {
            this.ingredient = ingredient;
            this.haveText = haveText;
            this.pantryAlt = pantryAlt;
            this.options = options;
        }
    }

    /** The result of analysing one recipe against the pantry. */
    public static class Plan {
        public final Recipe recipe;
        public final List<Gap> gaps;

        Plan(Recipe recipe, List<Gap> gaps) {
            this.recipe = recipe;
            this.gaps = gaps;
        }

        /** Every ingredient is covered (strict rule). */
        public boolean ready() {
            return gaps.isEmpty() && !recipe.getIngredients().isEmpty();
        }

        /** Something is missing, but every gap can be swapped for something already in the pantry. */
        public boolean readyWithSwaps() {
            if (gaps.isEmpty()) {
                return false;
            }
            for (Gap gap : gaps) {
                if (gap.pantryAlt == null) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Works out what is missing for a recipe and which swaps could fill each gap. */
    public static Plan analyse(Recipe recipe, List<PantryItem> pantry) {
        List<Gap> gaps = new ArrayList<>();
        for (String missingName : IngredientMatcher.missing(recipe, pantry)) {
            String key = IngredientMatcher.normalise(missingName);

            // Does the pantry have some of it (just not enough)?
            String haveText = null;
            for (PantryItem item : pantry) {
                if (IngredientMatcher.normalise(item.getName()).equals(key)) {
                    haveText = IngredientMatcher.formatQty(item.getQuantity()) + " " + item.getUnit();
                    break;
                }
            }

            // Is one of the known swaps already in the pantry?
            List<Substitutions.Alt> options = Substitutions.optionsFor(key);
            Substitutions.Alt inPantry = null;
            search:
            for (Substitutions.Alt alt : options) {
                String altKey = IngredientMatcher.normalise(alt.name);
                for (PantryItem item : pantry) {
                    if (IngredientMatcher.normalise(item.getName()).equals(altKey)) {
                        inPantry = alt;
                        break search;
                    }
                }
            }
            gaps.add(new Gap(missingName, haveText, inPantry, options));
        }
        return new Plan(recipe, gaps);
    }

    // ------------------------------------------------------------------
    // Searching for a requested meal
    // ------------------------------------------------------------------

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "how", "do", "does", "you", "we", "to", "the", "some", "can", "could", "me", "my",
            "make", "cook", "cooking", "making", "want", "wanna", "would", "like", "give", "show",
            "tell", "recipe", "recipes", "for", "with", "please", "what", "is", "of", "and", "dish",
            "meal", "food", "something", "need", "get", "prepare", "bake", "best", "way", "fix",
            "an", "am", "are", "it", "i"));

    /** Turns "How do I make Pizzas?" into ["pizza"]. */
    public static List<String> terms(String query) {
        List<String> terms = new ArrayList<>();
        for (String word : query.toLowerCase().split("[^a-z]+")) {
            if (word.length() > 1 && !STOP_WORDS.contains(word)) {
                terms.add(IngredientMatcher.normalise(word));
            }
        }
        return terms;
    }

    /**
     * Recipes matching the request. Recipes whose NAME contains every search word come first
     * ("pizza" -> Pizza). If none match by name, recipes that use the word as an ingredient
     * are returned instead ("chicken" -> every chicken recipe).
     */
    public static List<Recipe> search(String query, List<Recipe> all) {
        List<String> terms = terms(query);
        List<Recipe> byName = new ArrayList<>();
        List<Recipe> byIngredient = new ArrayList<>();
        if (terms.isEmpty()) {
            return byName;
        }

        for (Recipe recipe : all) {
            Set<String> nameWords = new HashSet<>(Arrays.asList(
                    IngredientMatcher.normalise(recipe.getName()).split(" ")));
            Set<String> ingredientWords = new HashSet<>();
            for (RecipeIngredient ing : recipe.getIngredients()) {
                ingredientWords.addAll(Arrays.asList(
                        IngredientMatcher.normalise(ing.getName()).split(" ")));
            }

            boolean allInName = true;
            boolean allAnywhere = true;
            for (String term : terms) {
                boolean inName = nameWords.contains(term);
                boolean inIngredients = ingredientWords.contains(term);
                if (!inName) allInName = false;
                if (!inName && !inIngredients) allAnywhere = false;
            }
            if (allInName) {
                byName.add(recipe);
            } else if (allAnywhere) {
                byIngredient.add(recipe);
            }
        }
        return byName.isEmpty() ? byIngredient : byName;
    }
}
