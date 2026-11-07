package com.example.survivor.game;

import java.util.Scanner;

final class LocationPreview {
    // Stable location IDs preserve artwork assignments when loading older saves.
    static String artwork(int number) {
        if(number==1 || number==2 || number>=4 && number<=32 || number>=41 && number<=50)return "route_"+number;
        return switch (number) {
            case 4, 34 -> "location_food";
            case 10 -> "clinic_exterior";
            case 11 -> "pharmacy_exterior";
            case 12 -> "hospital_exterior";
            case 13, 48 -> "location_medical";
            case 2, 19, 21, 25, 26 -> "location_forest";
            case 22, 23, 24, 42 -> "location_lake";
            case 9, 14, 27, 33, 35, 36, 37, 43 -> "location_workshop";
            case 28, 30, 31 -> "location_mine";
            case 38, 39, 40 -> "location_railway";
            case 15, 16, 32, 45, 46, 47 -> "location_military";
            case 41, 49, 50 -> "location_laboratory";
            case 18, 44 -> "location_shelter";
            case 51, 52, 53 -> "location_night";
            default -> "location_house";
        };
    }
    static final int TRAVEL_MINUTES = 25;
    static int untilDawn(WorldStories clock) {
        int time = clock.minuteOfDay();
        return time >= 1200 ? 1800 - time : time < 360 ? 360 - time : 0;
    }
    static boolean canDepart(Locations world, int number) {
        Location place = world.getLocation(number);
        if(!world.isAvailable(number))return false;
        MonsterDens.Den den=world.getStories().monsterDens().ensure(world,number);
        return (!place.isDepleted() || den!=null && den.accessible())
                && (!place.isNightOnly() || untilDawn(world.getStories()) > TRAVEL_MINUTES + SearchMode.FAST.minutes);
    }
    static String profile(Location place) {
        return switch (place.getLootProfile()) {
            case FOOD -> "Продукты и запасы еды";
            case MEDICAL -> "Бинты, аптечки и лекарства";
            case WORKSHOP -> "Инструменты, ткань, материалы и ближнее оружие";
            case ARMS -> "Оружие, броня и боеприпасы";
            default -> "Разные припасы и материалы";
        };
    }
    static String risk(Location place) {
        if(place.getName().equals("Районная поликлиника"))return "Риск: низкий–средний";
        if(place.getName().equals("Аптечный склад"))return "Риск: средний";
        if(place.getName().equals("Городская больница"))return "Риск: средний–высокий";
        return switch (place.getDifficultyName()) {
            case "лёгкая" -> "Риск: низкий";
            case "средняя" -> "Риск: средний";
            case "опасная" -> "Риск: высокий";
            default -> "Риск: неизвестен";
        };
    }
    static boolean show(Locations world, int number, SurvivorInfo player, Scanner scanner) {
        Location place = world.getLocation(number);
        while (true) {
            WorldStories clock = world.getStories();
            GameIO.menu("Локация", "");
            String subtitle = place.isNightOnly() ? "Ночной маршрут · открыт только с 20:00 до 06:00" : place.getDescription();
            GameIO.illustratedCard(place.getName(), subtitle, "location_hero:" + artwork(number));
            if (number == Locations.EVACUATION_LOCATION) GameIO.card("Точка эвакуации",
                    "Военный склад — конечная точка маршрута спасения. Подтверждения прибытия эвакуации пока нет.");
            int returnTime = (clock.minuteOfDay() + 50) % 1440;
            int arrivalTime = (clock.minuteOfDay() + TRAVEL_MINUTES) % 1440;
            boolean arrivalNight = arrivalTime < 360 || arrivalTime >= 1200;
            String road = "25 мин\nв одну сторону";
            String points = place.getSuppliesStatus().replace("точек обыска: ", "") + "\nпоиска";
            GameIO.card("__location_metrics", road + "|" + points + "|" + risk(place));
            GameIO.card("Возможная добыча", profile(place) + "\nСодержимое конкретной находки заранее неизвестно.");
            MonsterDens.Den den=clock.monsterDens().ensure(world,number);
            if(den!=null && den.accessible())GameIO.card(den.label(),den.cleared?"Логово зачищено; внутри остались трофеи.":"Опасная группа: "+den.count+" заражённых. Пока логово активно, встречи с заражёнными чаще, случайная добыча может быть ценнее.");
            if ((number == 6 || number == 8) && !world.isAvailable(number)) GameIO.card("Состояние входа", number == 6 ? "Закрыто. Откройте чердачный люк в жилом подъезде." : "Закрыто. Откройте вход в школу со школьного двора.");
            else GameIO.illustratedCard("", "", "location_state:" + place.getState().name());
            String reason = "Открыть маршрут";
            if (!world.isAvailable(number)) reason = place.isNightOnly() && !clock.isNight() ? "Днём проход закрыт"
                    : number == 6 ? "Откройте чердачный люк в жилом подъезде" : number == 8 ? "Откройте вход в школу со школьного двора" : "Сначала посетите «" + world.getLocation(place.getRequiredLocation()).getName() + "»";
            else if (place.isDepleted() && (den==null || !den.accessible())) reason = "Запасы исчерпаны";
            else if (place.isNightOnly() && !canDepart(world, number)) reason = "Не успеете дойти и закончить даже быстрый поиск до рассвета";
            if (canDepart(world, number)) GameIO.illustratedAction(1, "Выйти на вылазку", reason, "location_depart");
            else GameIO.action(1, "Выйти на вылазку", reason, false);
            GameIO.action(0, "Назад к локациям");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return false;
            if (choice == 1 && canDepart(world, number)) return true;
        }
    }
}
