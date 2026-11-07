package com.example.survivor.game;

import java.io.Serializable;
import java.util.Scanner;

/** Independent investigations following the police archive. */
final class RouteAfterArchive implements Serializable {
    private static final long serialVersionUID=1L;
    private static final String KIT="Аптечка экипажа №17";
    boolean gateSeen, guardJournal, roster, report, metOlga, kitRequested, kitTaken, kitReturned, routeSheet;

    void open(Fighter p,WorldStories w,int place,Scanner scanner) {
        if(place==41)industrial(w,scanner);
        else if(place==13)station(p,w,scanner);
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
    private void station(Fighter p,WorldStories w,Scanner scanner){
        while(true){
            GameIO.storyMenu("Станция скорой помощи","В гараже стоят несколько машин. Одно парковочное место пустует. В диспетчерской и комнате отдыха могли остаться следы экипажа №17.");
            GameIO.action(1,"Проверить список экипажей",roster?"Уже прочитан":"Диспетчерская");
            GameIO.action(2,"Изучить отчёт диспетчера",report?"Уже прочитан":"Документы в шкафу");
            GameIO.action(3,metOlga?"Поговорить с Ольгой":"Осмотреть комнату отдыха", "Следы человека");
            if(kitRequested&&!kitReturned)GameIO.action(4,"Проверить служебный автомобиль",kitTaken?"Аптечка уже взята":"Аптечка экипажа №17");
            GameIO.action(0,"Уйти");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1){roster=true;String t="Возле номера 17 сделана отметка: «Возвращён. Без пассажиров. Экипаж не опрошен».";w.note("Список экипажей",t);GameIO.checkpoint();ResidentialStories.pause("Список экипажей",t,scanner);}
            if(c==2){report=true;String t="Отчёт с пропущенными строками подтверждает: машина №17 добралась до пункта сбора и вернулась. На следующей странице: «Приём гражданских прекращён. У въезда ожидали не менее двадцати человек. Распоряжений по размещению не поступало».";w.note("Отчёт экипажа №17",t);GameIO.checkpoint();ResidentialStories.pause("Отчёт диспетчера",t,scanner);}
            if(c==3)olga(p,w,scanner);
            if(c==4&&kitRequested&&!kitReturned){
                if(p.getInventory().stream().anyMatch(i->i.getName().equals(KIT))){ResidentialStories.pause("Служебный автомобиль","Аптечка уже взята. Верните её Ольге.",scanner);continue;}
                if(!p.addItem(new Tools(KIT,"Аптечка, которую Ольга оставила в служебном автомобиле.",1,0))){ResidentialStories.pause("Служебный автомобиль","Аптечка найдена, но в рюкзаке нет места. Можно вернуться позже.",scanner);continue;}
                kitTaken=true;w.note("Аптечка экипажа №17","Аптечка найдена в служебном автомобиле. Её можно вернуть Ольге.");GameIO.checkpoint();ResidentialStories.pause("Служебный автомобиль","Джон находит аптечку экипажа №17 среди вещей в автомобиле.",scanner);
            }
        }
    }
    private void olga(Fighter p,WorldStories w,Scanner scanner){
        if(!metOlga){
            metOlga=true;GameIO.checkpoint();
            ResidentialStories.pause("Ольга","В комнате отдыха от заражённых скрывается женщина. Она насторожена, но не нападает. Это Ольга, бывший фельдшер экипажа №17.\n\nДжон: Ты работала на семнадцатой машине?\nОльга: Работала. А что?\nДжон: Мне нужно узнать, что происходит на военном складе.\nОльга: Охуенный вопрос. Я его военным тоже задавала. Ответа, правда, не получила.\nДжон: Вы туда ездили?\nОльга: Ездили. Привезли двоих. Одного еле живого. Нас даже за ворота нормально не пустили.\nДжон: А люди?\nОльга: Какие?\nДжон: Те, кого должны были эвакуировать.\nОльга: Стояли у забора. С сумками, детьми, колясками. Один мужик всё спрашивал, когда придут автобусы.\nДжон: И что?\nОльга: А ничего. Автобусов не было.\nДжон: Значит, эвакуации нет?\nОльга: Я сказала, что автобусов не было. Не надо за меня додумывать.",scanner);
            w.note("Ольга, фельдшер экипажа №17","Ольга видела у склада ожидающих гражданских. Автобусов тогда не было; дальнейшая судьба людей ей неизвестна.");
        }
        while(true){
            GameIO.storyMenu("Ольга",kitReturned?"Ольга забрала аптечку и готова показать копию маршрутного листа.":"Ольга просит принести аптечку экипажа №17 из служебного автомобиля на территории станции.");
            if(!kitRequested){GameIO.action(1,"Согласиться найти аптечку");GameIO.action(0,"Вернуться позже");if(SurvivalSystems.choice(scanner)==1){kitRequested=true;w.note("Аптечка Ольги","Найти аптечку экипажа №17 в служебном автомобиле станции скорой помощи.");GameIO.checkpoint();}return;}
            if(!kitReturned){boolean has=p.getInventory().stream().anyMatch(i->i.getName().equals(KIT));GameIO.action(1,"Вернуть аптечку",has?"Аптечка в рюкзаке":"Нужна аптечка экипажа",has);GameIO.action(0,"Вернуться позже");if(SurvivalSystems.choice(scanner)==1&&has){p.getInventory().removeIf(i->i.getName().equals(KIT));kitReturned=true;w.note("Помощь Ольге","Джон вернул аптечку экипажа. Ольга согласилась показать маршрутный лист.");GameIO.checkpoint();}else return;}
            else {GameIO.action(1,"Изучить маршрутный лист",routeSheet?"Уже изучен":"Копия Ольги");GameIO.action(0,"Уйти");if(SurvivalSystems.choice(scanner)!=1)return;
                routeSheet=true;String t="В копии маршрутного листа указано: 17 — медицинский маршрут, Б — служебный сектор склада, 04 — пропускной пункт медицинского транспорта. Комбинация помогает найти въезд и проверить документы; сама ворота не открывает.\n\nОльга: «Там какой-то офицер орал на электриков. Вроде хотели отключить восточные ворота, но они сказали, что без питания даже аварийную систему не запустят».";w.note("Маршрутный лист №17",t);w.note("Восточный технический въезд","Проверить служебный сектор Б и пропускной пункт 04. Восточным воротам требуется питание.");GameIO.checkpoint();ResidentialStories.pause("Маршрутный лист",t,scanner);}
        }
    }
}
