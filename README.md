# Smart Pantry Manager

An Android app, written in **Java**, that helps reduce food waste. You record the ingredients you actually have at home (your "pantry"), and the app suggests recipes you can cook using **strictly** those ingredients, so no shopping trip is needed.

Built for **Mobile App Development 700** (Richfield Graduate Institute of Technology), Practical Assignment.

- **Author:** Kgopotso Mbuli (402309196)
- **Language / IDE:** Java, Android Studio
- **Minimum Android version:** API 24 (Android 7.0)
- **Database:** SQLite (local, on the device)

Github link: 
[GitHub Repository](https://github.com/Kgopotso24/SmartPantry)



---

## The core idea: strict matching

A recipe is only shown as "ready to cook" if **every** ingredient it needs is in the pantry, **in at least the required quantity**.

- A recipe that needs 5 ingredients when the pantry has 4 is **not** suggested.
- Recipes that are only one or two ingredients short appear in a separate **"Almost there"** section, never in the strict list.
- Matching copes with everyday messiness:
  - Names are normalised, so `Tomatoes` and `tomato` are the same ingredient.
  - Units are converted before comparing (kg to g, l / cup / tbsp / tsp to ml).
  - The same ingredient added more than once is added up.

The logic lives in `IngredientMatcher.java`.

## Features

- **Pantry management:** add, edit and delete ingredients (name, quantity, unit, optional expiry date), with input validation.
- **Home dashboard:** greeting, ingredient count, items to use soon, a pantry freshness bar, a recipe to cook tonight, and recipes that are almost ready.
- **Suggestions:**
  - *Cook with what you have*: recipes the pantry fully covers.
  - *Almost there*: recipes that are one or two ingredients short.
  - *Ask for a meal*: type something like "pizza" or "How do I make a burger?". The app shows what is missing and suggests swaps, preferring ingredients you already have. This works offline from a built-in swap table.
- **Recipes:** 25 built-in recipes loaded on first run, plus your own recipes (name, method, any number of ingredient rows). Your own recipes can be deleted; the built-in ones are protected.
- **Browse all recipes:** a *Can make now* view and an *All recipes* view showing what is missing.
- **Settings:** your name (used in the greeting), expiring-soon alerts on/off, default unit, and buttons to load a sample pantry or clear the pantry.
- **Persistence:** all data is stored in SQLite and is still there after the app is closed and reopened.

No maps, GPS or location features are used.

## Why SQLite?

The data in this app (one person's pantry and recipes) belongs on their own device. SQLite:

- needs **no internet connection, account or server**,
- **persists** data between sessions,
- is built into Android and is the approach covered in the module's persistent-data chapter,
- keeps the app simple and reliable to run, unlike a cloud database, which would add network and login complexity this app does not need.

## Screens

| Screen | Class | Purpose |
|---|---|---|
| Home | `HomeActivity` | Dashboard and quick actions |
| Pantry | `MainActivity` | List of ingredients (RecyclerView) with Edit and Delete |
| Add / Edit Ingredient | `AddEditActivity` | Create and update an ingredient, with validation |
| Suggestions | `SuggestionsActivity` | Ready recipes, almost-there recipes, meal requests with swaps |
| All Recipes | `SuggestedActivity` | Browse every recipe |
| Recipe Detail | `RecipeDetailActivity` | Ingredients and method; delete your own recipes |
| New Recipe | `AddRecipeActivity` | Write your own recipe |
| Settings | `SettingsActivity` | Preferences and sample data |

Navigation uses a bottom navigation bar (Home, Pantry, Suggest, Settings) and Intents with extras to pass ids between screens.

## Database design

`smart_pantry.db`, managed by `DatabaseHelper` (a `SQLiteOpenHelper`):

- **pantry**: `id`, `name`, `quantity`, `unit`, `expiry`
- **recipes**: `id`, `name`, `steps`, `is_custom`
- **recipe_ingredients**: `id`, `recipe_id` (foreign key to `recipes`, cascade delete), `name`, `quantity`, `unit`

Settings (name, alerts, default unit) are stored in `SharedPreferences`.

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/smartpantrysystem/
│   ├── HomeActivity.java            Home dashboard
│   ├── MainActivity.java            Pantry list
│   ├── AddEditActivity.java         Add / edit ingredient
│   ├── SuggestionsActivity.java     Suggestions and meal requests
│   ├── SuggestedActivity.java       Browse all recipes
│   ├── RecipeDetailActivity.java    Recipe detail
│   ├── AddRecipeActivity.java       New recipe
│   ├── SettingsActivity.java        Settings
│   ├── DatabaseHelper.java          SQLite tables, CRUD, seed data
│   ├── IngredientMatcher.java       Strict-matching logic
│   ├── SuggestionEngine.java        Gap analysis and meal search
│   ├── Substitutions.java           Ingredient swap table
│   ├── PantryAdapter.java           RecyclerView adapter (pantry)
│   ├── RecipeAdapter.java           RecyclerView adapter (recipes)
│   ├── NavHelper.java               Bottom navigation helper
│   └── PantryItem / Recipe / RecipeIngredient   Model classes
└── res/                             Layouts, drawables, menu, colours, theme
```

## Setup and run instructions

### Requirements

- A recent version of **Android Studio** (with the Android SDK installed)
- An Android phone running Android 7.0 or newer, **or** an emulator
- A USB cable (for a physical phone)

### Steps

1. **Get the code**
   ```
   git clone https://github.com/Kgopotso24/SmartPantry.git
   ```
   Or download the project as a ZIP and extract it.
2. **Open it in Android Studio:** *File > Open* and select the project folder.
3. **Let Gradle sync.** The first sync downloads dependencies and can take a few minutes. No extra libraries need to be added by hand.
4. **Choose where to run it:**
   - *Physical phone (what this project was tested on):* open *Settings > About phone* and tap **Build number** seven times, then open *Developer options* and turn on **USB debugging**. Plug the phone in, tap **Allow** on the prompt, and pick the phone in the device dropdown at the top of Android Studio.
   - *Emulator:* create a device in *Device Manager* and select it in the same dropdown.
5. **Press Run** (the green triangle).

### Trying it out

1. Open **Settings > Load sample pantry** to fill the pantry with 17 demo ingredients (some expire within a few days).
2. Check **Home**, **Pantry** and **Suggest**.
3. In **Suggest**, type `pizza` and press *Find recipe and swaps* to see missing ingredients and swaps.
4. To see the strict rule at work, delete an ingredient from the Pantry and watch a recipe disappear from the ready list, then add it back.
5. Use **Settings > Clear pantry** to start from an empty pantry.

## Known limitations

- The app only knows its 25 built-in recipes plus the ones you create.
- Weight and volume cannot be converted into each other without ingredient density data. If the pantry has an ingredient in a different kind of unit than the recipe uses (for example salt in grams, recipe in teaspoons), the app counts it as present.
- When a swap is already in your pantry, the app does not check that you have enough of it; its note says how much to use.
- The swap table covers about 25 common ingredients.

## Version control

The project was developed under Git, with small, descriptive commits made throughout development.
