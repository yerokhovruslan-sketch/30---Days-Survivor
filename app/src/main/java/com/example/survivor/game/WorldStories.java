package com.example.survivor.game;

import java.io.Serializable;
import java.util.*;

/** Persistent memories of one world; the catalogue itself is not written into saves. */
final class WorldStories implements Serializable {
    private static final long serialVersionUID = 1L;
    record Story(int id, String title, String text, int weight, int[] places) {
        boolean belongsTo(int place) { return Arrays.stream(places).anyMatch(number -> number == place); }
    }
    record Discovery(int id, String title, String text, List<ItemInfo> items) { }

    private final Set<Integer> seen = new HashSet<>();
    private final List<String> journal = new ArrayList<>();
    private final Map<Integer, Integer> visits = new HashMap<>();
    private final Set<Integer> searched = new HashSet<>();
    private final Map<Integer, Integer> donations = new HashMap<>();
    private final Set<Integer> chalkPlaces = new HashSet<>();
    private final Set<Integer> rescuerPlaces = new HashSet<>();
    private int day = 1;
    private int minuteOfDay = 480;
    private boolean clockInitialized = true;
    private SemenStory semen;
    SemenStory semen(){if(semen==null)semen=new SemenStory();return semen;}
    private MonsterDens monsterDens;
    MonsterDens monsterDens(){if(monsterDens==null)monsterDens=new MonsterDens();return monsterDens;}
    private ForestStories forest;
    ForestStories forest(){if(forest==null)forest=new ForestStories();return forest;}
    private CityHospital hospital;
    CityHospital hospital() { if(hospital==null)hospital=new CityHospital();return hospital; }
    private PharmacyWarehouse pharmacy;
    PharmacyWarehouse pharmacy() { if(pharmacy==null)pharmacy=new PharmacyWarehouse();return pharmacy; }
    private DistrictClinic clinic;
    DistrictClinic clinic() { if(clinic==null)clinic=new DistrictClinic();return clinic; }
    private School school;
    School school() { if (school == null) school = new School(); return school; }
    private Schoolyard schoolyard;
    Schoolyard schoolyard() { if (schoolyard == null) schoolyard = new Schoolyard(); return schoolyard; }
    private Rooftops rooftops;
    Rooftops rooftops() { if (rooftops == null) rooftops = new Rooftops(); return rooftops; }
    private ApartmentBuilding apartment;
    ApartmentBuilding apartment() { if (apartment == null) apartment = new ApartmentBuilding(); return apartment; }
    private GroceryStore grocery;
    GroceryStore grocery() { if (grocery == null) grocery = new GroceryStore(); return grocery; }
    private QuietCourtyard courtyard;
    QuietCourtyard courtyard() { if (courtyard == null) courtyard = new QuietCourtyard(); return courtyard; }
    private ResidentialStories residential;
    private int craftStockDay,powderBought,kitsBought;
    private Map<Integer,int[]> casingSites;
    private Map<Integer,Integer> marketStock,marketDue;
    private long marketSeed;
    private int marketEventUntil,marketEventId,marketSaleCategory,eventDue;
    private boolean chipPolice,chipPower;
    WorldStories(){this(new Random().nextLong());}
    WorldStories(long seed){marketSeed=seed;}
    int absoluteMinute(){return (day()-1)*1440+minuteOfDay();}
    void refreshMarket() {
        if(marketStock==null){marketStock=new HashMap<>();marketDue=new HashMap<>();if(marketSeed==0)marketSeed=new Random().nextLong();eventDue=(absoluteMinute()/2880+1)*2880;}
        int now=absoluteMinute();
        if(now>=eventDue) {
            int cycle=now/2880;Random r=new Random(marketSeed+cycle*7919L);
            int previous=marketEventId;marketEventId=r.nextInt(100)<23?1+r.nextInt(6):0;
            if(previous==1 && marketEventId==1)marketEventId=0;
            marketSaleCategory=r.nextInt(6);marketEventUntil=now+2880;eventDue=(cycle+1)*2880;
            if(marketEventId!=0)note("Торговые вести",marketEventText());
        }
        for(int n=1;n<=Shop.LAST_PRODUCT;n++)if(Shop.createItem(n)!=null && now>=marketDue.getOrDefault(n,0)) {
            int period=Economy.period(n),cycle=now/period;
            Random r=new Random(marketSeed+n*104729L+cycle*15485863L);
            int target=Economy.target(n,day,r);
            if(marketEvent()==2 && Economy.category(n)==1)target=Math.min(1,target);
            if(marketEvent()==3 && Economy.category(n)==3)target+=1;
            // Delayed essentials keep their current stock, never repeatedly postpone.
            boolean delayed=marketEvent()==1 && Economy.category(n)<=3;
            if(n==16 && now<2880)target=2; // Modest guaranteed starter food.
            if(Economy.category(n)<=1) {
                int used=0;for(int other:marketStock.keySet())if(other!=n && Economy.category(other)==Economy.category(n))used+=marketStock.get(other);
                target=Math.min(target,Math.max(0,(Economy.category(n)==0?12:5)-used));
            }
            if(!delayed)marketStock.put(n,Math.max(marketStock.getOrDefault(n,0),target));
            marketDue.put(n,delayed?now+2880:(cycle+1)*period);
        }
    }
    int marketEvent(){return absoluteMinute()<marketEventUntil?marketEventId:0;}
    int saleCategory(){return marketSaleCategory;}
    String marketEventText(){return switch(marketEvent()) {
        case 1->"Поставка задержана: караван обходит перекрытую дорогу. Ориентировочное ожидание: ещё "+Math.max(0,marketEventUntil-absoluteMinute())/60+" игровых ч. Оставшиеся товары продаются.";
        case 2->"Дефицит медикаментов: цены +30%, поставка сокращена. До конца события: "+Math.max(0,marketEventUntil-absoluteMinute())+" мин.";
        case 3->"Доставлены армейские припасы: в текущей поставке больше патронов.";
        case 4->"Перебои с энергией: чипы и медная проволока дороже на 25% на 2 дня.";
        case 5->"Распродажа одной категории: скидка 20% на 2 дня.";
        case 6->"Прибыла группа выживших: еда +15%, инструменты принимают дороже на 2 дня.";
        default->"Поставки идут по расписанию.";};}
    String marketStatus(){refreshMarket();int now=absoluteMinute(),due=Integer.MAX_VALUE;for(int n:marketDue.keySet())if(Economy.category(n)<=3)due=Math.min(due,marketDue.get(n));int left=Math.max(0,due-now);return marketEventText()+"\nОсновное пополнение примерно через "+left/1440+" д. "+left%1440/60+" ч. "+left%60+" мин. Оружие и инструменты: раз в 4–6 дней. После дня 14 ассортимент постепенно редеет.";}
    int marketMinutesUntil(int product){refreshMarket();return Math.max(0,marketDue.getOrDefault(product,absoluteMinute())-absoluteMinute());}
    int craftStock(int product){refreshMarket();return marketStock.getOrDefault(product,0);}
    void buyCraftStock(int product,int count){refreshMarket();marketStock.put(product,Math.max(0,craftStock(product)-count));}
    List<ItemInfo> guaranteedChips(int place) {
        List<ItemInfo> items=new ArrayList<>();
        if(place==15 && !chipPolice){chipPolice=true;items.add(Shop.createItem(49));note("Чип дежурной рации","В полицейском участке найден исправный электрочип. Другие источники: блок школьной котельной или оборудование электроподстанции.");}
        if(place==43 && !chipPower){chipPower=true;items.add(Shop.createItem(49));note("Чип подстанции","Из блока управления извлечён электрочип для радиостанции.");}
        if((place==9 || place==35) && !workshopControllerFound){workshopControllerFound=true;items.add(new Tools("Блок управления радиостанцией","Неисправная электроника. На верстаке I с рабочим инструментом можно извлечь электрочип.",3,0));note("Запасной путь","В служебном оборудовании найден блок управления. Его можно разобрать на верстаке I рабочим инструментом и получить электрочип.");}
        return items;
    }
    void collectEquipment(int place,Fighter player,List<ItemInfo> found,Scanner scanner) {
        try{SurvivalSystems.collect(player,found,scanner);}
        finally{for(ItemInfo item:found)if(!player.getInventory().contains(item)){
            if(item.getName().equals("Блок управления радиостанцией"))workshopControllerFound=false;
            else if(place==15)chipPolice=false;else if(place==43)chipPower=false;
        }GameIO.checkpoint();}
    }
    void addCases(int place,AmmoType type,int count) {
        if(place<1 || count<=0)return;if(casingSites==null)casingSites=new HashMap<>();
        int[] values=casingSites.computeIfAbsent(place,key->new int[AmmoType.values().length]);
        if(values.length<AmmoType.values().length){values=Arrays.copyOf(values,AmmoType.values().length);casingSites.put(place,values);}
        values[type.ordinal()]=Math.min(1000,values[type.ordinal()]+count);
    }
    int caseCount(int place) {if(casingSites==null || !casingSites.containsKey(place))return 0;return Arrays.stream(casingSites.get(place)).sum();}
    int[] takeCases(int place) {if(casingSites==null)return new int[AmmoType.values().length];int[] values=casingSites.remove(place);return values==null?new int[AmmoType.values().length]:values;}
    ResidentialStories residential() {if(residential==null)residential=new ResidentialStories();return residential;}
    PoliceArchive.Attempt archiveAttempt;
    void leavePoliceLocation() { }
    private java.util.Map<Integer,TacticalBattle> pendingBattles;
    private java.util.Map<Integer,java.util.Map<String,TacticalBattle>> resolvedBattles;
    TacticalBattle pendingBattle(int place){return pendingBattles==null?null:pendingBattles.get(place);}
    void rememberBattle(int place,TacticalBattle battle){if(pendingBattles==null)pendingBattles=new java.util.HashMap<>();pendingBattles.put(place,battle);}
    void clearPendingBattle(int place){if(pendingBattles!=null)pendingBattles.remove(place);}
    void rememberResolvedBattle(int place,TacticalBattle battle){if(resolvedBattles==null)resolvedBattles=new java.util.HashMap<>();resolvedBattles.computeIfAbsent(place,key->new java.util.HashMap<>()).put(battle.terrain,battle);}
    TacticalBattle consumeResolvedBattle(int place,String terrain){if(resolvedBattles==null)return null;java.util.Map<String,TacticalBattle> battles=resolvedBattles.get(place);if(battles==null)return null;TacticalBattle battle=battles.remove(terrain);if(battles.isEmpty())resolvedBattles.remove(place);return battle;}

    boolean archiveComplete;
    int archiveGoal;
    RouteAfterArchive routeAfterArchive;
    RouteAfterArchive routeAfterArchive(){if(routeAfterArchive==null)routeAfterArchive=new RouteAfterArchive();return routeAfterArchive;}
    private boolean archiveKeyFound;
    void markSearched(int place){searched.add(place);}
    boolean searchedPlace(int place){return searched.contains(place);}
    boolean archiveAvailable(Fighter p){return PoliceArchive.unlocked(p);}
    boolean findArchiveKey(Fighter p,int place){
        if(place!=15 || archiveKeyFound)return false;
        Tools key=new Tools(PoliceArchive.KEY,"Джону кажется, этот ключ важен. Он открывает архив участка; продать или выбросить его нельзя.",1,0);
        if(!p.addItem(key)){GameIO.out().println("В дежурной части найден ключ от архива, но рюкзак полон. Освободите место и вернитесь.");return false;}
        archiveKeyFound=true;note("Ключ от архива","В дежурной части найден ключ от архива участка. Ключ в рюкзаке. "+(PoliceArchive.unlocked(p)?"После радиосигнала известно, что здесь нужно искать схему резервного прохода.":"Какие документы нужны, пока неизвестно."));
        GameIO.out().println("Найден ключ от архива. Джон убирает его в рюкзак: кажется, он ещё пригодится.");GameIO.checkpoint();return true;
    }
    private boolean radioOpeningSeen,radioInspected,workshopControllerFound;
    boolean radioOpeningHeard(){return radioOpeningSeen;}
    private int lastSignalClueSearch=-3;
    void radioOpening(Fighter player,Scanner scanner) {
        if(radioOpeningSeen || player.getUpgrades().radio>0)return;
        radioOpeningSeen=true;
        GameIO.sound(GameSound.QUEST);note("Кто-то ещё говорит","Связь оборвалась до названия точки сбора. Нужно восстановить устойчивый приём. Источник сигнала пока неизвестен.");
        ResidentialStories.pause("Что удалось услышать","Ты успел услышать главное: эвакуация через тридцать дней. Название пункта сбора утонуло в помехах. Ты записал обрывок сообщения. Теперь нужно восстановить устойчивый приём.",scanner);
        inspectRadio(player,scanner);GameIO.checkpoint();
    }
    void inspectRadio(Fighter player,Scanner scanner) {
        while(true) {
            GameIO.storyMenu("Кто-то ещё говорит",player.getUpgrades().radio==0?"Восстановить устойчивую связь. Чтение не тратит время.":"Приёмник восстановлен. Чтение не тратит время.");
            GameIO.card("Приёмник",player.getUpgrades().radio>0?"Радиостанция исправна. Теперь можно попробовать поймать сигнал.":"Ты прижимаешь провод большим пальцем. Шипение становится громче. На секунду кажется, что за ним кто-то дышит. Отпускаешь — тишина.\nОтлично. Теперь ты часть радиостанции.");
            if(player.getUpgrades().radio==0){GameIO.action(1,"Осмотреть приёмник");GameIO.action(2,"Узнать, какие детали нужны");}
            else GameIO.action(4,player.getUpgrades().signalStage>0?"Продолжить приём сообщения":player.getUpgrades().signalReceived?"Просмотреть запись сигнала":"Настроить частоту",player.getUpgrades().signalHint(this),player.getUpgrades().signalReceived || player.getUpgrades().canListen(this));
            boolean dismantleReady=player.getUpgrades().bench>=1 && Crafting.possessions(player).stream().anyMatch(i->i.isRepairTool() && i.getDurability()>0) && Crafting.possessions(player).stream().anyMatch(i->!Crafting.salvage(player,i).isEmpty());
            if(dismantleReady)GameIO.action(3,"Разобрать найденную электронику");
            GameIO.action(5,"Вспомнить первую передачу");
            GameIO.action(0,"Отложить. Подготовиться к вылазке");
            int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==5)ResidentialStories.pause("Обрывок передачи","«…Эвакуация через тридцать дней. Точка сбора…» Затем помехи. Ни адреса, ни подтверждения, что сообщение свежее. Это всё, что Джон успел записать.",scanner);
            if(c==4 && player.getUpgrades().radio>0){if(player.getUpgrades().signalReceived && player.getUpgrades().signalStage==0)ResidentialStories.pause("Запись в журнале","Пункт сбора — военный склад. Северный блокпост закрыт. Схема резервного прохода — в архиве участка. Дата записи неизвестна.",scanner);else player.getUpgrades().listen(player,scanner,this);}
            if(c==1 && player.getUpgrades().radio==0){if(!radioInspected){radioInspected=true;note("Неисправность приёмника","Контакты повреждены, приём неустойчив. Нужны два электрочипа, проволока, запчасти и изолента.");}
                ResidentialStories.pause("Неустойчивый контакт","Голос удалось услышать, но неизвестно, живая ли это передача или запись. Контакты и электронные узлы приёмника повреждены. Нужны два исправных чипа. Осмотр ничего не расходует.",scanner);}
            if(c==2 && player.getUpgrades().radio==0)ResidentialStories.pause("Несколько путей",player.getUpgrades().description(player,3)+"\nПервый чип: полицейский участок. Второй: электроподстанция или блок управления в школьной котельной. Блок разбирается дома на верстаке I рабочим инструментом. Чип также продаёт торговец: базовая цена 600 $. Припасы и улучшения можно готовить параллельно.",scanner);
            if(c==3 && dismantleReady)Crafting.dismantleMenu(player,this,scanner);
        }
    }
    boolean signalKnown(){return journal.stream().anyMatch(t->t.contains("Сигнал «Маяк» записан."));}
    Discovery signalClue(int place,String name) {
        if(signalKnown())return null;
        if(searches-lastSignalClueSearch<3)return null;
        int id;String title,text;
        switch(place) {
            case 3->{id=201;title="Чужая отметка";text="На схеме у остановки обведён промышленный район. Рядом стрелка на восток и подпись: «На сбор». Кто-то уже выбирал дорогу. Когда именно — непонятно.";}
            case 5->{id=202;title="Собирались в дорогу";text="У лестницы лежит раскрытая сумка. Одежду оставили, отделения для воды и лекарств пусты. Рядом список: «вода, бинты, документы». Похоже, собирались уходить. Связано ли это с передачей — пока неизвестно.";}
            case 4->{id=203;title="Мел на двери";text="На двери магазина мелом: «Дальше по улице после темноты не ходите». Ниже свежая стрелка к двору. Это предупреждение, но кто его оставил и можно ли доверять обходу, неизвестно.";}
            case 1->{id=204;title="Одна частота";text="Среди газет зажат клочок бумаги: «МАЯК». Ниже частоты, почти все зачёркнуты. На обороте: «Не знаю, кто отвечает. Но отвечает». Кто-то уже пытался настроить приёмник.";}
            default->{return null;}
        }
        if(seen.contains(id))return null;lastSignalClueSearch=searches;
        return remember(id,title,text,name,Collections.emptyList());
    }
    int currentPlace() {return currentPlace;}
    void note(String title,String text) {journal.add("День "+day+" — "+title+"\n"+text);}
    private void ensureClock() { if (!clockInitialized) { minuteOfDay = 480; clockInitialized = true; } }
    int minuteOfDay() { ensureClock(); return minuteOfDay; }
    boolean isNight() { int time = minuteOfDay(); return time < 360 || time >= 1200; }
    String time() { return String.format(Locale.ROOT, "%02d:%02d", minuteOfDay() / 60, minuteOfDay() % 60); }
    private transient Fighter recoveryPlayer;
    void bindRecovery(Fighter player) { recoveryPlayer=player; }
    void advanceMinutes(int minutes) {
        if(recoveryPlayer!=null)recoveryPlayer.recoverInjuries(minutes);
        ensureClock();
        long total = (long) minuteOfDay + Math.max(0, minutes);
        day += (int) (total / 1440);
        minuteOfDay = (int) (total % 1440);
    }
    private int searches;
    private int lastStorySearch = -4;
    private int lastStoryDay;
    private int currentPlace;
    private String currentName = "";
    private boolean storyVisit;
    private boolean visitUsed;
    private int photoDay, photoSearch, photoPlace;
    private boolean photoRecognized;
    private int radioPlace, radioSearch, radioDay;
    private boolean radioPlaying;

    int day() { return day; }
    void recordRadioSignal() {
        String entry="Сигнал «Маяк» записан. Точка сбора — Военный склад. Северный блокпост закрыт.\n"
                +"Цель: найти схему резервного прохода в архиве участка и проверить дату сообщения.";
        if(journal.stream().noneMatch(text->text.contains("Сигнал «Маяк» записан.")))journal.add("День "+day+" — Радиостанция\n"+entry);
    }
    int searches() { return searches; }
    boolean hasSeen(int id) { return seen.contains(id); }
    List<String> journal() { return java.util.Collections.unmodifiableList(new ArrayList<>(journal)); }
    static List<Story> catalogue() { return STORIES; }

    void sleep() { advanceMinutes(1440); }
    void beginVisit(int place, String name, Random random) {
        currentPlace = place;
        currentName = name;
        visits.merge(place, 1, Integer::sum);
        // Most entire visits stay ordinary, even if the player searches repeatedly.
        storyVisit = random.nextInt(100) < 45;
        visitUsed = false;
    }
    private boolean eligible(Story story, int place) {
        if (!story.belongsTo(place)) return false;
        if (story.id == 76) return chalkPlaces.size() < 3 && !chalkPlaces.contains(place);
        if (story.id == 86) return rescuerPlaces.size() < 3 && !rescuerPlaces.contains(place);
        if (seen.contains(story.id)) return false;
        if (story.id == 89) return visits.getOrDefault(place, 0) > 1 && searched.contains(place);
        if (story.id == 90) return donations.containsKey(place) && day - donations.get(place) >= 2;
        return true;
    }
    List<Story> candidates(int place) { return STORIES.stream().filter(story -> eligible(story, place)).collect(java.util.stream.Collectors.toList()); }
    private boolean cooldownReady() { return searches - lastStorySearch >= 3 || day > lastStoryDay; }

    Discovery discover(int place, String name, Random random, boolean completedSearch) {
        return discover(place, name, random, completedSearch, 0);
    }
    Discovery discover(int place, String name, Random random, boolean completedSearch, int bonus) {
        if (!storyVisit && bonus > 0 && random.nextInt(100) < bonus) storyVisit = true;
        if (completedSearch) {
            searches++;
        }
        if(place==15 || place==16){if(completedSearch)searched.add(place);return null;}
        Discovery result = null;
        if(completedSearch) {
            Discovery clue=signalClue(place,name);
            if(clue!=null){print(clue);searched.add(place);return null;}
        }
        if (radioPlaying && place == radioPlace && (searches > radioSearch || day > radioDay)) {
            radioPlaying = false;
            result = remember(78, "Радио замолчало", "Песня обрывается посреди строки. Лампочка приёмника медленно гаснет. "
                    + "Несколько секунд ты ещё ждёшь следующую ноту, но в комнате остаётся только ветер.", name, java.util.Arrays.asList());
        } else if (storyVisit && !visitUsed && cooldownReady()) {
            List<Story> available = candidates(place).stream()
                    .filter(story -> completedSearch || story.id != 4 && story.id != 27 && story.id != 35).collect(java.util.stream.Collectors.toList());
            int total = available.stream().mapToInt(Story::weight).sum();
            if (total > 0) {
                int roll = random.nextInt(total);
                for (Story story : available) {
                    if (roll < story.weight) { result = reveal(story, place, name); break; }
                    roll -= story.weight;
                }
            }
        }
        if (completedSearch) searched.add(place);
        return result;
    }
    private Discovery reveal(Story story, int place, String name) {
        String text = story.text;
        if (story.id == 76) {
            chalkPlaces.add(place);
            text = switch (chalkPlaces.size()) {
                case 1 -> text;
                case 2 -> "На стене снова тот же меловой знак: круг с короткой стрелкой. "
                        + "Его рисовали разными руками, но стрелки неизменно ведут мимо открытых площадей.";
                default -> "Под знаком сохранилась поясняющая надпись: «Круг — проходили без потерь. "
                        + "Стрелка — следующий ночлег». Кто-то отмечал безопасные пути для тех, кто шёл следом. "
                        + "Мел почти смыт. Ты не знаешь, когда эти маршруты перестали быть безопасными.";
            };
        }
        if (story.id == 86) {
            rescuerPlaces.add(place);
            text = switch (rescuerPlaces.size()) {
                case 1 -> text;
                case 2 -> "На выломанной двери снова белая стрелка. За ней — бинтовые обёртки и носилки "
                        + "из двух лыж. Кто-то вернулся за человеком, которого остальные не смогли унести.";
                default -> "Белые стрелки сходятся у запасного выхода. На стене: «Если можешь идти — помоги тому, "
                        + "кто не может». Подписи нет. Ты видел следы этого человека в нескольких местах, но самого — ни разу.";
            };
        }
        if (story.id == 77) { photoDay = day; photoSearch = searches; photoPlace = place; }
        if (story.id == 78) { radioPlaying = true; radioPlace = place; radioSearch = searches; radioDay = day; }
        if (story.id == 90) donations.remove(place);
        List<ItemInfo> items = switch (story.id) {
            case 4 -> java.util.Arrays.asList(new Food("Галеты из детского рюкзака", "Запечатанная пачка. На упаковке написано: «Не ешь всё сразу».", 1, 3, 20));
            case 27 -> java.util.Arrays.asList(new Food("Консервы из кладовой", "Одна из банок ещё герметична. Вода здесь закончилась раньше еды.", 2, 8, 40));
            case 35 -> java.util.Arrays.asList(new Food("Шоколад из шкафчика", "В кармане формы сохранилась запечатанная плитка.", 1, 5, 20));
            default -> java.util.Arrays.asList();
        };
        return remember(story.id, story.title, text, name, items);
    }
    private Discovery remember(int id, String title, String text, String name, List<ItemInfo> items) {
        seen.add(id);
        visitUsed = true;
        lastStorySearch = searches;
        lastStoryDay = day;
        journal.add("День " + day + " — " + name + "\n" + title + "\n" + text);
        return new Discovery(id, title, text, items);
    }
    static void print(Discovery discovery) {
        if (discovery == null) return;
        GameIO.narrative();
        GameIO.sound(GameSound.PAPER);
        GameIO.out().println("\n── " + discovery.title + " ──");
        // Wrap narrative prose independently of the combat table.
        for (String paragraph : discovery.text.split("\n")) {
            int width = 0;
            for (String word : paragraph.split(" +")) {
                if (width > 0 && width + word.length() + 1 > 88) { GameIO.out().println(); width = 0; }
                if (width > 0) { GameIO.out().print(" "); width++; }
                GameIO.out().print(word); width += word.length();
            }
            GameIO.out().println();
        }
        if (discovery.id != 0) GameIO.out().println("\nДжон запомнил это. Запись добавлена в журнал.");
    }
    void observePeople(Random random) {
        if (!seen.contains(77) || photoRecognized || day - photoDay < 2 || searches - photoSearch < 4
                || currentPlace == photoPlace || visitUsed || !storyVisit || !cooldownReady()
                || random.nextInt(100) >= 35) return;
        photoRecognized = true;
        print(remember(77, "Лицо с фотографии", "Один из вооружённых людей оборачивается. Ты узнаёшь мужчину "
                + "с найденной семейной фотографии: та же рассечённая бровь, только лицо сильно осунулось. "
                + "Рядом с ним другие люди, ни одного знакомого по снимку лица. Он поднимает оружие. "
                + "Фотография ничего не объясняет и ни от чего тебя не защищает.", currentName, java.util.Arrays.asList()));
    }
    void leaveSupplies(Fighter player, Scanner scanner) {
        if (currentPlace <= 0) { GameIO.out().println("Оставить припасы можно только на локации."); return; }
        List<ItemInfo> supplies = new ArrayList<>(player.getInventory());
        if (supplies.isEmpty()) { GameIO.out().println("В рюкзаке нет вещей, которые можно оставить."); return; }
        GameIO.menu("Помочь выжившим", "Оставленная вещь исчезнет из рюкзака. Возможно, кто-то найдёт её позже.");
        for (int i = 0; i < supplies.size(); i++) GameIO.item(i + 1, supplies.get(i), "Оставить: ", true);
        GameIO.action(0, "Отмена");
        int choice = SurvivalSystems.choice(scanner);
        if (choice == 0) return;
        if (choice < 1 || choice > supplies.size()) { GameIO.out().println("Неверный номер."); return; }
        player.getInventory().remove(supplies.get(choice - 1));
        player.getEquippedWeapon();
        player.getEquippedArmor();
        donations.putIfAbsent(currentPlace, day);
        GameIO.out().println("Ты оставляешь припасы на видном месте. Может быть, кто-то ещё вернётся сюда.");
    }
    void showJournal(Scanner scanner) {
        if (journal.isEmpty()) { GameIO.out().println("В журнале пока нет чужих историй."); return; }
        while (true) {
            GameIO.menu("Журнал историй", "То, что Джон запомнил. Выберите запись для чтения.");
            for (int i = 0; i < journal.size(); i++) {
                String[] lines = journal.get(i).split("\n", 3);
                GameIO.action(i + 1, lines[1], lines[0]);
            }
            GameIO.action(0, "Назад");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return;
            if (choice < 1 || choice > journal.size()) { GameIO.out().println("Неверный номер."); continue; }
            String[] lines = journal.get(choice - 1).split("\n", 3);
            print(new Discovery(0, lines[1], lines[2], java.util.Arrays.asList()));
        }
    }

    private static Story scene(int id, String title, String text, int weight, int... places) {
        return new Story(id, title, text, weight, places);
    }
    private static Story note(int id, String text, int... places) {
        String[] titles = {"Здесь дует", "До темноты", "Какой сегодня день", "Батарейки", "Лена, я ждал",
                "Квартира 27", "Птицы", "Море", "Максим, 24", "Четыре банки", "Обычные пробки",
                "День рождения", "Я жду папу", "Поезд", "Заведу собаку", "Крыса Гена", "Настоящий кофе",
                "Понедельники", "Оля, я не сердился", "Ни одного выстрела", "Больше не считаем", "Мёд для дочери",
                "Фотография жены", "Ужасный цвет", "Я не герой"};
        return scene(id, "Записка: " + titles[id - 51], "На сложенном листке сохранилось несколько строк:\n«" + text + "»", 8, places);
    }
    private static final List<Story> STORIES = java.util.Arrays.asList(
        scene(1, "Женщина и ребёнок", "В спальне одной из квартир ты находишь тела женщины и маленького ребёнка. Женщина сидит у кровати; ребёнок рядом, укрытый её курткой. На тумбочке — пустая бутылка воды и две ложки, в шкафу почти нет еды. На краю стола карандашом отмечены две одинаковые порции. Даже последнюю она разделила пополам.", 1, 5, 28),
        scene(2, "Три тарелки", "На кухонном столе стоят три тарелки. В двух еда давно покрылась плесенью. Третья так и осталась пустой. Рядом перевёрнут стул; под ним лежит вилка, которую никто не поднял.", 3, 5, 28, 20),
        scene(3, "Перестаньте стучать", "На двери аптечного склада приклеен лист: «Инсулина больше нет. Пожалуйста, перестаньте стучать». Нижний край стёрт десятками прикосновений. Изнутри дверь подпёрта шкафом. На полу у порога лежит пустой футляр для шприца.", 2, 11),
        scene(4, "Детский рюкзак", "Под скамьёй лежит маленький рюкзак. Внутри — запечатанные галеты, пустая бутылка воды и записка: «Не ешь всё сразу. Я скоро». Пыль забилась даже в застёжку. На лямке пришита варежка, чтобы не потерялась.", 2, 1, 3, 7, 8, 18, 21, 38),
        scene(5, "Ужин на четверых", "В квартире накрыт стол на четверых. На одной тарелке аккуратно лежат нетронутые приборы. Остальные три стула отодвинуты так, будто люди поднялись одновременно. На кухне открыта духовка. Ужин так никто и не убрал.", 3, 5, 28),
        scene(6, "Лена, Одесса, 1987", "В кресле перед погасшим телевизором сидит умерший пожилой мужчина. На коленях фотография молодой женщины: «Лена, Одесса, 1987». Снимок затёрт у лица. Пульт лежит на подлокотнике, а домашние тапочки стоят рядом, носками к креслу.", 1, 5, 28),
        scene(7, "Последний звонок", "На полу разбитый телефон с подключённым пауэрбанком. Оба давно разряжены. На стене пять раз написан один номер. Первые цифры ровные, последние едва различимы. Под ними: «Ну возьми же».", 2, 5, 18, 28, 34, 38),
        scene(8, "Семейный рисунок", "В детской комнате на стене рисунок: четыре человека держатся за руки. Один перечёркнут чёрным карандашом. Остальные нарисованы заново, ближе друг к другу. Под рисунком аккуратно подписано: «Мы дома».", 2, 5, 28),
        scene(9, "За вами придут", "На доске в школьном классе написано: «Ждите здесь. За вами придут». Под надписью другим почерком добавлено: «Не пришли». На партах стоят стаканчики для воды. Чьи-то очки всё ещё лежат на раскрытом журнале.", 2, 8),
        scene(10, "Спортивный зал", "В школьном спортзале разложены маты и одеяла. На переносной доске список фамилий. Возле половины маленькие крестики. Одну фамилию вычеркнули, потом вписали обратно и обвели. Ниже больше ничего не добавляли.", 1, 8),
        scene(11, "Восьмой день рождения", "В квартире выцветшие шарики и засохший торт с цифрой «8». Подарочная коробка закрыта, бант завязан старательно и неровно. На открытке: «Откроешь, когда вернусь с работы». На коробке больше пыли, чем на остальном столе.", 1, 5, 28),
        scene(12, "Лифт между этажами", "Двери лифта застряли между этажами. Кабина пуста. На металлической стенке короткие вертикальные царапины, собранные в группы. Последняя группа не закончена. Возле кнопки вызова засох след от ладони.", 2, 5, 10, 12, 17, 49),
        scene(13, "Одежда на утро", "Ванная заполнена ржавой водой и пустыми бутылками. На стиральной машине сложена одежда для троих: две взрослые стопки и одна маленькая. Сверху детские носки, вывернутые на лицевую сторону. Всё приготовили на утро.", 2, 5, 28),
        scene(14, "Последняя смена", "В диспетчерской пожарной части висит расписание дежурств. У последней даты напротив каждой фамилии — «выезд». В графе возвращения пусто. Под доской кто-то оставил нетронутый термос и пачку сигарет для сменщика.", 2, 14),
        scene(15, "Заявления о пропавших", "На столе полицейского участка десятки заявлений. Последние написаны на тетрадных листах: бланки закончились раньше людей. В одном описании примета — «смеётся, когда волнуется». На полях рукой дежурного: «Звонила снова».", 2, 15, 16),
        scene(16, "Закончить до субботы", "В комнате сняты обои, на полу инструменты и засохшая краска. На стене карандашом: «Закончить до субботы». Рядом отмечен рост ребёнка и дата. Новая отметка должна была оказаться уже поверх свежих обоев.", 4, 5, 28),
        scene(17, "Два рюкзака", "В спальне стоят два рюкзака для эвакуации. Один туго набит пустыми теперь упаковками, второй почти пуст. В обоих по половине одной фотографии. На маленьком кармане пришита бумажка: «Если потеряемся — у моста».", 2, 5, 20, 28),
        scene(18, "Ниша за шкафом", "За шкафом обнаруживается ниша с матрасом, погасшим фонариком и четырьмя пустыми бутылками. На стене царапины. Последняя спичка так и осталась зажатой в расщеплённой коробке. Шкаф сдвигали снаружи.", 1, 9, 18, 28, 31, 44),
        scene(19, "Номер 286", "Перед магазином мокнут бумажные талоны очереди. Последний — 286. На обратной стороне кто-то записал список покупок: хлеб, вода, свечи. Слово «вода» подчёркнуто дважды. Дверь давно выбита, очередь больше не нужна.", 4, 4, 33),
        scene(20, "Ричи выпустил", "На кухне миска с высохшим ободком воды и поводок на крючке. На двери: «Ричи выпустил. Может, выжил». Под надписью отпечаток грязной собачьей лапы. Других следов в квартире нет.", 3, 5, 20, 28),
        scene(21, "Пустое детское кресло", "В брошенной машине ключи лежат на водительском сиденье. Детское кресло сзади пустое, пассажирская дверь открыта. Под креслом маленький ботинок. Впереди, на приборной панели, расправлена дорожная карта.", 3, 3, 13, 27, 33, 35, 37, 45),
        scene(22, "Готовый чемодан", "В коридоре собран чемодан: документы, одежда, семейные фотографии. Сверху лежат билеты, сложенные по порядку. Ручка обмотана яркой лентой, чтобы узнавать среди других. Чемодан никто не забрал.", 3, 5, 18, 28, 38),
        scene(23, "Программирование, 13:20", "В комнате бывшего общежития висит расписание. Последняя отмеченная пара: «Программирование, 13:20». Рядом кружка с высохшим кофе и тетрадь с исправленной ошибкой. На полях: «Потом разберусь».", 4, 28, 46),
        scene(24, "Стул у кровати", "В больничной палате пустая капельница. На стуле спальный мешок, под ним сменные носки и зубная щётка. Кто-то несколько дней жил рядом с пациентом. В журнале посещений одно имя повторяется на каждой странице.", 2, 10, 12, 48),
        scene(25, "Непрочитанный конец", "На прикроватной тумбочке роман. Закладка почти в конце: осталось двадцать страниц. На ней чужой список дел, среди которых «вернуть книгу». Ты закрываешь обложку на том же месте.", 5, 5, 12, 20, 28, 46, 48),
        scene(26, "Последний лист альбома", "В шкафу семейный альбом. На последнем снимке люди улыбаются у забаррикадированного окна. Кто-то держит перед камерой лист: «У нас всё хорошо». Снимок вклеен криво, но очень бережно.", 3, 5, 18, 28),
        scene(27, "Без воды", "В кладовой ещё остались консервы, но почти все вздуты. На полу десятки пустых бутылок. На стене нормы воды, с каждым днём всё меньше. Среди испорченных банок находится одна целая. Рядом детская чашка с нарисованной чертой посередине.", 2, 4, 18, 20, 28, 31, 34, 47),
        scene(28, "Обведённая частота", "Радиоприёмник проводами соединён с мёртвым автомобильным аккумулятором. На листке несколько частот, одна обведена красным три раза. Возле неё: «Женский голос. Говорит по вечерам». Приёмник молчит.", 4, 18, 20, 26, 32, 35, 44, 49),
        scene(29, "Марс", "Квартира пуста. На подоконнике ошейник с именем «Марс», окно специально оставлено приоткрытым. На оконной раме старые царапины. Кто-то убрал с подоконника цветы, оставив свободный проход.", 4, 5, 28),
        scene(30, "Не ждите нас здесь", "На двери квартиры: «Мы ушли на север. Если вы это читаете, не ждите нас здесь». Ниже другой рукой: «Я приходил». Дата под второй строкой на неделю позже первой.", 3, 5, 28),
        scene(31, "Две кружки на крыше", "На крыше два складных стула и две металлические кружки. Отсюда город кажется почти целым. Возле одного стула аккуратная горка семечек, возле другого — маленький букет сухой травы. Люди продолжали приходить сюда, когда внизу уже стало опасно.", 5, 6, 12, 17, 32, 43),
        scene(32, "Дом с большим солнцем", "На стене детской рукой нарисованы огромный дом, солнце и четыре человечка. Взрослый подписал: «Мы обязательно построим такой». Над дверью мелкие насечки роста. Последняя заметно выше остальных — обещание прожило здесь несколько лет.", 3, 5, 7, 18, 28),
        scene(33, "Если завтра", "В будке охранника термос и раскрытая газета. На полях начато письмо: «Если завтра...» Ручка лежит поперёк строки. За стеклом висит табличка «Пост не оставлять».", 4, 15, 27, 33, 37, 40, 41, 43, 45, 47),
        scene(34, "Уходим утром", "На кухонном календаре перечёркнуты семь дней. На следующем: «Уходим утром». Рядом список, в котором вычеркнуто почти всё, кроме лекарств и фотографии. Карандаш привязан к календарю ниткой.", 4, 5, 20, 28, 34),
        scene(35, "Чтобы возвращался", "В шкафчике сотрудника форма, фотография семьи и запечатанная шоколадка. На обороте снимка: «Папе, чтобы не забывал возвращаться домой». Шоколадку он так и не съел. Фотография стояла лицом к дверце.", 2, 13, 14, 15, 36, 39, 41, 43, 47, 49),
        scene(36, "HELP", "На крыше больницы камнями выложено HELP. Рядом пустые сигнальные ракетницы и обгоревшая перчатка. Камни перекладывали несколько раз, делая буквы крупнее. Последняя стрелка направлена в небо.", 2, 12),
        scene(37, "Мама, я здесь был", "У остановки сохранилась телефонная будка. Трубка без провода качается от ветра. На стекле: «Мама, я здесь был». Надпись сделана снаружи, на уровне детских плеч.", 2, 3, 33, 38, 40),
        scene(38, "Список учеников", "В учительской список учеников: кого забрали, кого эвакуировали, кто остался ждать. Напротив последних имён вместо времени стоит «со мной». На стуле взрослое пальто, а рядом несколько слишком маленьких курток.", 2, 8),
        scene(39, "До безопасного города", "В гараже почти целая машина с пустым баком. На столе расчёты топлива до безопасного города. Последняя строка обведена: «Ещё семь литров». Рядом стоят пустые канистры, выстроенные по размеру.", 4, 35, 36, 39),
        scene(40, "Наблюдатель", "В комнате сотни книг, один стул у окна и бинокль. В тетради ежедневные наблюдения: машины, дым, люди на перекрёстке. Последние страницы всё короче. Последняя запись: «Сегодня тоже никого».", 3, 5, 6, 28, 32),
        scene(41, "Запертая снаружи", "На двери засов с внешней стороны. Внутри только матрас и миска. На дереве следы ударов и длинные царапины. У самого пола карандашом: «Я больше не кашляю». Комната пуста.", 1, 5, 9, 18, 28, 44, 49),
        scene(42, "Открытое пианино", "В музыкальном кабинете школы открыто пианино. На пюпитре ноты со следами пальцев. Над сложным тактом карандашная пометка учителя: «Не торопись». Одна клавиша осталась нажатой.", 5, 8),
        scene(43, "Для Маши", "В разграбленном отделе игрушек остался большой плюшевый медведь. На нём бумажка: «Для Маши. Не брать». Вокруг пустые коробки, но медведя никто не тронул. Кто-то даже посадил его повыше, подальше от воды.", 3, 4),
        scene(44, "Чужим не заходить", "В подземном помещении ряды пустых бутылок. На полу меловая линия, за ней: «Чужим не заходить». По обе стороны одинаковые одеяла и одинаковые пустые миски. Линия пережила тех, кто её провёл.", 2, 9, 18, 30, 31, 44),
        scene(45, "Четвёртый кадр", "В старой фотокабинке полоска из четырёх снимков. На первых трёх молодая пара смеётся. На четвёртом один смотрит за камеру, а второй всё ещё улыбается. Полоска так и осталась в выдающем лотке.", 3, 17, 38, 40),
        scene(46, "План эвакуации", "В офисе на экране ноутбука, подключённого к аварийному питанию, незаконченный документ: «План эвакуации сотрудников». Внутри одна строка: «В первую очередь вывести людей». Дальше пустая страница. Рядом стопка отчётов, которые больше никому не нужны.", 3, 17, 37, 41, 49),
        scene(47, "Закрылись на два дня", "В бывшей парикмахерской на первом этаже накидка лежит на кресле, под ним давно высохшие срезанные волосы. На зеркале: «Закрылись на два дня. Скоро вернёмся». Слово «скоро» обведено сердечком.", 4, 5, 28),
        scene(48, "Маленькие таблички", "За домом несколько самодельных табличек с кличками животных. У каждой лежит камень или игрушка. Последняя яма выкопана, но пуста. Лопата прислонена к забору; на ручке ещё осталась ткань, которой берегли ладони.", 2, 1, 7, 20, 28),
        scene(49, "Там отвечают", "В дежурной комнате карта с цветными отметками. Многие районы перечёркнуты. Возле одного: «Там всё ещё отвечают по радио». Последняя стрелка ведёт к краю карты, за которым ничего не нарисовано. Это старая отметка, а не обещание спасения.", 4, 13, 14, 15, 18, 32, 40, 44, 45, 49),
        scene(50, "Последняя фотография", "В резервной памяти уцелевшего телефона открывается единственный снимок: семь человек у входа в убежище. Дата совпадает с последним днём, когда здесь ещё работала связь. Один человек зажмурился, другой поднял руку, прося переснять. Второго кадра нет.", 2, 18, 32, 44, 49, 50),
        note(51, "Если ты это читаешь, пожалуйста, закрой дверь. Здесь всё время дует.", 5, 9, 18, 20, 28, 31, 46),
        note(52, "Мам, я ушёл искать Андрея. Не переживай. Вернусь до темноты.", 1, 3, 5, 7, 20, 28),
        note(53, "Я больше не помню, какой сегодня день. Наверное, это уже неважно.", 18, 20, 24, 28, 30, 31, 44),
        note(54, "Свет отключили снова. Батарей осталось на две ночи.", 5, 9, 18, 20, 28, 32, 43, 44),
        note(55, "Лена, если ты придёшь сюда, я ушёл к вокзалу. Я ждал три дня.", 3, 5, 18, 28, 33, 38, 40),
        note(56, "Не открывайте квартиру 27. Я серьёзно.", 5, 28),
        note(57, "Сегодня впервые за неделю видел птиц. Странно, насколько сильно я этому обрадовался.", 1, 2, 6, 19, 20, 22, 25, 26, 32),
        note(58, "Катя сказала, что море всё ещё выглядит так же. Хочу ей верить.", 18, 20, 22, 23, 24, 28, 38),
        note(59, "Если кто-нибудь найдёт эту тетрадь: меня зовут Максим. Мне было 24. Я был здесь.", 9, 18, 20, 28, 30, 31, 44, 46),
        note(60, "У нас осталось четыре банки фасоли. Сергей всё равно говорит, что завтра будет лучше.", 4, 18, 20, 28, 31, 34, 46),
        note(61, "Я скучаю по обычным пробкам. По людям, которые ругались из-за парковки. Даже по соседской дрели.", 3, 5, 28, 33, 35, 37, 40),
        note(62, "Сегодня мой день рождения. Мне 31. Никто, кроме меня, об этом не знает.", 5, 18, 20, 28, 32, 46),
        note(63, "Не знаю, куда ушёл папа. Он сказал ждать здесь. Я жду.", 3, 5, 7, 8, 18, 38),
        note(64, "Вчера слышали поезд. Или мне показалось. Пути ведь давно пустые.", 28, 37, 38, 39, 40),
        note(65, "Если вернусь, первым делом заведу собаку. Всегда хотел и всё откладывал.", 1, 5, 18, 20, 26, 28, 46),
        note(66, "Мы назвали крысу Геной. Теперь Гена живёт возле батареи и ворует хлеб.", 5, 9, 18, 28, 31, 46),
        note(67, "Я нашёл кофе. Настоящий. Выпили втроём и минут десять делали вид, будто всё нормально.", 5, 18, 20, 28, 34, 36, 46),
        note(68, "Никогда не думал, что буду скучать по понедельникам.", 5, 17, 28, 36, 37, 41, 49),
        note(69, "Оля, я не сердился. Если вдруг когда-нибудь прочитаешь это, знай.", 5, 18, 20, 28, 35, 38),
        note(70, "Сегодня не было слышно ни одного выстрела. Не знаю, хороший это знак или плохой.", 6, 18, 20, 26, 28, 32, 44, 46),
        note(71, "Мы решили больше не считать погибших. От этого никому не становится легче.", 10, 12, 18, 28, 44, 45, 48),
        note(72, "В шкафу оставил еду. Возьми, если тебе нужнее. Только банку мёда не трогай. Это для дочери.", 5, 18, 20, 28),
        note(73, "Я всё ещё разговариваю с фотографией жены. Наверное, глупо. Но она хотя бы не перебивает.", 5, 20, 26, 28, 32),
        note(74, "Если мир когда-нибудь снова станет нормальным, пусть кто-нибудь покрасит эту стену. Цвет ужасный.", 5, 8, 17, 18, 28, 34, 46),
        note(75, "Я не герой. Просто однажды мне некуда было убежать.", 9, 18, 20, 28, 31, 44, 45, 48),
        scene(76, "Меловой знак", "На стене круг с короткой стрелкой, нарисованный мелом. Рядом более старый такой же знак, почти смытый дождём. Ты запоминаешь его, хотя пока не понимаешь значения.", 3, 1, 3, 5, 7, 15, 18, 19, 21, 25, 26, 27, 28, 33, 35, 38, 40, 44, 45),
        scene(77, "Чужая семейная фотография", "В кармане оставленной куртки семейная фотография: мужчина с рассечённой бровью, женщина и две девочки у реки. На обороте только «Июль». Ты задерживаешь взгляд на лицах и бережно прячешь снимок между страницами своего блокнота.", 3, 5, 18, 20, 28, 35, 38, 46),
        scene(78, "Обычная песня", "В пустой комнате тихо играет старый радиоприёмник от почти разряженного аккумулятора. Не сообщение об эвакуации, не предупреждение — обычная песня. Между куплетами слышно, как заедает подключённая кассетная приставка. Это запись, а не работающая радиостанция. Ты ненадолго остаёшься у двери.", 3, 5, 18, 20, 28, 32, 36, 46),
        scene(79, "Последний свет", "В обесточенном здании одна лампа ещё горит от аварийного аккумулятора. Она освещает пустой коридор с выцветшими объявлениями. Ты машинально стараешься не задеть выключатель. Пусть здесь ещё немного будет светло.", 4, 8, 10, 12, 17, 18, 32, 43, 44, 49),
        scene(80, "Цветные дни", "На детском календаре дни зачёркнуты разными карандашами. Красный, зелёный, синий — будто кто-то старался сделать каждый день особенным. Последние четыре дня перечёркнуты одним чёрным. Коробка цветных карандашей рядом всё ещё полна.", 2, 5, 8, 18, 28),
        scene(81, "Через коридор", "На двух дверях напротив друг друга записки: «Есть вода?», «Оставлю у порога», «Спасибо. Как ваша мама?». Люди разговаривали, боясь выйти. Последнее «Вы там?» приклеено поверх предыдущего. На противоположной двери ответа нет.", 2, 5, 28, 46),
        scene(82, "Дневник погоды", "Кто-то ежедневно записывал температуру, ветер и осадки. Между отметками о стрельбе и пропавшей воде всё те же аккуратные столбцы. Последняя строка: «Ясно. Ветер южный». Ни объяснения, ни прощания.", 5, 6, 20, 22, 23, 26, 32, 43),
        scene(83, "Ход чёрных", "На столе незаконченная шахматная партия и две кружки. Белые сделали ход, чёрные нет. Рядом листок со счётом прошлых игр. Кто-то вёл его годами, терпеливо записывая даже ничьи.", 5, 5, 18, 20, 28, 34, 46),
        scene(84, "Возвращать необязательно", "В подъезде выставлены книги. На картонке: «Берите. Возвращать уже необязательно». На одной обложке свежий отпечаток пальца в пыли. Полка не пуста, но между томами видны промежутки.", 5, 5, 18, 28),
        scene(85, "Свежая отметка", "На стене среди выцветших надписей свежая стрелка. Краска ещё липнет к отщепившейся штукатурке, а на пыльном полу след обуви. Здесь были недавно. Почему-то эта мысль тревожит сильнее пустоты.", 3, 1, 3, 5, 9, 18, 21, 26, 28, 30, 31, 35, 38, 44, 47),
        scene(86, "Белые стрелки", "Заклинившая дверь вскрыта снаружи. За ней белой краской указано направление к выходу. Возле стрелки пустая упаковка бинта и отрезанные ремни, которыми, похоже, стягивали носилки. Кто-то помогал людям выбраться.", 3, 8, 9, 10, 12, 13, 15, 18, 28, 30, 31, 38, 44, 48, 49),
        scene(87, "Маленькая могила", "За зданием простой деревянный крест без имени. Рядом игрушечная машинка с одним потерянным колесом. Земля просела, но машинку кто-то недавно поставил обратно на камень. Ты обходишь это место, не наступая на траву у креста.", 1, 1, 7, 20, 28, 48),
        scene(88, "Просто ушли", "Обычная спальня: застеленная кровать, книги, фотография на стене. Нет следов борьбы, тел и последних признаний. В шкафу пустые плечики, на полке прямоугольник без пыли, где лежала сумка. Люди просто ушли. Комната продолжает ждать следующего вечера.", 6, 5, 20, 28, 46),
        scene(89, "После тебя", "В комнате, которую ты уже обыскивал, закрыт оставленный открытым шкаф. На столе стоит бутылка воды. Чужие следы почти неразличимы, борьбы не было. Ты точно помнишь, что в прошлый раз бутылки здесь не было. Пока ты ходил по другим местам, кто-то вернулся в это.", 3, 5, 18, 20, 28, 31, 34, 36, 46),
        scene(90, "Ответ", "Там, где ты оставил припасы, теперь пусто. Под камнем сложенный листок. На нём одно слово: «Спасибо». Ни имени, ни просьбы, ни указания, куда идти. Ты ещё раз смотришь на пустое место. Этого почему-то достаточно.", 4, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50)
    );
}

