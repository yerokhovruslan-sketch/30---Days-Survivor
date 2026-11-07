package com.example.survivor.game;

import java.io.Serializable;
import java.util.Scanner;

/** Independent investigations following the police archive. */
final class RouteAfterArchive implements Serializable {
    private static final long serialVersionUID=1L;
    private static final String KIT="Аптечка экипажа №17";
    boolean gateSeen, guardJournal, roster, report, metOlga, kitRequested, kitTaken, kitReturned, routeSheet;
    boolean olgaIntroComplete, warehouseTalk, driverLead;
    boolean vanInspected, garageNote, cabinetNote, watchmanSearched, garageOpen, repairReceipt;
    boolean workshopVan, workshopJournal, terminalPass, yardMap;
    boolean terminalIntro, terminalDocument1, terminalDocument2, terminalDocument3, puzzleSolved, cargoFound, terminalExitShown, nightReturnShown;
    int cargoProgress, cargoDay;
    private java.util.Set<String> ordinaryLoot;

    void open(Fighter p,WorldStories w,int place,Scanner scanner) {
        if(place==41)industrial(w,scanner);

    }
    private void industrial(WorldStories w,Scanner scanner) {
        while(true){
            GameIO.storyMenu("Резервный маршрут","Схема из архива приводит Джона к бетонным ограждениям промышленной территории. Возле брошенного грузовика стоит технический контрольно-пропускной пункт. Ворота закрыты.");
            GameIO.action(1,"Попробовать открыть ворота",gateSeen?"Питание отсутствует":"Осмотреть замок");
            GameIO.action(2,"Изучить журнал дежурного",guardJournal?"Уже прочитан":"Проверить КПП");
            GameIO.action(0,"Вернуться");
            int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1){gateSeen=true;String text="Электронный замок не реагирует. Индикаторы погасли. Ручной механизм заблокирован изнутри.\n\nРядом выцветшая табличка: «РЕЗЕРВНОЕ ЭЛЕКТРОСНАБЖЕНИЕ. ПОДСТАНЦИЯ №2».";w.note("Подстанция №2",text+" Нужно выяснить, как восстановить питание служебного маршрута.");GameIO.checkpoint();ResidentialStories.pause("Закрытые ворота",text,scanner);}
            if(c==2){guardJournal=true;String text="«Основную колонну отменили. Приказ о закрытии пункта сбора не поступал. Гражданские продолжают прибывать».";w.note("Журнал дежурного КПП",text);GameIO.checkpoint();ResidentialStories.pause("Последняя запись",text,scanner);}
        }
    }

    static final String KEY="Ключ сторожа от гаража", PASS="Повреждённый пропуск терминала";
    boolean migrated, serviceOpen, watchmanCleared;
    int olgaReply;
    static boolean handles(int place){return place==13||place==35||place==36||place==37;}
    private boolean has(Fighter p,String item){return p.getInventory().stream().anyMatch(i->i.getName().equals(item));}
    private boolean take(Fighter p,String item){
        if(has(p,item))return true;
        if(p.addItem(new Tools(item,"Сюжетный предмет",1,0)))return true;
        GameIO.out().println("В рюкзаке нет места. Предмет остался здесь.");
        return false;
    }
    private boolean tool(Fighter p){
        return p.getInventory().stream().anyMatch(i->i instanceof Weapons weapon && weapon.getStyle()==WeaponStyle.CROWBAR && i.getDurability()>0);
    }
    private boolean spend(Fighter p,WorldStories w,int minutes,int stamina){
        if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил.");return false;}
        w.advanceMinutes(minutes);return true;
    }
    void visit(Fighter p, Location location, WorldStories w, int place, Scanner scanner){
        if(!migrated && (!terminalPass || has(p,PASS) || p.getStorage().stream().anyMatch(i->i.getName().equals(PASS)) || take(p,PASS))){
            migrated=true;if(terminalPass)GameIO.checkpoint();
        }
        if(place==35 && garageOpen)location.getSearchPoints().stream()
                .filter(point->point.name.equals("Металлический гараж")).findFirst().ifPresent(Location.SearchPoint::open);
        if(place==37){
            if(!terminalIntro){terminalIntro=true;w.note("Грузовой терминал","Пути, контейнеры, брошенные фуры и грузовик с открытой дверью. Диспетчерская хранит разрозненные бумажные записи.");GameIO.checkpoint();ResidentialStories.pause("Грузовой терминал","Джон оказывается среди железнодорожных путей, грузовых контейнеров и брошенных фур. Возле погрузочного крана стоит грузовик с открытой дверью. В диспетчерской документы разложены по нескольким столам. После отключения электричества учёт продолжили вручную; часть накладных отсутствует.",scanner);}
        }
        while(p.isAlive()&&w.pendingBattle(place)==null&&!w.residential().aborted){
            GameIO.menu(location.getName(),"Выберите место для осмотра.");
            var points=location.getSearchPoints();
            for(int i=0;i<points.size();i++){
                var point=points.get(i);
                GameIO.point(i+1,point.name,point.description,point.isSearched());
            }
            GameIO.action(90,"Передохнуть","20 минут");GameIO.action(0,"Вернуться домой");
            int n=SurvivalSystems.choice(scanner);
            if(n==0)return;
            if(n==90){Survivor.rest(p,30);continue;}
            if(n>0&&n<=points.size())inspect(p,location,points.get(n-1),w,place,scanner);
        }
    }
    private void inspect(Fighter p,Location location,Location.SearchPoint point,WorldStories w,int place,Scanner scanner){
        while(p.isAlive()&&w.pendingBattle(place)==null&&!w.residential().aborted){
            GameIO.menu(point.name,point.description);
            boolean special=true;
            switch(point.name){
                case "Диспетчерская" -> {
                    if(place==13 && w.archiveComplete){
                        GameIO.action(1,"Проверить список экипажей");
                        GameIO.action(2,"Изучить отчёт диспетчера");
                    }else if(place==37){
                        GameIO.action(1,"Изучить распоряжение");GameIO.action(2,"Изучить запись о доставке");GameIO.action(3,"Проверить журнал транспорта");
                        if(terminalDocument1&&terminalDocument2&&terminalDocument3&&!puzzleSolved)GameIO.action(4,"Восстановить грузовой маршрут");
                    }else special=false;
                }
                case "Комната отдыха" -> {
                    GameIO.action(3,metOlga?"Поговорить с Олей":"Осмотреть комнату","",w.archiveComplete);
                    if(w.residential().points(place,w).contains(1))GameIO.action(7,"Поговорить с Мариной");
                }
                case "Служебный микроавтобус" -> GameIO.action(1,"Осмотреть салон");
                case "Гаражный бокс" -> GameIO.action(2,"Прочитать записку");
                case "Сторожка" -> {
                    GameIO.action(4,"Обыскать погибшего сторожа","5 минут",!watchmanSearched);
                    if(w.residential().points(place,w).contains(2))GameIO.action(7,"Проверить зацепку Кирилла");
                }
                case "Металлический гараж" -> {
                    if(!garageOpen){
                        GameIO.action(3,"Вскрыть гараж ломом","20 минут · риск нападения",tool(p));
                        GameIO.action(4,"Открыть ключом сторожа","5 минут",has(p,KEY));
                    }else{GameIO.action(5,"Осмотреть шкаф");GameIO.action(6,"Проверить инструменты и квитанцию");}
                }
                case "Подъёмники" -> GameIO.action(1,"Осмотреть фургон");
                case "Кабинет мастера" -> {
                    if(point.isLocked())GameIO.action(8,"Вскрыть кабинет ломом","10 минут · риск нападения",tool(p));
                    else{
                        GameIO.action(2,"Изучить журнал заказов");
                        GameIO.action(3,"Обыскать металлический шкафчик","5 минут",!terminalPass);
                        GameIO.action(4,"Изучить схему площадок");
                    }
                }
                case "Площадка Г · служебный сектор" -> {
                    if(!serviceOpen)GameIO.action(8,"Открыть служебный проход","5 минут · нужен пропуск",has(p,PASS));
                    else if(puzzleSolved)GameIO.action(cargoFound?6:5,cargoFound?"Перечитать документы Б-17":"Открыть контейнер Б-17");
                    else GameIO.card("Служебный сектор","Место хранения уточняется по документам диспетчерской.");
                }
                default -> special=false;
            }
            boolean storyContainer = !point.name.equals("Комната отдыха") && !point.name.equals("Площадка Г · служебный сектор") && !point.name.equals("Металлический гараж") && !point.name.equals("Кабинет мастера");
            String lootKey=place+":"+point.name;
            boolean looted=point.isSearched() || ordinaryLoot!=null && ordinaryLoot.contains(lootKey);
            if(storyContainer && special)GameIO.action(9,"Обыскать вещи",looted?"Уже обыскано":"10 минут · 3 силы",!looted);
            if(!special)GameIO.action(9,"Обыскать",point.isLocked()?"Нужен лом":"10 минут",!point.isSearched()&&(!point.isLocked()||tool(p)));
            GameIO.action(0,"Назад к точкам");
            int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==9&&!special&&!point.isSearched()&&(!point.isLocked()||tool(p))&&spend(p,w,10,3)){
                if(point.isLocked()){
                    point.open();GameIO.checkpoint();
                    if(!BreakIn.attack(p,scanner,java.util.concurrent.ThreadLocalRandom.current()))return;
                }
                ItemInfo found=location.searchPoint(point,java.util.concurrent.ThreadLocalRandom.current(),0);
                if(found!=null)SurvivalSystems.collect(p,java.util.List.of(found),scanner);
                GameIO.checkpoint();
            }
            if(c==9 && storyContainer && special && !looted && spend(p,w,10,3)){
                ItemInfo item=Item.createLootForLocation(location,java.util.concurrent.ThreadLocalRandom.current());
                if(item==null || p.addItem(item)){
                    if(ordinaryLoot==null)ordinaryLoot=new java.util.HashSet<>();
                    ordinaryLoot.add(lootKey);
                    if(item!=null){GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());}
                    else GameIO.out().println("Полезных вещей не осталось.");
                    GameIO.checkpoint();
                }else GameIO.out().println("Рюкзак полон. Освободите место и вернитесь.");
            }
            switch(point.name){
                case "Диспетчерская" -> {
                    if(place==13&&w.archiveComplete){if(c==1&&(roster||spend(p,w,5,1))){boolean fresh=!roster;roster=true;String t="Возле номера 17 сделана отметка: «Возвращён. Без пассажиров. Экипаж не опрошен». Время записи и судьба пациентов неизвестны.";if(fresh)w.note("Список экипажей",t);GameIO.checkpoint();ResidentialStories.pause("Список экипажей",t,scanner);}
if(c==2&&(report||spend(p,w,5,1))){boolean fresh=!report;report=true;String t="Отчёт с пропущенными строками подтверждает: машина №17 добралась до пункта сбора и вернулась. На следующей странице: «Приём гражданских прекращён. У въезда ожидали не менее двадцати человек. Распоряжений по размещению не поступало».";if(fresh)w.note("Отчёт экипажа №17",t);GameIO.checkpoint();ResidentialStories.pause("Отчёт диспетчера",t,scanner);}}
                    else if(place==37){if(c==1&&(terminalDocument1||spend(p,w,5,1))){boolean fresh=!terminalDocument1;terminalDocument1=true;if(fresh)w.note("Распоряжение о грузе","Гуманитарная поставка перенаправлена по распоряжению эвакуационного штаба. В накладной М-24 указаны медицинские и гуманитарные грузы, площадка Б.");GameIO.checkpoint();ResidentialStories.pause("Распоряжение","«Гуманитарная поставка. Перенаправлено по распоряжению эвакуационного штаба». Накладная М-24: медицинские и гуманитарные грузы, площадка Б.",scanner);}
if(c==2&&(terminalDocument2||spend(p,w,5,1))){boolean fresh=!terminalDocument2;terminalDocument2=true;if(fresh)w.note("Запись о доставке","Груз доставлен на терминал, дальнейшее направление не подтверждено. Контейнер Б-17 не отгружен; поздняя отметка указывает защищённую зону Г.");GameIO.checkpoint();ResidentialStories.pause("Запись о доставке","«Груз доставлен на терминал. Дальнейшее направление не подтверждено». Ручная отметка: Б-17 не отгружен, перемещён в защищённую зону Г.",scanner);}
if(c==3&&(terminalDocument3||spend(p,w,5,1))){boolean fresh=!terminalDocument3;terminalDocument3=true;if(fresh)w.note("Журнал транспорта","Машина скорой помощи №17 проходила через терминал в день перераспределения поставок; перевозчиком груза её не записали.");GameIO.checkpoint();ResidentialStories.pause("Журнал транспорта","Машина скорой помощи №17 проходила через терминал в день перераспределения поставок. Перевозчиком груза её не записали. Внизу приписка: «Проверить Б-17 в зоне Г».",scanner);}
if(c==4&&terminalDocument1&&terminalDocument2&&terminalDocument3&&!puzzleSolved)puzzle(w,scanner);}
                }
                case "Комната отдыха" -> {
                    if(c==3&&w.archiveComplete)olga(p,w,scanner);
                    if(c==7&&w.residential().points(place,w).contains(1))
                        w.residential().run(1,p,place,w,scanner,java.util.concurrent.ThreadLocalRandom.current());
                }
                case "Служебный микроавтобус" -> {if(c==1&&(vanInspected||spend(p,w,5,1))){if(!vanInspected){vanInspected=true;w.note("Микроавтобус у гаражей","На повреждённом микроавтобусе старая наклейка станции скорой помощи. Это не машина №17. Внутри сигареты, пустая бутылка и рабочая перчатка.");GameIO.checkpoint();}ResidentialStories.pause("Служебный микроавтобус","Передняя часть повреждена. На стекле старая наклейка скорой помощи, но это не машина №17. Внутри — пачка сигарет, пустая бутылка воды и рабочая перчатка.",scanner);}}
                case "Гаражный бокс" -> {if(c==2&&(garageNote||spend(p,w,5,1))){if(!garageNote){garageNote=true;w.note("Записка для Димы","Деталь оставили у Серого в мастерской. Если Дима поедет к терминалу, ему понадобятся документы. Возможно, это водитель машины №17.");GameIO.checkpoint();}ResidentialStories.pause("Записка в гараже","«Димка, деталь привёз... Я не знаю куда ее ложить так-что оставил у Серого в мастерской имей ввиду. Если опять поедешь к терминалу, не забудь документы. Там без них ворота хрен кто откроет, лично я не люблю ходить дважды.»\n\nВозможно, Дима — водитель семнадцатой машины, но пока это не доказано.",scanner);}}
                case "Сторожка" -> {
                    if(c==7&&w.residential().points(place,w).contains(2))
                        w.residential().run(2,p,place,w,scanner,java.util.concurrent.ThreadLocalRandom.current());
                    if(c==4&&!watchmanSearched&&spend(p,w,5,2)){if(!watchmanCleared&&GameIO.supportsTactics()){Zombie injured=new Zombie("Раненый заражённый","Подбитый заражённый возле сторожа",15);ResidentialStories.pause("Сторож","Сторож мёртв. Когда Джон наклоняется к телу, рядом поднимается раненый заражённый.",scanner);if(!TacticalBattle.story(p,scanner,java.util.concurrent.ThreadLocalRandom.current(),java.util.List.of(injured),"Гаражный кооператив",java.util.List.of(),false,false,true))return;}watchmanCleared=true;GameIO.checkpoint();if(!take(p,KEY))continue;watchmanSearched=true;w.note("Ключ сторожа","У погибшего сторожа найден ключ от гаража с инструментами.");GameIO.checkpoint();ResidentialStories.pause("Ключ сторожа","У погибшего сторожа нашёлся ключ. Теперь можно открыть гараж с инструментами.",scanner);}
                }
                case "Металлический гараж" -> {
                    if(c==4&&!garageOpen&&has(p,KEY)&&spend(p,w,5,1)){
                        garageOpen=true;point.open();GameIO.checkpoint();
                    }
                    if(c==3&&!garageOpen&&tool(p)&&spend(p,w,20,4)){garageOpen=true;point.open();p.addNoise(30);GameIO.checkpoint();ResidentialStories.pause("Вскрытие гаража","Джон тратит двадцать минут на замок. Скрежет разносится между металлическими воротами.",scanner);if(!BreakIn.attack(p,scanner,java.util.concurrent.ThreadLocalRandom.current()))return;garageOpen=true;w.note("Гараж с инструментами","Замок вскрыт.");GameIO.checkpoint();}
if(c==5&&garageOpen&&(cabinetNote||spend(p,w,5,1))){if(!cabinetNote){cabinetNote=true;w.note("Запись о Дмитрии","Дима забрал ремкомплект и поехал к грузовому терминалу; обещал вернуться к вечеру. Его судьба неизвестна.");GameIO.checkpoint();}ResidentialStories.pause("Запись в шкафу","«Дима забрал ремкомплект. Сказал, что едет к грузовому терминалу типа шо-то ему там нужно взять. Вернуться обещал к вечеру».\n\nШкаф находится внутри открытого гаража; запись не сообщает, вернулся ли Дмитрий.",scanner);}
if(c==6&&garageOpen&&(repairReceipt||spend(p,w,5,1))){if(!repairReceipt){repairReceipt=true;w.note("Квитанция на ремонт","В гараже найдена квитанция с названием автомастерской. Она отмечена на карте.");GameIO.checkpoint();}ResidentialStories.pause("Квитанция на ремонт","Среди инструментов лежит квитанция с названием автомастерской. Джон отмечает её на карте.",scanner);}
                }
                case "Подъёмники" -> {if(c==1&&(workshopVan||spend(p,w,5,1))){workshopVan=true;GameIO.checkpoint();ResidentialStories.pause("Фургон на подъёмнике","Старый грузовой фургон стоит на подъёмнике. Вокруг разбросаны инструменты, на полу лежит снятый аккумулятор.",scanner);}}
                case "Кабинет мастера" -> {
                    if(c==8&&point.isLocked()&&tool(p)&&spend(p,w,10,3)){
                        point.open();GameIO.checkpoint();
                        if(!BreakIn.attack(p,scanner,java.util.concurrent.ThreadLocalRandom.current()))return;
                    }
                    if(!point.isLocked()){if(c==2&&(workshopJournal||spend(p,w,5,1))){if(!workshopJournal){workshopJournal=true;w.note("Заказ на ремонт","Дмитрий уехал без ремонта к терминалу за бумагами, которые могли доказать что-то о перевозках.");GameIO.checkpoint();}ResidentialStories.pause("Журнал заказов","«Служебный автомобиль. Замена ремня и проверка генератора. Водитель Дмитрий. Оплата не получена».\n\nНиже: «Уехал без ремонта. Торопился на терминал. Сказал, что там остались бумаги, без которых никто не поверит».",scanner);}
if(c==3&&!terminalPass&&spend(p,w,5,1)&&take(p,PASS)){if(!terminalPass){terminalPass=true;w.note("Пропуск грузового терминала","Найдена повреждённая карточка: она позволяет пройти в служебную часть терминала, но не открывает все двери. Карточка лежит в рюкзаке, а сведения о ней внесены в журнал.");GameIO.checkpoint();}ResidentialStories.pause("Повреждённая карточка","В металлическом шкафчике лежит старая карточка грузового терминала. Она повреждена: пригодится для служебной части, но не откроет все двери. Джон убирает её в рюкзак.",scanner);}
if(c==4&&(yardMap||spend(p,w,5,1))){if(!yardMap){yardMap=true;w.note("Схема грузовых площадок","А — стройматериалы; Б — медицинские и гуманитарные грузы; В — техническое оборудование; Г — закрытый военный сектор.");GameIO.checkpoint();}ResidentialStories.pause("Схема площадок","Площадка А — обычные строительные материалы.\nПлощадка Б — медицинские и гуманитарные грузы.\nПлощадка В — техническое оборудование.\nПлощадка Г — закрытый военный сектор.",scanner);}}
                }
                case "Площадка Г · служебный сектор" -> {
                    if(c==8&&!serviceOpen&&has(p,PASS)&&spend(p,w,5,1)){
                        serviceOpen=true;point.open();GameIO.checkpoint();
                    }
                    if(serviceOpen){if(c==5&&puzzleSolved&&!cargoFound){boolean tool=p.getInventory().stream().anyMatch(item->(item.isRepairTool() || item.getName().toLowerCase(java.util.Locale.ROOT).contains("лом") || item.getName().toLowerCase(java.util.Locale.ROOT).contains("монтиров")) && item.getDurability()>0);if(!terminalPass||!tool){ResidentialStories.pause("Контейнер Б-17","Нужны пропуск в служебную часть и исправный инструмент или лом в рюкзаке. Одну пломбу сорвали, но второй замок ещё держится.",scanner);continue;}if(!spend(p,w,10,3))continue;cargoFound=true;cargoDay=w.day();w.note("Последний груз","Передачу груза на военный склад отменили; резерв оставили на терминале до подтверждения получателя. Самовольная выдача запрещена. В папке найдена неподписанная запись Дмитрия; его судьба неизвестна.");GameIO.checkpoint();cargoDocuments(scanner);}
if(c==6&&cargoFound)cargoDocuments(scanner);}
                }
                default -> { }
            }
            boolean done=switch(point.name){
                case "Диспетчерская" -> place==13?roster&&report:terminalDocument1&&terminalDocument2&&terminalDocument3;
                case "Комната отдыха" -> driverLead;
                case "Служебный микроавтобус" -> vanInspected;
                case "Гаражный бокс" -> garageNote;
                case "Сторожка" -> watchmanSearched;
                case "Металлический гараж" -> cabinetNote&&repairReceipt;
                case "Подъёмники" -> workshopVan;
                case "Кабинет мастера" -> workshopJournal&&terminalPass&&yardMap;
                case "Площадка Г · служебный сектор" -> cargoFound;
                default -> false;
            };
            if(done&&!point.isSearched()){location.consumePoint(point);w.markSearched(place);GameIO.checkpoint();}
        }
    }
    private static final int[] CARGO_SEQUENCE={2,5,8,11};
    private static final String[] CARGO_LABELS={"А-09 · стройматериалы · отгружен","Б-24 · медгруз · накладная М-24","В-04 · оборудование · не отгружен","Б-12 · медгруз · отгружен","Б-17 · медгруз · не отгружен","А-21 · стройматериалы · не отгружен","В-11 · оборудование · перемещён в В","Б-17 · отметка перемещения в Г","Г-08 · оборудование · отгружен","Г-12 · пломбы целы · сверка завершена","Б-17 · сектор Г · одна пломба сорвана","Г-19 · пломбы целы · сверка не нужна"};
    boolean cargoChoice(int choice){
        if(choice<1||choice>12||puzzleSolved)return false;
        if(choice==CARGO_SEQUENCE[cargoProgress])cargoProgress++;else cargoProgress=0;
        if(cargoProgress==4)puzzleSolved=true;
        return puzzleSolved;
    }
    private boolean cargoSelected(int choice){for(int i=0;i<cargoProgress && i<CARGO_SEQUENCE.length;i++)if(CARGO_SEQUENCE[i]==choice)return true;return false;}
    private void puzzle(WorldStories w,Scanner scanner){
        GameIO.musicCue("tense");
        while(!puzzleSolved){
            GameIO.menu("Восстановить грузовой маршрут","Выберите четыре карточки по порядку: накладная → контейнер → маршрут → проверка. Ошибка сбросит последовательность. Сверяйтесь с тремя документами диспетчерской.");
            for(int n=1;n<=12;n++){if(cargoSelected(n))GameIO.selectedAction(n,CARGO_LABELS[n-1],"",true);else GameIO.action(n,CARGO_LABELS[n-1]);}
            GameIO.action(0,"Отложить");int choice=SurvivalSystems.choice(scanner);if(choice==0)return;
            cargoChoice(choice);GameIO.checkpoint();
        }
        w.note("Восстановленный маршрут","М-24 → контейнер Б-17 → защищённая зона Г → проверить Б-17. Груз не подтверждён как доставленный на военный склад.");GameIO.checkpoint();ResidentialStories.pause("Маршрут восстановлен","Документы указывают на контейнер Б-17 в защищённой зоне Г. Теперь нужно добраться до него и проверить содержимое.",scanner);
    }
    private void cargoDocuments(Scanner scanner){ResidentialStories.pause("Последний груз","Одна пломба на контейнере сорвана, второй замок ещё держался. Внутри пустые крепления для медицинских ящиков, повреждённые коробки и папка документов.\n\n«Передача груза на военный склад отменена. Предписание: сохранить резерв до подтверждения получателя».\n\nНа обороте: «В связи с отсутствием подтверждения от принимающей стороны товар оставить на территории грузового терминала».\n\nДругой лист: «Самовольная выдача гражданским запрещена. При необходимости переместить резерв в защищённую зону».\n\nПоследний лист, без подписи и даты:\n\n«Тот кто читает этот документ прошу, передай на станцию. Машины возвращались пустыми не потому, что не было людей. Нас просто сука перестали принимать. Нас бросили . . .\n\nЯ хотел вывезти хотя бы часть лекарств обратно в город, люди гибнут. На терминале сказали, что без разрешения нельзя. Пошёл искать, кто здесь вообще ещё за что-то отвечает, но он уже лакомился другом.. это конец».\n\nСудьба Дмитрия остаётся неизвестной.",scanner);}
    void beforeTerminalExit(WorldStories w,Scanner scanner){if(!cargoFound||terminalExitShown)return;GameIO.storyMenu("После терминала","Так а где же всё-таки Дмитрий? Нужно будет вернуться завтра ночью, а может и не стоит...");GameIO.action(0,"Продолжить");SurvivalSystems.choice(scanner);terminalExitShown=true;GameIO.checkpoint();}
    void terminalArrival(WorldStories w,Scanner scanner){if(cargoFound&&terminalExitShown&&!nightReturnShown&&w.day()>cargoDay&&w.isNight()){nightReturnShown=true;GameIO.checkpoint();ResidentialStories.pause("Ночной терминал","Похоже, тут никого...",scanner);}}
    private void olga(Fighter p,WorldStories w,Scanner scanner){
        if(!metOlga){ResidentialStories.pause("Встреча с Олей","Джон прибывает на станцию скорой помощи.\n\nВ гараже стоит несколько автомобилей. Один находится на ремонте, у второго сняты колёса. На полу засохшие следы грязи, пустые упаковки медикаментов и брошенные инструменты.\n\nУ дальней стены стоит автомобиль с номером 17.\n\nКлюча в замке нет. Переднее сиденье испачкано, приборная панель частично разобрана.\n\nРядом с диспетчерской Джон замечает открытую дверь. Изнутри раздаётся металлический стук.\n\nПри попытке войти слышится женский голос:\n\nНеизвестная: «Даже не думай заходить. Я тебя слышу».\n\nДжон: «Я не заражённый».\n\nНеизвестная: «Да ладно? А я-то думала, они уже научились разговаривать».\n\nДжон: «Мне нужно поговорить».\n\nНеизвестная: «А мне нужен горячий душ, нормальный сон и отпуск. У нас обоих проблемы».\n\nИз-за двери появляется женщина лет тридцати пяти. В руках у неё медицинские ножницы. На куртке сохранилась нашивка станции скорой помощи.\n\nДжон: «Ты здесь работала?»\n\nОля: «Нет, блядь, экскурсию провожу. По руинам современной медицины».\n\nДжон: «Ищу экипаж семнадцатой машины».\n\nОля перестаёт улыбаться.\n\nОля: «А вот теперь интересно. Откуда ты про неё знаешь?»",scanner);metOlga=true;GameIO.checkpoint();}
        if(!olgaIntroComplete){
            GameIO.storyMenu("Откуда ты знаешь?","Оля ждёт ответа, не выпуская ножниц из рук.");
            GameIO.action(1,"Из полицейского архива. У них отмечено, что машина вернулась со склада.");
            GameIO.action(2,"Мне сказали, что вы ездили к военному складу.");
            GameIO.action(3,"Сначала убери ножницы.");GameIO.action(4,"Я ищу эвакуацию. У меня заканчивается время.");
            int choice=SurvivalSystems.choice(scanner);
            String answer=choice==1?"Из полицейского архива. У них отмечено, что машина вернулась со склада.":choice==2?"Мне сказали, что вы ездили к военному складу.":choice==3?"Сначала убери ножницы.":"Я ищу эвакуацию. У меня заканчивается время.";
            olgaReply=choice;
            String reply=switch(choice){case 1 -> "Значит, в архиве осталась запись. Да, я была в том экипаже.";case 2 -> "Ездили. Только тот, кто тебе сказал, в машине не сидел.";case 3 -> "Ладно. Но оставайся у двери. Я работала на семнадцатой.";default -> "У всех здесь заканчивается время. Что ты хочешь узнать?";};
            olgaIntroComplete=true;
            GameIO.checkpoint();
            ResidentialStories.pause("Экипаж №17","Джон: "+answer+"\nОля: "+reply,scanner);w.note("Оля, фельдшер экипажа №17","На станции скорой помощи Джон встретил Олю, работавшую на машине №17.");GameIO.checkpoint();
        }
        while(true){
            GameIO.storyMenu("Разговор с Олей","Темы можно обсуждать в любом порядке. Оля рассказывает только то, что знает сама.");
            GameIO.action(1,"Что случилось на военном складе?");
            if(w.archiveComplete)GameIO.action(2,"Почему эвакуацию отменили?","Приказ из полицейского архива");
            if(w.hospital().patientNotebook || w.hospital().registry || w.hospital().route17)GameIO.action(3,"Что стало с пациентами?","Записи городской больницы");
            GameIO.action(4,"Ты видела военных?");GameIO.action(5,"Почему ты сама здесь осталась?");
            if(warehouseTalk)GameIO.action(6,"Каким путём вы возвращались?",driverLead?"Обратный маршрут уже обсуждали":"Спросить об обратной дороге");
            GameIO.action(0,"Вернуться позже");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1){ResidentialStories.pause("Военный склад","Джон: «Мне нужно знать, что там произошло».\n\nОля: «А что ты хочешь услышать? Что всё было под контролем?»\n\nДжон: «Хотя бы с самого начала».\n\nОля: «Ладно. Нас вызвали утром. Два пациента, один после операции, другой с высокой температурой. Сказали доставить к военному пункту сбора. Там якобы организовали приём гражданских».\n\nДжон: «И вы поехали?»\n\nОля: «А что мне было делать? Сказать диспетчеру, что у меня, блядь, плохое предчувствие?»\n\nДжон: «Доехали нормально?»\n\nОля: «Почти. На въезде пробка. Машины брошены, люди с сумками, кто-то пешком шёл. Возле ворот стояли человек двадцать, может, больше».\n\nДжон: «Их пропускали?»\n\nОля: «Некоторых. По каким-то спискам. Остальным велели ждать».\n\nДжон: «А ваших пациентов?»\n\nОля: «Вот тут и началось самое интересное. Нам сказали, что медицинский приём временно закрыт».\n\nДжон: «То есть вы приехали с двумя больными, а вас не пустили?»\n\nОля: «Именно. Я чуть не послала их всех нахуй. Один лежит на носилках, а мне объясняют, что у них распоряжение».\n\nДжон: «Чем всё закончилось?»\n\nОля: «Одного пациента согласились принять через служебный вход. Второго приказали везти обратно. Даже фамилию врача, который должен был его встретить, никто назвать не смог».",scanner);if(!warehouseTalk){warehouseTalk=true;w.note("Рассказ Оли о складе","На воротах ждали гражданские. Оля говорит, что одного пациента приняли, второго приказали везти обратно. Запись о возвращении машины без пассажиров требует объяснения.");GameIO.checkpoint();}}
            if(c==2 && w.archiveComplete)ResidentialStories.pause("Отмена колонны","Джон: «В полиции лежит приказ об отмене транспорта. Он датирован раньше радиопередачи».\n\nОля: «Да?»\n\nДжон: «То есть они уже знали, что колонна не придёт. И всё равно продолжали звать людей».\n\nОля: «А ты уверен, что речь об одной и той же эвакуации?»\n\nДжон: «Что?»\n\nОля: «Слушай. В больнице тоже по нескольку раз всё меняли. Сегодня автобус, завтра санитарная машина, послезавтра вообще ничего. Может, одну колонну отменили, а другую собирались отправить».\n\nДжон: «Или людей просто обманули».\n\nОля: «Или обманули. Я там штабом не командовала, Джон. Я больных возила».",scanner);
            if(c==3 && (w.hospital().patientNotebook || w.hospital().registry || w.hospital().route17))ResidentialStories.pause("Пациенты","Джон: «В больнице остались люди, которых обещали забрать позже».\n\nОля: «Знаю».\n\nДжон: «Кто-нибудь за ними вернулся?»\n\nОля: «При мне нет».\n\nДжон: «Ты даже не пыталась?»\n\nОля: «Осторожнее, Джон».\n\nДжон: «Я просто спрашиваю».\n\nОля: «А я просто отвечаю. Когда мы вернулись со склада, у нас почти не было топлива. Водитель сказал, что до следующего рейса машина может не дотянуть. А потом связь пропала».\n\nДжон: «И всё?»\n\nОля: «Нет, не всё. Я ещё два дня пыталась найти работающую машину. Потом станцию начали штурмовать заражённые. Хочешь, я теперь по минутам отчитаюсь, кого успела спасти, а кого нет?»\n\nПауза.\n\nДжон: «Нет. Извини».\n\nОля: «Вот и хорошо. Потому что я этот список и без тебя каждую ночь вспоминаю».",scanner);
            if(c==4)ResidentialStories.pause("Военные","Джон: «Сколько их было на складе?»\n\nОля: «На воротах человек шесть. Может, больше. Не все в одинаковой форме».\n\nДжон: «С ними можно договориться?»\n\nОля: «С некоторыми да. С некоторыми лучше вообще рот не открывать».\n\nДжон: «А кто там главный?»\n\nОля: «Тогда командовал какой-то майор. Фамилию не помню. Один офицер пытался помочь нам с пациентом, а другой только орал, что мы нарушаем порядок».\n\nДжон: «И что мне с этим делать?»\n\nОля: «Не пытайся строить из себя героя. Если дойдёшь, сначала разговаривай. Оружие успеешь достать, когда всё окончательно пойдёт по пизде».",scanner);
            if(c==5)ResidentialStories.pause("Почему ты осталась?","Джон: «Почему не ушла из города?»\n\nОля: «А куда?»\n\nДжон: «Куда-нибудь подальше».\n\nОля: «Слушай, вот этот совет я обожаю. Просто уйди подальше. А куда, чем питаться, где ночевать — разберёмся по дороге».\n\nДжон: «Ну а здесь безопасно?»\n\nОля: «Нет. Но здесь я хотя бы знаю, где лежат медикаменты, как запереть двери и куда спрятаться, если кто-то войдёт».\n\nДжон: «А если заражённые прорвутся?»\n\nОля: «Тогда у меня будет очень херовый день».",scanner);
            if(c==6 && warehouseTalk){ResidentialStories.pause("Обратный маршрут","Оля: «Слушай, в тот день мы обратно ехали не тем путём, которым приехали».\n\nДжон: «Почему?»\n\nОля: «Основную дорогу перекрыли. Нас завернули через грузовой терминал возле железки».\n\nДжон: «Что за терминал?»\n\nОля: «Большой. Контейнеры, грузовики, погрузочные платформы. Там ещё раньше фуры разгружали».\n\nДжон: «И что там было?»\n\nОля: «Когда мы приехали, там стояли военные грузовики. На некоторых были коробки с красной маркировкой. Я точно такую видела на медицинских поставках».\n\nДжон: «Может, медикаменты?»\n\nОля: «Может. А может, они просто использовали одинаковые коробки. Ты хочешь факт или красивую догадку?»\n\nДжон: «А документы?»\n\nОля: «У водителя был маршрутный лист. После поездки он должен был сдать его диспетчеру, но до станции он толком не добрался».\n\nДжон: «Где его искать?»\n\nОля: «Последний раз я видела его возле автомастерской, рядом с гаражным кооперативом. Он хотел найти запчасть для машины».",scanner);if(!driverLead){driverLead=true;w.note("Водитель экипажа №17","Найти водителя или маршрутные документы у автомастерской возле гаражного кооператива. Оля не знает, жив ли водитель. На обратном пути экипаж проезжал грузовой терминал у железной дороги.");GameIO.checkpoint();}}
        }
    }
}
