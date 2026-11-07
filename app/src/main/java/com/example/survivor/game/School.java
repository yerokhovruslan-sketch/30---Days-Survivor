package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

final class School implements Serializable {
    private static final long serialVersionUID=1L;
    static final String HOUSE_KEY="Ключ от кабинета завхоза", MED_KEY="Ключ от школьного медкабинета";
    private boolean legacyChecked, desk, board, houseKey, classDiary, classroomLoot, medKey, diaryCompared;
    private boolean medOpen, medicine, medJournal, canteenFood, pantryKnown, pantryThreat, pantryCleared, pantryFood;
    private boolean libraryNote, directorOpen, directorDocuments, householdOpen, householdNote, householdTool;
    private boolean time(Fighter p,WorldStories w,int minutes,int stamina){
        if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Передохните или вернитесь позже.");return false;}
        w.advanceMinutes(minutes);return true;
    }
    private boolean take(Fighter p,ItemInfo item){
        if(!p.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется здесь до следующего посещения.");return false;}
        GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;
    }
    private boolean has(Fighter p,String name){return p.getInventory().stream().anyMatch(i->i.getName().equals(name));}
    private void mark(WorldStories w){w.markSearched(8);GameIO.checkpoint();}
    private void note(WorldStories w,String title,String text){w.note(title,text);GameIO.out().println(text);mark(w);}
    private void migrate(Location place){
        if(legacyChecked)return;
        for(Location.SearchPoint point:place.getSearchPoints())if(point.isSearched()){
            if(point.name.equals("Столовая")){canteenFood=true;pantryFood=true;}
            if(point.name.equals("Медкабинет"))medicine=true;
        }
        legacyChecked=true;
    }
    private void compareDiary(WorldStories world){
        if(classDiary && world.schoolyard().busListsRead() && !diaryCompared){
            diaryCompared=true;world.note("Дневник и списки автобуса","В дневнике ученик писал, что завтра детей заберут. В списках автобуса группа А завершила посадку, а группа Б ждала второго транспорта. Неизвестно, кто действительно уехал.");GameIO.checkpoint();
        }
    }
    void visit(Fighter p,Location place,WorldStories world,Scanner scanner,Random random){
        migrate(place);
        while(p.isAlive()){
            GameIO.menu("Школа","Перевёрнутые скамейки, брошенные вещи и тихие коридоры. Некоторые двери заперты; из дальней части столовой слышится движение.");
            GameIO.point(1,"Вестибюль","Стойка, доска объявлений и шкаф для ключей",desk&&board&&houseKey);
            GameIO.point(2,"Учебный класс","Парты, тетради и личные вещи",classDiary&&classroomLoot&&medKey);
            GameIO.point(3,"Медицинский кабинет",medOpen?"Дверь открыта":"Дверь заперта",medOpen&&medicine&&medJournal);
            GameIO.point(4,"Столовая","Доступная еда и опасный дальний склад",canteenFood&&pantryCleared&&pantryFood);
            GameIO.point(5,"Библиотека","Книги и записи преподавателей",libraryNote);
            GameIO.point(6,"Кабинет директора",directorOpen?"Дверь открыта":"Кабинет заперт",directorOpen&&directorDocuments);
            GameIO.point(7,"Хозяйственный кабинет",householdOpen?"Дверь открыта":"Кабинет завхоза заперт",householdOpen&&householdNote&&householdTool);
            GameIO.action(8,"Передохнуть","20 минут · восстановить силы");GameIO.action(0,"Вернуться домой");
            int section=SurvivalSystems.choice(scanner);if(section==0)return;if(section==8){Survivor.rest(p,30);continue;}
            if(section<1||section>7)continue;
            while(p.isAlive()){
                if(section==1){
                    GameIO.menu("Вестибюль школы","Скамейки опрокинуты. На стене список классов, рядом — доска объявлений и шкаф для ключей.");
                    GameIO.action(1,"Осмотреть стойку дежурного",desk?"Уже осмотрена":"10 минут · батарейки",!desk);
                    GameIO.action(2,"Прочитать доску объявлений",board?"Объявление прочитано":"5 минут",!board);
                    GameIO.action(3,"Осмотреть шкаф для ключей",houseKey?"Полезный ключ уже взят":"5 минут",!houseKey);GameIO.action(0,"Назад");
                    int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!desk&&time(p,world,10,2)&&take(p,new Tools("Батарейки AA","Комплект элементов питания",1,6))){desk=true;mark(world);}
                    if(a==2&&!board&&time(p,world,5,1)){board=true;note(world,"Объявление школы","Группа Б остаётся в здании до прибытия второго транспорта.");}
                    if(a==3&&!houseKey&&time(p,world,5,1)&&take(p,new Tools(HOUSE_KEY,"Подписанный ключ от хозяйственного кабинета школы.",1,0))){houseKey=true;mark(world);}
                }else if(section==2){
                    compareDiary(world);
                    GameIO.menu("Учебный класс","Парты сдвинуты к стенам. На доске осталась дата последнего учебного дня, в шкафах — тетради и личные вещи.");
                    GameIO.action(1,"Прочитать дневник на парте",classDiary?"Дневник прочитан":"10 минут",!classDiary);
                    GameIO.action(2,"Обыскать шкафы",classroomLoot?"Уже осмотрены":"10 минут · бытовые вещи",!classroomLoot);
                    GameIO.action(3,"Взять найденный ключ",medKey?"Уже взят":"Ключ от медкабинета",!medKey);GameIO.action(0,"Назад");
                    int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!classDiary&&time(p,world,10,2)){classDiary=true;note(world,"Дневник школьника","Учитель сказал, что завтра нас заберут. Мама обещала ждать возле ворот.");compareDiary(world);}
                    if(a==2&&!classroomLoot&&time(p,world,10,2)&&take(p,Shop.createItem(48))){classroomLoot=true;mark(world);}
                    if(a==3&&!medKey&&time(p,world,5,1)&&take(p,new Tools(MED_KEY,"Ключ от медицинского кабинета школы.",1,0))){medKey=true;mark(world);}
                }else if(section==3){
                    GameIO.menu("Медицинский кабинет",medOpen?"За дверью аптечный шкаф, кушетка и стол медработника.":"Дверь закрыта. Можно открыть её найденным ключом или вскрыть ломом.");
                    if(!medOpen){GameIO.action(1,"Открыть ключом","5 минут",has(p,MED_KEY));GameIO.action(2,"Вскрыть ломом","10 минут · шум",place.hasCrowbar(p));}
                    if(medOpen){GameIO.action(3,"Взять бинт",medicine?"Уже взят":"Медицинский предмет",!medicine);GameIO.action(4,"Прочитать журнал выдачи",medJournal?"Уже прочитан":"5 минут",!medJournal);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(!medOpen && ((a==1&&has(p,MED_KEY))||(a==2&&place.hasCrowbar(p))) && time(p,world,a==1?5:10,2)){medOpen=true;if(a==2)GameIO.sound(GameSound.METAL_DOOR);mark(world);if(a==2&&!BreakIn.attack(p,scanner,random))return;}
                    if(a==3&&medOpen&&!medicine&&time(p,world,10,2)&&take(p,Shop.createItem(1))){medicine=true;mark(world);}
                    if(a==4&&medOpen&&!medJournal&&time(p,world,5,1)){medJournal=true;note(world,"Журнал медкабинета","Часть запасов передана сопровождающему медицинскому персоналу незадолго до эвакуации.");}
                }else if(section==4){
                    if(!pantryKnown){pantryKnown=true;pantryThreat=random.nextBoolean();GameIO.checkpoint();}
                    GameIO.menu("Столовая","Пустые коробки на полу, в углу металлическая тележка. "+(pantryThreat&&!pantryCleared?"Из дальнего склада слышится движение.":"Из дальнего помещения не слышно движения."));
                    GameIO.action(1,"Обыскать доступные шкафы",canteenFood?"Уже осмотрены":"10 минут · еда без входа на склад",!canteenFood);
                    if(!pantryCleared)GameIO.action(2,"Проверить склад продуктов","15 минут"+(pantryThreat?" · возможен бой":""));
                    if(pantryCleared)GameIO.action(3,"Взять консервы со склада",pantryFood?"Уже взяты":"Пригодная еда",!pantryFood);
                    GameIO.action(0,"Уйти, сохранив находки");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!canteenFood&&time(p,world,10,2)&&take(p,Shop.createItem(16))){canteenFood=true;mark(world);}
                    if(a==2&&!pantryCleared&&time(p,world,15,3)){
                        if(pantryThreat){GameIO.out().println("Из темноты склада выходит заражённый.");if(!TacticalBattle.story(p,scanner,random,List.of(new Zombie(1,ZombieType.NORMAL)),"Склад школьной столовой",List.of(),false,false,false))return;}
                        pantryCleared=true;mark(world);
                    }
                    if(a==3&&pantryCleared&&!pantryFood&&take(p,Shop.createItem(17))){pantryFood=true;mark(world);}
                }else if(section==5){
                    GameIO.menu("Библиотека","Книги разбросаны по полу, между стеллажами лежат сумки.");
                    GameIO.action(1,"Прочитать записку преподавателя",libraryNote?"Уже прочитана":"10 минут",!libraryNote);GameIO.action(0,"Назад");
                    int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!libraryNote&&time(p,world,10,2)){libraryNote=true;note(world,"Записка преподавателя","Директор получил распоряжение оставить группу Б в здании до отдельного уведомления.");}
                }else if(section==6){
                    GameIO.menu("Кабинет директора",directorOpen?"Внутри стол, старые телефоны и сейф. Основные документы лежат в письменном столе.":"Дверь кабинета на втором этаже заперта. Её можно вскрыть ломом.");
                    if(!directorOpen)GameIO.action(1,"Вскрыть дверь ломом","10 минут · шум",place.hasCrowbar(p));
                    if(directorOpen)GameIO.action(2,"Изучить документы в столе",directorDocuments?"Уже изучены":"15 минут",!directorDocuments);
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!directorOpen&&place.hasCrowbar(p)&&time(p,world,10,3)){directorOpen=true;GameIO.sound(GameSound.WOOD_DOOR);mark(world);if(!BreakIn.attack(p,scanner,random))return;}
                    if(a==2&&directorOpen&&!directorDocuments&&time(p,world,15,2)){directorDocuments=true;note(world,"Переписка директора","Транспорт для второй группы задерживается. Обеспечить размещение и продовольствие до дополнительного распоряжения. Часть медицинских запасов направлена на станцию скорой помощи. Неизвестно, состоялся ли вывоз.");}
                }else if(section==7){
                    GameIO.menu("Хозяйственный кабинет",householdOpen?"Схемы коммуникаций, инструменты и стол завхоза.":"Кабинет заперт. Ключ можно найти в вестибюле; дверь также можно вскрыть ломом.");
                    if(!householdOpen){GameIO.action(1,"Открыть ключом завхоза","5 минут",has(p,HOUSE_KEY));GameIO.action(2,"Вскрыть дверь ломом","10 минут · шум",place.hasCrowbar(p));}
                    if(householdOpen){GameIO.action(3,"Прочитать техническую записку",householdNote?"Уже прочитана":"10 минут",!householdNote);GameIO.action(4,"Взять инструмент",householdTool?"Уже взят":"Набор инструментов",!householdTool);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(!householdOpen&&((a==1&&has(p,HOUSE_KEY))||(a==2&&place.hasCrowbar(p)))&&time(p,world,a==1?5:10,2)){householdOpen=true;if(a==2)GameIO.sound(GameSound.WOOD_DOOR);mark(world);if(a==2&&!BreakIn.attack(p,scanner,random))return;}
                    if(a==3&&householdOpen&&!householdNote&&time(p,world,10,2)){householdNote=true;note(world,"Записка завхоза","Котельная отключена. Пуск оборудования запрещён до замены аварийного предохранителя.");}
                    if(a==4&&householdOpen&&!householdTool&&take(p,Shop.createItem(20))){householdTool=true;mark(world);}
                }
            }
        }
    }
}
