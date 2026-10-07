package com.example.smartpantrysystem;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The core business logic of the app: the STRICT-MATCHING RULE.
 *
 * A recipe may only be suggested if EVERY ingredient it needs is in the pantry
 * in at least the required quantity. One missing or too-small ingredient = not suggested.
 *
 * To cope with real-world messiness it:
 *  1. normalises names (lower case, trimmed, plural -> singular: "Tomatoes" == "tomato")
 *  2. converts units to a base unit (kg -> g, l -> ml, cup/tbsp/tsp -> ml)
 *  3. adds up the same ingredient if it appears in several pantry rows
 */
public class IngredientMatcher {

    // The three kinds of measurement we can compare with each other
    private static final int WEIGHT = 0; // base unit: grams
    private static final int VOLUME = 1; // base unit: millilitres
    private static final int COUNT = 2;  // base unit: pieces

    /** Returns true only if the pantry covers every ingredient of the recipe. */
    public static boolean canMake(Recipe recipe, List<PantryItem> pantry) {
        if (recipe.getIngredients().isEmpty()) {
            return false;
        }

        // Step 1: total up what we have, per normalised ingredient name
        Map<String, double[]> stock = new HashMap<>();
        for (PantryItem item : pantry) {
            String key = normalise(item.getName());
            double[] totals = stock.get(key);
            if (totals == null) {
                totals = new double[3];
                stock.put(key, totals);
            }
            totals[category(item.getUnit())] += toBase(item.getQuantity(), item.getUnit());
        }

        // Step 2: every recipe ingredient must be covered
        for (RecipeIngredient needed : recipe.getIngredients()) {
            double[] have = stock.get(normalise(needed.getName()));
            if (have == null) {
                return false; // ingredient not in the pantry at all
            }

            int cat = category(needed.getUnit());
            double required = toBase(needed.getQuantity(), needed.getUnit());

            if (have[cat] > 0) {
                if (have[cat] + 0.0001 < required) {
                    return false; // we have it, but not enough
                }
            } else if (have[WEIGHT] + have[VOLUME] + have[COUNT] <= 0) {
                return false;
            }
            // else: the pantry has this ingredient but measured in a different kind of
            // unit (e.g. salt in grams, recipe in teaspoons). Grams and millilitres cannot
            // be converted without density data, so we accept it as "present".
        }
        return true;
    }

    /** Lower-cases, trims and turns plural words into singular ("Tomatoes " -> "tomato"). */
    public static String normalise(String raw) {
        if (raw == null) {
            return "";
        }
        String[] words = raw.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(singular(word));
        }
        return result.toString();
    }

    private static String singular(String w) {
        if (w.length() <= 3) {
            return w;
        }
        if (w.length() > 4 && w.endsWith("ies")) {
            return w.substring(0, w.length() - 3) + "y";          // berries -> berry
        }
        if (w.endsWith("oes") || w.endsWith("ches") || w.endsWith("shes")
                || w.endsWith("xes") || w.endsWith("sses")) {
            return w.substring(0, w.length() - 2);                // tomatoes -> tomato
        }
        if (w.endsWith("s") && !w.endsWith("ss") && !w.endsWith("us")) {
            return w.substring(0, w.length() - 1);                // eggs -> egg
        }
        return w;
    }

    private static int category(String unit) {
        switch (unit.trim().toLowerCase(Locale.ROOT)) {
            case "g":
            case "kg":
                return WEIGHT;
            case "ml":
            case "l":
            case "cup":
            case "tbsp":
            case "tsp":
                return VOLUME;
            default:
                return COUNT; // "pcs"
        }
    }

    private static double toBase(double quantity, String unit) {
        switch (unit.trim().toLowerCase(Locale.ROOT)) {
            case "kg":   return quantity * 1000;
            case "l":    return quantity * 1000;
            case "cup":  return quantity * 250;
            case "tbsp": return quantity * 15;
            case "tsp":  return quantity * 5;
            default:     return quantity; // g, ml, pcs
        }
    }

    /** 2.0 -> "2", 2.5 -> "2.5" (nicer to read on screen). */
    public static String formatQty(double quantity) {
        if (quantity == Math.floor(quantity)) {
            return String.valueOf((long) quantity);
        }
        return String.valueOf(quantity);
    }
}
