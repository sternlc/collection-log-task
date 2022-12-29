package com.untc;

public enum Tiers {

    EASY(new String[] {
            ""
    }),
    MEDIUM(new String[] {
            ""
    }),
    HARD(new String[] {
            ""
    }),
    ELITE(new String[] {
            ""
    }),
    PASSIVE(new String[] {
            "Gilded",
            "3rd age",
            "Ring of",
            "Victor's cape",
            "Icthlarin's shroud",
            "Xeric's",
            "Sinhaza shroud",
            "Godsword shard",
            "Evil chicken",
            "(dusk)",
            "Broken dragon hasta",
            "Jar of",
            "Bucket helm (g)",
            "Lava dragon mask",
            "Iasor seed",
            "Attas seed",
            "Kronos seed",
            "Bottomless compost bucket",
            "Infernal cape",
            "Fire cape",
            "Draconic visage",
            "Skeletal visage",
            "remnant",
            "mutagen",
            "Dark relic",
            "Torn prayer scroll",
            "Imbued heart",
            "Eternal gem",
            "Golden tench"
    });

    private Tiers(final String[] items) {
        this.items = items;
    }

    public String[] items;

    public String[] getItems() {
        return items;
    }

}
