package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

final class GroceryStore implements Serializable {
    private static final long serialVersionUID = 1L;
    static final String KEY="Ключ от склада магазина";
    private boolean floorSearched, keyTaken, foodTaken, waterTaken, cashTaken, recordsRead, journalRead;
    private boolean storageOpen, storageCleared, storageHint, tin1, tin2, fridgeNote, fridgeFood, fridgeDrink;
    private boolean hiddenFound, basementOpen, basementTin1, basementTin2, basementMaterial, basementTool;
    private boolean researched;
    private boolean time(Fighter player,WorldStories world,int minutes,int stamina) {
        if(!player.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Вернитесь после отдыха.");return false;}
        world.advanceMinutes(minutes);return true;
    }
    private boolean take(Fighter player, ItemInfo item) {
        if(!player.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется здесь, за ней можно вернуться.");return false;}
        GameIO.sound(GameSound.BAG); GameIO.out().println("Найдено: "+item.getName());return true;
    }
    private void progress(Location location,WorldStories world,String point) {
        researched=true;world.markSearched(4);
        for(Location.SearchPoint p:location.getSearchPoints())if(p.name.equals(point)){location.consumePoint(p);break;}
        GameIO.checkpoint();
    }
    private boolean keyInBag(Fighter p){return p.getInventory().stream().anyMatch(i->i.getName().equals(KEY));}
    int visit(Fighter p,Location place,WorldStories world,Scanner scanner,Random random,int balance) {
        ResidentialStories stories=world.residential();
        if (stories.basementOpen) basementOpen=true;
        while(p.isAlive()) {
            GameIO.balance(balance);
            GameIO.menu("Продуктовый магазин","Главная дверь повреждена. Через разбитую витрину можно пройти без инструмента.");
            GameIO.point(1,"Торговый зал","Остатки еды · низкий риск",floorSearched&&keyTaken&&foodTaken&&waterTaken);
            GameIO.point(2,"Кассовая зона","Ящики, записи и шкафчик администратора",cashTaken&&recordsRead&&journalRead);
            GameIO.point(3,"Склад продуктов",storageCleared?"Заражённый побеждён":"Дверь закрыта и завалена стеллажом",storageHint&&storageCleared&&tin1&&tin2);
            GameIO.point(4,"Холодильная комната","Электричества давно нет",fridgeNote&&fridgeFood&&fridgeDrink);
            if(researched || stories.viktor>0)GameIO.action(5,"Подсобка заведующего","Из-за двери слышен осторожный голос");
            if(hiddenFound || stories.basementKnown)GameIO.action(6,"Скрытый подвал",stories.basementLoot?"Уже обыскан":"За холодильным оборудованием",!stories.basementLoot);
            GameIO.action(7,"Передохнуть","20 минут · восстановить силы");
            GameIO.action(0,"Вернуться домой");
            int section=SurvivalSystems.choice(scanner);if(section==0)return balance;
            if(section==7){Survivor.rest(p,30);continue;}
            if(section==5 && (researched || stories.viktor>0)){stories.visitShopkeeper(p,world,scanner,random);if(stories.aborted)return balance;continue;}
            if(section<1 || section>6 || section==6 && ((!hiddenFound && !stories.basementKnown) || stories.basementLoot))continue;
            while(p.isAlive()) {
                if(section==1) {
                    GameIO.menu("Торговый зал","Разбитая посуда и упаковки покрывают пол. Некоторые полки опустошали аккуратно.");
                    if(!floorSearched)GameIO.action(1,"Обыскать зал","10 минут · безопасно");
                    else {
                        GameIO.action(2,"Взять ключ от склада",keyTaken?"Уже взят":"Ключ у пустых коробок",!keyTaken);
                        GameIO.action(3,"Взять галеты",foodTaken?"Уже взяты":"Пригодная еда",!foodTaken);
                        GameIO.action(4,"Взять бутылку воды",waterTaken?"Уже взята":"Закрытая бутылка",!waterTaken);
                    }
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!floorSearched&&time(p,world,10,2)){floorSearched=true;world.note("Вывоз товаров","Полки опустошали аккуратно: кто-то вывозил товары коробками.");GameIO.out().println("Часть полок опустошена аккуратно. Кто-то вывозил товары коробками. Рядом лежат ключ, еда и вода.");progress(place,world,"Торговый зал");}
                    if(a==2&&floorSearched&&!keyTaken&&take(p,new Tools(KEY,"Ключ от складской двери продуктового магазина.",1,0))){keyTaken=true;GameIO.checkpoint();}
                    if(a==3&&floorSearched&&!foodTaken&&take(p,Shop.createItem(16))){foodTaken=true;GameIO.checkpoint();}
                    if(a==4&&floorSearched&&!waterTaken&&take(p,new Food("Бутылка минеральной воды","Чистая питьевая вода",1,4,0))){waterTaken=true;GameIO.checkpoint();}
                } else if(section==2) {
                    GameIO.menu("Кассовая зона","За кассой лежат документы, старый телефон и шкафчик администратора.");
                    GameIO.action(1,"Обыскать кассовые ящики",cashTaken?"Уже обысканы":"5 минут · 10–20 $",!cashTaken);
                    GameIO.action(2,"Просмотреть записи поставок",recordsRead?"Уже проверены":"5 минут",!recordsRead);
                    GameIO.action(3,"Проверить шкафчик администратора",journalRead?"Журнал найден":"5 минут",!journalRead);
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!cashTaken&&time(p,world,5,1)){int amount=10+random.nextInt(11);balance+=amount;GameIO.balance(balance);cashTaken=true;GameIO.out().println("В кассе осталось "+amount+" $.");progress(place,world,"Касса");}
                    if(a==2&&!recordsRead&&time(p,world,5,1)){recordsRead=true;GameIO.out().println("Кто-то стёр записи поставок. Отследить их больше невозможно.");world.note("Записи поставок","Электронные записи стёрты, отследить поставки невозможно.");progress(place,world,"Касса");}
                    if(a==3&&!journalRead&&time(p,world,5,1)){journalRead=true;String text="По официальной ведомости вывезено 180 консервов и 200 литров питьевой воды. Куда направили груз, в журнале не указано.";world.note("Журнал поставок магазина",text);GameIO.out().println(text);progress(place,world,"Касса");}
                } else if(section==3) {
                    GameIO.menu("Склад продуктов",storageCleared?"Помещение очищено. Остатки припасов можно забрать.":storageOpen?"Стеллаж отодвинут. За дверью слышен заражённый.":"Дверь заперта, проход перегорожен упавшим металлическим стеллажом. Подойдёт прочный рычаг: его можно поискать среди хозяйственного мусора во дворе.");
                    if(!storageHint)GameIO.action(1,"Осмотреть складскую зону","Найти возможные проходы · 5 минут");
                    if(!storageOpen)GameIO.action(2,"Открыть дверь и отойти","Ключ и лом · шум; из склада выйдет заражённый",keyInBag(p)&&place.hasCrowbar(p));
                    if(!storageOpen)GameIO.action(3,"Открыть дверь и войти","Ключ и лом · бой с заражённым",keyInBag(p)&&place.hasCrowbar(p));
                    if(storageOpen&&!storageCleared)GameIO.action(4,"Войти на склад","Внутри заражённый");
                    if(storageCleared){GameIO.action(5,"Взять консервы",tin1?"Уже взяты":"Первая банка",!tin1);GameIO.action(6,"Взять вторые консервы",tin2?"Уже взяты":"Вторая банка",!tin2);}
                    GameIO.action(0,"Вернуться позже");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!storageHint&&time(p,world,5,1)){storageHint=true;hiddenFound=true;world.note("Скрытый подвал","За холодильным оборудованием обнаружен служебный вход в подвал.");progress(place,world,"Запертая кладовая");}
                    if((a==2||a==3)&&!storageOpen&&keyInBag(p)&&place.hasCrowbar(p)&&time(p,world,15,4)){storageOpen=true;GameIO.sound(GameSound.METAL_DOOR);GameIO.out().println("Металл громко скрежещет. Из помещения появляется заражённый.");GameIO.checkpoint();if(!BreakIn.attack(p,scanner,random))return balance;a=4;}
                    if(a==4&&storageOpen&&!storageCleared){boolean won=TacticalBattle.story(p,scanner,random,List.of(new Zombie(1,ZombieType.NORMAL)),"Склад магазина",List.of(),false,false,false);if(!won)return balance;storageCleared=true;progress(place,world,"Запертая кладовая");}
                    if(a==5&&storageCleared&&!tin1&&take(p,Shop.createItem(17))){tin1=true;GameIO.checkpoint();}
                    if(a==6&&storageCleared&&!tin2&&take(p,Shop.createItem(17))){tin2=true;GameIO.checkpoint();}
                } else if(section==4) {
                    GameIO.menu("Холодильная комната","Холодильники давно не работают; большая часть продуктов испортилась.");
                    GameIO.action(1,"Осмотреть полки",fridgeNote?"Записка прочитана":"10 минут",!fridgeNote);
                    if(fridgeNote){GameIO.action(2,"Взять закрытую упаковку",fridgeFood?"Уже взята":"Галеты",!fridgeFood);GameIO.action(3,"Взять напиток",fridgeDrink?"Уже взят":"Бутылка воды",!fridgeDrink);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!fridgeNote&&time(p,world,10,2)){fridgeNote=true;String t="Не трогать коробки из последней поставки. Заведующий сказал оставить до возвращения грузовика.";world.note("Записка работника магазина",t);GameIO.out().println(t);progress(place,world,"Подсобка");}
                    if(a==2&&fridgeNote&&!fridgeFood&&take(p,Shop.createItem(16))){fridgeFood=true;GameIO.checkpoint();}
                    if(a==3&&fridgeNote&&!fridgeDrink&&take(p,new Food("Бутылка минеральной воды","Чистая питьевая вода",1,4,0))){fridgeDrink=true;GameIO.checkpoint();}
                } else if(section==6) {
                    boolean quiet=stories.viktor==2 || stories.basementKey;
                    GameIO.menu("Скрытый подвал",basementOpen?"Подвал открыт. Здесь остались коробки с припасами.":quiet?"Виктор объяснил, как открыть проход без шума.":"Служебная дверь заперта. Для вскрытия нужен лом.");
                    if(!basementOpen)GameIO.action(1,quiet?"Открыть проход тихо":"Вскрыть проход ломом","15 минут",quiet||place.hasCrowbar(p));
                    if(basementOpen){GameIO.action(2,"Взять консервы",basementTin1?"Уже взяты":"Первая банка",!basementTin1);GameIO.action(3,"Взять ещё консервы",basementTin2?"Уже взяты":"Вторая банка",!basementTin2);GameIO.action(4,"Взять материалы",basementMaterial?"Уже взяты":"Изолента",!basementMaterial);GameIO.action(5,"Взять инструмент",basementTool?"Уже взят":"Набор инструментов",!basementTool);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!basementOpen&&(quiet||place.hasCrowbar(p))&&time(p,world,15,3)){basementOpen=true;stories.basementKnown=true;stories.basementOpen=true;GameIO.checkpoint();if(!quiet&&!BreakIn.attack(p,scanner,random))return balance;}
                    if(a==2&&basementOpen&&!basementTin1&&take(p,Shop.createItem(17)))basementTin1=true;
                    if(a==3&&basementOpen&&!basementTin2&&take(p,Shop.createItem(17)))basementTin2=true;
                    if(a==4&&basementOpen&&!basementMaterial&&take(p,Shop.createItem(42)))basementMaterial=true;
                    if(a==5&&basementOpen&&!basementTool&&take(p,Shop.createItem(20)))basementTool=true;
                    if(basementTin1&&basementTin2&&basementMaterial&&basementTool)stories.basementLoot=true;
                    GameIO.checkpoint();
                } else break;
            }
        }
        return balance;
    }
}
