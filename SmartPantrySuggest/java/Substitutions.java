package com.example.smartpantry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in table of ingredient swaps ("no yeast? use baking powder").
 * Keys are NORMALISED ingredient names (lower case, singular) - see IngredientMatcher.normalise().
 * Works fully offline, so the suggestion feature needs no internet connection.
 */
public class Substitutions {

    /** One possible swap: the replacement ingredient and a short how-to note. */
    public static class Alt {
        public final String name;
        public final String note;

        public Alt(String name, String note) {
            this.name = name;
            this.note = note;
        }
    }

    private static final Map<String, List<Alt>> TABLE = new HashMap<>();

    /** Each swap is written as "replacement|note". */
    private static void add(String ingredient, String... swaps) {
        List<Alt> list = new ArrayList<>();
        for (String swap : swaps) {
            String[] parts = swap.split("\\|", 2);
            list.add(new Alt(parts[0], parts.length > 1 ? parts[1] : ""));
        }
        TABLE.put(ingredient, list);
    }

    static {
        add("yeast", "baking powder|quick dough: 2 tsp baking powder + 3 tbsp yoghurt, no rising needed",
                "self-raising flour|use it instead of plain flour and skip the yeast");
        add("flour", "self-raising flour|use the same amount", "wholewheat flour|use the same amount");
        add("tomato", "tomato paste|1 tbsp paste + a splash of water per tomato",
                "tinned tomatoes|about half a tin per 3 tomatoes",
                "tomato sauce|about 3 tbsp per tomato");
        add("cheese", "mozzarella|melts beautifully", "cheddar|stronger flavour, use a little less",
                "feta|crumble it and go easy on the salt");
        add("milk", "cream|mix half and half with water", "plant milk|oat, soy or almond, same amount",
                "water|use water and add a little extra butter");
        add("butter", "oil|use the same amount", "margarine|use the same amount");
        add("oil", "butter|melt it, same amount", "margarine|melt it, same amount");
        add("egg", "banana|half a mashed banana per egg (sweet dishes)", "yoghurt|1/4 cup per egg");
        add("sugar", "honey|use about 3/4 of the amount", "syrup|use about 3/4 of the amount");
        add("onion", "spring onion|use 3-4 per onion", "onion powder|1 tsp per onion",
                "leek|use about half a leek");
        add("garlic", "garlic powder|1/4 tsp per clove", "onion|adds flavour without the garlic taste");
        add("soy sauce", "worcestershire sauce|use the same amount",
                "salt|a pinch of salt with a splash of water");
        add("chicken", "beef|use the same weight", "tuna|use a tin and add it at the end",
                "chickpea|vegetarian swap, same weight");
        add("beef mince", "chicken mince|use the same weight",
                "lentil|vegetarian swap, use about 3/4 of the weight", "mushroom|chop finely, same weight");
        add("pasta", "macaroni|use the same weight", "noodle|use the same weight",
                "rice|serve the sauce over rice");
        add("rice", "pasta|use the same weight", "couscous|soak in hot water, same weight");
        add("bread", "tortilla|one per slice", "pita|one per two slices");
        add("bun", "bread|two slices per bun", "pita|one per bun");
        add("tortilla", "bread|toast it and fold", "pita|warm it first");
        add("potato", "sweet potato|use the same weight", "pumpkin|use the same weight");
        add("carrot", "sweet potato|use the same weight", "pumpkin|use the same weight");
        add("lentil", "chickpea|use the same weight", "bean|use the same weight");
        add("tuna", "chicken|cooked and shredded", "chickpea|drain and mash lightly");
        add("curry powder", "cumin|1 tsp cumin + 1 tsp turmeric + a pinch of chilli per 2 tbsp",
                "turmeric|combine with cumin and chilli");
        add("banana", "apple|mash or grate, same amount");
        add("salt", "soy sauce|salty, so use a splash instead");
    }

    /** Swap options for a normalised ingredient name (empty list if none are known). */
    public static List<Alt> optionsFor(String normalisedIngredient) {
        List<Alt> options = TABLE.get(normalisedIngredient);
        return options == null ? new ArrayList<Alt>() : options;
    }
}
