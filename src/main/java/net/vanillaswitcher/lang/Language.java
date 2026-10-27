package net.vanillaswitcher.lang;

public enum Language {
    ENGLISH("English"),
    RUSSIAN("Russian"),
    MIXED_RUSSIAN("Mixed Russian");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public Language next() {
        Language[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
