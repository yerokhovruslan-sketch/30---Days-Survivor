package com.example.survivor.game;

import java.util.Scanner;

enum SearchMode {
    FAST("Быстрый поиск", 10, 4, -10, -15, 0, 1),
    CAREFUL("Осторожный поиск", 25, 5, 10, 0, 0, 2),
    THOROUGH("Тщательный поиск", 45, 8, 0, 20, 15, 3);
    final String label;
    final int minutes, stamina, stealth, lootBonus, storyBonus, needs;
    SearchMode(String label, int minutes, int stamina, int stealth, int lootBonus, int storyBonus, int needs) {
        this.label = label; this.minutes = minutes; this.stamina = stamina;
        this.stealth = stealth; this.lootBonus = lootBonus; this.storyBonus = storyBonus; this.needs = needs;
    }
    boolean begin(Fighter player, WorldStories clock) {
        if (!player.spendStamina(stamina)) return false;
        GameIO.sound(GameSound.SEARCH);
        clock.advanceMinutes(minutes);
        player.increaseHunger(needs);
        player.increaseSleepiness(needs);
        player.searchStealthModifier = stealth;
        return true;
    }
    static SearchMode choose(Fighter player, WorldStories clock, Scanner scanner) {
        return choose(player, clock, scanner, null);
    }
    static SearchMode choose(Fighter player, WorldStories clock, Scanner scanner, Location location) {
        while (true) {
            GameIO.menu("Способ поиска", clock.time() + (clock.isNight() ? " · Ночь: врагов заметить сложнее" : " · День"));
            for (int i = 0; i < values().length; i++) {
                SearchMode mode = values()[i];
                String detail = mode.minutes + " мин · " + player.staminaCost(mode.stamina) + " сил · "
                        + (mode == FAST ? "легче попасться врагам, ниже шанс добычи" : mode == CAREFUL ? "лучше обнаружение врагов, безопаснее обход" : "шанс добычи +20 п.п., чаще истории");
                boolean time = location == null || !location.isNightOnly() || LocationPreview.untilDawn(clock) > mode.minutes;
                GameIO.action(i + 1, mode.label, detail + (time ? "" : " · не закончить до рассвета"), time && player.getStamina() >= player.staminaCost(mode.stamina));
            }
            GameIO.action(4, player.isFlashlightOn() ? "Выключить фонарик" : "Включить фонарик",
                    player.hasFlashlight() ? "Ночью лучше обзор, но враги легче замечают свет" : "Купите фонарик или найдите его на вылазке", player.hasFlashlight());
            GameIO.action(5, "Передохнуть", "+30 выносливости · 20 минут");
            if(clock.caseCount(clock.currentPlace())>0)GameIO.action(7,"Осмотреть место стрельбы","Собрать оставшиеся гильзы");
            if(location!=null && !clock.residential().points(clock.currentPlace(),clock).isEmpty())
                GameIO.action(6,"Точки интереса","Известные встречи и помещения");
            if(clock.currentPlace()==15 && PoliceArchive.unlocked(player))GameIO.action(8,"Попасть в архив","Ключ в рюкзаке · два связанных зала");
            if(clock.archiveComplete && (clock.currentPlace()==13 || clock.currentPlace()==41))GameIO.action(9,clock.currentPlace()==13?"Проверить медицинский экипаж №17":"Разведать резервный маршрут","Сюжетное исследование");
            if(clock.currentPlace()==5 && clock.courtyard().passageAvailable()) GameIO.action(11,"Осмотреть служебный проход","Отмечен на плане дворника");
            GameIO.action(0, "Вернуться домой");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return null;
            if (choice >= 1 && choice <= 3) {
                SearchMode selected = values()[choice - 1];
                if (location == null || !location.isNightOnly() || LocationPreview.untilDawn(clock) > selected.minutes) return selected;
            }
            if(choice==8 && clock.currentPlace()==15 && PoliceArchive.unlocked(player)){PoliceArchive.enter(player,clock,scanner,java.util.concurrent.ThreadLocalRandom.current());if(!player.isAlive())return null;}
            if(choice==9 && clock.archiveComplete && (clock.currentPlace()==13 || clock.currentPlace()==41))PoliceArchive.followup(player,clock,clock.currentPlace(),scanner);
            if (choice == 11 && clock.currentPlace()==5) clock.courtyard().inspectPassage(clock,scanner);
            if (choice == 4) player.toggleFlashlight();
            if (choice == 5) Survivor.rest(player, 30);
            if(choice==7 && !Crafting.searchCases(player,clock,scanner,java.util.concurrent.ThreadLocalRandom.current()))return null;
            if(choice==6 && location!=null) {
                clock.residential().pointsMenu(player,clock.currentPlace(),clock,scanner,java.util.concurrent.ThreadLocalRandom.current());
                if(clock.residential().aborted || !player.isAlive())return null;
            }
        }
    }
}
