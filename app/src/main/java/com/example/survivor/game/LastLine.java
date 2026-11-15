package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Persistent paths to the reserve circuit; no calendar-day gates. */
final class LastLine implements Serializable {
    private static final long serialVersionUID=1L;
    static final String BLOCK="Контактный блок подстанции №2";
    static final String FUSE="Предохранитель резервного контура";
    static final String FUEL="Канистра топлива";
    static final String RAIDERS="Подстанция: основной пост рейдеров";
    static final String INFECTED="Очистные сооружения: служебный обход";
    boolean migrated, raidersDefeated, serviceClue, bypassCleared, bypassOpen, powerRestored, eastGateOpen, blockTaken;
    boolean bunkerClue, checkpointClue, barracksClue;
    boolean panelInspected,panelRepaired,generatorRunning,circuitB,circuitC,technicalLoot,knowledgeRecorded;

    void searchClue(int place,Location.SearchPoint point,WorldStories w){
        if(point==null)return;
        String title=null,text=null;
        if(place==44 && point.name.equals("Пункт управления") && !bunkerClue){
            bunkerClue=true;title="Схема промышленного бункера";
            text="На схеме отмечены укреплённый шлюз и запасной выход. Приписка предупреждает о вооружённых людях возле подстанции; дата записи неразборчива.";
        }else if(place==45 && point.name.equals("КПП") && !checkpointClue){
            checkpointClue=true;title="Запись северного блокпоста";
            text="В журнале северный проезд к военному складу отмечен как закрытый. Кто и когда отдавал дальнейшие приказы, из записи неясно.";
        }else if(place==46 && point.name.equals("Комната сержанта") && !barracksClue){
            barracksClue=true;title="Запись гарнизона";
            text="В списке гарнизона упомянут последний вылет и ожидание распоряжений. Подтверждения вылета или перечня пассажиров среди сохранившихся листов нет.";
        }
        if(title!=null){w.note(title,text);GameIO.out().println(text);GameIO.checkpoint();}
    }

    void migrate(Locations locations){
        // Old saves may already have completed the former one-step repair.
        if(powerRestored||eastGateOpen){panelInspected=true;panelRepaired=true;generatorRunning=true;powerRestored=true;}
        if(migrated)return;
        // Earlier versions did not record a distinct raider group. A fully searched
        // substation is the only reliable evidence that the player already cleared it.
        Location substation=locations.getLocation(43);
        if(locations.isVisited(43) && substation.isDepleted())raidersDefeated=true;
        migrated=true;
    }
    void battleWon(String terrain){
        if(RAIDERS.equals(terrain))raidersDefeated=true;
        if(INFECTED.equals(terrain))bypassCleared=true;
    }
    boolean canAccessEquipment(){return raidersDefeated||bypassOpen;}
    private boolean spend(Fighter p,WorldStories w,int minutes,int stamina){
        if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Отдохните и вернитесь позже.");return false;}
        w.advanceMinutes(minutes);GameIO.checkpoint();return true;
    }
    private void note(WorldStories w,String title,String text){w.note(title,text);GameIO.checkpoint();}
    private ItemInfo item(Fighter p,String name){return p.getInventory().stream().filter(i->i.getName().equals(name)).findFirst().orElse(null);}
    private boolean hasTool(Fighter p){return p.getInventory().stream().anyMatch(i->i.getDurability()>0 && (i.isRepairTool() || i.getName().toLowerCase(java.util.Locale.ROOT).contains("лом")));}
    private boolean knowsDestination(WorldStories w){return w.routeAfterArchive!=null && (w.routeAfterArchive.cargoFound||w.routeAfterArchive.gateSeen);}
    void syncKnowledge(WorldStories w){
        if(!knowledgeRecorded && knowsDestination(w) && (panelInspected||generatorRunning||powerRestored)){
            knowledgeRecorded=true;
            note(w,"Подстанция и восточный КПП",powerRestored?"Документы подтверждают: ранее включённый контур А питает восточный технический проход к военному складу.":"Документы связывают резервный контур подстанции №2 с восточным техническим проходом военного склада.");
        }
    }
    private void panel(Fighter p,WorldStories w,Scanner scanner){
        while(p.isAlive()){
            GameIO.menu("Распределительный щит",panelRepaired?"Контактный блок и предохранитель установлены. Автоматический ввод резерва остаётся неисправным: запуск выполняется вручную.":"Индикаторы погасли. Один автоматический выключатель повреждён; на дверце схема резервных линий.");
            if(!panelInspected)GameIO.action(1,"Осмотреть щит и ведомость","10 минут · 2 силы");
            if(panelInspected&&!panelRepaired){
                boolean hasParts=item(p,BLOCK)!=null&&item(p,FUSE)!=null&&hasTool(p);
                GameIO.action(2,"Отремонтировать щит","Контактный блок + предохранитель + инструмент · 20 минут · 5 сил",hasParts);
                if(!hasParts)GameIO.card("Что требуется","Нужны контактный блок, предохранитель и исправный инструмент или лом в рюкзаке. Детали можно найти при обыске или купить; комплект также отмечен на грузовом терминале.");
            }
            GameIO.action(0,"Назад");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1&&!panelInspected&&spend(p,w,10,2)){
                panelInspected=true;note(w,"Ведомость резервных линий","«Линия восточного КПП переведена на резервный контур. Автоматический ввод резерва неисправен. Запуск только вручную».");syncKnowledge(w);
                ResidentialStories.pause("Ведомость", "«Линия восточного КПП переведена на резервный контур. Автоматический ввод резерва неисправен. Запуск только вручную». Нужны контактный блок и предохранитель.",scanner);
            }
            if(c==2&&panelInspected&&!panelRepaired){ItemInfo block=item(p,BLOCK),fuse=item(p,FUSE);if(block==null||fuse==null||!hasTool(p))continue;
                if(!spend(p,w,20,5))continue;p.getInventory().remove(block);p.getInventory().remove(fuse);panelRepaired=true;
                note(w,"Щит восстановлен","Контактный блок и предохранитель установлены. Для подачи питания нужен резервный генератор и ручной выбор контура.");
                ResidentialStories.pause("Щит восстановлен","Джон заменяет повреждённые детали. Щит готов, но сам по себе ещё не даёт электричества.",scanner);
            }
        }
    }
    private void generator(Fighter p,WorldStories w,Scanner scanner){
        while(p.isAlive()){
            GameIO.menu("Резервный генератор",generatorRunning?"Генератор работает; расход топлива учтён при первом запуске.":"Генератор долго не обслуживали. Для запуска нужно подготовить щит и залить топливо.");
            if(!generatorRunning)GameIO.action(1,"Подготовить и запустить генератор","Топливо · 25 минут · 5 сил",panelRepaired&&item(p,FUEL)!=null);
            GameIO.action(0,"Назад");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1&&!generatorRunning&&panelRepaired){ItemInfo fuel=item(p,FUEL);if(fuel==null||!spend(p,w,25,5))continue;p.getInventory().remove(fuel);generatorRunning=true;
                note(w,"Резервный генератор","Генератор запущен. На щите загорелся жёлтый индикатор; нужный контур ещё необходимо выбрать.");
                ResidentialStories.pause("Запуск генератора","Джон открывает технический кожух. Внутри пахнет старым маслом, на контактах следы окисления. После нескольких попыток двигатель запускается. На распределительном щите загорается жёлтый индикатор.",scanner);
            }
        }
    }
    private void control(Fighter p,WorldStories w,Scanner scanner){
        while(p.isAlive()){
            GameIO.menu("Пост управления",generatorRunning?"Схема распределения энергии: каждый контур можно включить отдельно.":"Схема на стене цела, но генератор пока не подаёт питание.");
            if(generatorRunning){
                GameIO.action(1,"Контур А · восточный технический КПП",powerRestored?"Питание подано":"Основной маршрут · 5 минут",!powerRestored);
                GameIO.action(2,"Контур Б · узел связи",circuitB?"Питание подано":"Дополнительные записи · 5 минут",!circuitB);
                GameIO.action(3,"Контур В · технические помещения",circuitC?"Питание подано":"Дополнительное обследование · 5 минут",!circuitC);
                if(circuitB)GameIO.action(4,"Прочитать записи узла связи");
                if(circuitC)GameIO.action(5,"Осмотреть служебное оборудование",technicalLoot?"Уже осмотрено":"Технические материалы",!technicalLoot);
            }
            GameIO.action(0,"Назад");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1&&generatorRunning&&!powerRestored&&spend(p,w,5,1)){
                powerRestored=true;note(w,"Контур А включён",knowsDestination(w)?"Питание подано на восточный технический КПП военного склада; безопасность и эвакуация не подтверждены.":"Питание подано на восточный технический КПП. Куда ведёт проход, пока неизвестно.");syncKnowledge(w);
                ResidentialStories.pause("Контур А","На схеме загорается отметка восточного технического КПП. Питание подано; сами ворота потребуется проверить отдельно.",scanner);
            }
            if(c==2&&generatorRunning&&!circuitB&&spend(p,w,5,1)){circuitB=true;note(w,"Контур Б","На узел связи подано резервное питание.");}
            if(c==3&&generatorRunning&&!circuitC&&spend(p,w,5,1)){circuitC=true;note(w,"Контур В","На служебное оборудование подано резервное питание.");}
            if(c==4&&circuitB){String text="«Колонна отменена. Пункт гражданского сбора сохранять до отдельного распоряжения».\n\n«Автоматическое вещание „Маяка“ не отключать. Резервный пункт продолжает работу».\n\n«Подтверждение приёма гражданских отсутствует».";if(w.journal().stream().noneMatch(x->x.contains("Записи узла связи")))note(w,"Записи узла связи",text);ResidentialStories.pause("Записи узла связи",text,scanner);}
            if(c==5&&circuitC&&!technicalLoot){ItemInfo found=Shop.createItem(19);if(!p.addItem(found)){GameIO.out().println("В рюкзаке нет места. Материалы останутся здесь.");continue;}technicalLoot=true;GameIO.sound(GameSound.BAG);note(w,"Служебное оборудование","В техническом помещении найдены запчасти.");}
        }
    }
    /** Returns true when player elects to continue the existing ordinary search. */
    boolean visit(Fighter p,Locations locations,int place,WorldStories w,Scanner scanner,Random random){
        migrate(locations);
        boolean viaBypass=false;
        while(p.isAlive()){
            boolean access=raidersDefeated||viaBypass;
            if(place==42){
                GameIO.menu("Очистные сооружения","Мокрые мостки, технические помещения и ограждения. За сооружениями может быть путь к подстанции.");
                GameIO.action(1,"Искать служебный маршрут",serviceClue?"Маршрут найден":"15 минут · 4 силы",!serviceClue);
                if(serviceClue&&!bypassCleared)GameIO.action(2,"Проверить заражённый проход","20 минут · 6 сил · возможен бой");
                if(serviceClue&&bypassCleared&&!bypassOpen)GameIO.action(3,"Открыть служебное ограждение","Нужен лом · 15 минут · 4 силы",locations.getLocation(42).hasCrowbar(p));
                if(bypassOpen)GameIO.card("Обход открыт","Служебный путь к технической части подстанции доступен при следующих посещениях.");
                GameIO.action(9,"Обыскать сооружения","Обычные точки поиска");GameIO.action(0,"Вернуться домой");
                int c=SurvivalSystems.choice(scanner);if(c==0)return false;if(c==9)return true;
                if(c==1&&!serviceClue&&spend(p,w,15,4)){serviceClue=true;note(w,"Служебный маршрут очистных","Схема показывает проход к тыльной стороне подстанции. Между сооружениями замечены заражённые; ограждение придётся открыть.");ResidentialStories.pause("Служебная схема","За насосной сохранилась схема обслуживания. Узкий проход ведёт к тыльной стороне подстанции, минуя главный въезд. На мостках заметны заражённые.",scanner);}
                if(c==2&&serviceClue&&!bypassCleared&&spend(p,w,20,6)){
                    boolean won=TacticalBattle.story(p,scanner,random,List.of(new Zombie(1,ZombieType.NORMAL),new Zombie(2,ZombieType.NORMAL)),INFECTED,List.of(),false,false,false);
                    if(!won)return false;bypassCleared=true;note(w,"Проход через очистные","Мостки очищены от заражённых. Дальше остаётся служебное ограждение.");
                }
                if(c==3&&serviceClue&&bypassCleared&&!bypassOpen&&locations.getLocation(42).hasCrowbar(p)&&spend(p,w,15,4)){
                    bypassOpen=true;note(w,"Обход подстанции","Ограждение открыто. Через очистные сооружения можно попасть к технической части подстанции, минуя основной пост рейдеров.");ResidentialStories.pause("Путь открыт","Джон освобождает проход ломом. Позади остаются тёмные бассейны; впереди — тыльная сторона подстанции. Открытый путь сохранится.",scanner);
                }
            }else{
                syncKnowledge(w);
                GameIO.menu("Электроподстанция №2",access?"Выберите технический объект или обычные точки поиска.":"Вооружённые рейдеры держат главный въезд. Их можно обойти через очистные сооружения.");
                if(!raidersDefeated)GameIO.action(1,"Прорваться через главный въезд","Вооружённые рейдеры · высокий риск");
                if(bypassOpen)GameIO.action(2,"Пройти через очистные","Тыльный технический проход открыт");
                if(access){
                    GameIO.action(3,"Техническое помещение · распределительный щит",panelRepaired?"Щит отремонтирован":panelInspected?"Нужны контактный блок и предохранитель":"Осмотреть щит и схему");
                    GameIO.action(4,"Резервный генератор",generatorRunning?"Генератор работает":panelRepaired?"Нужно топливо":"Сначала восстановите щит");
                    GameIO.action(5,"Пост управления",generatorRunning?"Выбор резервных контуров":"Генератор выключен");
                    GameIO.action(9,"Обыскать подстанцию","Обычные точки поиска");
                }
                GameIO.action(0,"Вернуться домой");int c=SurvivalSystems.choice(scanner);if(c==0)return false;if(c==9&&access)return true;
                if(c==1&&!raidersDefeated){
                    List<CombatEnemy> group=List.of(new HostileSurvivor(1,true,random),new HostileSurvivor(2,true,random));
                    boolean won=TacticalBattle.story(p,scanner,random,group,RAIDERS,List.of(),false,false,false);
                    if(!won)return false;raidersDefeated=true;note(w,"Подстанция: главный въезд","Основной пост рейдеров уничтожен. Эти противники больше не занимают проход к оборудованию.");
                }
                if(c==2&&bypassOpen){if(!spend(p,w,20,5))continue;viaBypass=true;ResidentialStories.pause("Тыльный проход","Джон проходит по служебному маршруту от очистных к технической части подстанции. Главный пост рейдеров остаётся позади.",scanner);}
                if(c==3&&access)panel(p,w,scanner);
                if(c==4&&access)generator(p,w,scanner);
                if(c==5&&access)control(p,w,scanner);
            }
        }
        return false;
    }
}
