package io.github.cs32272610mp2xcode.finderskeepers.report;

/** Supported categories for school lost-and-found reports. */
public enum ItemCategory {
    /** Pens, pencils, cases, and other stationery. */
    STATIONERY("Stationery"),

    /** Books, workbooks, and notebooks. */
    BOOKS("Books"),

    /** Uniforms, jackets, shoes, and other clothing. */
    CLOTHING("Clothing"),

    /** School bags, pouches, and similar containers. */
    BAGS("Bags"),

    /** Water bottles and drink containers. */
    WATER_BOTTLES("Water bottles"),

    /** Electronic devices and accessories. */
    ELECTRONICS("Electronics"),

    /** Sports clothing and equipment. */
    SPORTS_EQUIPMENT("Sports equipment"),

    /** Personal belongings that do not fit a narrower category. */
    PERSONAL_ITEMS("Personal items"),

    /** Any item outside the listed categories. */
    OTHER("Other");

    private final String displayName;

    ItemCategory(String displayLabel) {
        displayName = displayLabel;
    }

    /**
     * Returns the stable, case-sensitive value used in storage.
     *
     * @return stored enum name
     */
    public String storedName() {
        return name();
    }

    /**
     * Returns the label intended for user interfaces.
     *
     * @return readable category name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Parses an exact stored item category.
     *
     * @param storedValue stored enum name
     * @return parsed category
     * @throws IllegalArgumentException if the value is absent, blank, or unknown
     */
    public static ItemCategory fromStoredName(String storedValue) {
        if (storedValue == null) {
            throw new IllegalArgumentException("Item category is required.");
        }
        if (storedValue.isBlank()) {
            throw new IllegalArgumentException("Item category cannot be blank.");
        }
        try {
            return valueOf(storedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown item category: " + storedValue + ".", exception);
        }
    }
}
