package com.example.survivor.game;

import java.io.File;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.List;

/** Per-session console port; never redirects the Android process's System streams. */
final class GameIO {
    private static final ThreadLocal<GameSession> CURRENT = new ThreadLocal<>();

    static void attach(GameSession session) { CURRENT.set(session); }
    static void detach() { CURRENT.remove(); }
    static PrintStream out() { return CURRENT.get() == null ? System.out : CURRENT.get().output; }
    static Scanner scanner() { return CURRENT.get().scanner; }
    static boolean hasInput(Scanner scanner) { return CURRENT.get() != null || scanner.hasNext(); }
    static File saveFile() { return CURRENT.get().saveFile; }
    static int difficulty(){return CURRENT.get()==null?2:CURRENT.get().difficulty();}
    static WorldStories clock() { return CURRENT.get() == null ? null : CURRENT.get().clock(); }
    static Locations world() { return CURRENT.get() == null ? null : CURRENT.get().world(); }
    static void advanceMinutes(int minutes) { if (clock() != null) clock().advanceMinutes(minutes); }
    static boolean atHome() { return CURRENT.get() != null && CURRENT.get().atHome; }
    static void leaveHome() { if (CURRENT.get() != null) CURRENT.get().atHome = false; }
    static boolean player(SurvivorInfo player) {
        if (CURRENT.get() == null) return false;
        CURRENT.get().player(player);
        return true;
    }
    static boolean battle(SurvivorInfo player, List<? extends CombatEnemy> enemies, CombatEnemy target) {
        if (CURRENT.get() == null) return false;
        CURRENT.get().battle(player, enemies, target);
        return true;
    }
    static void clearBattle() { if (CURRENT.get() != null) CURRENT.get().battle(null, null, null); }
    static boolean supportsTactics() { return CURRENT.get() != null; }
    static void board(BattleBoard board) { if (CURRENT.get() != null) CURRENT.get().board(board); }
    static void scene(String scene) { if(CURRENT.get()!=null)CURRENT.get().scene(scene); }
    static void checkpoint() { if(CURRENT.get()!=null)CURRENT.get().checkpoint(); }
    static void musicCue(String cue){if(CURRENT.get()!=null)CURRENT.get().musicCue(cue);}
    static void hurt(SurvivorInfo player){if(CURRENT.get()!=null && CURRENT.get().isCurrentPlayer(player))sound(GameSound.HURT);}
    static GameSound footsteps(){return footsteps(clock()==null?0:clock().currentPlace());}
    static GameSound footsteps(int place){return place==2 || place>=19 && place<=32?GameSound.EARTH_STEPS:GameSound.CONCRETE_STEPS;}
    static void travelTransition(String text){if(CURRENT.get()!=null)CURRENT.get().travelTransition(text);}
    static void sound(GameSound sound) {if(CURRENT.get()!=null)CURRENT.get().sound(sound);}
    static void balance(int balance) { if (CURRENT.get() != null) CURRENT.get().balance(balance); }
    static void narrative(){if(CURRENT.get()!=null)CURRENT.get().narrative();}
    static void storyMenu(String title,String hint) {menu(title,hint);if(CURRENT.get()!=null)CURRENT.get().pauseReading();}
    static void menu(String title, String hint) {
        if (CURRENT.get() != null) CURRENT.get().menu(title, hint);
    }
    static void shopTab(int number,String label,boolean selected){if(CURRENT.get()!=null)CURRENT.get().shopTab(number,label,selected);}
    static void shopAction(int number,String label,String detail,String priceTag,boolean enabled){if(CURRENT.get()!=null)CURRENT.get().shopAction(number,label,detail,priceTag,enabled);}
    static void removeActions(int... numbers){if(CURRENT.get()!=null)CURRENT.get().removeActions(numbers);}
    static void action(int number, String label) { action(number, label, "", true); }
    static void point(int number,String label,String detail,boolean completed){point(number,label,detail,completed,true);}
    static void point(int number,String label,String detail,boolean completed,boolean enabled){
        if(CURRENT.get()!=null)CURRENT.get().pointAction(number,label,detail,completed,enabled);
    }
    static void selectedAction(int number,String label,String detail,boolean enabled) {
        if(CURRENT.get()!=null)CURRENT.get().selectedAction(number,label,detail,enabled);
    }
    static void illustratedAction(int number,String label,String detail,String artwork) {
        if(CURRENT.get()!=null)CURRENT.get().illustratedAction(number,label,detail,artwork);
    }
    static void action(int number, String label, String detail) { action(number, label, detail, true); }
    static void action(int number, String label, String detail, boolean enabled) {
        if (CURRENT.get() != null) CURRENT.get().action(number, label, detail, enabled);
    }
    static void card(String label, String detail) { if (CURRENT.get() != null) CURRENT.get().card(label, detail); }
    static void illustratedCard(String label, String detail, String image) {
        if (CURRENT.get() != null) CURRENT.get().illustratedCard(label, detail, image);
    }
    static void quantity(String title, String hint, int max, int unitPrice) {
        menu(title, hint);
        if (CURRENT.get() != null) CURRENT.get().quantity(max, unitPrice);
    }
    static void targets(String title, List<? extends CombatEnemy> enemies) {
        menu(title, "Нажмите на карточку живого противника.");
        if (CURRENT.get() != null) CURRENT.get().targeting();
        for (int i = 0; i < enemies.size(); i++) {
            CombatEnemy enemy = enemies.get(i);
            action(i + 1, enemy.getName(), enemy.getHealth() + "/" + enemy.getMaxHealth() + " HP", enemy.isAlive());
        }
        action(0, "Отмена");
    }
    static void item(int number, ItemInfo item, String verb, boolean enabled) {
        item(number,item,verb,enabled,false);
    }
    static void item(int number,ItemInfo item,String verb,boolean enabled,boolean selected) {
        String detail = item.getDescription() + "\n" + item.physicalStats();
        if (item instanceof Weapons) detail += "\n" + ((Weapons) item).combatStats();
        if (item instanceof Food) detail += "\nУтоляет голод: " + ((Food) item).getNutrition();
        if (item instanceof MedicalItem) detail += item.getName().toLowerCase(java.util.Locale.ROOT).contains("бинт") ? "\nКровотечение −3; обработка одной части тела" : "\nОстанавливает кровотечение; обработка одной части тела";
        if(selected)selectedAction(number,verb+item.getName(),detail,enabled);
        else action(number, verb + item.getName(), detail, enabled);
    }
    static void mainMenu(SurvivorInfo player, int balance, Locations world) {
        CURRENT.get().mainMenu(player, balance, world);
    }
    static String repeat(String value, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) text.append(value);
        return text.toString();
    }
}
