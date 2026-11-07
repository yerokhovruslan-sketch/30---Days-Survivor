package com.example.survivor.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

enum LootProfile { GENERAL, FOOD, MEDICAL, ARMS, WORKSHOP }

/** One persistent condition is rolled for each location when a world is created. */
enum LocationState {
    OPEN("Открыто", "Вы можете сюда пройти. Отдельные помещения могут быть заперты."),
    LOOTED("Разграблено", "Обычные тайники почти пусты: шанс добычи ниже, но могут остаться следы, ключи и забытые тайники."),
    LOCKED("Заперто", "Главный вход закрыт. Нужен лом; после вскрытия доступ не пропадёт."),
    FLOODED("Затоплено", "Часть проходов залита. Можно потратить время на сухой обход или силы на воду."),
    INFESTED("Заражено", "Добыча почти не тронута, но заражённых больше и они ближе."),
    RAIDERS("Занято рейдерами", "Люди контролируют объект. Можно попытаться пройти тихо или вернуться подготовленным.");

    final String label, detail;
    LocationState(String label, String detail) { this.label = label; this.detail = detail; }
}

class Location implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /** A named place inside a location. Its state is saved together with the world. */
    static final class SearchPoint implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        final String name;
        final String description;
        final boolean locked;
        final boolean highInterest;
        private boolean opened;
        private boolean searched;

        SearchPoint(String name, String description, boolean locked, boolean highInterest) {
            this.name = name;
            this.description = description;
            this.locked = locked;
            this.highInterest = highInterest;
            this.opened = !locked;
        }
        boolean isLocked() { return locked && !opened; }
        boolean isSearched() { return searched; }
        void open() { opened = true; }
        void markSearched() { searched = true; }
    }

    private final String name;
    private final String description;
    private final int requiredLocation;
    private int difficulty = 1;
    private int searchedSites;
    private boolean nightOnly;
    private LootProfile lootProfile;
    private LocationState state;
    private boolean stateResolved;
    private boolean legacySchoolGateOpened;
    private boolean carefulEntry;
    // Old saves do not have this field; an empty list simply uses the old generic search flow.
    private List<SearchPoint> searchPoints = new ArrayList<>();
    public boolean isNightOnly() { return nightOnly; }
    void markNightOnly() { nightOnly = true; }

    public int getSearchCapacity() { return hasDetailedPoints() ? searchPoints.size() : 10 + difficulty * 5; }
    public int getRemainingSearches() {
        if (hasDetailedPoints()) {
            int remaining = 0;
            for (SearchPoint point : searchPoints) if (!point.isSearched()) remaining++;
            return remaining;
        }
        return Math.max(0, getSearchCapacity() - searchedSites);
    }
    private boolean hasForestActivities() {return java.util.Set.of("Лесная опушка","Лесная тропа","Охотничья избушка","Заброшенный кемпинг","Берег озера","Лодочная станция","Рыбацкий остров","Болотная тропа","Лесничество").contains(name);}
    public boolean isDepleted() { return !hasForestActivities() && !name.equals("Тихий двор") && !name.equals("Продуктовый магазин") && !name.equals("Жилой подъезд") && !name.equals("Крыши домов") && !name.equals("Школьный двор") && !name.equals("Школа") && !name.equals("Районная поликлиника") && !name.equals("Аптечный склад") && !name.equals("Городская больница") && getRemainingSearches() == 0; }
    public int getLootChance() { return getRemainingSearches() * 100 / getSearchCapacity(); }
    public String getSuppliesStatus() {
        if (name.equals("Продуктовый магазин")) return "торговый зал, касса и подсобные помещения";
        if (name.equals("Жилой подъезд")) return "вестибюль, лестница, квартиры и чердак";
        if (name.equals("Крыши домов")) return "чердачный выход, переход и техническая площадка";
        if (name.equals("Школьный двор")) return "6 точек";
        if (name.equals("Школа")) return "7 точек";
        if (isDepleted()) return "запасы исчерпаны";
        if (hasDetailedPoints()) return "точек обыска: " + getRemainingSearches() + "/" + getSearchCapacity();
        return "мест для поиска: " + getRemainingSearches() + "/" + getSearchCapacity()
                + ", шанс добычи: " + getLootChance() + "%";
    }
    public boolean searchSupplies(java.util.Random random) {
        return searchSupplies(random, 0);
    }
    public boolean searchSupplies(java.util.Random random, int bonus) {
        if (isDepleted()) return false;
        int chance = Math.max(0, Math.min(100, getLootChance() + bonus + stateLootModifier()));
        searchedSites++;
        return random.nextInt(100) < chance;
    }

    public Location(String name, String description, int requiredLocation) {
        this.name = name;
        this.description = description;
        this.requiredLocation = requiredLocation;
    }

    public String getName() { return name; }
    public LootProfile getLootProfile() {
        if (lootProfile != null) return lootProfile;
        String text = name.toLowerCase(java.util.Locale.ROOT);
        if (text.matches(".*(больниц|аптеч|аптек|поликлиник|скорой|госпитал|медицин).*")) return LootProfile.MEDICAL;
        if (text.matches(".*(продукт|кафе|столов|супермаркет).*")) return LootProfile.FOOD;
        if (text.matches(".*(полицей|казарм|военн|блокпост).*")) return LootProfile.ARMS;
        if (text.matches(".*(гараж|мастерск|строй|строит|лесопил|котельн).*")) return LootProfile.WORKSHOP;
        return LootProfile.GENERAL;
    }
    public String getDescription() { return description; }
    public int getRequiredLocation() { return requiredLocation; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    void setLootProfile(LootProfile profile) { lootProfile = profile; }
    boolean hasAssignedLootProfile() { return lootProfile != null; }
    void setState(LocationState value) {
        if (name.equals("Школьный двор") && state == LocationState.LOCKED && stateResolved && value == LocationState.OPEN) legacySchoolGateOpened=true;
        state = value;
    }
    boolean legacySchoolGateOpened(){return legacySchoolGateOpened;}
    boolean hasAssignedState() { return state != null; }
    LocationState getState() { return state == null ? LocationState.LOOTED : state; }
    String getStateLabel() { return getState().label; }
    String getStateDetail() { return getState().detail; }
    int getEncounterDifficulty() {
        LocationState value = getState();
        int modifier = value == LocationState.INFESTED && !carefulEntry ? 1
                : value == LocationState.RAIDERS && !carefulEntry ? 1 : 0;
        return Math.min(3, difficulty + modifier);
    }
    private int stateLootModifier() {
        return switch (getState()) {
            case OPEN -> 0;
            case LOOTED -> -35;
            case LOCKED -> stateResolved ? 8 : 0;
            case INFESTED -> 15;
            case RAIDERS -> 10;
            case FLOODED -> -8;
        };
    }
    public String getDifficultyName() {
        return difficulty == 1 ? "лёгкая" : difficulty == 2 ? "средняя" : "опасная";
    }

    void setSearchPoints(List<SearchPoint> points) {
        searchPoints = new ArrayList<>(points);
    }
    boolean hasDetailedPoints() { return searchPoints != null && !searchPoints.isEmpty(); }
    List<SearchPoint> getSearchPoints() {
        if (searchPoints == null) searchPoints = new ArrayList<>();
        return searchPoints;
    }
    boolean hasCrowbar(Fighter player) {
        return player != null && player.getInventory().stream().anyMatch(item ->
                item instanceof Weapons && ((Weapons) item).getStyle() == WeaponStyle.CROWBAR
                        || item.getName().toLowerCase(java.util.Locale.ROOT).contains("лом"));
    }

    /** Shows the rooms/areas, opens a locked one with a crowbar and returns the chosen unsearched point. */
    SearchPoint chooseSearchPoint(Fighter player, Scanner scanner) {
        if (!hasDetailedPoints()) return null;
        while (true) {
            GameIO.menu(name, "Точки обыска. Их состояние сохраняется после ухода из локации.");
            List<SearchPoint> points = getSearchPoints();
            boolean crowbar = hasCrowbar(player);
            for (int i = 0; i < points.size(); i++) {
                SearchPoint point = points.get(i);
                String status = point.isSearched() ? "Обыскано"
                        : point.isLocked() ? "Заперто · нужен лом"
                        : "Не обыскано";
                String detail = point.description + "\n" + status
                        + (point.highInterest && !point.isSearched() ? " · точка повышенного интереса" : "");
                GameIO.action(i + 1, point.name, detail, !point.isSearched() && (!point.isLocked() || crowbar));
            }
            Locations world=GameIO.world();WorldStories stories=GameIO.clock();
            MonsterDens.Den den=world==null||stories==null?null:stories.monsterDens().ensure(world,world.getNumber(this));
            if(den!=null&&den.accessible())GameIO.action(88,den.label(),den.cleared?"Оставленные трофеи":"Опасный бой · "+den.count+" заражённых");
            GameIO.action(0, "Вернуться домой");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return null;
            if(choice==88&&den!=null&&den.accessible()){
                if(!stories.monsterDens().visit(player,stories,world.getNumber(this),scanner,java.util.concurrent.ThreadLocalRandom.current()))return null;
                continue;
            }
            if (choice < 1 || choice > points.size()) continue;
            SearchPoint chosen = points.get(choice - 1);
            if (chosen.isSearched()) continue;
            if (chosen.isLocked()) {
                if (!crowbar) { GameIO.sound(GameSound.ERROR); continue; }
                chosen.open();
                GameIO.sound(GameSound.WOOD_DOOR);
                GameIO.out().println("Ломом вскрыта точка: " + chosen.name + ". Лом не расходуется.");
                if(!BreakIn.attack(player,scanner,java.util.concurrent.ThreadLocalRandom.current()))return null;
            }
            if (name.equals("Тихий двор") && chosen.name.equals("Подвальный вход")) {
                GameIO.storyMenu("Подвальный вход", "Из-за двери иногда слышится движение. Вскрытая дверь останется открытой; можно вернуться позже.");
                GameIO.action(1, "Войти и осмотреть подвал");
                GameIO.action(0, "Отойти от двери");
                if (SurvivalSystems.choice(scanner) != 1) continue;
            }
            return chosen;
        }
    }

    ItemInfo searchPoint(SearchPoint point, java.util.Random random, int bonus) {
        if (point == null || point.isSearched() || point.isLocked()) return null;
        point.markSearched();
        searchedSites++;
        if (name.equals("Тихий двор") && point.name.equals("Мусорные контейнеры")) {
            GameIO.out().println("Ты отодвигаешь обломок двери и вытаскиваешь лом. Он пригодится для закрытых помещений.");
            return Shop.createItem(27);
        }
        int chance = Math.min(95, 62 + difficulty * 8 + bonus + (point.highInterest ? 10 : 0) + stateLootModifier());
        if (random.nextInt(100) >= chance) {
            GameIO.out().println("Пыль, вскрытые ящики и ничего полезного. " + point.name + " уже отмечено как обысканное.");
            return null;
        }
        return Item.createLootForLocation(this, random);
    }
    void consumePoint(SearchPoint point) {
        if (point != null && !point.isSearched()) {
            point.markSearched();
            searchedSites++;
        }
    }

    /** Applies the location-wide state before the player may start searching. */
    boolean prepareForVisit(Fighter player, WorldStories world, Scanner scanner, java.util.Random random) {
        carefulEntry = false;
        LocationState value = getState();
        if (value == LocationState.OPEN) return true;
        if (value == LocationState.LOOTED) {
            GameIO.out().println("Состояние: разграблено. Обычная добыча здесь редка, но не всё ценное лежит на виду.");
            return true;
        }
        if (value == LocationState.LOCKED) {
            if (stateResolved) {
                GameIO.out().println("Состояние: вход уже вскрыт ломом.");
                return true;
            }
            GameIO.storyMenu(name + " · заперто", "Главный вход закрыт. Лом не расходуется.");
            GameIO.card("Запертый объект", getStateDetail());
            GameIO.action(1, "Вскрыть вход ломом", "10 минут · лом не расходуется", hasCrowbar(player));
            GameIO.action(0, "Вернуться домой");
            if (SurvivalSystems.choice(scanner) != 1 || !hasCrowbar(player)) return false;
            stateResolved = true;
            world.advanceMinutes(10);
            GameIO.sound(GameSound.WOOD_DOOR);
            GameIO.out().println("Вход вскрыт. В закрытых объектах шанс полезной добычи немного выше.");
            return BreakIn.attack(player,scanner,random);
        }
        if (value == LocationState.FLOODED) {
            GameIO.storyMenu(name + " · затоплено", "Вода перекрыла часть проходов.");
            GameIO.card("Затопленный объект", getStateDetail());
            GameIO.action(1, "Обойти по сухому", "20 минут · безопаснее");
            GameIO.action(2, "Пробраться по воде", "5 минут · −12 выносливости");
            GameIO.action(0, "Вернуться домой");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return false;
            if (choice == 1) { world.advanceMinutes(20); player.increaseHunger(1); player.increaseSleepiness(1); }
            else if (choice == 2) player.spendAvailableStamina(12);
            else return false;
            stateResolved = true;
            return true;
        }
        if (value == LocationState.INFESTED) {
            GameIO.out().println("Здесь замечены заражённые. Во время исследования возможна встреча.");
            return true;
        }
        // Raiders are not removed by one dialogue: a successful quiet approach only helps this visit.
        GameIO.storyMenu(name + " · рейдеры", "У входа замечены вооружённые люди. Прямой бой может начаться во время поиска.");
        int chance = SurvivalSystems.bypassChance(player, world, player.getOverloadPercent());
        GameIO.action(1, "Попробовать пройти тихо", "Шанс: " + chance + "%");
        GameIO.action(2, "Вернуться домой");
        int choice = SurvivalSystems.choice(scanner);
        if (choice == 2 || choice == 0) return false;
        if (choice != 1) return false;
        carefulEntry = random.nextInt(100) < chance;
        GameIO.out().println(carefulEntry ? "Джон обошёл дозор. Этот визит начнётся тише." : "Рейдеры заметили движение. Поиск будет опаснее.");
        return true;
    }
}

class Locations implements java.io.Serializable {
    static final int EVACUATION_LOCATION = 47;
    private static final long serialVersionUID = 1L;
    private final String[] groupNames = {
            "Жилые кварталы", "Городские службы и убежища", "Лес и озеро",
            "Карьер и горы", "Дороги и железная дорога", "Промышленная и военная зона"
    };
    private final int[][] groups = {
            {1, 4, 5, 6, 7, 8, 9},
            {10, 11, 12, 13, 14, 15, 16, 17, 18},
            {2, 19, 20, 21, 22, 23, 24, 25, 26},
            {27, 28, 29, 30, 31, 32},
            {3, 33, 34, 35, 36, 37, 38, 39, 40},
            {41, 42, 43, 44, 45, 46, 47, 48, 49, 50}
    };
    private final List<Location> locations = new ArrayList<>();
    // Номера посещённых локаций начинаются с 1; 0 означает отсутствие условия.
    private final Set<Integer> visited = new HashSet<>();
    private WorldStories stories = new WorldStories();
    public WorldStories getStories() {
        if (stories == null) stories = new WorldStories();
        return stories;
    }
    public int getNumber(Location location) {
        int index = locations.indexOf(location);
        if (index < 0) throw new IllegalArgumentException("Неизвестная локация");
        return index + 1;
    }

    public Locations() {
        add("Тихий двор", "Качели скрипят между опустевшими домами. Здесь начинается путь в жилой район.", 0);
        add("Лесная опушка", "Среди сосен видны старые тропы и следы брошенного лагеря.", 0);
        add("Автобусная остановка", "На дороге застыли автобусы. За ними начинается промышленная окраина.", 0);
        add("Продуктовый магазин", "За выбитой витриной стоят пустые стеллажи и запертая кладовая.", 1);
        add("Жилой подъезд", "На лестнице разбросаны вещи жильцов, а наверху слышен скрип дверей.", 4);
        add("Крыши домов", "С крыш открывается вид на квартал. Между зданиями лежат шаткие мостки.", 5);
        add("Школьный двор", "Заросший стадион окружён покосившимся забором.", 6);
        add("Школа", "В тёмных классах остались парты, учебники и следы временного убежища.", 7);
        add("Школьная котельная", "Под школой тянутся трубы и узкие проходы между холодными котлами.", 8);
        add("Районная поликлиника", "Коридоры завалены каталками. На стенах сохранились указатели кабинетов.", 9);
        add("Аптечный склад", "В прохладных комнатах стоят шкафы с потускневшими медицинскими этикетками.", 10);
        add("Городская больница", "Огромный корпус погружён в тишину, которую нарушает эхо шагов.", 11);
        add("Станция скорой помощи", "Во дворе больницы стоят машины с открытыми задними дверями.", 12);
        add("Пожарная часть", "В гараже остались тяжёлые машины, рукава и шкафы со снаряжением.", 13);
        add("Полицейский участок", "За решётками окон видны перевёрнутые столы и разбитая рация.", 14);
        add("Архив участка", "Пыльные папки хранят сведения о последних днях эвакуации.", 15);
        add("Ратуша", "В опустевшем зале заседаний висит карта городских убежищ.", 16);
        add("Городское убежище", "За массивной дверью сохранились нары и следы поспешного ухода.", 17);
        add("Лесная тропа", "Тропа уходит от опушки в густой лес, где трудно заметить движение.", 2);
        add("Охотничья избушка", "У маленького сруба сложены дрова, а окна закрыты изнутри.", 19);
        add("Заброшенный кемпинг", "Между деревьями висят рваные палатки и забытые гамаки.", 20);
        add("Берег озера", "Вода скрывает затопленные лодки. На песке тянутся свежие следы.", 21);
        add("Лодочная станция", "На причале скрипят доски, а в сарае пахнет сыростью и топливом.", 22);
        add("Рыбацкий остров", "На небольшом острове стоит домик с обрушившейся верандой.", 23);
        add("Болотная тропа", "Кочки ведут через туманное болото к старой наблюдательной вышке.", 24);
        add("Лесничество", "У здания лесников сохранились карты дорог и выцветшие предупреждения.", 25);
        add("Лесопилка", "Под навесами лежат штабеля досок и ржавеют огромные пилы.", 26);
        add("Каменный карьер", "Дорога спускается между каменных стен к неподвижным экскаваторам.", 27);
        add("Шахтёрский посёлок", "Пустые бараки стоят у склона. Издалека виден вход в шахту.", 28);
        add("Старая шахта", "Рельсы исчезают в темноте, а с потолка непрерывно капает вода.", 29);
        add("Подземный склад", "За шахтным ответвлением спрятаны ящики и старое оборудование.", 30);
        add("Горная станция связи", "На вершине стоит антенна, соединённая проводами с бетонным домиком.", 31);
        add("Автозаправка", "Под навесом застыли машины, магазин при станции давно разграблен.", 3);
        add("Придорожное кафе", "В кухне темно, а в зале ещё накрыты столы для последних посетителей.", 33);
        add("Гаражный кооператив", "Ряды металлических ворот скрывают мастерские и брошенные автомобили.", 34);
        add("Автомастерская", "Машины на подъёмниках покрыты пылью. На полу рассыпаны инструменты.", 35);
        add("Грузовой терминал", "Между контейнерами образовался лабиринт узких проходов.", 36);
        add("Железнодорожная платформа", "Вдоль платформы стоит состав, который так и не отправился в путь.", 37);
        add("Вагонное депо", "В огромном ангаре слышно, как ветер раскачивает незакреплённые двери.", 38);
        add("Товарная станция", "Стрелки и пути сходятся у диспетчерской с разбитыми окнами.", 39);
        add("Химический завод", "Пустые резервуары возвышаются над сетью труб и служебных лестниц.", 40);
        add("Очистные сооружения", "Мостки проходят над тёмной водой, вокруг гудят расшатанные ограждения.", 41);
        add("Электроподстанция", "Между трансформаторами лежат оборванные кабели и обломки изоляторов.", 42);
        add("Промышленный бункер", "Под заводом скрывается укреплённый пункт управления с тяжёлыми шлюзами.", 43);
        add("Военный блокпост", "Мешки с песком перекрывают дорогу. Брошенная техника заросла травой.", 44);
        add("Казармы", "Длинные ряды коек и пустые шкафчики напоминают о гарнизоне.", 45);
        add("Военный склад", "За двойным ограждением стоят ангары с опечатанными ящиками.", 46);
        add("Полевой госпиталь", "Под выцветшими палатками сохранились операционные столы и генераторы.", 47);
        add("Исследовательский комплекс", "Коридоры разделены защитными дверями, на стенах знаки биологической опасности.", 48);
        add("Центральная лаборатория", "В глубине комплекса мерцают аварийные лампы над запертыми лабораторными боксами.", 49);
    }

    private void add(String name, String description, int requiredLocation) {
        Location location = new Location(name, description, requiredLocation);
        int depth = 0;
        int previous = requiredLocation;
        while (previous != 0) {
            depth++;
            previous = locations.get(previous - 1).getRequiredLocation();
        }
        location.setDifficulty(depth < 3 ? 1 : depth < 7 ? 2 : 3);
        locations.add(location);
        location.setLootProfile(profileFor(locations.size()));
        location.setState(randomStateFor(locations.size(), java.util.concurrent.ThreadLocalRandom.current()));
        location.setSearchPoints(detailedPoints(locations.size()));
    }

    private static LootProfile profileFor(int number) {
        return switch (number) {
            case 4, 34, 52 -> LootProfile.FOOD;
            case 10, 11, 12, 13, 48, 49, 50, 53 -> LootProfile.MEDICAL;
            case 15, 45, 46, 47 -> LootProfile.ARMS;
            // Forest, mining, transport and industrial objects deliberately feed the workbench economy.
            case 2, 3, 9, 14, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
                    33, 35, 36, 37, 39, 41, 42, 43, 44, 51 -> LootProfile.WORKSHOP;
            default -> LootProfile.GENERAL;
        };
    }

    private static LocationState randomStateFor(int number, java.util.Random random) {
        if (number == 1 || number == 4 || number == 5 || number == 6 || number == 7 || number == 8 || number == 10 || number == 11 || number == 12) return LocationState.OPEN;
        List<LocationState> options = new ArrayList<>();
        options.add(LocationState.LOOTED);
        options.add(LocationState.INFESTED);
        // Story gates must never require a crowbar merely to keep the campaign moving.
        if (number != 15 && number != 16 && number != 47) options.add(LocationState.LOCKED);
        if (number == 22 || number == 23 || number == 24 || number == 25 || number == 31 || number == 42) options.add(LocationState.FLOODED);
        if (number == 4 || number == 33 || number == 34 || number == 35 || number == 36 || number == 37 || number == 38
                || number == 39 || number == 40 || number == 41 || number == 44 || number == 45 || number == 46 || number == 47) {
            options.add(LocationState.RAIDERS);
        }
        return options.get(random.nextInt(options.size()));
    }

    private static Location.SearchPoint point(String name, String description) {
        return new Location.SearchPoint(name, description, false, false);
    }
    private static Location.SearchPoint locked(String name, String description) {
        return new Location.SearchPoint(name, description, true, true);
    }
    /** First three districts get hand-authored rooms instead of anonymous repeated searches. */
    private static List<Location.SearchPoint> detailedPoints(int number) {
        return switch (number) {
            // 1. Residential quarter.
            case 1 -> List.of(point("Детская площадка", "Рюкзак ребёнка и коробка под скамейкой; рядом скрипят качели."),
                    point("Припаркованные автомобили", "Салоны, багажники и красная машина у площадки."),
                    point("Мусорные контейнеры", "Под обломком двери зажат лом; завал можно отодвинуть руками."),
                    locked("Подвальный вход", "Металлическая дверь; внутри инструментальный шкаф."),
                    point("Гараж дворника", "Приоткрытая дверь, инструменты и план служебного прохода."));
            case 3 -> List.of(point("Салон автобуса", "Сиденья, сумки пассажиров и забытые куртки."),
                    point("Киоск у остановки", "Разбитая витрина, вода и мелкие бытовые припасы."),
                    locked("Служебный автобус", "Запертая дверь. Внутри могли остаться инструменты водителя."));
            case 4 -> List.of(point("Торговый зал", "Полки почти пусты, но за ними могли остаться забытые банки."),
                    locked("Запертая кладовая", "Служебная дверь. Повышенный шанс еды и бытовых припасов — не гарантия."),
                    point("Касса", "Разбитый терминал и выдвижные ящики под прилавком."),
                    locked("Подсобка", "Тесное помещение за холодильниками; внутри можно встретить не только добычу."));
            case 5 -> List.of(point("Квартира 12 · кухня", "Шкафы, холодильник и стол с засохшей посудой."),
                    locked("Квартира 18 · ванная", "Запертая дверь. Внутри могут быть лекарства и бытовые вещи."),
                    point("Почтовые ящики", "Письма, квитанции, ключи и чужие фамилии."),
                    locked("Квартира 23 · кладовка", "За крепкой дверью слышится только тишина. Точка повышенного интереса."),
                    point("Лестничная площадка", "На ступенях разбросаны вещи жильцов; здесь проще заметить опасность."));
            case 6 -> List.of(point("Технический этаж", "Провода, антенны и старые вентиляционные шкафы."),
                    point("Переход между крышами", "Шаткий настил над двором. Можно найти следы других выживших."),
                    locked("Голубятня", "Маленькая запертая постройка. В ней мог остаться тайник."));
            case 7 -> List.of(point("Детская площадка", "Ржавые горки и игрушки в мокрой траве."),
                    point("Стадион", "Под трибунами лежат сумки и спортивный инвентарь."),
                    locked("Склад инвентаря", "Запертое помещение у спортплощадки."));
            case 8 -> List.of(point("Столовая", "Кухня, кладовая и перевёрнутые столы."),
                    point("Медкабинет", "Шкафы с перевязочными материалами и журнал посещений."),
                    locked("Учительская", "Запертая дверь. Документы и ключи могут оказаться полезнее еды."),
                    point("Спортзал", "Матрасы, раздевалки и следы временного убежища."));
            case 9 -> List.of(point("Угольный склад", "Мешки, щепки и тяжёлые инструменты."),
                    locked("Щитовая", "За металлической дверью — предохранители, провода и риск короткого замыкания."),
                    point("Технический тоннель", "Узкий проход между трубами. Здесь легко пропустить движение."));
            // 2. City services and shelters.
            case 10 -> List.of(point("Регистратура", "Карточки пациентов и разбросанные ключи."),
                    point("Процедурный кабинет", "Стеклянные шкафы и перевязочные столы."),
                    locked("Служебный шкаф", "Запертый шкаф за регистратурой. Повышенный шанс медикаментов."),
                    point("Подвал", "Сырой архив и старые коммуникации."));
            case 11 -> List.of(point("Приёмная и журнал поставок", "Рабочее место, связка ключей и журнал движения лекарств."),
                    point("Основной склад", "Передние стеллажи доступны, в глубине слышны шаги."),
                    locked("Холодильный отсек", "Запертая дверь и партия М-24; электричества давно нет."),
                    point("Зона погрузки", "Грузовик, поддоны и следы последних организованных поставок."));
            case 12 -> List.of(point("Приёмное отделение", "Носилки, регистрационные документы и небольшой медицинский запас."),
                    point("Перевязочная", "Бинты у входа и медицинские шкафы за ширмой."),
                    point("Палаты второго этажа", "Частично перекрытый коридор и личные записи пациентов."),
                    locked("Диспетчерская эвакуации", "Карты, таблица перевозок и записи о маршрутах."));
            case 13 -> List.of(point("Гараж скорой", "Машины, носилки и ящики с расходниками."),
                    point("Диспетчерская", "Рации, журналы вызовов и карта района."),
                    locked("Склад медикаментов", "Служебное помещение с повышенным шансом медицинской добычи."));
            case 14 -> List.of(point("Гараж", "Пожарные машины, рукава и шкафчики смены."),
                    point("Раздевалка", "Личные вещи спасателей и защитная одежда."),
                    locked("Склад снаряжения", "Тяжёлая дверь. Возможны инструменты и пожарный топор."));
            case 15 -> List.of(point("Дежурная часть", "Перевёрнутые столы, рация и журнал последних вызовов."),
                    locked("Оружейная", "Нужен лом. Внутри может оказаться оружие, а может — только пустые шкафы."),
                    point("Камеры", "Ключи, личные вещи задержанных и риск встречи."),
                    locked("Кабинет начальника", "Документы, карта города и закрытый сейф."));
            case 16 -> List.of(point("Стеллажи эвакуации", "Папки о маршрутах, пропусках и людях, которые должны были уехать."),
                    locked("Закрытая картотека", "Служебный шкаф с архивными документами."),
                    point("Кабинет архивиста", "Стол, фонарик и журналы выдачи документов."));
            case 17 -> List.of(point("Зал заседаний", "Карта убежищ и папки с решениями последних дней."),
                    locked("Кабинет мэра", "Запертый кабинет с документами и возможным тайником."),
                    point("Подземный вход", "Служебный коридор к старым коммуникациям."));
            case 18 -> List.of(point("Жилой блок", "Нары, шкафчики и оставленные вещи."),
                    point("Склад пайков", "Полки с пустыми коробками и редкими остатками."),
                    locked("Генераторная", "Запертая дверь; здесь нужны инструменты, но можно найти электронику."),
                    point("Диспетчерская", "Журналы дежурств и записи о последних сменах."));
            // 3. Forest and lake.
            case 2 -> List.of(point("Брошенный лагерь", "Потухший костёр, палатка и следы у тропы."),
                    point("Старая тропа", "В кустах легко пропустить тайник или опасность."),
                    locked("Охотничий ящик", "Небольшой ящик на цепи. Нужен лом."));
            case 19 -> List.of(point("Рюкзак туриста", "Промокший рюкзак у поваленного дерева."),
                    point("Охотничья вышка", "Сверху можно увидеть дорогу и оставить пометки."),
                    locked("Тайник под корнями", "Спрятанный ящик. Может быть пустым — это всё ещё конец света."));
            case 20 -> List.of(point("Кухня домика", "Печь, стол и ржавые банки."),
                    locked("Оружейный шкаф", "Запертый шкаф хозяина. Возможны патроны или только старый хлам."),
                    point("Чердак", "Коробки, одеяла и старые карты."),
                    point("Сарай", "Дрова, верёвки и инструменты."));
            case 21 -> List.of(point("Палатки", "Рваная ткань, спальники и мелкие вещи туристов."),
                    point("Общий костёр", "Котелок, пустые банки, остатки топлива."),
                    locked("Склад смотрителя", "Запертая бытовка с походным снаряжением."));
            case 22 -> List.of(point("Береговая линия", "На песке остаются следы и прибитые водой вещи."),
                    point("Затопленная лодка", "Можно найти снасти и металл, если рискнуть намочить ноги."),
                    locked("Рыбацкий ящик", "Старый ящик у причала; лом открывает его без расхода."));
            case 23 -> List.of(point("Причал", "Снасти, тросы и мокрые доски."),
                    point("Сарай лодочника", "Пахнет бензином и сыростью."),
                    locked("Топливный шкаф", "Запертый металлический шкаф с редкими компонентами."));
            case 24 -> List.of(point("Веранда домика", "Мокрые доски и забытая посуда."),
                    point("Рыбацкая кладовка", "Сети, банки и снасти."),
                    locked("Подпол", "Тесный вход под домом. Здесь мог быть тайник."));
            case 25 -> List.of(point("Наблюдательная вышка", "Сверху видны безопасные и опасные участки болота."),
                    point("Брошенная лодка", "Старая лодка застряла в камышах."),
                    locked("Шкаф смотрителя", "Запертый ящик с картами и снаряжением."));
            case 26 -> List.of(point("Кабинет лесника", "Карты троп и журналы обходов."),
                    point("Склад снастей", "Верёвки, инструменты и одежда."),
                    locked("Оружейный сейф", "Запертый сейф. Добыча не гарантирована, но шанс выше обычного."));
            // 4. Quarry and mountains.
            case 27 -> List.of(point("Навес с досками", "Штабеля древесины, гвозди и сломанные поддоны."),
                    point("Мастерская пилорамы", "Верстаки, ремни и тяжёлые инструменты."),
                    locked("Склад инструмента", "Запертое помещение. Здесь могут быть материалы для убежища."),
                    point("Кабина оператора", "Журналы смен и старая схема лесных дорог."));
            case 28 -> List.of(point("Бытовка рабочих", "Шкафчики, термосы и личные вещи смены."),
                    point("Экскаватор", "В кабине можно найти инструменты или ключи от техники."),
                    locked("Склад взрывных работ", "Тяжёлая запертая дверь. Внутри опасно даже без заражённых."),
                    point("Погрузочная площадка", "Металлолом, тросы и детали среди камней."));
            case 29 -> List.of(point("Барак №1", "Общая кухня и тесные комнаты шахтёров."),
                    point("Медпункт посёлка", "Старый шкаф с перевязочными материалами."),
                    locked("Кладовая старосты", "Запертая кладовая с хозяйственными припасами."),
                    point("Гараж", "Канистры, инструменты и разобранная техника."));
            case 30 -> List.of(point("Вагонетки", "Ржавые вагонетки и следы недавнего прохода."),
                    point("Комната диспетчера", "План тоннелей и журналы спуска смен."),
                    locked("Шахтный склад", "Запертые ящики за решёткой. Повышенный шанс металла и деталей."),
                    point("Боковой штрек", "Узкий тёмный ход. Здесь трудно заметить врага первым."));
            case 31 -> List.of(point("Ящики у ответвления", "Тара, кабели и старое горное оборудование."),
                    locked("Технический контейнер", "Заперт. Внутри могут быть редкие комплектующие."),
                    point("Комната смены", "Сухая одежда, аптечка и доска объявлений."),
                    point("Дренажный тоннель", "Вода по щиколотку и следы, ведущие глубже."));
            case 32 -> List.of(point("Антенная площадка", "Оборванные провода и обзор на весь район."),
                    point("Домик смотрителя", "Термоодеяло, карта маршрутов и бытовые припасы."),
                    locked("Щитовая связи", "Запертая щитовая. Возможны провода, предохранители и электроника."),
                    point("Склад аккумуляторов", "Пустые стеллажи и тяжёлые батарейные корпуса."));
            // 5. Roads and railway.
            case 33 -> List.of(point("Магазин заправки", "Полки с водой, снеками и дорожной мелочью."),
                    point("Гараж", "Домкраты, масло и простые инструменты."),
                    locked("Служебный склад", "Запертый склад за кассой. Небольшой шанс редких запчастей."),
                    point("Колонки и цистерны", "Опасная зона: шум и огонь здесь плохая идея."));
            case 34 -> List.of(point("Обеденный зал", "Остывшие столы, сумки посетителей и касса."),
                    point("Кухня", "Консервы, посуда и кухонные ножи."),
                    locked("Холодильная камера", "Запертая камера с шансом на еду, если её не успели разграбить."));
            case 35 -> List.of(point("Гаражный бокс", "Верстак, домкрат и разобранная машина."),
                    locked("Металлический гараж", "Тяжёлые ворота под лом. Внутри может быть как клад, так и пустой кузов."),
                    point("Сторожка", "Рация, ключи и журнал кооператива."),
                    point("Яма обслуживания", "Инструменты и детали под машиной."));
            case 36 -> List.of(point("Подъёмники", "Разобранные машины, ключи и детали подвески."),
                    point("Склад запчастей", "Фильтры, ремни и металлические пластины."),
                    locked("Кабинет мастера", "Запертый офис с документами и инструментами."),
                    point("Разбитые автомобили", "Можно найти аккумулятор или только очередную пустую бардачную легенду."));
            case 37 -> List.of(point("Контейнеры", "Узкие проходы между грузом. Здесь легко попасть в засаду."),
                    point("Диспетчерская", "Накладные, маршруты и ключи от техники."),
                    locked("Опечатанный контейнер", "Нужен лом. Маркировка не обещает, что внутри действительно ценное."));
            case 38 -> List.of(point("Кассы", "Билеты, мелочь и забытые сумки."),
                    point("Камеры хранения", "Часть ячеек вскрыта, часть ещё заперта."),
                    point("Диспетчерская", "Расписания, служебные бумаги и рация."),
                    locked("Товарный вагон", "Запертая дверь вагона. Тип груза определит риск и награду."));
            case 39 -> List.of(point("Ремонтный ангар", "Инструменты, сварочное оборудование и детали поездов."),
                    point("Кабина локомотива", "Панель управления, карта путей и личные вещи машиниста."),
                    locked("Склад депо", "Запертый склад с комплектующими и рабочей одеждой."));
            case 40 -> List.of(point("Вагон с медикаментами", "Ящики с маркировкой. Шанс лекарств выше, но не стопроцентный."),
                    locked("Вагон-рефрижератор", "Тяжёлая дверь под лом; внутри могли сохраниться продукты."),
                    point("Кабина машиниста", "Документы, карта путей и сигнальные принадлежности."),
                    locked("Закрытый контейнер", "Неизвестный груз. Повышенный интерес не равен гарантированной выгоде."));
            // 6. Industrial and military zones.
            case 41 -> List.of(point("Цех реакторов", "Трубы, клапаны и защитные костюмы."),
                    point("Склад реагентов", "Разбитые контейнеры и редкие промышленные материалы."),
                    locked("Серверная", "Запертая комната управления. Электроника ценна, но шум привлечёт внимание."),
                    point("Котельная", "Инструменты, металл и опасные проходы между трубами."));
            case 42 -> List.of(point("Насосная", "Моторы, фильтры и мокрые мостки."),
                    point("Склад фильтров", "Расходники, ткань и редкие детали."),
                    locked("Щитовая", "Запертая электрощитовая с проводами и предохранителями."));
            case 43 -> List.of(point("Трансформаторы", "Оборванные кабели и металлические корпуса."),
                    point("Пульт управления", "Схемы питания и журналы аварий."),
                    locked("Кабельный тоннель", "Запертый служебный вход. Внутри часто лежат провода и электроника."));
            case 44 -> List.of(point("Пункт управления", "Карты производства, терминалы и служебные ключи."),
                    point("Шлюз", "Тяжёлые двери и следы поспешной эвакуации."),
                    locked("Аварийный склад", "Запертый склад: инструменты, пайки и компоненты для убежища."),
                    point("Генераторная", "Старый генератор и детали, нужные для будущих улучшений."));
            case 45 -> List.of(point("Песчаные мешки", "Брошенные позиции и пустые магазины оружия."),
                    point("КПП", "Журналы пропуска и рация."),
                    locked("Оружейный ящик", "Под лом. Возможны боеприпасы или пустая тара."),
                    point("Брошенная техника", "Металл, инструменты и редкие детали."));
            case 46 -> List.of(point("Казарменный блок", "Койки, шкафчики и личные вещи солдат."),
                    point("Столовая", "Пайки, посуда и складские ведомости."),
                    locked("Комната сержанта", "Запертая комната с ключами, картами и снаряжением."),
                    point("Спортзал", "Матрасы, шкафы и следы временного размещения людей."));
            case 47 -> List.of(point("Ангар", "Опечатанные ящики и военная тара."),
                    locked("Склад боеприпасов", "Тяжёлая дверь. Лом откроет её, но шум может дорого обойтись."),
                    point("Кабинет снабжения", "Ведомости поставок, карты и ключи от секций."),
                    locked("Подвалы выезда", "Следы техники и закрытые служебные помещения."));
            case 48 -> List.of(point("Операционная палатка", "Инструменты, медикаменты и аварийное освещение."),
                    point("Гараж санитарных машин", "Носилки, аптечки и детали транспорта."),
                    locked("Холодильный контейнер", "Запертый контейнер с шансом редких лекарств."));
            case 49 -> List.of(point("Внешний пост", "Пропуска, защитные маски и журналы смен."),
                    point("Лабораторный коридор", "Кабинеты за защитными дверями и разбитые колбы."),
                    locked("Биозащитный бокс", "Тяжёлая дверь. Внутри могут быть редкие медицинские компоненты."),
                    point("Склад образцов", "Контейнеры с маркировкой и выключенные холодильники."));
            case 50 -> List.of(point("Главная лаборатория", "Приборы, документы и мерцающие аварийные лампы."),
                    point("Серверная", "Оборудование, носители данных и электроника."),
                    locked("Запертый бокс", "Служебная дверь под лом. Риск и ценность здесь одинаково высоки."),
                    point("Кабинет руководителя", "Записки, карта комплекса и личный сейф."));
            // 7. Night-only routes.
            case 51 -> List.of(point("Техническая галерея", "Ночной обход между машинами и ржавыми трубами."),
                    locked("Ночной склад", "Запертая секция с инструментами. Ночью здесь особенно опасно."));
            case 52 -> List.of(point("Погрузочная рампа", "Ящики, тележки и редкие остатки еды."),
                    locked("Холодный склад", "Ночной доступ к запертому складу продуктов."));
            case 53 -> List.of(point("Подземный вход", "Следы когтей и тёмный спуск к медпункту."),
                    locked("Медицинский шкаф", "Заперт, но внутри могут уцелеть лекарства."));
            default -> List.of();
        };
    }

    int explorationStage(int number) {
        ensureNightLocations();
        if(number < 1 || number > locations.size()) return 1;
        int depth=1, current=number;
        while (locations.get(current-1).getRequiredLocation()!=0 && depth<50) {
            current=locations.get(current-1).getRequiredLocation();depth++;
        }
        if(number==2 || number==3) depth=2;
        return Math.max(depth,locations.get(number-1).isNightOnly()?6:1);
    }

    public boolean isAvailable(int number) {
        ensureNightLocations();
        ensureDetailedPoints();
        if (number < 1 || number > locations.size()) return false;

        if (number == 6) return visited.contains(6) || getStories().apartment().hatchOpen();
        if (number == 7) return visited.contains(7) || visited.contains(6) || visited.contains(5);
        if (number == 8) return visited.contains(8) || getStories().schoolyard().schoolEntranceOpen();
        int required = locations.get(number - 1).getRequiredLocation();
        return (required == 0 || visited.contains(required)) && (!locations.get(number - 1).isNightOnly() || getStories().isNight());
    }

    private int searchedCount(int number) {
        int count = 0;
        for (Location.SearchPoint point : locations.get(number - 1).getSearchPoints())
            if (point.isSearched()) count++;
        return count;
    }
    private boolean searchedPoint(int number, String name) {
        for (Location.SearchPoint point : locations.get(number - 1).getSearchPoints())
            if (point.name.equals(name) && point.isSearched()) return true;
        return false;
    }

    private void ensureNightLocations() {
        if (locations.size() != 50) return;
        add("Ночная автомастерская", "Днём проход перекрыт. Ночью открывается обход через техническую галерею.", 1);
        add("Ночной продуктовый склад", "Сумеречный проход к запасам под лесным распределительным пунктом.", 2);
        add("Ночной медицинский пункт", "Подземный вход доступен только под покровом ночи. На стенах глубокие следы когтей.", 3);
        for (int i = 50; i < 53; i++) { locations.get(i).markNightOnly(); locations.get(i).setDifficulty(3); }
    }
    /** Adds the new room layout to saves created before this update, without resetting any old loot progress. */
    private void ensureDetailedPoints() {
        int limit = Math.min(53, locations.size());
        for (int number = 1; number <= limit; number++) {
            Location location = locations.get(number - 1);
            if (!location.hasAssignedLootProfile()) location.setLootProfile(profileFor(number));
            if (number == 1 || number == 4 || number == 5 || number == 6 || number == 7 || number == 8 || number == 10 || number == 11 || number == 12) location.setState(LocationState.OPEN);
            else if (!location.hasAssignedState()) location.setState(randomStateFor(number, java.util.concurrent.ThreadLocalRandom.current()));
            List<Location.SearchPoint> expected = detailedPoints(number);
            if (number == 1 || number == 11 || number == 12) {
                List<Location.SearchPoint> old = location.getSearchPoints();
                boolean current = old.size() == expected.size();
                if (current) for (int i = 0; i < old.size(); i++)
                    if (!old.get(i).name.equals(expected.get(i).name)) { current = false; break; }
                if (!current) {
                    for (Location.SearchPoint fresh : expected) for (Location.SearchPoint previous : old)
                        if ((fresh.name.equals(previous.name) || number==11 && (fresh.name.equals("Холодильный отсек") && previous.name.equals("Холодильная камера") || fresh.name.equals("Приёмная и журнал поставок") && previous.name.equals("Бухгалтерия")) || number==12 && (fresh.name.equals("Перевязочная") && previous.name.equals("Операционная") || fresh.name.equals("Палаты второго этажа") && previous.name.equals("Морг") || fresh.name.equals("Диспетчерская эвакуации") && previous.name.equals("Аптечный блок"))) && previous.isSearched()) fresh.markSearched();
                    location.setSearchPoints(expected);
                }
            } else if (!location.hasDetailedPoints() && !expected.isEmpty()) location.setSearchPoints(expected);
        }
    }
    public int getGroupCount() { return groups.length + 1; }
    public String getGroupName(int group) { return group == groups.length ? "Ночные маршруты" : groupNames[group]; }
    public int[] getGroupLocations(int group) { return group == groups.length ? new int[] {51, 52, 53} : groups[group].clone(); }
    public Location getLocation(int number) { ensureNightLocations(); ensureDetailedPoints(); return locations.get(number - 1); }
    public boolean isVisited(int number) { return visited.contains(number); }
    public void visit(int number) {
        if (!isAvailable(number)) throw new IllegalArgumentException("Локация закрыта");
        visited.add(number);
    }

    public Location choose(Scanner scanner, SurvivorInfo survivor) {
        while (true) {
            Survivor.showHealth(survivor);
            GameIO.menu("Карта локаций", "Выберите открытую локацию.");
            for (int number=1;number<=53;number++) {
                Location place=getLocation(number);
                boolean available=isAvailable(number), seen=isVisited(number);
                if(!available&&!seen)continue;
                String detail=(available?"Доступна":"Сейчас закрыта")+" · "+(seen?"Посещена":"Не посещена")
                    +"\n"+place.getStateLabel()+" · "+place.getSuppliesStatus();
                if(place.isNightOnly()&&!getStories().isNight())detail+="\nОткрыта только ночью: 20:00–06:00";
                GameIO.illustratedAction(number,place.getName(),detail,"map_point_"+number);
            }
            GameIO.action(100,"Осмотреть тело");GameIO.action(0,"В главное меню");
            int number=readChoice(scanner);
            if(number==0)return null;
            if(number==100){if(survivor instanceof Fighter)Survivor.inspectBody((Fighter)survivor,scanner);continue;}
            if(number<1||number>53||!isAvailable(number)&&!isVisited(number))continue;
            Location location=getLocation(number);
            if(LocationPreview.show(this,number,survivor,scanner))return location;
        }
    }

    private int readChoice(Scanner scanner) {
        while (scanner.hasNext()) {
            if (scanner.hasNextInt()) return scanner.nextInt();
            scanner.next();
            GameIO.out().println("Введите число.");
        }
        return 0;
    }

    public void enter(Location location) {
        int index = locations.indexOf(location);
        if (index < 0 || !isAvailable(index + 1)) {
            throw new IllegalArgumentException("Локация недоступна");
        }
        if(index+1!=15 && index+1!=16)getStories().leavePoliceLocation();
        getStories().beginVisit(index + 1, location.getName(), java.util.concurrent.ThreadLocalRandom.current());
        GameIO.out().println("Вы вошли в локацию: " + location.getName());
        GameIO.out().println(location.getDescription());
        GameIO.out().println("Запасы: " + location.getSuppliesStatus());
        GameIO.out().println("Профиль добычи: " + switch (location.getLootProfile()) {
            case FOOD -> "продукты";
            case MEDICAL -> "лекарства";
            case ARMS -> "оружие и боеприпасы";
            case WORKSHOP -> "инструменты и ближнее оружие";
            default -> "разные припасы";
        });
        if (visited.add(index + 1)) {
            for (Location next : locations) {
                if (next.getRequiredLocation() == index + 1 && isAvailable(getNumber(next))) {
                    GameIO.out().println("Открыта новая локация: " + next.getName());
                }
            }
        }
    }
}
