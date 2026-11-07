package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

final class Schoolyard implements Serializable {
    private static final long serialVersionUID=1L;
    private boolean schoolEntranceOpen;
    private boolean scouted, gateOpen, gateNotice, sportNote, sportWater, sportFood, sportCloth, sportBandage;
    private boolean busLists, busWater, busItems, shedOpen, shedWire, shedTape, shedPlan;
    private boolean serviceThreatKnown, serviceThreat, serviceCleared, serviceNote, serviceMaterial, legacyChecked;
    boolean schoolScouted(){return scouted;}
    boolean busListsRead(){return busLists;}
    boolean gateOpened(){return gateOpen;}
    boolean schoolEntranceOpen(){return schoolEntranceOpen;}
    void scoutSchool(WorldStories world){
        if(scouted)return;scouted=true;
        String text="С крыши видны школьные ворота и повреждённая секция ограждения сбоку. Через неё можно попасть во двор быстрее.";
        world.note("Разведка школьного двора",text);GameIO.out().println(text);GameIO.checkpoint();
    }
    private boolean time(Fighter p,WorldStories w,int minutes,int stamina){
        if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Передохните или вернитесь позже.");return false;}
        w.advanceMinutes(minutes);return true;
    }
    private boolean take(Fighter p,ItemInfo item){
        if(!p.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется здесь до следующего посещения.");return false;}
        GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;
    }
    private void mark(WorldStories w){w.markSearched(7);GameIO.checkpoint();}
    private void migrate(Location place){
        if(legacyChecked)return;
        if(place.legacySchoolGateOpened())gateOpen=true;
        for(Location.SearchPoint point:place.getSearchPoints())if(point.isSearched()){
            if(point.name.equals("Детская площадка") || point.name.equals("Стадион")){sportWater=true;sportFood=true;sportCloth=true;sportBandage=true;}
            if(point.name.equals("Склад инвентаря")){shedWire=true;shedTape=true;}
        }
        legacyChecked=true;
    }
    void visit(Fighter p,Location place,WorldStories world,Scanner scanner,Random random){
        migrate(place);boolean inside=false;
        while(p.isAlive()&&!inside){
            if(gateOpen){GameIO.out().println("Открытые ворота позволяют сразу пройти во двор.");inside=true;break;}
            int bypass=scouted?5:10;
            GameIO.menu("Школьные ворота","Главные ворота закрыты цепью. Через повреждённую секцию ограждения можно попасть внутрь без лома."+(scouted?" Вы уже заметили её с крыши.":""));
            GameIO.action(1,"Пройти через ограждение",bypass+" минут · без инструмента");
            GameIO.action(2,"Вскрыть цепь ломом","10 минут · шум; ворота останутся открыты",place.hasCrowbar(p));
            GameIO.action(0,"Вернуться домой");int a=SurvivalSystems.choice(scanner);if(a==0)return;
            if(a==1&&time(p,world,bypass,2)){inside=true;GameIO.out().println("Джон проходит через повреждённое ограждение.");}
            if(a==2&&place.hasCrowbar(p)&&time(p,world,10,3)){gateOpen=true;inside=true;GameIO.sound(GameSound.METAL_DOOR);GameIO.out().println("Цепь с громким скрежетом падает на землю. Ворота открыты.");mark(world);if(!BreakIn.attack(p,scanner,random))return;}
        }
        while(p.isAlive()&&inside){
            GameIO.menu("Школьный двор","У входа тихо; ближе к зданию могут встречаться одиночные заражённые.");
            GameIO.point(1,"Главные ворота",gateOpen?"Цепь вскрыта":"Цепь с замком и табличка",gateNotice&&gateOpen);
            GameIO.point(2,"Спортивная площадка","Рюкзаки, инвентарь и порванный тент",sportNote&&sportWater&&sportFood&&sportCloth&&sportBandage);
            GameIO.point(3,"Автобусная остановка","Два школьных автобуса",busLists&&busWater&&busItems);
            GameIO.point(4,"Хозяйственный сарай",shedOpen?"Дверь открыта":"Дверь закрыта навесным замком",shedOpen&&shedWire&&shedTape&&shedPlan);
            GameIO.point(5,"Двор хозяйственной части","Служебный автомобиль у школы · средний риск",serviceCleared&&serviceNote&&serviceMaterial);
            GameIO.action(6,"Вход в школу",schoolEntranceOpen?"Проход открыт":"Боковой проход или главные двери");
            GameIO.action(8,"Передохнуть","20 минут · восстановить силы");GameIO.action(0,"Вернуться домой");
            int section=SurvivalSystems.choice(scanner);if(section==0)return;if(section==8){Survivor.rest(p,30);continue;}
            if(section<1||section>6)continue;
            while(p.isAlive()){
                if(section==1){
                    GameIO.menu("Главные ворота","На воротах цепь с замком, рядом старая табличка с расписанием. С внутренней стороны к ограждению прикреплён лист.");
                    GameIO.action(1,"Прочитать лист",gateNotice?"Уже прочитан":"5 минут",!gateNotice);
                    if(!gateOpen)GameIO.action(2,"Вскрыть цепь ломом","10 минут · шум; ворота останутся открыты",place.hasCrowbar(p));
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!gateNotice&&time(p,world,5,1)){gateNotice=true;String t="Сбор возле спортивной площадки. Документы иметь при себе.";world.note("Объявление на школьных воротах",t);GameIO.out().println(t);mark(world);}
                    if(a==2&&!gateOpen&&place.hasCrowbar(p)&&time(p,world,10,3)){gateOpen=true;GameIO.sound(GameSound.METAL_DOOR);GameIO.out().println("Цепь громко падает. Теперь ворота останутся открыты.");mark(world);if(!BreakIn.attack(p,scanner,random))return;}
                }else if(section==2){
                    GameIO.menu("Спортивная площадка","На площадке брошенные рюкзаки, спортивный инвентарь и порванный тент.");
                    GameIO.action(1,"Осмотреть школьный рюкзак",sportNote?"Записка прочитана":"10 минут",!sportNote);
                    if(sportNote){
                    GameIO.action(2,"Взять воду",sportWater?"Уже взята":"Бутылка",!sportWater);
                    GameIO.action(3,"Взять галеты",sportFood?"Уже взяты":"Закрытая упаковка",!sportFood);
                    GameIO.action(4,"Взять ткань",sportCloth?"Уже взята":"Кусок плотной ткани",!sportCloth);
                    GameIO.action(5,"Взять спортивный бинт",sportBandage?"Уже взят":"Перевязочный материал",!sportBandage);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!sportNote&&time(p,world,10,2)){sportNote=true;String t="Нам сказали взять только самое важное. Я взял фотографию. Надеюсь, это считается.";world.note("Записка школьника",t);GameIO.out().println(t);mark(world);}
                    if(a==2&&sportNote&&!sportWater&&take(p,new Food("Бутылка минеральной воды","Чистая питьевая вода",1,4,0))){sportWater=true;mark(world);}
                    if(a==3&&sportNote&&!sportFood&&take(p,Shop.createItem(16))){sportFood=true;mark(world);}
                    if(a==4&&sportNote&&!sportCloth&&take(p,Shop.createItem(48))){sportCloth=true;mark(world);}
                    if(a==5&&sportNote&&!sportBandage&&take(p,new MedicalItem("Спортивный бинт",15,20))){sportBandage=true;mark(world);}
                }else if(section==3){
                    GameIO.menu("Автобусная остановка","Два школьных автобуса стоят возле двора. Один пустой, у другого открыта передняя дверь.");
                    GameIO.action(1,"Просмотреть списки пассажиров",busLists?"Уже прочитаны":"10 минут",!busLists);
                    if(busLists){GameIO.action(2,"Взять бутылку воды",busWater?"Уже взята":"Оставлена в автобусе",!busWater);
                    GameIO.action(3,"Осмотреть оставленные вещи",busItems?"Уже осмотрены":"5 минут",!busItems);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!busLists&&time(p,world,10,2)){busLists=true;String t="Группа А: посадка завершена. Группа Б: ожидать дополнительного транспорта — последняя строка перечёркнута. Неизвестно, удалось ли группе А выехать из города.";world.note("Школьные списки пассажиров",t);GameIO.out().println(t);mark(world);}
                    if(a==2&&busLists&&!busWater&&take(p,new Food("Бутылка минеральной воды","Чистая питьевая вода",1,4,0))){busWater=true;mark(world);}
                    if(a==3&&busLists&&!busItems&&time(p,world,5,1)&&take(p,Shop.createItem(16))){busItems=true;mark(world);}
                }else if(section==4){
                    GameIO.menu("Хозяйственный сарай",shedOpen?"Сарай открыт. На стене висит технический план школы.":"Дверь закрыта навесным замком. За дверью могут быть инструменты и хозяйственные материалы.");
                    if(!shedOpen)GameIO.action(1,"Вскрыть замок ломом","10 минут",place.hasCrowbar(p));
                    if(shedOpen){GameIO.action(2,"Взять проволоку",shedWire?"Уже взята":"Медная проволока",!shedWire);GameIO.action(3,"Взять изоленту",shedTape?"Уже взята":"Материал для ремонта",!shedTape);GameIO.action(4,"Изучить технический план",shedPlan?"Уже изучен":"5 минут",!shedPlan);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!shedOpen&&place.hasCrowbar(p)&&time(p,world,10,3)){shedOpen=true;GameIO.sound(GameSound.WOOD_DOOR);mark(world);if(!BreakIn.attack(p,scanner,random))return;}
                    if(a==2&&shedOpen&&!shedWire&&take(p,Shop.createItem(43))){shedWire=true;mark(world);}
                    if(a==3&&shedOpen&&!shedTape&&take(p,Shop.createItem(42))){shedTape=true;mark(world);}
                    if(a==4&&shedOpen&&!shedPlan&&time(p,world,5,1)){shedPlan=true;String t="Технический план показывает расположение школьной котельной и служебного входа, но не объясняет состояние оборудования.";world.note("План школьной котельной",t);GameIO.out().println(t);mark(world);}
                }else if(section==6){
                    GameIO.menu("Вход в школу",schoolEntranceOpen?"Вход открыт. Можно исследовать школу.":"Главные двери заперты. Сбоку находится повреждённый служебный проход.");
                    if(!schoolEntranceOpen){GameIO.action(1,"Открыть боковой проход","10 минут · без лома");GameIO.action(2,"Вскрыть главные двери ломом","10 минут · шум",place.hasCrowbar(p));}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(!schoolEntranceOpen && (a==1 || a==2 && place.hasCrowbar(p)) && time(p,world,10,2)){
                        schoolEntranceOpen=true;if(a==2)GameIO.sound(GameSound.METAL_DOOR);
                        world.note("Вход в школу","Открыт "+(a==1?"боковой проход":"главный вход")+" в школу. Теперь здание доступно для исследования.");
                        GameIO.out().println("Открыта локация: Школа.");mark(world);if(a==2&&!BreakIn.attack(p,scanner,random))return;
                    }
                }else if(section==5){
                    if(!serviceThreatKnown){serviceThreatKnown=true;serviceThreat=random.nextInt(3)==0;GameIO.checkpoint();}
                    GameIO.menu("Двор хозяйственной части",serviceThreat&&!serviceCleared?"Возле машины слышится движение: там может быть заражённый. Можно отойти.":"У здания стоят мусорные баки, поддоны и старый служебный автомобиль.");
                    if(!serviceCleared)GameIO.action(1,"Осторожно подойти к автомобилю","10 минут"+(serviceThreat?" · возможен бой":""));
                    if(serviceCleared){GameIO.action(2,"Прочитать накладную",serviceNote?"Уже прочитана":"В кузове пустая коробка от предохранителей",!serviceNote);GameIO.action(3,"Взять технические материалы",serviceMaterial?"Уже взяты":"Запчасти",!serviceMaterial);}
                    GameIO.action(0,"Вернуться позже");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!serviceCleared&&time(p,world,10,3)){
                        if(serviceThreat){GameIO.out().println("У автомобиля появляется заражённый.");if(!TacticalBattle.story(p,scanner,random,List.of(new Zombie(1,ZombieType.NORMAL)),"Школьный двор",List.of(),false,false,false))return;}
                        serviceCleared=true;mark(world);
                    }
                    if(a==2&&serviceCleared&&!serviceNote){serviceNote=true;String t="В кузове пустая коробка из-под предохранителей и накладная: «Котельная. Замена аварийного блока». Возможно, у котельной были проблемы с электроснабжением.";world.note("Накладная служебной машины",t);GameIO.out().println(t);mark(world);}
                    if(a==3&&serviceCleared&&!serviceMaterial&&take(p,Shop.createItem(19))){serviceMaterial=true;mark(world);}
                }
            }
        }
    }
}
