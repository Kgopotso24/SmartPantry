package com.example.smartpantrysystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local on-device SQLite database.
 * Tables: pantry (user data), recipes + recipe_ingredients (seeded on first run).
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "smart_pantry.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pantry ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "quantity REAL NOT NULL, "
                + "unit TEXT NOT NULL, "
                + "expiry TEXT)");

        db.execSQL("CREATE TABLE recipes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "steps TEXT NOT NULL)");

        db.execSQL("CREATE TABLE recipe_ingredients ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "recipe_id INTEGER NOT NULL, "
                + "name TEXT NOT NULL, "
                + "quantity REAL NOT NULL, "
                + "unit TEXT NOT NULL, "
                + "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE)");

        seedRecipes(db); // pre-load the recipe collection on first run
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS recipe_ingredients");
        db.execSQL("DROP TABLE IF EXISTS recipes");
        db.execSQL("DROP TABLE IF EXISTS pantry");
        onCreate(db);
    }

    // ------------------------------------------------------------------
    // PANTRY: Create / Read / Update / Delete
    // ------------------------------------------------------------------

    /** CREATE */
    public long addItem(String name, double quantity, String unit, String expiry) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("expiry", expiry);
        return getWritableDatabase().insert("pantry", null, values);
    }

    /** UPDATE */
    public int updateItem(int id, String name, double quantity, String unit, String expiry) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("quantity", quantity);
        values.put("unit", unit);
        values.put("expiry", expiry);
        return getWritableDatabase().update("pantry", values, "id = ?",
                new String[]{String.valueOf(id)});
    }

    /** DELETE */
    public int deleteItem(int id) {
        return getWritableDatabase().delete("pantry", "id = ?",
                new String[]{String.valueOf(id)});
    }

    /** READ (all items, alphabetical) */
    public List<PantryItem> getAllItems() {
        List<PantryItem> items = new ArrayList<>();
        Cursor c = getReadableDatabase().query("pantry", null, null, null, null, null,
                "name COLLATE NOCASE ASC");
        while (c.moveToNext()) {
            items.add(readItem(c));
        }
        c.close();
        return items;
    }

    /** READ (one item) */
    public PantryItem getItem(int id) {
        Cursor c = getReadableDatabase().query("pantry", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        PantryItem item = null;
        if (c.moveToFirst()) {
            item = readItem(c);
        }
        c.close();
        return item;
    }

    private PantryItem readItem(Cursor c) {
        int expiryCol = c.getColumnIndexOrThrow("expiry");
        return new PantryItem(
                c.getInt(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getString(c.getColumnIndexOrThrow("unit")),
                c.isNull(expiryCol) ? null : c.getString(expiryCol));
    }

    // ------------------------------------------------------------------
    // RECIPES (read only - the collection is seeded)
    // ------------------------------------------------------------------

    /** All recipes, each with its ingredient list filled in. */
    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        Map<Integer, Recipe> recipes = new LinkedHashMap<>();

        Cursor rc = db.query("recipes", null, null, null, null, null, "name ASC");
        while (rc.moveToNext()) {
            int id = rc.getInt(rc.getColumnIndexOrThrow("id"));
            recipes.put(id, new Recipe(id,
                    rc.getString(rc.getColumnIndexOrThrow("name")),
                    rc.getString(rc.getColumnIndexOrThrow("steps"))));
        }
        rc.close();

        Cursor ic = db.query("recipe_ingredients", null, null, null, null, null, "id ASC");
        while (ic.moveToNext()) {
            Recipe recipe = recipes.get(ic.getInt(ic.getColumnIndexOrThrow("recipe_id")));
            if (recipe != null) {
                recipe.addIngredient(readIngredient(ic));
            }
        }
        ic.close();

        return new ArrayList<>(recipes.values());
    }

    /** One recipe by id (null if not found). */
    public Recipe getRecipe(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Recipe recipe = null;

        Cursor rc = db.query("recipes", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        if (rc.moveToFirst()) {
            recipe = new Recipe(id,
                    rc.getString(rc.getColumnIndexOrThrow("name")),
                    rc.getString(rc.getColumnIndexOrThrow("steps")));
        }
        rc.close();

        if (recipe != null) {
            Cursor ic = db.query("recipe_ingredients", null, "recipe_id = ?",
                    new String[]{String.valueOf(id)}, null, null, "id ASC");
            while (ic.moveToNext()) {
                recipe.addIngredient(readIngredient(ic));
            }
            ic.close();
        }
        return recipe;
    }

    /** Every distinct ingredient name used by the recipes (used for auto-complete hints). */
    public List<String> getIngredientNames() {
        List<String> names = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT name FROM recipe_ingredients ORDER BY name ASC", null);
        while (c.moveToNext()) {
            names.add(c.getString(0));
        }
        c.close();
        return names;
    }

    private RecipeIngredient readIngredient(Cursor c) {
        return new RecipeIngredient(
                c.getString(c.getColumnIndexOrThrow("name")),
                c.getDouble(c.getColumnIndexOrThrow("quantity")),
                c.getString(c.getColumnIndexOrThrow("unit")));
    }

    // ------------------------------------------------------------------
    // SEED DATA: 18 recipes. Ingredient format: "name|quantity|unit"
    // ------------------------------------------------------------------

    private void seedRecipes(SQLiteDatabase db) {
        addRecipe(db, "Scrambled Eggs",
                "1. Crack the eggs into a bowl, add the salt and whisk.\n"
                        + "2. Melt the butter in a pan over medium heat.\n"
                        + "3. Pour in the eggs and stir gently until just set.\n"
                        + "4. Serve straight away.",
                "egg|3|pcs", "butter|10|g", "salt|1|tsp");

        addRecipe(db, "Cheese Omelette",
                "1. Whisk the eggs with the salt.\n"
                        + "2. Melt the butter in a pan and pour in the eggs.\n"
                        + "3. When almost set, sprinkle the cheese on one half.\n"
                        + "4. Fold the omelette over and cook for 1 more minute.",
                "egg|3|pcs", "cheese|50|g", "butter|10|g", "salt|1|tsp");

        addRecipe(db, "Vegetable Omelette",
                "1. Chop the onion and tomato finely.\n"
                        + "2. Fry them in the oil for 3 minutes.\n"
                        + "3. Pour in the whisked eggs and salt.\n"
                        + "4. Cook until set, fold and serve.",
                "egg|3|pcs", "onion|1|pcs", "tomato|1|pcs", "oil|1|tbsp", "salt|1|tsp");

        addRecipe(db, "Tomato Pasta",
                "1. Boil the pasta in salted water until soft, then drain.\n"
                        + "2. Fry the chopped onion and garlic in the oil.\n"
                        + "3. Add the chopped tomatoes and simmer for 10 minutes.\n"
                        + "4. Mix the sauce through the pasta and serve.",
                "pasta|200|g", "tomato|3|pcs", "onion|1|pcs", "garlic|2|pcs", "oil|2|tbsp", "salt|1|tsp");

        addRecipe(db, "Tuna Pasta",
                "1. Boil the pasta in salted water and drain.\n"
                        + "2. Fry the chopped onion in the oil until soft.\n"
                        + "3. Add the tuna and stir for 2 minutes.\n"
                        + "4. Toss with the pasta and serve.",
                "pasta|200|g", "tuna|1|pcs", "onion|1|pcs", "oil|1|tbsp", "salt|1|tsp");

        addRecipe(db, "Garlic Butter Rice",
                "1. Cook the rice in salted water and drain.\n"
                        + "2. Melt the butter in a pan and fry the crushed garlic for 1 minute.\n"
                        + "3. Stir the rice into the garlic butter.\n"
                        + "4. Serve hot.",
                "rice|200|g", "butter|20|g", "garlic|2|pcs", "salt|1|tsp");

        addRecipe(db, "Egg Fried Rice",
                "1. Cook the rice and let it cool.\n"
                        + "2. Fry the chopped onion in the oil.\n"
                        + "3. Push the onion aside, scramble the eggs in the pan.\n"
                        + "4. Add the rice and soy sauce, stir-fry for 3 minutes.",
                "rice|250|g", "egg|2|pcs", "onion|1|pcs", "soy sauce|2|tbsp", "oil|1|tbsp");

        addRecipe(db, "Pancakes",
                "1. Mix the flour, sugar, egg and milk into a smooth batter.\n"
                        + "2. Melt a little butter in a pan.\n"
                        + "3. Pour in a ladle of batter and cook until bubbles appear.\n"
                        + "4. Flip, cook 1 more minute and repeat.",
                "flour|150|g", "milk|250|ml", "egg|1|pcs", "sugar|2|tbsp", "butter|20|g");

        addRecipe(db, "French Toast",
                "1. Whisk the eggs, milk and sugar in a shallow dish.\n"
                        + "2. Dip each slice of bread into the mixture.\n"
                        + "3. Fry in the butter until golden on both sides.\n"
                        + "4. Serve warm.",
                "bread|4|pcs", "egg|2|pcs", "milk|100|ml", "sugar|1|tbsp", "butter|10|g");

        addRecipe(db, "Cheese Toastie",
                "1. Butter one side of each slice of bread.\n"
                        + "2. Place the cheese between the unbuttered sides.\n"
                        + "3. Fry in a pan until golden and the cheese melts.\n"
                        + "4. Cut in half and serve.",
                "bread|2|pcs", "cheese|60|g", "butter|10|g");

        addRecipe(db, "Potato Soup",
                "1. Peel and dice the potatoes and onion.\n"
                        + "2. Fry the onion in the butter, add the potatoes and 500 ml water.\n"
                        + "3. Simmer for 20 minutes until soft.\n"
                        + "4. Mash roughly, stir in the milk and salt, and serve.",
                "potato|4|pcs", "onion|1|pcs", "butter|20|g", "milk|200|ml", "salt|1|tsp");

        addRecipe(db, "Mashed Potatoes",
                "1. Peel and chop the potatoes.\n"
                        + "2. Boil in salted water for 15 minutes and drain.\n"
                        + "3. Mash with the butter and warm milk.\n"
                        + "4. Season with salt and serve.",
                "potato|4|pcs", "butter|30|g", "milk|100|ml", "salt|1|tsp");

        addRecipe(db, "Roast Potatoes",
                "1. Heat the oven to 200 C.\n"
                        + "2. Cut the potatoes into chunks and toss with the oil, salt and garlic.\n"
                        + "3. Spread on a tray.\n"
                        + "4. Roast for 40 minutes, turning once.",
                "potato|5|pcs", "oil|3|tbsp", "salt|1|tsp", "garlic|2|pcs");

        addRecipe(db, "Tomato Soup",
                "1. Chop the tomatoes, onion and garlic.\n"
                        + "2. Fry the onion and garlic in the butter.\n"
                        + "3. Add the tomatoes, salt and a cup of water; simmer 20 minutes.\n"
                        + "4. Blend until smooth and serve.",
                "tomato|6|pcs", "onion|1|pcs", "garlic|2|pcs", "butter|20|g", "salt|1|tsp");

        addRecipe(db, "Chicken and Rice",
                "1. Cut the chicken into pieces and season with salt.\n"
                        + "2. Fry the chicken and chopped onion in the oil until golden.\n"
                        + "3. Add the rice and 400 ml water.\n"
                        + "4. Cover and simmer for 20 minutes until the rice is cooked.",
                "chicken|300|g", "rice|200|g", "onion|1|pcs", "oil|2|tbsp", "salt|1|tsp");

        addRecipe(db, "Chicken Stir Fry",
                "1. Slice the chicken, carrots and onion.\n"
                        + "2. Heat the oil in a pan and fry the chicken for 5 minutes.\n"
                        + "3. Add the vegetables and cook for 4 minutes.\n"
                        + "4. Pour in the soy sauce, stir and serve.",
                "chicken|300|g", "carrot|2|pcs", "onion|1|pcs", "soy sauce|2|tbsp", "oil|1|tbsp");

        addRecipe(db, "Lentil Stew",
                "1. Rinse the lentils.\n"
                        + "2. Fry the chopped onion and garlic, then add the carrots and tomatoes.\n"
                        + "3. Add the lentils, salt and 600 ml water.\n"
                        + "4. Simmer for 30 minutes until the lentils are soft.",
                "lentil|200|g", "carrot|2|pcs", "onion|1|pcs", "garlic|2|pcs", "tomato|2|pcs", "salt|1|tsp");

        addRecipe(db, "Banana Smoothie",
                "1. Peel the bananas and break them into pieces.\n"
                        + "2. Add them to a blender with the milk and sugar.\n"
                        + "3. Blend until smooth.\n"
                        + "4. Pour into a glass and enjoy.",
                "banana|2|pcs", "milk|250|ml", "sugar|1|tbsp");
    }

    private void addRecipe(SQLiteDatabase db, String name, String steps, String... ingredients) {
        ContentValues recipe = new ContentValues();
        recipe.put("name", name);
        recipe.put("steps", steps);
        long recipeId = db.insert("recipes", null, recipe);

        for (String line : ingredients) {
            String[] parts = line.split("\\|");
            ContentValues ingredient = new ContentValues();
            ingredient.put("recipe_id", recipeId);
            ingredient.put("name", parts[0]);
            ingredient.put("quantity", Double.parseDouble(parts[1]));
            ingredient.put("unit", parts[2]);
            db.insert("recipe_ingredients", null, ingredient);
        }
    }
}
