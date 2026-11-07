package com.example.survivor.game;

import java.io.Serializable;
import java.util.Random;
import java.util.Scanner;

/** One-time discoveries remain available across visits and saves. */
final class QuietCourtyard implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean backpack, note, car, trunk, lockedTrunk, redCar, boxes, crowbar, basementOpen, basementSearched, garageLoot, garagePlan;
    private boolean basementThreatKnown, basementThreat, basementCleared, passageExamined;

    private void mark(Location location, WorldStories world, String name) {
        for (Location.SearchPoint p : location.getSearchPoints()) if (p.name.equals(name)) { location.consumePoint(p); break; }
        world.markSearched(1); GameIO.checkpoint();
    }
    private boolean take(Fighter p, ItemInfo item) {
        if (!p.addItem(item)) { GameIO.out().println("В рюкзаке нет места. Освободите вес и вернитесь: находка останется здесь."); return false; }
        GameIO.sound(GameSound.BAG); GameIO.out().println("Найдено: " + item.getName()); return true;
    }
    private boolean time(Fighter p, WorldStories world, int minutes, int stamina) {
        if (!p.spendStamina(stamina)) { GameIO.out().println("Недостаточно сил. Можно отдохнуть или вернуться позже."); return false; }
        world.advanceMinutes(minutes); return true;
    }
    void visit(Fighter p, Location location, WorldStories world, Scanner scanner, Random random) {
        ResidentialStories stories=world.residential();SemenStory semen=world.semen();semen.beginVisit();
        while (p.isAlive()) {
            GameIO.menu("Тихий двор", "Ветер качает ржавые качели. У подъездов слышны редкие звуки. Выберите место для осмотра.");
            GameIO.point(1,"Детская площадка","Рюкзак и коробка под скамейкой",backpack&&note);
            GameIO.point(2,"Припаркованные автомобили","Салоны и багажники",car&&trunk&&lockedTrunk&&(!note||redCar));
            GameIO.point(3,"Мусорные контейнеры","Коробки и обломок двери",boxes&&crowbar);
            GameIO.point(4,"Подвальный вход",basementOpen?"Вход вскрыт":"Металлическая дверь заперта",basementSearched);
            GameIO.point(5,"Гараж дворника","Приоткрытая дверь и план территории",garageLoot&&garagePlan);
            if (world.day()>=3 || stories.family>0) GameIO.action(6,"Окно третьего этажа","За стеклом можно заметить движение");
            GameIO.action(7,"Передохнуть","20 минут · восстановить силы");
            GameIO.action(0,"Вернуться домой");
            int choice=SurvivalSystems.choice(scanner);
            if(choice==0)return;
            if(choice==7){Survivor.rest(p,30);continue;}
            if(choice==6 && (world.day()>=3 || stories.family>0)) { stories.visitWindow(world,p,scanner,random); if(stories.aborted || !p.isAlive())return; continue; }
            if(choice<1 || choice>5)continue;
            while(p.isAlive()) {
                if(choice==1) {
                    GameIO.menu("Детская площадка","Песочница, качели и разбросанные игрушки. Под скамейкой виднеется жестяная коробка.");
                    GameIO.action(1,"Проверить рюкзак",backpack?"Уже осмотрен":"5 минут",!backpack);
                    GameIO.action(2,"Осмотреть под скамейкой",note?"Коробка осмотрена":"5 минут",!note);GameIO.action(0,"Не задерживаться");
                    int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1 && !backpack && time(p,world,5,2) && take(p,Shop.createItem(16))) {backpack=true;mark(location,world,"Детская площадка");}
                    if(a==2 && !note && time(p,world,5,2)) {note=true;world.note("Записка на площадке","Папа сказал ждать его возле красной машины.");GameIO.out().println("В коробке записка: «Папа сказал ждать его возле красной машины».");take(p,Shop.createItem(19));mark(location,world,"Детская площадка");}
                } else if(choice==2) {
                    if(!semen.semen_met)semen.firstMeeting(p,world,scanner);
                    GameIO.menu("Припаркованные автомобили","Два автомобиля брошены, один закрыт. Красная машина стоит у дома.");
                    GameIO.action(1,"Осмотреть салон",car?"Уже осмотрен":"10 минут",!car);
                    GameIO.action(2,"Проверить открытый багажник",trunk?"Уже осмотрен":"10 минут",!trunk);
                    GameIO.action(3,"Вскрыть закрытый багажник",lockedTrunk?"Уже обыскан":"Нужен лом · 10 минут",!lockedTrunk && location.hasCrowbar(p));
                    if(note)GameIO.action(4,"Осмотреть красную машину",redCar?"Уже осмотрена":"10 минут",!redCar);
                    GameIO.action(5,semen.semen_trust_lost?"Осмотреть синюю машину":"Поговорить с Семёном",semen.canApproach()?"Старая синяя машина":"Семён просит вернуться позже",semen.canApproach());
                    if(semen.rewardAvailable())GameIO.action(6,"Забрать припасы Семёна","Оставлены в багажнике");
                    if(semen.bonusAvailable())GameIO.action(7,"Проверить ящик за красной машиной","Подсказка Семёна");
                    GameIO.action(8,"Разговор по душам",SemenHeartTalk.hint(semen,world),SemenHeartTalk.available(semen,world));
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!car&&time(p,world,10,2)&&take(p,new Food("Бутылка минеральной воды","Чистая питьевая вода",1,4,0))){car=true;mark(location,world,"Припаркованные автомобили");}
                    if(a==2&&!trunk&&time(p,world,10,2)&&take(p,Shop.createItem(48))){trunk=true;mark(location,world,"Припаркованные автомобили");}
                    if(a==3&&!lockedTrunk&&location.hasCrowbar(p)&&time(p,world,10,3)&&take(p,Shop.createItem(19))){lockedTrunk=true;mark(location,world,"Припаркованные автомобили");if(!BreakIn.attack(p,scanner,random))return;}
                    if(a==4&&note&&!redCar&&time(p,world,10,2)){redCar=true;world.note("Красная машина","Машина пуста. На сиденье пакет с детской курткой и записка: «Не жди меня возле дома».");GameIO.out().println("На сиденье детская куртка и записка: «Не жди меня возле дома».");mark(location,world,"Припаркованные автомобили");}
                    if(a==5&&semen.canApproach())semen.talk(p,world,scanner);
                    if(a==6&&semen.rewardAvailable())semen.rewards(p,scanner);
                    if(a==7&&semen.bonusAvailable()&&time(p,world,5,1))semen.bonus(p,world);
                    if(a==8)SemenHeartTalk.open(semen,world,scanner);
                } else if(choice==3) {
                    GameIO.menu("Мусорные контейнеры","Коробки и сломанная мебель. Под тяжёлым обломком двери виднеется металлический рычаг.");
                    GameIO.action(1,"Осмотреть коробки",boxes?"Уже осмотрены":"5 минут",!boxes);
                    GameIO.action(2,"Разобрать завал",crowbar?"Лом уже забран":"10 минут · немного сил",!crowbar);
                    GameIO.action(0,"Уйти");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!boxes&&time(p,world,5,2)&&take(p,Shop.createItem(42))){boxes=true;mark(location,world,"Мусорные контейнеры");}
                    if(a==2&&!crowbar&&time(p,world,10,4)&&take(p,Shop.createItem(27))){crowbar=true;GameIO.out().println("Лом позволит вскрывать некоторые закрытые помещения. Но шум может привлечь заражённых.");mark(location,world,"Мусорные контейнеры");}
                } else if(choice==4) {
                    if(!basementThreatKnown){basementThreatKnown=true;basementThreat=random.nextInt(3)==0;}
                    GameIO.menu("Подвальный вход",(basementOpen?"Дверь открыта.":"Дверь заперта. Для вскрытия нужен лом.")+(basementThreat&&!basementCleared?" Из-за двери слышится движение.":" За дверью тихо."));
                    if(!basementOpen)GameIO.action(1,"Вскрыть дверь ломом","10 минут; после вскрытия можно отойти",location.hasCrowbar(p));
                    if(basementOpen)GameIO.action(2,"Войти и обыскать",basementSearched?"Уже обыскано":"10 минут",!basementSearched);
                    GameIO.action(0,"Отойти");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!basementOpen&&location.hasCrowbar(p)&&time(p,world,10,3)){basementOpen=true;GameIO.checkpoint();if(!BreakIn.attack(p,scanner,random))return;}
                    if(a==2&&basementOpen&&!basementSearched&&time(p,world,10,3)) {
                        if(basementThreat&&!basementCleared){GameIO.out().println("Из темноты показывается заражённый.");if(!TacticalBattle.story(p,scanner,random,java.util.List.of(new Zombie(1,ZombieType.NORMAL)),"Подвал",java.util.List.of(),false,false,true))return;basementCleared=true;}
                        if(take(p,Shop.createItem(43))){basementSearched=true;mark(location,world,"Подвальный вход");}
                    }
                } else {
                    GameIO.menu("Гараж дворника","Дверь приоткрыта. На стене висит план жилого комплекса.");
                    GameIO.action(1,"Осмотреть инструменты",garageLoot?"Уже осмотрены":"10 минут",!garageLoot);
                    GameIO.action(2,"Изучить план территории",garagePlan?"План изучен":"5 минут",!garagePlan);GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!garageLoot&&time(p,world,10,2)&&take(p,Shop.createItem(19))){garageLoot=true;mark(location,world,"Гараж дворника");}
                    if(a==2&&!garagePlan&&time(p,world,5,1)){garagePlan=true;world.note("План жилого комплекса","Между домами отмечен служебный проход к жилому подъезду и верхним этажам.");mark(location,world,"Гараж дворника");}
                }
            }
        }
    }
    boolean knowsServicePassage(){return garagePlan;}
    boolean passageAvailable(){return garagePlan && !passageExamined;}
    void inspectPassage(WorldStories world, Scanner scanner) {
        if (!passageAvailable()) return;
        passageExamined=true;
        world.note("Служебный проход", "План дворника привёл к боковому проходу между домами. Отсюда виден подъезд и путь к верхним этажам.");
        ResidentialStories.pause("Служебный проход", "У стены подъезда найден проход, отмеченный на плане дворника. Здесь можно обойти основной двор; дальше видна лестница к верхним этажам.",scanner);
        GameIO.checkpoint();
    }
}

