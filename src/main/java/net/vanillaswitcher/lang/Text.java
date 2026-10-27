package net.vanillaswitcher.lang;

public enum Text {
    TITLE("Vanilla Switcher", "Vanilla Switcher", "Vanilla Switcher"),

    SECTION_VISUALS("Visuals", "Визуальные эффекты", "Визуалы"),
    SECTION_BRIGHTNESS("Brightness", "Яркость", "Яркость"),
    SECTION_RENDER_DISTANCE("Render Distance", "Дальность прорисовки", "Прорисовка чанков"),
    SECTION_TOTEM("Totem", "Тотем", "Тотем"),
    SECTION_BINDS("Binds", "Клавиши", "Бинды"),
    SECTION_OTHERS("Others", "Другое", "Другое"),

    PARTICLES("Particles", "Частицы", "Партиклы"),
    NO_FOG("No Fog (all types)", "Туман (все типы)", "Туман (все типы)"),

    SMOOTH_FULLBRIGHT("Smooth Fullbright", "Полная яркость", "Яркость"),
    FULLBRIGHT_STRENGTH("Fullbright Strength", "Сила яркости", "Сила яркости"),
    BRIGHTNESS_TRANSITION("Brightness Transition", "Переход яркости", "Переход яркости"),

    RENDER_DISTANCE_SWITCH("Render Distance Switch", "Переключение дальности прорисовки", "Прорисовка чанков"),
    RENDER_DISTANCE_ON("Render Distance (mod ON)", "Дальность прорисовки (мод включён)", "Прорисовка чанков (мод включён)"),
    RENDER_DISTANCE_OFF("Render Distance (mod OFF)", "Дальность прорисовки (мод выключен)", "Прорисовка чанков (мод выключен)"),

    TOTEM_RESIZE("Totem Resize", "Изменение размера тотема", "Ресайз тотема"),
    TOTEM_SIZE("Totem Size", "Размер тотема", "Размер тотема"),
    TOTEM_POP_SIZE("Totem Pop Size", "Размер анимации тотема", "Размер поп тотема"),

    ACTIVATE_BIND("Activate bind", "Клавиша активации", "Бинд активации"),
    MENU_BIND("Open menu bind", "Клавиша настроек", "Бинд настроек"),

    FIRE_SHIELD_OFFSETS("Fire/Shield Offsets", "Смещение огня/щита", "Оффсэт огня/щита"),
    ARMOR_HUD("Armor HUD", "Индикатор брони", "HUD брони"),
    ALERT_TOAST("Alert Toast", "Уведомление", "Уведомление"),

    SMOOTH_LIGHTING_SWITCH("Smooth Lighting Switch", "Плавное освещение", "Плавное освещение"),

    DONE("Done", "Готово", "Готово"),

    ON("ON", "ВКЛ", "ВКЛ"),
    OFF("OFF", "ВЫКЛ", "ВЫКЛ"),
    PRESS_A_KEY("Press a key...", "Нажмите клавишу…", "Выберите бинд…"),
    NOT_BOUND("Not bound", "Не назначена", "Не назначен"),

    SECONDS_SUFFIX("s", " с", " с"),

    STRENGTH_SUFFIX("/15", "/15", "/15");

    private final String english;
    private final String russian;
    private final String mixedRussian;

    Text(String english, String russian, String mixedRussian) {
        this.english = english;
        this.russian = russian;
        this.mixedRussian = mixedRussian;
    }

    public String in(Language language) {
        if (language == null) {
            return english;
        }
        return switch (language) {
            case ENGLISH -> english;
            case RUSSIAN -> russian;
            case MIXED_RUSSIAN -> mixedRussian;
        };
    }

    public static String chunks(Language language, int count) {
        if (language == null || language == Language.ENGLISH) {
            return count + " chunks";
        }
        int lastTwo = count % 100;
        int last = count % 10;
        String word;
        if (lastTwo >= 11 && lastTwo <= 14) {
            word = "чанков";
        } else if (last == 1) {
            word = "чанк";
        } else if (last >= 2 && last <= 4) {
            word = "чанка";
        } else {
            word = "чанков";
        }
        return count + " " + word;
    }
}
