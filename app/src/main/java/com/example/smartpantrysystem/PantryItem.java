package com.example.smartpantrysystem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** One ingredient the user currently has at home (a row in the "pantry" table). */
public class PantryItem {

    private final int id;
    private final String name;
    private final double quantity;
    private final String unit;
    private final String expiry; // format yyyy-MM-dd, or null if no expiry date

    public PantryItem(int id, String name, double quantity, String unit, String expiry) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiry = expiry;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getExpiry() { return expiry; }

    /**
     * Number of days from today until the item expires.
     * Negative means already expired. Returns null when there is no (valid) expiry date.
     */
    public Long daysUntilExpiry() {
        if (expiry == null || expiry.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date expiryDate = format.parse(expiry);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            long diff = expiryDate.getTime() - today.getTimeInMillis();
            return Math.round(diff / 86400000.0);
        } catch (ParseException e) {
            return null;
        }
    }
}
