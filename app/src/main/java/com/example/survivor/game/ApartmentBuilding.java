package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** The building's individual interactions and discoveries are retained in saved worlds. */
final class ApartmentBuilding implements Serializable {
    private static final long serialVersionUID=1L;
    static final String ATTIC_KEY="Ключ от чердачного люка";
    private boolean underStairs, mailbox, notice, stairsSeen, stairsDanger, stairsCleared;
    private boolean kitchen, bedroom, balcony, apartment24Open, apartment24Loot, atticKeyTaken, calendar;
    private boolean apartment34Note, apartment34Food, apartment34Material, hatchOpen, hatchLoot;
    boolean hatchOpen(){return hatchOpen;}
    private boolean take(Fighter p,ItemInfo item){
        if(!p.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется здесь — можно вернуться позже.");return false;}
        GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;
    }
    private boolean time(Fighter p,WorldStories w,int minutes,int stamina){
        if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Можно вернуться после отдыха.");return false;}
        w.advanceMinutes(minutes);return true;
    }
    private void mark(WorldStories w){w.markSearched(5);GameIO.checkpoint();}
    private boolean hasAtticKey(Fighter p){return p.getInventory().stream().anyMatch(i->i.getName().equals(ATTIC_KEY));}
    void visit(Fighter p,Location place,WorldStories world,Scanner scanner,Random random){
        while(p.isAlive()){
            GameIO.menu("Жилой подъезд","Основной вход открыт. На лестнице виден перевёрнутый шкаф; прислушайтесь перед тем, как идти дальше.");
            GameIO.point(1,"Вестибюль и почтовые ящики","Письма, объявления и вещи под лестницей",underStairs&&mailbox&&notice);
            GameIO.point(2,"Лестничные пролёты",stairsCleared?"Проход доступен":"Узнайте, что за завалом",stairsCleared);
            GameIO.point(3,"Квартира №21 · Последний ужин","Приоткрытая дверь",kitchen&&bedroom&&balcony,stairsCleared);
            GameIO.point(4,"Квартира №24 · Запертая дверь","На двери глубокие царапины",apartment24Open&&apartment24Loot&&atticKeyTaken&&calendar&&!world.semen().canInspectWardrobe(),stairsCleared);
            GameIO.action(5,"Квартира №31 · Марина и Артём","Из-за двери можно услышать голос",stairsCleared);
            GameIO.point(6,"Квартира №34 · Пустые чемоданы","Следы подготовки к эвакуации",apartment34Note&&apartment34Food&&apartment34Material,stairsCleared);
            GameIO.point(7,"Чердачный люк",hatchOpen?"Проход открыт":"Нужен ключ или лом",hatchOpen&&hatchLoot,stairsCleared);
            if(world.courtyard().passageAvailable()) GameIO.action(8,"Осмотреть служебный проход","Отмечен на плане дворника");
            GameIO.action(9,"Передохнуть","20 минут · восстановить силы");GameIO.action(0,"Вернуться домой");
            int section=SurvivalSystems.choice(scanner);if(section==0)return;
            if(section==8 && world.courtyard().passageAvailable()){world.courtyard().inspectPassage(world,scanner);continue;}
            if(section==9){Survivor.rest(p,30);continue;}
            if(section==5 && stairsCleared){world.residential().visitFamilyApartment(world,p,scanner,random);continue;}
            if(section<1 || section>7 || section>=3 && !stairsCleared)continue;
            while(p.isAlive()){
                if(section==1){
                    GameIO.menu("Вестибюль","Почтовые ящики частично вскрыты, на полу газеты и счета.");
                    GameIO.action(1,"Обыскать под лестницей",underStairs?"Уже осмотрено":"5 минут",!underStairs);
                    GameIO.action(2,"Осмотреть почтовые ящики",mailbox?"Письма прочитаны":"5 минут",!mailbox);
                    GameIO.action(3,"Проверить доску объявлений",notice?"Уже прочитана":"5 минут",!notice);GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!underStairs&&time(p,world,5,2)&&take(p,Shop.createItem(48))){underStairs=true;mark(world);}
                    if(a==2&&!mailbox&&time(p,world,5,1)){mailbox=true;String t="Среди неотправленных писем просьба пожилой женщины проверить квартиру её дочери на четвёртом этаже.";world.note("Письма жильцов",t);GameIO.out().println(t);mark(world);}
                    if(a==3&&!notice&&time(p,world,5,1)){notice=true;String t="Ключи от чердака находятся в квартире 24. Не забывайте закрывать люк.";world.note("Объявление в подъезде",t);GameIO.out().println(t);mark(world);}
                } else if(section==2){
                    if(!stairsSeen){stairsSeen=true;stairsDanger=random.nextBoolean();}
                    GameIO.menu("Лестничные пролёты",stairsCleared?"Путь к квартирам и чердаку доступен.":stairsDanger?"За перевёрнутым шкафом слышно движение. Возможно, там заражённый.":"Перевёрнутый шкаф загораживает часть лестницы. За ним тихо.");
                    if(!stairsCleared){GameIO.action(1,"Осторожно осмотреть проход","5 минут; если за шкафом заражённый, возможен бой");GameIO.action(2,"Обойти через боковой коридор","15 минут · без встречи у завала");}
                    GameIO.action(0,"Вернуться позже");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!stairsCleared&&time(p,world,5,2)){
                        if(stairsDanger){GameIO.out().println("Из-за шкафа выходит один заражённый.");if(!TacticalBattle.story(p,scanner,random,List.of(new Zombie(1,ZombieType.NORMAL)),"Лестничный пролёт",List.of(),false,false,false))return;}
                        stairsCleared=true;mark(world);
                    }
                    if(a==2&&!stairsCleared&&time(p,world,15,3)){stairsCleared=true;GameIO.out().println("Джон обходит завал боковым коридором.");mark(world);}
                } else if(section==3){
                    GameIO.menu("Квартира №21 · Последний ужин","В прихожей обувь, на кухне три тарелки. Записка: «Если вернётесь раньше меня, еда в верхнем шкафу. Только не открывайте балкон, там разбито окно».");
                    GameIO.action(1,"Осмотреть кухню",kitchen?"Уже осмотрена":"10 минут",!kitchen);
                    GameIO.action(2,"Осмотреть спальню",bedroom?"Уже осмотрена":"10 минут",!bedroom);
                    GameIO.action(3,"Открыть балкон и осмотреть верёвку",balcony?"Уже осмотрено":"5 минут",!balcony);GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!kitchen&&time(p,world,10,2)&&take(p,Shop.createItem(17))){kitchen=true;mark(world);}
                    if(a==2&&!bedroom&&time(p,world,10,2)&&take(p,Shop.createItem(1))){bedroom=true;mark(world);}
                    if(a==3&&!balcony&&time(p,world,5,1)){balcony=true;String t="Между соседними балконами закреплена верёвка. Похоже, кто-то пытался попасть в соседнюю квартиру, обходя лестницу.";world.note("Верёвка на балконе",t);GameIO.out().println(t);mark(world);}
                } else if(section==4){
                    GameIO.menu("Квартира №24 · Запертая дверь",apartment24Open?"Дверь открыта. Внутри хозяйственные вещи и металлическая коробка.":"На двери глубокие царапины. Замок выглядит исправным.");
                    if(!apartment24Open){GameIO.action(1,"Вскрыть дверь ломом","10 минут · шум",place.hasCrowbar(p));if(balcony)GameIO.action(2,"Попробовать проход со стороны балкона","15 минут · тихий обход");}
                    if(apartment24Open){GameIO.action(3,"Обыскать хозяйственные вещи",apartment24Loot?"Уже осмотрены":"10 минут",!apartment24Loot);GameIO.action(4,"Открыть металлическую коробку",atticKeyTaken?"Ключ уже взят":"Ключ от чердачного люка",!atticKeyTaken);GameIO.action(5,"Осмотреть календарь",calendar?"Записка прочитана":"5 минут",!calendar);if(world.semen().canInspectWardrobe())GameIO.action(6,"Осмотреть шкаф с личными вещами","Красный шарф и семейные вещи");}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!apartment24Open&&place.hasCrowbar(p)&&time(p,world,10,3)){apartment24Open=true;GameIO.sound(GameSound.WOOD_DOOR);GameIO.out().println("Дверь вскрыта; шум разносится по подъезду.");mark(world);if(!BreakIn.attack(p,scanner,random))return;}
                    if(a==2&&!apartment24Open&&balcony&&time(p,world,15,3)){apartment24Open=true;GameIO.out().println("Джон находит тихий проход к соседней квартире.");mark(world);}
                    if(a==3&&apartment24Open&&!apartment24Loot&&time(p,world,10,2)&&take(p,Shop.createItem(20))){apartment24Loot=true;mark(world);}
                    if(a==4&&apartment24Open&&!atticKeyTaken&&take(p,new Tools(ATTIC_KEY,"Ключ открывает чердачный люк жилого подъезда.",1,0))){atticKeyTaken=true;mark(world);}
                    if(a==5&&apartment24Open&&!calendar&&time(p,world,5,1)){calendar=true;String t="Завтра проверю крышу. Кто-то снова открывает люк ночью.";world.note("Календарь в квартире №24",t);GameIO.out().println(t);mark(world);}
                    if(a==6&&apartment24Open&&world.semen().canInspectWardrobe()&&time(p,world,5,1))world.semen().wardrobe(p,world,scanner);
                } else if(section==6){
                    GameIO.menu("Квартира №34 · Пустые чемоданы","Окна закрыты, кровати заправлены. У входа стоят два пустых чемодана; следов борьбы нет.");
                    GameIO.action(1,"Прочитать список на столе",apartment34Note?"Уже прочитан":"5 минут",!apartment34Note);
                    GameIO.action(2,"Обыскать кухню",apartment34Food?"Уже осмотрена":"10 минут",!apartment34Food);
                    GameIO.action(3,"Осмотреть вещи",apartment34Material?"Уже осмотрены":"10 минут",!apartment34Material);GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!apartment34Note&&time(p,world,5,1)){apartment34Note=true;String t="Вода, документы, лекарства, одежда. Не брать лишнего. Сказали, что места в транспорте мало.";world.note("Список эвакуации жильцов",t);GameIO.out().println(t);mark(world);}
                    if(a==2&&!apartment34Food&&time(p,world,10,2)&&take(p,Shop.createItem(16))){apartment34Food=true;mark(world);}
                    if(a==3&&!apartment34Material&&time(p,world,10,2)&&take(p,Shop.createItem(48))){apartment34Material=true;mark(world);}
                } else if(section==7){
                    GameIO.menu("Чердачный люк",hatchOpen?"Люк открыт. Узкий проход ведёт на крыши домов.":"На верхнем пролёте заперт люк. Подойдёт ключ из квартиры №24 или лом.");
                    if(!hatchOpen){GameIO.action(1,"Открыть ключом","5 минут",hasAtticKey(p));GameIO.action(2,"Вскрыть ломом","10 минут · шум",place.hasCrowbar(p));}
                    if(hatchOpen)GameIO.action(3,"Обыскать чердачный проход",hatchLoot?"Уже осмотрен":"10 минут",!hatchLoot);
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!hatchOpen&&hasAtticKey(p)&&time(p,world,5,1) || a==2&&!hatchOpen&&place.hasCrowbar(p)&&time(p,world,10,3)){
                        hatchOpen=true;if(a==2){GameIO.sound(GameSound.METAL_DOOR);GameIO.out().println("Лом громко ударяет по замку.");}
                        world.note("Чердачный проход","Люк открыт. С крыши можно осмотреть жилые кварталы.");GameIO.out().println("Открыта локация: Крыши домов.");mark(world);if(a==2&&!BreakIn.attack(p,scanner,random))return;
                    }
                    if(a==3&&hatchOpen&&!hatchLoot&&time(p,world,10,2)&&take(p,Shop.createItem(19))){hatchLoot=true;mark(world);}
                } else break;
            }
        }
    }
}
