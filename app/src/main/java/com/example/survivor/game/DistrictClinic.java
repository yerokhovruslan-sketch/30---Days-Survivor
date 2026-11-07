package com.example.survivor.game;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Four persistent investigation points in the district clinic. */
final class DistrictClinic implements Serializable {
    private static final long serialVersionUID = 1L;
    static final String CABINET_KEY = "Ключ от шкафа №4 поликлиники";
    boolean introduced, legacyChecked, clinic_patient_registry_found, keyJournal, registryLoot;
    boolean registryMarinaNote, archiveConnection;
    boolean entranceSupplies, procedureRoll, procedureThreat, procedureCleared, procedureSupplies, keyTaken;
    boolean cabinetOpen, cabinetBandage1, cabinetBandage2, cabinetKit, cabinetManifest;
    boolean nearArchive, archiveRoll, archiveThreat, archiveCleared, patientLists;

    private boolean time(Fighter p, WorldStories w, int minutes, int stamina) {
        if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Вернитесь после отдыха.");return false;}
        w.advanceMinutes(minutes);return true;
    }
    private boolean take(Fighter p,ItemInfo item) {
        if(!p.addItem(item)){GameIO.out().println("Рюкзак полон. Находка останется здесь до возвращения.");return false;}
        GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;
    }
    private void mark(Location place,WorldStories w,String name) {
        for(Location.SearchPoint point:place.getSearchPoints())if(point.name.equals(name)){place.consumePoint(point);break;}
        w.markSearched(10);GameIO.checkpoint();
    }
    private void note(WorldStories w,String title,String text){w.note(title,text);GameIO.out().println(text);GameIO.checkpoint();}
    private boolean hasKey(Fighter p){return p.getInventory().stream().anyMatch(i->i.getName().equals(CABINET_KEY));}
    private boolean helpedMarina(WorldStories w){int state=w.residential().family;return state==2||state==3||state==7;}
    private void connections(WorldStories w){
        if(clinic_patient_registry_found && helpedMarina(w) && !registryMarinaNote){registryMarinaNote=true;note(w,"Марина и списки","Марина говорила о таких списках. Похоже, их составляли не только в больнице.");}
        if(clinic_patient_registry_found && patientLists && !archiveConnection){archiveConnection=true;note(w,"Копия списков пациентов","Списки в архиве — копия документов, упомянутых в журнале регистратуры.");}
    }
    private void migrate(Location place,WorldStories w){
        if(legacyChecked)return;legacyChecked=true;
        for(Location.SearchPoint point:place.getSearchPoints())if(point.isSearched()){
            if(point.name.equals("Регистратура"))registryLoot=true;
            if(point.name.equals("Процедурный кабинет")){entranceSupplies=true;procedureSupplies=true;}
            if(point.name.equals("Служебный шкаф")){cabinetBandage1=true;cabinetBandage2=true;cabinetKit=true;}
            if(point.name.equals("Подвал"))nearArchive=true;
        }
        if(w.residential().clinicLoot){cabinetBandage1=true;cabinetBandage2=true;cabinetKit=true;}
    }
    private boolean encounter(Fighter p,Scanner s,Random r,String terrain){
        return TacticalBattle.story(p,s,r,List.of(new Zombie(1,ZombieType.NORMAL)),terrain,List.of(),false,false,false);
    }
    void visit(Fighter p,Location place,WorldStories w,Scanner scanner,Random random){
        migrate(place,w);
        if(!introduced){introduced=true;GameIO.checkpoint();ResidentialStories.pause("Районная поликлиника","Джон входит через повреждённые стеклянные двери. В холле сдвинуты скамейки, на полу медицинские карты, над регистратурой погасшее табло.\n\n«Пациентам, нуждающимся в транспортировке, обращаться к дежурному врачу. Самостоятельно покидать учреждение до завершения регистрации не рекомендуется».\n\nКарандашом ниже дописано: «А куда нам ехать?»",scanner);}
        connections(w);
        while(p.isAlive()){
            GameIO.menu("Районная поликлиника","Частично разграбленная поликлиника. Списки пациентов остались там, где медики ждали транспорт.");
            GameIO.point(1,"Регистратура","Открыта · безопасно у входа",registryLoot&&clinic_patient_registry_found&&keyJournal);
            GameIO.point(2,"Процедурный кабинет","Открыт · из-за ширмы слышится движение",entranceSupplies&&procedureCleared&&procedureSupplies&&keyTaken);
            GameIO.point(3,"Служебный шкаф №4",cabinetOpen?"Открыт":"Заперт · ключ или лом",cabinetOpen&&cabinetBandage1&&cabinetBandage2&&cabinetKit&&cabinetManifest);
            GameIO.point(4,"Подвальный архив","Лестница доступна · шаги в дальнем коридоре",nearArchive&&archiveCleared&&patientLists);
            GameIO.action(5,"Осмотреть тело","Проверить травмы и обработать раны");
            GameIO.action(6,"Передохнуть","Восстановить силы");GameIO.action(0,"Вернуться домой");
            int section=SurvivalSystems.choice(scanner);if(section==0)return;
            if(section==5){Survivor.inspectBody(p,scanner);continue;}if(section==6){Survivor.rest(p,30);continue;}
            while(p.isAlive() && section>=1 && section<=4){
                if(section==1){
                    GameIO.menu("Регистратура","Открытые ящики, карточки пациентов на полу и запертый металлический шкаф. У компьютера лежат журналы. Возле стойки осталась сумка с детской курткой; к ручке прикреплён талон на приём.");
                    GameIO.illustratedCard("", "", "clinic_art:clinic_registry");
                    GameIO.action(1,"Обыскать рабочие ящики",registryLoot?"Уже осмотрены":"5 минут · безопасно · базовый бинт",!registryLoot);
                    GameIO.action(2,"Изучить журнал регистратуры",clinic_patient_registry_found?"Записи прочитаны":"10 минут",!clinic_patient_registry_found);
                    GameIO.action(3,"Изучить журнал ключей",keyJournal?"Уже прочитан":"5 минут",!keyJournal);GameIO.action(0,"Назад");
                    int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!registryLoot&&time(p,w,5,1)&&take(p,Shop.createItem(1))){registryLoot=true;mark(place,w,"Регистратура");}
                    if(a==2&&!clinic_patient_registry_found&&time(p,w,10,1)){clinic_patient_registry_found=true;note(w,"Журнал регистратуры","Вместо времени приёма указаны фамилии и отметки: «Самостоятельно», «С сопровождением», «Требуется транспорт». На последней странице: «Списки переданы старшей медсестре. Копия в нижнем архиве».");connections(w);}
                    if(a==3&&!keyJournal&&time(p,w,5,1)){keyJournal=true;note(w,"Журнал ключей поликлиники","Последняя отметка: «Шкаф №4. Ключ у старшей медсестры».");}
                } else if(section==2){
                    GameIO.menu("Процедурный кабинет",procedureCleared?"За ширмой теперь тихо. Большинство открытых шкафов пусты: бинты и лекарства отсюда забрали раньше. У дальней стены осталась закрытая металлическая коробка.":"Дверь приоткрыта. На ближнем перевязочном столе остались бинты, открытые шкафы почти пусты. Похоже, внутри кто-то есть: из-за ширмы слышится движение.");
                    GameIO.illustratedCard("", "", "clinic_art:clinic_procedure");
                    GameIO.action(1,"Проверить стол возле входа",entranceSupplies?"Уже осмотрен":"5 минут · низкий риск · бинт",!entranceSupplies);
                    GameIO.action(2,"Осмотреть дальние шкафы",procedureCleared?"Дальний шкаф доступен":"15 минут · возможна встреча",!procedureCleared);
                    if(procedureCleared){GameIO.action(3,"Взять медицинские материалы",procedureSupplies?"Уже взяты":"Бинт",!procedureSupplies);GameIO.action(4,"Открыть металлическую коробку",keyTaken?"Ключ уже взят":"Служебный ключ",!keyTaken);}
                    GameIO.action(0,"Покинуть кабинет");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!entranceSupplies&&time(p,w,5,1)&&take(p,Shop.createItem(1))){entranceSupplies=true;mark(place,w,"Процедурный кабинет");}
                    if(a==2&&!procedureCleared){
                        if(!procedureRoll){procedureRoll=true;procedureThreat=random.nextInt(100)<45;GameIO.checkpoint();}
                        if(!time(p,w,15,3))continue;
                        if(procedureThreat&&!encounter(p,scanner,random,"Процедурный кабинет поликлиники"))return;
                        procedureCleared=true;mark(place,w,"Процедурный кабинет");
                    }
                    if(a==3&&procedureCleared&&!procedureSupplies&&take(p,Shop.createItem(1))){procedureSupplies=true;GameIO.checkpoint();}
                    if(a==4&&procedureCleared&&!keyTaken&&take(p,new Tools(CABINET_KEY,"Ключ от аварийного медицинского шкафа №4.",1,0))){keyTaken=true;GameIO.checkpoint();}
                } else if(section==3){
                    GameIO.menu("Служебный шкаф №4",cabinetOpen?"Запертый аварийный запас уцелел, пока открытые шкафы опустошали. На внутренней стороне дверцы прикреплена ведомость.":"За стойкой стоит запертый шкаф с надписью: «Аварийный медицинский запас. Выдача по разрешению старшей медсестры».");
                    GameIO.illustratedCard("", "", "clinic_art:clinic_cabinet");
                    if(!cabinetOpen){GameIO.action(1,"Открыть служебным ключом","5 минут · тихо",hasKey(p));GameIO.action(2,"Вскрыть ломом","10 минут · шум",place.hasCrowbar(p));}
                    if(cabinetOpen){GameIO.action(3,"Взять первый бинт",cabinetBandage1?"Уже взят":"Бинт",!cabinetBandage1);GameIO.action(4,"Взять второй бинт",cabinetBandage2?"Уже взят":"Бинт",!cabinetBandage2);GameIO.action(5,"Взять аптечку",cabinetKit?"Уже взята":"Аптечка",!cabinetKit);GameIO.action(6,"Прочитать ведомость",cabinetManifest?"Уже прочитана":"Партия М-24",!cabinetManifest);}
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(!cabinetOpen&&((a==1&&hasKey(p))||(a==2&&place.hasCrowbar(p)))&&time(p,w,a==1?5:10,2)){cabinetOpen=true;GameIO.sound(GameSound.METAL_DOOR);mark(place,w,"Служебный шкаф");if(a==2&&!BreakIn.attack(p,scanner,random))return;}
                    if(a==3&&cabinetOpen&&!cabinetBandage1&&take(p,Shop.createItem(1))){cabinetBandage1=true;GameIO.checkpoint();}
                    if(a==4&&cabinetOpen&&!cabinetBandage2&&take(p,Shop.createItem(1))){cabinetBandage2=true;GameIO.checkpoint();}
                    if(a==5&&cabinetOpen&&!cabinetKit&&take(p,Shop.createItem(2))){cabinetKit=true;GameIO.checkpoint();}
                    if(a==6&&cabinetOpen&&!cabinetManifest){cabinetManifest=true;note(w,"Партия М-24","Основная партия медикаментов передана на центральный аптечный склад. Для повторного получения требуется накладная. Номер партии: М-24.");}
                } else {
                    GameIO.menu("Подвальный архив",archiveCleared?"Сырая лестница, подтопленный пол и опрокинутые шкафы. В дальнем коридоре тихо.":"Сырая лестница, подтопленный пол и опрокинутые шкафы. Из соседнего коридора иногда слышны шаги.");
                    GameIO.illustratedCard("", "", "clinic_art:clinic_archive");
                    GameIO.action(1,"Осмотреть ближний шкаф",nearArchive?"Уже осмотрен":"5 минут · бытовые материалы",!nearArchive);
                    GameIO.action(2,"Пройти в дальнюю часть",archiveCleared?"Доступна":"10 минут · возможна встреча",!archiveCleared);
                    if(archiveCleared)GameIO.action(3,"Изучить списки пациентов",patientLists?"Уже изучены":"5 минут",!patientLists);
                    GameIO.action(0,"Назад");int a=SurvivalSystems.choice(scanner);if(a==0)break;
                    if(a==1&&!nearArchive&&time(p,w,5,1)&&take(p,random.nextInt(5)==0?Shop.createItem(43):Shop.createItem(48))){nearArchive=true;mark(place,w,"Подвал");}
                    if(a==2&&!archiveCleared){if(!archiveRoll){archiveRoll=true;archiveThreat=random.nextInt(100)<45;GameIO.checkpoint();}if(!time(p,w,10,2))continue;if(archiveThreat&&!encounter(p,scanner,random,"Архив поликлиники"))return;archiveCleared=true;mark(place,w,"Подвал");}
                    if(a==3&&archiveCleared&&!patientLists&&time(p,w,5,1)){patientLists=true;note(w,"Списки пациентов поликлиники","Три категории: передвигаются самостоятельно, нуждаются в сопровождении, нуждаются в медицинском транспорте. Рядом письмо: «Просим подтвердить время прибытия транспорта. В учреждении остаются пациенты, не способные самостоятельно добраться до пункта сбора. Предыдущий запрос остался без ответа». Дата и адрес сохранились частично. На обратной стороне карандашом: «У нас остались люди, которые не смогут дойти сами. Пожалуйста, ответьте».");connections(w);}
                }
            }
        }
    }
}
