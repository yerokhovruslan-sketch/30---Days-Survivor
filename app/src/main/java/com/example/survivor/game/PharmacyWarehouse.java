package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** The distribution warehouse keeps each clue, threat and item across visits. */
final class PharmacyWarehouse implements Serializable {
    private static final long serialVersionUID=1L;
    static final String KEY="Ключ от помещений аптечного склада";
    boolean introduced,legacyChecked,deskLoot,keyTaken,journalRead,journalConnection;
    boolean frontBandage,frontSupplies,farRoll,farThreat,farCleared,farLoot,farValuable,farNote;
    boolean coldOpen,coldBandage1,coldBandage2,coldKit,coldManifest,coldConnection;
    boolean loadingRoll,loadingThreat,loadingCleared,truckDocs,dispatchDocs,notice,containerOpen,containerMedicine,containerMaterial;
    private boolean time(Fighter p,WorldStories w,int minutes,int stamina){if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Можно вернуться после отдыха.");return false;}w.advanceMinutes(minutes);return true;}
    private boolean take(Fighter p,ItemInfo item){if(!p.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется здесь.");return false;}GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;}
    private void mark(Location place,WorldStories w,String name){for(Location.SearchPoint point:place.getSearchPoints())if(point.name.equals(name)){place.consumePoint(point);break;}w.markSearched(11);GameIO.checkpoint();}
    private void note(WorldStories w,String title,String message){w.note(title,message);GameIO.out().println(message);GameIO.checkpoint();}
    private boolean key(Fighter p){return p.getInventory().stream().anyMatch(x->x.getName().equals(KEY));}
    private boolean known(WorldStories w){return w.clinic().cabinetManifest;}
    private void connections(WorldStories w){
        if(known(w)&&journalRead&&!journalConnection){journalConnection=true;note(w,"След партии М-24","По номеру из поликлиники Джон находит запись: партия М-24 принята на аптечный склад, помещена в холодильный отсек и не отгружена.");}
        if(known(w)&&coldManifest&&!coldConnection){coldConnection=true;note(w,"Найденная партия М-24","Вот она. Значит, запись в поликлинике не ошибалась: часть партии осталась на складе.");}
    }
    private boolean encounter(Fighter p,Scanner s,Random r,String terrain){return TacticalBattle.story(p,s,r,List.of(new Zombie(1,ZombieType.NORMAL)),terrain,List.of(),false,false,false);}
    private void migrate(Location loc){if(legacyChecked)return;legacyChecked=true;for(Location.SearchPoint p:loc.getSearchPoints())if(p.isSearched()){
        if(p.name.equals("Основной склад"))frontBandage=true;
        if(p.name.equals("Холодильная камера")||p.name.equals("Холодильный отсек")){coldBandage1=true;coldBandage2=true;coldKit=true;}
        if(p.name.equals("Бухгалтерия")||p.name.equals("Приёмная и журнал поставок"))deskLoot=true;
    }}
    void visit(Fighter p,Location loc,WorldStories w,Scanner scanner,Random random){
        migrate(loc);
        if(!introduced){introduced=true;GameIO.checkpoint();ResidentialStories.pause("Аптечный склад","Возле запертых грузовых ворот стоят два грузовика. Один пуст; в другом остались повреждённые поддоны и коробки. Через повреждённую боковую дверь можно пройти внутрь.\n\nНад входом: «Центральный распределительный склад медицинских товаров». На двери объявление: «Выдача частным лицам прекращена. Поставки осуществляются по распоряжению городского штаба»."+(known(w)?"\n\nВозможно, здесь сохранились сведения о партии М-24 из поликлиники.":""),scanner);}
        connections(w);
        while(p.isAlive()){
            GameIO.menu("Аптечный склад","Грузовые ворота заперты; внутрь ведёт повреждённая служебная дверь. Часть помещений разграблена."+(known(w)?" Партия М-24 может ещё находиться здесь.":""));
            GameIO.point(1,"Приёмная и журнал поставок","Открыта · низкий риск",deskLoot&&keyTaken&&journalRead);GameIO.point(2,"Основной склад","Передние и дальние стеллажи",frontBandage&&frontSupplies&&farCleared&&farLoot&&farNote);
            GameIO.point(3,"Холодильный отсек",coldOpen?"Открыт":"Заперт · ключ или лом",coldOpen&&coldBandage1&&coldBandage2&&coldKit&&coldManifest);GameIO.point(4,"Зона погрузки","Возможны заражённые · следы отгрузок",loadingCleared&&truckDocs&&dispatchDocs&&notice&&containerOpen&&containerMedicine&&containerMaterial);
            GameIO.action(5,"Осмотреть тело","Проверить и обработать раны");GameIO.action(6,"Передохнуть","Восстановить силы");GameIO.action(0,"Вернуться домой");
            int section=SurvivalSystems.choice(scanner);if(section==0)return;if(section==5){Survivor.inspectBody(p,scanner);continue;}if(section==6){Survivor.rest(p,30);continue;}
            while(p.isAlive() && section>=1 && section<=4){
                if(section==1){
                    GameIO.menu("Приёмная и журнал поставок","Стол диспетчера, шкафы и металлическая стойка для документов. Внутри тихо.");GameIO.illustratedCard("","","warehouse_art:pharmacy_reception");
                    GameIO.action(1,"Обыскать рабочее место",deskLoot?"Уже осмотрено":"5 минут · технические материалы",!deskLoot);
                    GameIO.action(2,"Взять связку служебных ключей",keyTaken?"Ключи уже взяты":"Ключ от внутренних помещений",!keyTaken);
                    GameIO.action(3,"Изучить журнал поставок",journalRead?"Уже прочитан":"10 минут",!journalRead);GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!deskLoot&&time(p,w,5,1)&&take(p,Shop.createItem(43))){deskLoot=true;mark(loc,w,"Приёмная и журнал поставок");}
                    if(a==2&&!keyTaken&&time(p,w,5,1)&&take(p,new Tools(KEY,"Связка служебных ключей; подходит к холодильному отсеку.",1,0))){keyTaken=true;mark(loc,w,"Приёмная и журнал поставок");}
                    if(a==3&&!journalRead&&time(p,w,10,1)){journalRead=true;note(w,"Журнал аптечного склада","Последние партии распределяли централизованно. Некоторые направлены: «Городской резерв. Специальное распределение». Подписи есть, адрес сокращён. Отдельная запись: «М-24. Принято. Размещено в холодильном отсеке. Не отгружено».");connections(w);mark(loc,w,"Приёмная и журнал поставок");}
                }else if(section==2){
                    GameIO.menu("Основной склад",farCleared?"Передние стеллажи разграблены. За дальними рядами теперь тихо.":"Высокие стеллажи и вскрытые коробки. В глубине слышатся шаги; за рядами почти ничего не видно.");GameIO.illustratedCard("","","warehouse_art:pharmacy_main");
                    GameIO.action(1,"Осмотреть передние стеллажи",frontBandage?"Основная коробка уже осмотрена":"10 минут · низкий риск · бинт",!frontBandage);
                    GameIO.action(2,"Проверить ближние материалы",frontSupplies?"Уже взяты":"5 минут · ткань",!frontSupplies);
                    GameIO.action(3,"Пройти к дальним стеллажам",farCleared?"Проход проверен":"15 минут · возможна встреча",!farCleared);
                    if(farCleared){GameIO.action(4,"Осмотреть целые коробки",farLoot?"Уже осмотрены":"Медицинские материалы",!farLoot);GameIO.action(5,"Изучить отметки на коробках",farNote?"Уже изучены":"5 минут",!farNote);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!frontBandage&&time(p,w,10,2)&&take(p,Shop.createItem(1))){frontBandage=true;mark(loc,w,"Основной склад");}
                    if(a==2&&!frontSupplies&&time(p,w,5,1)&&take(p,Shop.createItem(48))){frontSupplies=true;mark(loc,w,"Основной склад");}
                    if(a==3&&!farCleared){if(!farRoll){farRoll=true;farThreat=random.nextInt(100)<50;farValuable=random.nextInt(100)<55;GameIO.checkpoint();}if(!time(p,w,15,3))continue;if(farThreat&&!encounter(p,scanner,random,"Дальние стеллажи аптечного склада"))return;farCleared=true;mark(loc,w,"Основной склад");}
                    if(a==4&&farCleared&&!farLoot&&take(p,Shop.createItem(farValuable?2:1))){farLoot=true;GameIO.checkpoint();}
                    if(a==5&&farCleared&&!farNote&&time(p,w,5,1)){farNote=true;note(w,"Резерв на аптечном складе","На коробках красные отметки: «НЕ ОТГРУЖАТЬ. РЕЗЕРВ». Некоторые вскрыты после прекращения обычных поставок. Кто сюда приходил, неизвестно.");}
                }else if(section==3){
                    GameIO.menu("Холодильный отсек",coldOpen?"Электричества давно нет. Один металлический шкаф вскрыт, второй запечатан и помечен М-24. Препараты, требующие холода, могли испортиться.":"Тяжёлая металлическая дверь заперта. Внутри давно нет электричества; подходят служебный ключ или лом.");GameIO.illustratedCard("","","warehouse_art:pharmacy_cold");
                    if(!coldOpen){GameIO.action(1,"Открыть служебным ключом","5 минут · тихо",key(p));GameIO.action(2,"Вскрыть дверь ломом","10 минут · шум",loc.hasCrowbar(p));}
                    if(coldOpen){GameIO.action(3,"Взять первый бинт",coldBandage1?"Уже взят":"Сухая упаковка",!coldBandage1);GameIO.action(4,"Взять второй бинт",coldBandage2?"Уже взят":"Сухая упаковка",!coldBandage2);GameIO.action(5,"Взять аптечку",coldKit?"Уже взята":"Перевязочные материалы",!coldKit);GameIO.action(6,"Изучить ведомость М-24",coldManifest?"Уже изучена":"5 минут",!coldManifest);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(!coldOpen&&((a==1&&key(p))||(a==2&&loc.hasCrowbar(p)))&&time(p,w,a==1?5:10,2)){coldOpen=true;GameIO.sound(GameSound.METAL_DOOR);mark(loc,w,"Холодильный отсек");if(a==2&&!BreakIn.attack(p,scanner,random))return;}
                    if(a==3&&coldOpen&&!coldBandage1&&take(p,Shop.createItem(1))){coldBandage1=true;GameIO.checkpoint();}
                    if(a==4&&coldOpen&&!coldBandage2&&take(p,Shop.createItem(1))){coldBandage2=true;GameIO.checkpoint();}
                    if(a==5&&coldOpen&&!coldKit&&take(p,Shop.createItem(2))){coldKit=true;GameIO.checkpoint();}
                    if(a==6&&coldOpen&&!coldManifest&&time(p,w,5,1)){coldManifest=true;note(w,"Ведомость партии М-24","Часть медицинского резерва оставить для экстренного обеспечения пункта сбора. Выдача только по распоряжению. Сам пункт сбора не назван; другая часть медикаментов передана городской больнице.");connections(w);}
                }else{
                    GameIO.menu("Зона погрузки",loadingCleared?"Поддоны, открытый грузовик и неподвижный погрузчик. В дальнем углу теперь тихо.":"Поддоны, открытый грузовик и неподвижный погрузчик. В дальнем углу слышится движение — здесь возможна встреча с заражённым.");GameIO.illustratedCard("","","warehouse_art:pharmacy_loading");
                    if(!loadingCleared)GameIO.action(1,"Осторожно войти в зону погрузки","10 минут · возможна встреча");
                    if(loadingCleared){GameIO.action(2,"Осмотреть грузовой автомобиль",truckDocs?"Документы уже найдены":"5 минут",!truckDocs);GameIO.action(3,"Прочитать погрузочную ведомость",dispatchDocs?"Уже прочитана":"5 минут",!dispatchDocs);GameIO.action(4,"Найти служебное уведомление",notice?"Уже прочитано":"5 минут",!notice);GameIO.action(5,"Вскрыть металлический контейнер",containerOpen?"Контейнер открыт":"10 минут · лом · шум",!containerOpen&&loc.hasCrowbar(p));
                        if(containerOpen){GameIO.action(6,"Взять перевязочные материалы",containerMedicine?"Уже взяты":"Бинт",!containerMedicine);GameIO.action(7,"Взять технические материалы",containerMaterial?"Уже взяты":"Запчасти",!containerMaterial);}}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!loadingCleared){if(!loadingRoll){loadingRoll=true;loadingThreat=random.nextInt(100)<55;GameIO.checkpoint();}if(!time(p,w,10,2))continue;if(loadingThreat&&!encounter(p,scanner,random,"Зона погрузки аптечного склада"))return;loadingCleared=true;mark(loc,w,"Зона погрузки");}
                    if(a==2&&loadingCleared&&!truckDocs&&time(p,w,5,1)){truckDocs=true;note(w,"Грузовик аптечного склада","В почти пустом кузове сохранились документы последней отгрузки. Один груз предназначался городской больнице.");}
                    if(a==3&&loadingCleared&&!dispatchDocs&&time(p,w,5,1)){dispatchDocs=true;note(w,"Погрузочная ведомость","Среди получателей несколько медицинских учреждений. Последняя пометка: «Дальнейшая отгрузка по распоряжению эвакуационного штаба».");}
                    if(a==4&&loadingCleared&&!notice&&time(p,w,5,1)){notice=true;note(w,"Служебное уведомление","Медицинским учреждениям обеспечить подготовку транспорта и сопровождающего персонала. Очерёдность перевозок будет доведена дополнительно. Подпись неразборчива; подтверждения отправки нет.");}
                    if(a==5&&loadingCleared&&!containerOpen&&loc.hasCrowbar(p)&&time(p,w,10,3)){containerOpen=true;containerMedicine=random.nextInt(100)>=80;containerMaterial=random.nextInt(100)>=70;GameIO.sound(GameSound.METAL_DOOR);if(containerMedicine&&containerMaterial)GameIO.out().println("Контейнер почти пуст: пригодных материалов не осталось.");GameIO.checkpoint();if(!BreakIn.attack(p,scanner,random))return;}
                    if(a==6&&containerOpen&&!containerMedicine&&take(p,Shop.createItem(1))){containerMedicine=true;GameIO.checkpoint();}
                    if(a==7&&containerOpen&&!containerMaterial&&take(p,Shop.createItem(19))){containerMaterial=true;GameIO.checkpoint();}
                }
            }
        }
    }
}
