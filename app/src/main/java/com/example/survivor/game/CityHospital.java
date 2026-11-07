package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Human stories and transport records in the city hospital. */
final class CityHospital implements Serializable {
    private static final long serialVersionUID=1L;
    static final String KEY="Служебный ключ больницы";
    static final String CODE="17 / Б / 04";
    boolean introduced,legacyChecked,schoolConnection,radioConnection;
    boolean receptionLoot,keyTaken,registry,request;
    boolean dressingBandage,dressingRoll,dressingThreat,dressingCleared,dressingKit,dressingValuable,dressingNote,codeFound;
    boolean wardRoll,wardThreat,wardCleared,wardLoot,patientNotebook;
    boolean dispatchOpen,routeBoard,dispatcherNote,route17;
    private boolean time(Fighter p,WorldStories w,int minutes,int stamina){if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Можно передохнуть или вернуться позже.");return false;}w.advanceMinutes(minutes);return true;}
    private boolean take(Fighter p,ItemInfo item){if(!p.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется здесь.");return false;}GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;}
    private void mark(Location place,WorldStories w,String name){for(Location.SearchPoint point:place.getSearchPoints())if(point.name.equals(name)){place.consumePoint(point);break;}w.markSearched(12);GameIO.checkpoint();}
    private void note(WorldStories w,String title,String message){w.note(title,message);GameIO.out().println(message);GameIO.checkpoint();}
    private boolean key(Fighter p){return p.getInventory().stream().anyMatch(x->x.getName().equals(KEY));}
    private boolean fight(Fighter p,Scanner s,Random r,String terrain){return TacticalBattle.story(p,s,r,List.of(new Zombie(1,ZombieType.NORMAL)),terrain,List.of(),false,false,false);}
    private void connections(Fighter p,WorldStories w){
        if(introduced&&w.schoolyard().busListsRead()&&!schoolConnection){schoolConnection=true;note(w,"Группа Б в школе и больнице","В школьных списках тоже была группа Б. Джон замечает совпадение, но не знает, обозначает ли оно одну и ту же систему распределения.");}
        if(route17&&p.getUpgrades().signalReceived&&!radioConnection){radioConnection=true;note(w,"Маршрут и сигнал","В больничных записях упоминается медицинский маршрут и пункт сбора. Возможно, это связано с радиопередачей, но доказательств пока нет.");}
    }
    private void migrate(Location loc){if(legacyChecked)return;legacyChecked=true;for(Location.SearchPoint point:loc.getSearchPoints())if(point.isSearched()){
        if(point.name.equals("Приёмное отделение"))receptionLoot=true;
        if(point.name.equals("Перевязочная")||point.name.equals("Операционная")){dressingBandage=true;dressingKit=true;}
        if(point.name.equals("Палаты второго этажа")||point.name.equals("Морг"))wardLoot=true;
    }}
    void visit(Fighter p,Location loc,WorldStories w,Scanner scanner,Random random){
        migrate(loc);
        if(!introduced){introduced=true;GameIO.checkpoint();ResidentialStories.pause("Городская больница","В приёмном отделении у входа стоят пустые инвалидные кресла; вдоль стены выстроены носилки, пол покрыт документами.\n\nНа двери: «Временный пункт подготовки к эвакуации пациентов». Рядом рукописный лист: «Людям из группы Б оставаться в палатах. Не покидать отделение без сопровождающего».",scanner);}
        connections(p,w);
        while(p.isAlive()){
            GameIO.menu("Городская больница","Большой повреждённый корпус. Часть помещений разграблена, в дальних отделениях может быть опасно.");
            GameIO.point(1,"Приёмное отделение","Открыто · низкий риск",receptionLoot&&keyTaken&&registry&&request);GameIO.point(2,"Перевязочная","Открыта · движение за ширмой",dressingBandage&&dressingCleared&&dressingKit&&dressingNote&&codeFound);
            GameIO.point(3,"Палаты второго этажа","Проход частично заблокирован",wardCleared&&wardLoot&&patientNotebook);GameIO.point(4,"Диспетчерская эвакуации",dispatchOpen?"Открыта":"Заперта · ключ или лом",dispatchOpen&&routeBoard&&dispatcherNote&&route17);
            GameIO.action(5,"Осмотреть тело","Проверить и обработать раны");GameIO.action(6,"Передохнуть","Восстановить силы");GameIO.action(0,"Вернуться домой");
            int section=SurvivalSystems.choice(scanner);if(section==0)return;if(section==5){Survivor.inspectBody(p,scanner);continue;}if(section==6){Survivor.rest(p,30);continue;}
            while(p.isAlive()&&section>=1&&section<=4){
                if(section==1){
                    GameIO.menu("Приёмное отделение","На стойке лежат карты пациентов; часть разорвана. Рядом шкаф с остатками медицинских принадлежностей.");GameIO.illustratedCard("","","hospital_art:hospital_reception");
                    GameIO.action(1,"Обыскать медицинский шкаф",receptionLoot?"Уже осмотрен":"10 минут · безопасно · бинт",!receptionLoot);
                    GameIO.action(2,"Взять служебный ключ",keyTaken?"Ключ уже взят":"Ключ от диспетчерской",!keyTaken);
                    GameIO.action(3,"Изучить регистрационные документы",registry?"Уже изучены":"5 минут",!registry);
                    GameIO.action(4,"Проверить журнал запросов",request?"Уже прочитан":"5 минут",!request);GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!receptionLoot&&time(p,w,10,2)&&take(p,Shop.createItem(1))){receptionLoot=true;mark(loc,w,"Приёмное отделение");}
                    if(a==2&&!keyTaken&&time(p,w,5,1)&&take(p,new Tools(KEY,"Ключ от диспетчерской эвакуации городской больницы.",1,0))){keyTaken=true;mark(loc,w,"Приёмное отделение");}
                    if(a==3&&!registry&&time(p,w,5,1)){registry=true;note(w,"Регистрация пациентов больницы","В списках отметки «Переведён» и «Ожидает». У нескольких пациентов указан номер транспортного маршрута, но сами маршруты не расшифрованы.");mark(loc,w,"Приёмное отделение");}
                    if(a==4&&!request&&time(p,w,5,1)){request=true;note(w,"Запрос на медицинское сопровождение","Для транспортировки пациентов требуется медицинское сопровождение. Запрос с номером заявки направлен на станцию скорой помощи.");mark(loc,w,"Приёмное отделение");}
                }else if(section==2){
                    GameIO.menu("Перевязочная",dressingCleared?"У входа перевязочный стол, за ширмой медицинские шкафы. В дальней части тихо.":"У входа перевязочный стол и несколько бинтов. За ширмой стоят шкафы; из дальней части слышатся шаги.");GameIO.illustratedCard("","","hospital_art:hospital_dressing");
                    GameIO.action(1,"Забрать материалы у входа",dressingBandage?"Уже взяты":"5 минут · безопасно · бинт",!dressingBandage);
                    GameIO.action(2,"Пройти за ширму",dressingCleared?"Дальняя часть осмотрена":"15 минут · возможна встреча",!dressingCleared);
                    GameIO.action(3,"Осмотреть отметку на шкафчике",codeFound?"Комбинация записана":"5 минут",!codeFound);
                    if(dressingCleared){GameIO.action(4,"Обыскать медицинский шкаф",dressingKit?"Уже обыскан":"Медицинские материалы",!dressingKit);GameIO.action(5,"Прочитать медицинский журнал",dressingNote?"Уже прочитан":"5 минут",!dressingNote);}
                    GameIO.action(0,"Вернуться позже");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!dressingBandage&&time(p,w,5,1)&&take(p,Shop.createItem(1))){dressingBandage=true;mark(loc,w,"Перевязочная");}
                    if(a==2&&!dressingCleared){if(!dressingRoll){dressingRoll=true;dressingThreat=random.nextInt(100)<55;dressingValuable=random.nextInt(100)<65;GameIO.checkpoint();}if(!time(p,w,15,3))continue;if(dressingThreat&&!fight(p,scanner,random,"Перевязочная городской больницы"))return;dressingCleared=true;mark(loc,w,"Перевязочная");}
                    if(a==3&&!codeFound&&time(p,w,5,1)){codeFound=true;note(w,"Загадочная комбинация","На внутренней стороне шкафчика в перевязочной Джон обнаружил комбинацию: «17 / Б / 04». Что она означает и где её применять, пока неизвестно.");}
                    if(a==4&&dressingCleared&&!dressingKit&&take(p,Shop.createItem(dressingValuable?2:1))){dressingKit=true;GameIO.checkpoint();}
                    if(a==5&&dressingCleared&&!dressingNote&&time(p,w,5,1)){dressingNote=true;note(w,"Медицинский журнал больницы","Приоритет транспортировки изменён. Пациенты, требующие постоянного наблюдения, остаются до прибытия специализированного транспорта. Отметка сделана после первых записей об эвакуации.");}
                }else if(section==3){
                    GameIO.menu("Палаты второго этажа",wardCleared?"Кровати отодвинуты. В тихом коридоре большинство палат пусты.":"Коридор частично заблокирован кроватями. За одной из дверей слышится движение.");GameIO.illustratedCard("","","hospital_art:hospital_wards");
                    if(!wardCleared)GameIO.action(1,"Расчистить проход и проверить палаты","15 минут · возможна встреча");
                    if(wardCleared){GameIO.action(2,"Прочитать блокнот пациента",patientNotebook?"Уже прочитан":"5 минут",!patientNotebook);GameIO.action(3,"Проверить личные вещи",wardLoot?"Уже осмотрены":"5 минут · небольшая находка",!wardLoot);}
                    GameIO.action(0,"Отступить");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!wardCleared){if(!wardRoll){wardRoll=true;wardThreat=random.nextInt(100)<50;GameIO.checkpoint();}if(!time(p,w,15,3))continue;if(wardThreat&&!fight(p,scanner,random,"Палаты городской больницы"))return;wardCleared=true;mark(loc,w,"Палаты второго этажа");}
                    if(a==2&&wardCleared&&!patientNotebook&&time(p,w,5,1)){patientNotebook=true;note(w,"Блокнот пациента","В почти нетронутой палате на кровати аккуратно сложено одеяло, рядом стоят ботинки. В блокноте: «Сказали, что скоро нас вывезут. Нужно дождаться врача, он объяснит, куда именно».\n\n«Сегодня забрали тех, кто может ходить самостоятельно. Нас попросили остаться».\n\n«Машина приезжала утром. Но не за нами». Последняя запись обрывается; дальнейшая судьба пациента неизвестна.");}
                    if(a==3&&wardCleared&&!wardLoot&&time(p,w,5,1)&&take(p,Shop.createItem(16))){wardLoot=true;GameIO.checkpoint();}
                }else{
                    GameIO.menu("Диспетчерская эвакуации",dispatchOpen?"На стене карта города и таблица перевозок; на столе телефон и записки диспетчера.":"Дверь кабинета закрыта. За стеклом видны карта города и доска расписания.");GameIO.illustratedCard("","","hospital_art:hospital_dispatch");
                    if(!dispatchOpen){GameIO.action(1,"Открыть служебным ключом","5 минут · тихо",key(p));GameIO.action(2,"Вскрыть ломом","10 минут · шум",loc.hasCrowbar(p));}
                    if(dispatchOpen){GameIO.action(3,"Изучить таблицу перевозок",routeBoard?"Уже изучена":"5 минут",!routeBoard);GameIO.action(4,"Прочитать записку диспетчера",dispatcherNote?"Уже прочитана":"5 минут",!dispatcherNote);GameIO.action(5,"Проверить маршрут №17",route17?"Уже записан":"5 минут",!route17);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(!dispatchOpen&&((a==1&&key(p))||(a==2&&loc.hasCrowbar(p)))&&time(p,w,a==1?5:10,2)){dispatchOpen=true;GameIO.sound(GameSound.METAL_DOOR);mark(loc,w,"Диспетчерская эвакуации");if(a==2&&!BreakIn.attack(p,scanner,random))return;}
                    if(a==3&&dispatchOpen&&!routeBoard&&time(p,w,5,1)){routeBoard=true;note(w,"Таблица медицинских перевозок","Некоторые маршруты отмечены как выполненные, другие зачёркнуты. Из таблицы нельзя установить судьбу всех пациентов.");}
                    if(a==4&&dispatchOpen&&!dispatcherNote&&time(p,w,5,1)){dispatcherNote=true;note(w,"Записка диспетчера больницы","Перевозки гражданских согласовывать через районный отдел. Самостоятельно направлять машины к пункту сбора запрещено.");}
                    if(a==5&&dispatchOpen&&!route17&&time(p,w,5,1)){route17=true;note(w,"Маршрут №17","Маршрут №17. Медицинское сопровождение. Подтверждение о возвращении не получено. Это запись на конкретный момент; неизвестно, вернулась ли машина позже.");connections(p,w);}
                }
            }
        }
    }
}
