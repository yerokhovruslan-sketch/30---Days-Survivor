package com.example.survivor.game;

import java.io.Serializable;
import java.util.Random;
import java.util.Scanner;

final class Rooftops implements Serializable {
    private static final long serialVersionUID=1L;
    private boolean legacyChecked, entranceInspected, boardsTaken, nailsTaken, cableTaken, cabinetTaken, passageCleared, dovecoteSearched, dovecoteOpen;
    boolean passageCleared(){return passageCleared;}
    private boolean time(Fighter p,WorldStories w,int minutes,int cost){
        if(!p.spendStamina(cost)){GameIO.out().println("Недостаточно сил. Передохните или вернитесь позже.");return false;}
        w.advanceMinutes(minutes);return true;
    }
    private boolean take(Fighter p,ItemInfo item){
        if(!p.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется на крыше.");return false;}
        GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;
    }
    private void progress(WorldStories w){w.markSearched(6);GameIO.checkpoint();}
    private ItemInfo find(Fighter p,String name){return p.getInventory().stream().filter(i->i.getName().equals(name)).findFirst().orElse(null);}
    private boolean tool(Fighter p,Location l){return l.hasCrowbar(p)||p.getInventory().stream().anyMatch(i->i.isRepairTool() && (!i.isDurable()||i.getDurability()>0));}
    private void migrate(Location l){
        if(legacyChecked)return;
        for(Location.SearchPoint point:l.getSearchPoints())if(point.isSearched()){
            if(point.name.equals("Технический этаж")){cableTaken=true;cabinetTaken=true;}
            if(point.name.equals("Голубятня"))dovecoteSearched=true;
        }
        legacyChecked=true;
    }
    void visit(Fighter p,Location place,WorldStories world,Scanner scanner,Random random){
        migrate(place);boolean farSide=false;
        while(p.isAlive()){
            GameIO.menu("Крыши домов",farSide?"Джон находится у технической площадки на соседней крыше.":"Из металлического люка видны доски, провода и следы чужого пребывания.");
            GameIO.point(1,"Чердачный выход",farSide?"Вернитесь к люку через переход":"Следы у люка и материалы",entranceInspected&&boardsTaken&&nailsTaken,!farSide);
            GameIO.point(2,"Техническая площадка",farSide?"Вентиляция и кабельные коробки":"На соседней крыше: выберите путь через переход",cableTaken&&cabinetTaken,farSide);
            GameIO.action(3,"Переход между крышами",passageCleared?"Расчищен · безопасный путь":"Настил частично повреждён");
            if(farSide)GameIO.action(4,"Голубятня","Старая запертая постройка",!dovecoteSearched);
            if(!world.schoolyard().schoolScouted()) GameIO.action(5,"Осмотреть школьный двор", "5 минут · разведать ограждение");
            GameIO.action(8,"Передохнуть","20 минут · восстановить силы");GameIO.action(0,"Вернуться домой",farSide?(passageCleared?"5 минут до люка по переходу":"15 минут до люка обычным маршрутом"):"");
            int section=SurvivalSystems.choice(scanner);if(section==0){if(!farSide || time(p,world,passageCleared?5:15,passageCleared?1:3))return;continue;}
            if(section==8){Survivor.rest(p,30);continue;}
            if(section==5 && !world.schoolyard().schoolScouted() && time(p,world,5,1)){world.schoolyard().scoutSchool(world);continue;}
            if(section==4&&farSide&&!dovecoteSearched){
                GameIO.menu("Голубятня",dovecoteOpen?"Дверь вскрыта. Внутри может остаться тайник.":"Маленькая запертая постройка, где может остаться тайник.");
                GameIO.action(1,dovecoteOpen?"Осмотреть голубятню":"Вскрыть ломом и осмотреть", "10 минут",dovecoteOpen||place.hasCrowbar(p));GameIO.action(0,"Назад");
                if(SurvivalSystems.choice(scanner)==1 && (dovecoteOpen||place.hasCrowbar(p)) && time(p,world,10,3)){
                    if(!dovecoteOpen){dovecoteOpen=true;GameIO.checkpoint();if(!BreakIn.attack(p,scanner,random))return;}
                    ItemInfo found=Item.createLootForLocation(place,random);
                    if(found==null || take(p,found)){dovecoteSearched=true;for(Location.SearchPoint old:place.getSearchPoints())if(old.name.equals("Голубятня"))place.consumePoint(old);progress(world);}
                }continue;
            }
            if(section==1&&!farSide){
                GameIO.menu("Чердачный выход","На крыше лежат старые доски, инструменты и обрывки проводов. Рядом — пустая бутылка, упаковка еды и аккуратно сложенная ткань. На стене: «Не ходить к северному краю после заката».");
                GameIO.action(1,"Осмотреть следы и надпись",entranceInspected?"Уже осмотрены":"5 минут",!entranceInspected);
                GameIO.action(2,"Взять деревянные доски",boardsTaken?"Уже взяты":"5 минут",!boardsTaken);
                GameIO.action(3,"Забрать коробку гвоздей",nailsTaken?"Уже взяты":"5 минут",!nailsTaken);
                GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);
                if(a==1&&!entranceInspected&&time(p,world,5,1)){entranceInspected=true;String t="У люка свежие следы: пустая бутылка, упаковка еды, сложенная ткань. На стене написано: «Не ходить к северному краю после заката».";world.note("Следы на крыше",t);GameIO.out().println(t);progress(world);}
                if(a==2&&!boardsTaken&&time(p,world,5,1)&&take(p,new Tools("Деревянные доски","Доски для укрепления укрытия и ремонта мебели",1,5))){boardsTaken=true;progress(world);}
                if(a==3&&!nailsTaken&&time(p,world,5,1)&&take(p,new Tools("Гвозди","Коробка строительных гвоздей",1,4))){nailsTaken=true;progress(world);}
            } else if(section==2&&farSide){
                GameIO.menu("Техническая площадка","Вентиляционное оборудование, старый металлический шкаф и несколько кабельных коробок.");
                GameIO.action(1,"Осмотреть кабельную коробку",cableTaken?"Уже осмотрена":"10 минут · проволока",!cableTaken);
                GameIO.action(2,"Открыть металлический шкаф",cabinetTaken?"Уже осмотрен":"15 минут · нужен подходящий инструмент",!cabinetTaken&&tool(p,place));GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);
                if(a==1&&!cableTaken&&time(p,world,10,2)&&take(p,Shop.createItem(43))){cableTaken=true;progress(world);}
                if(a==2&&!cabinetTaken&&tool(p,place)&&time(p,world,15,3)&&take(p,Shop.createItem(19))){cabinetTaken=true;progress(world);}
            } else if(section==3){
                boolean parts=find(p,"Деревянные доски")!=null&&find(p,"Гвозди")!=null;
                GameIO.menu("Переход между крышами",passageCleared?"Переход расчищен и укреплён. По нему можно безопасно попасть на соседнюю крышу.":"Часть настила повреждена. Можно укрепить его либо выбрать более долгий обычный маршрут.");
                if(!passageCleared)GameIO.action(1,"Расчистить и укрепить переход","30 минут · 1 доски и 1 гвозди из рюкзака",parts);
                GameIO.action(2,"Использовать обычный маршрут","15 минут · материалы не расходуются");
                if(passageCleared)GameIO.action(3,"Пройти по расчищенному переходу","5 минут");GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);
                if(a==1&&!passageCleared&&parts&&time(p,world,30,5)){p.getInventory().remove(find(p,"Деревянные доски"));p.getInventory().remove(find(p,"Гвозди"));passageCleared=true;world.note("Переход между крышами","Переход укреплён. Теперь по нему можно безопасно добраться до соседней крыши за пять минут.");GameIO.out().println("Переход укреплён. Материалы израсходованы.");progress(world);}
                if(a==2&&time(p,world,15,3)){farSide=!farSide;GameIO.out().println("Джон проходит обычным маршрутом.");}
                if(a==3&&passageCleared&&time(p,world,5,1)){farSide=!farSide;GameIO.out().println("Джон проходит по укреплённому настилу.");}
            }
        }
    }
}
