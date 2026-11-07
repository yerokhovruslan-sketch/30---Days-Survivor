package com.example.survivor.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

final class ShelterUpgrades implements Serializable {
    private static final long serialVersionUID = 1L;
    int bed, bench, radio, completed;
    int anvil;
    boolean accepted;
    int signalAttempts, signalStage;
    long signalReadyMinute;
    boolean signalReceived;
    enum SignalResult { LOCKED, NO_SIGNAL_DAY, NO_SIGNAL_HALF_DAY, RECEIVED, ALREADY_RECEIVED }
    static long minute(WorldStories clock) { return (clock.day()-1L)*1440+clock.minuteOfDay(); }
    long signalWait(WorldStories clock) { return Math.max(0,signalReadyMinute-minute(clock)); }
    boolean canListen(WorldStories clock) { return radio>0 && (signalStage>0 || !signalReceived && signalWait(clock)==0); }
    String signalHint(WorldStories clock) {
        if(radio==0)return "Сначала восстановите радиостанцию";
        if(signalStage>0)return "Продолжить приём сообщения";
        if(signalReceived)return "Сигнал «Маяк» уже записан. Новая цель — архив участка";
        long wait=signalWait(clock);
        return wait==0?"Настроить частоту «Маяк»":"До следующей попытки: "+wait/60+" ч "+wait%60+" мин игрового времени";
    }
    SignalResult attemptSignal(WorldStories clock, java.util.Random random) {
        if(radio==0 || signalWait(clock)>0)return SignalResult.LOCKED;
        if(signalReceived)return SignalResult.ALREADY_RECEIVED;
        signalAttempts++;
        if(signalAttempts==1) {signalReadyMinute=minute(clock)+1440;return SignalResult.NO_SIGNAL_DAY;}
        if(signalAttempts==2 && random.nextInt(100)>=50) {
            signalReadyMinute=minute(clock)+720;return SignalResult.NO_SIGNAL_HALF_DAY;
        }
        signalReceived=true;signalStage=1;signalReadyMinute=0;return SignalResult.RECEIVED;
    }
    void listen(Fighter player,java.util.Scanner scanner,WorldStories clock) {
        if(!canListen(clock))return;
        if(signalStage==0) {
            SignalResult result=attemptSignal(clock,java.util.concurrent.ThreadLocalRandom.current());
            GameIO.checkpoint();
            if(result==SignalResult.NO_SIGNAL_DAY || result==SignalResult.NO_SIGNAL_HALF_DAY) {
                GameIO.menu("Нет сигнала","Приёмник исправен. На частоте только помехи: передача может быть непостоянной. Чтобы беречь питание, Джон откладывает следующую длительную проверку.\n"+(result==SignalResult.NO_SIGNAL_DAY?"Попробуй завтра — через 24 игровых часа":"Попробуйте через 12 игровых часов"));
                GameIO.scene("radio_fail");GameIO.action(1,"Продолжить");
                SurvivalSystems.choice(scanner);return;
            }
            if(result!=SignalResult.RECEIVED)return;
        }
        if(signalStage==1) {
            GameIO.menu("Сигнал «Маяк»","");GameIO.scene("radio_audio");GameIO.action(1,"Продолжить");
            if(SurvivalSystems.choice(scanner)!=1)return;
            signalStage=2;GameIO.checkpoint();
        }
        if(signalStage==2) {
            GameIO.menu("После сигнала", "Военный склад. Теперь есть куда идти. Только кто знает, когда записали это сообщение.");
            GameIO.scene("radio_thought");GameIO.action(1,"Продолжить");
            if(SurvivalSystems.choice(scanner)!=1)return;
            signalStage=0;clock.recordRadioSignal();GameIO.checkpoint();
        }
    }

    int sleepHealing(Fighter player) {
        int base = new int[] {3, 8, 15}[bed];
        return player.getHunger() >= 75 ? Math.max(1, base / 2) : base;
    }
    static int sleep(Fighter player, WorldStories clock) {
        if (!player.canSleep()) return -1;
        int before = player.getHealth();
        player.sleep();
        clock.bindRecovery(player);
        clock.advanceMinutes(480);
        player.recoverInjuries(player.getUpgrades().bed * 240);
        return player.getHealth() - before;
    }
    int repairAmount() { return 30 + bench * 15; }
    String scoutingGoals(Fighter player, LootProfile profile) {
        StringBuilder result = new StringBuilder();
        if (profile == LootProfile.WORKSHOP || profile == LootProfile.GENERAL) {
            for (int type = 1; type <= 3; type++) {
                if (type == 1 && bed >= 2 || type == 2 && bench >= 2 || type == 3 && radio >= 1) continue;
                for (String[] part : recipe(type)) {
                    long owned = all(player).stream().filter(item -> item.getName().equals(part[0])).count();
                    long missing = Math.max(0, Integer.parseInt(part[1]) - owned);
                    if (part[0].equals("Электрочип")) {
                        if (missing > 0) result.append("Электрочип: полицейский участок, электроподстанция или магазин × ").append(missing).append(" · 600 $ за штуку\n");
                        continue;
                    }
                    if (missing > 0) result.append(type == 1 ? "Кровать: " : type == 2 ? "Верстак: " : "Радио: ")
                            .append(part[0]).append(" × ").append(missing).append("\n");
                }
            }
        }
        if (accepted && completed < 3 && (profile == LootProfile.GENERAL
                || completed == 0 && profile == LootProfile.MEDICAL || completed == 1 && profile == LootProfile.FOOD
                || completed == 2 && profile == LootProfile.WORKSHOP))
            for (String[] part : questRecipe()) {
                long missing = Math.max(0, Integer.parseInt(part[1]) - all(player).stream().filter(item -> item.getName().equals(part[0])).count());
                if (missing > 0) result.append("Поручение: ").append(part[0]).append(" × ").append(missing).append("\n");
            }
        return result.length() == 0 ? "Пополнить рюкзак и склад. Для текущих улучшений и поручений профильных целей здесь нет."
                : "Возможные цели по профилю добычи (не гарантированные находки):\n" + result;
    }
    private String[][] recipe(int type) {
        if (type == 1) return bed == 0 ? new String[][] {{"Плотная ткань", "2"}, {"Изолента", "1"}}
                : new String[][] {{"Плотная ткань", "3"}, {"Запчасти", "2"}, {"Изолента", "1"}};
        if (type == 2) return bench == 0 ? new String[][] {{"Запчасти", "2"}, {"Металлические пластины", "1"}}
                : new String[][] {{"Запчасти", "6"}, {"Металлические пластины", "3"}, {"Медная проволока", "4"}, {"Изолента", "2"}};
        return new String[][] {{"Медная проволока", "2"}, {"Запчасти", "2"}, {"Изолента", "1"}, {"Электрочип", "2"}};
    }
    private static List<ItemInfo> all(Fighter player) {
        List<ItemInfo> items = new ArrayList<>(player.getStorage());
        items.addAll(player.getInventory());
        return items;
    }
    private static List<ItemInfo> select(Fighter player, String[][] recipe) {
        List<ItemInfo> available = all(player), selected = new ArrayList<>();
        for (String[] requirement : recipe) {
            int left = Integer.parseInt(requirement[1]);
            for (ItemInfo item : available) {
                if (left > 0 && !selected.contains(item) && item.getName().equals(requirement[0])) { selected.add(item); left--; }
            }
            if (left > 0) return null;
        }
        return selected;
    }
    private static void consume(Fighter player, List<ItemInfo> selected) {
        for (ItemInfo item : selected) { player.getInventory().remove(item); player.getStorage().remove(item); }
        player.getEquippedWeapon(); player.getEquippedArmor();
    }
    boolean canBuild(Fighter player, int type) {
        if (type < 1 || type > 3 || (type == 1 && bed >= 2) || (type == 2 && bench >= 2) || (type == 3 && radio >= 1)) return false;
        if (type != 1 && all(player).stream().noneMatch(item -> item.isRepairTool() && (!item.isDurable() || item.getDurability() > 0))) return false;
        return select(player, recipe(type)) != null;
    }
    boolean build(Fighter player, int type, WorldStories clock) {
        if (!canBuild(player, type)) return false;
        consume(player, select(player, recipe(type)));
        int duration=type==2?(bench==1?360:180):120;
        if (type == 1) bed++; else if (type == 2) bench++; else radio++;
        clock.advanceMinutes(duration);
        return true;
    }
    String description(Fighter player, int type) {
        int level = type == 1 ? bed : type == 2 ? bench : radio;
        int max = type == 3 ? 1 : 2;
        if (level == max) return "Максимальный уровень";
        StringBuilder text = new StringBuilder();
        for (String[] part : recipe(type)) text.append(part[0]).append(": ").append(all(player).stream().filter(item -> item.getName().equals(part[0])).count()).append("/").append(part[1]).append("\n");
        text.append(type == 2 ? bench==1?"6 часов":"3 часа" : "2 часа");
        if (type != 1) text.append(" · нужен инструмент для ремонта (не расходуется)");
        return text.toString();
    }
    static void menu(Fighter player, Scanner scanner, WorldStories clock) {
        ShelterUpgrades upgrades = player.getUpgrades();
        while (true) {
            GameIO.menu("Улучшения убежища", "Материалы берутся со склада и из рюкзака. Строительство занимает время.");
            GameIO.card("План эвакуации", upgrades.evacuationPlan(player, GameIO.world()));
            GameIO.card("Качество сна", "Ночлег " + upgrades.bed + "/2 · сон 8 часов. Обработанные раны без кровотечения заживают: −" + (8 + 4 * upgrades.bed) + " травмы за сон. Для сна нужна сонливость выше 40.");
            GameIO.card("Верстак", "Уровень " + upgrades.bench + "/2 · ремонт дома +" + upgrades.repairAmount()
                    + " прочности за материал. На вылазке — обычные +30.");
            GameIO.action(1, "Кровать · уровень " + upgrades.bed + "/2", upgrades.description(player, 1), upgrades.canBuild(player, 1));
            GameIO.action(2, "Верстак · уровень " + upgrades.bench + "/2", upgrades.description(player, 2), upgrades.canBuild(player, 2));
            GameIO.action(3, "Радиостанция · " + (upgrades.radio == 0 ? "не собрана" : "работает"), upgrades.description(player, 3), upgrades.canBuild(player, 3));
            GameIO.action(4, "Радиопоручения", "Обмен припасами с другими выжившими", upgrades.radio > 0);
            GameIO.action(5,"Поймать сигнал",upgrades.signalHint(clock),upgrades.canListen(clock));
            GameIO.action(6,"Крафтинг","Верстак, наковальня и разборка предметов");
            GameIO.action(0, "Назад в убежище");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return;
            if (choice >= 1 && choice <= 3 && upgrades.build(player, choice, clock)) {if(choice==3)GameIO.musicCue("digital");GameIO.out().println("Улучшение построено. Материалы использованы.");}
            if (choice == 4 && upgrades.radio > 0) upgrades.quests(player, scanner);
            if (choice == 5) upgrades.listen(player,scanner,clock);
            if(choice==6)Crafting.menu(player,clock,scanner);
        }
    }
    String evacuationPlan(Fighter player, Locations world) {
        String goal = signalReceived?"Точка сбора по радиозаписи: Военный склад.\n":"Главная цель: восстановить устойчивую связь и узнать точку сбора.\n";
        if (radio == 0) {
            long chips = all(player).stream().filter(item -> item.getName().equals("Электрочип")).count();
            return goal + "Ближайшая задача: восстановить радиостанцию.\nЭлектрочип: " + chips
                    + "/2 · купить у торговца или добыть в полицейском участке, на электроподстанции; блок из школьной котельной разбирается на верстаке I.\nОстальные материалы указаны в рецепте радиостанции.";
        }
        if(!signalReceived)return goal+"Радиостанция восстановлена.\nБлижайшая задача: поймать сигнал «Маяк».\n"
                +(world==null?"Настройте частоту в убежище.":signalHint(world.getStories()));
        if(signalStage>0)return goal+"Сигнал найден. Дослушайте сообщение и запишите услышанное.";
        if(world!=null && world.getStories().archiveComplete){
            WorldStories stories=world.getStories(); RouteAfterArchive route=stories.routeAfterArchive();
            String next=stories.archiveGoal==2?"проверить экипаж №17 на станции скорой помощи":stories.archiveGoal==1?"разведать КПП на территории химического завода":"выбрать, какую зацепку проверить первой";
            return goal+"Схема резервного прохода скопирована. Транспорт отменён, но сбор приказано сохранить.\nБлижайшая цель: "+next+". Обе зацепки доступны."
                    +(route.gateSeen?"\nВорота КПП обесточены. Указана подстанция №2.":"")
                    +(route.kitRequested&&!route.kitReturned?"\nОльга ждёт аптечку из служебного автомобиля.":"")
                    +(route.routeSheet?"\nМаршрут №17: сектор Б, пропускной пункт 04.":"");
        }
        return goal + "Сигнал «Маяк» записан.\nБлижайшая задача: найти схему резервного прохода и проверить дату в архиве участка.\n"
                + (world != null && world.isVisited(Locations.EVACUATION_LOCATION)
                ? "Военный склад найден. Подтверждения эвакуации пока нет."
                : "Следующая цель: добраться до военного склада. Путь открывается через военный блокпост и казармы.");
    }
    private String[][] questRecipe() {
        if (completed == 0) return new String[][] {{"Бинт", "2"}};
        if (completed == 1) return new String[][] {{"Галеты", "3"}};
        return new String[][] {{"Запчасти", "3"}, {"Медная проволока", "2"}};
    }
    boolean accept() {
        if (radio == 0 || accepted || completed >= 3) return false;
        GameIO.sound(GameSound.QUEST);accepted = true; return true;
    }
    boolean deliver(Fighter player) {
        if (radio == 0 || !accepted || completed >= 3) return false;
        List<ItemInfo> selected = select(player, questRecipe());
        if (selected == null) return false;
        consume(player, selected);
        // Rewards go to storage, even when the backpack is full.
        if (completed == 0) { player.getStorage().add(Shop.createItem(17)); player.getStorage().add(Shop.createItem(19)); }
        else if (completed == 1) { player.getStorage().add(Shop.createItem(2)); player.getStorage().add(Shop.createItem(12)); }
        else { player.getStorage().add(Shop.createItem(29)); player.getStorage().add(Shop.createItem(22)); }
        GameIO.sound(GameSound.QUEST);completed++; accepted = false;
        return true;
    }
    private void quests(Fighter player, Scanner scanner) {
        String[] names = {"Перевязка для соседнего убежища", "Запас еды для дозорных", "Починить ретранслятор"};
        String[] rewards = {"Тушёнка и запчасти", "Аптечка и упаковка дробовых патронов", "Охотничий нож и боевой шлем"};
        while (true) {
            GameIO.menu("Радиопоручения", "Награды поступают на склад. Выполнено: " + completed + "/3");
            if (completed >= 3) GameIO.card("Все поручения выполнены", "Вы помогли трём группам выживших.");
            else {
                StringBuilder requirements = new StringBuilder();
                for (String[] part : questRecipe()) requirements.append(part[0]).append(" × ").append(part[1]).append("\n");
                GameIO.card(names[completed], requirements + "Награда: " + rewards[completed]);
                GameIO.action(1, "Принять поручение", "Передача припасов — в этом разделе", !accepted);
                GameIO.action(2, "Передать припасы и получить награду", "Будут использованы указанные вещи", accepted && select(player, questRecipe()) != null);
            }
            GameIO.action(0, "Назад");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return;
            if (choice == 1) accept();
            if (choice == 2 && deliver(player)) GameIO.out().println("Поручение выполнено. Награда доставлена на склад.");
        }
    }
    boolean valid() { return anvil>=0 && anvil<=1 && signalAttempts>=0 && signalAttempts<=3 && signalReadyMinute>=0 && signalStage>=0 && signalStage<=2
            && (signalStage==0 || signalReceived) && (!signalReceived || signalAttempts>=2 && radio==1)
            && (signalAttempts==0 || radio==1) && bed >= 0 && bed <= 2 && bench >= 0 && bench <= 2 && radio >= 0 && radio <= 1
            && completed >= 0 && completed <= 3 && (!accepted || radio == 1 && completed < 3) && (completed == 0 || radio == 1); }
}

