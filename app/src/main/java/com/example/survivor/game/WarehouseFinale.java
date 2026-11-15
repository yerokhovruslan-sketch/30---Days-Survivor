package com.example.survivor.game;

import java.io.Serializable;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The warehouse's solo route and the missed-flight outcome. */
final class WarehouseFinale implements Serializable {
    private static final long serialVersionUID=1L;
    boolean metBelov,departureAnnounced,missedFlight,evacuated;
    boolean registryFound,nightTalk,perimeterEnding,nightAssault,assaultWon;
    private static final String NIGHT_BATTLE="Ночной штурм военного склада";

    void announceDeparture(WorldStories w,Fighter p) {
        if(w.day()<31 || evacuated || departureAnnounced || p.getUpgrades().radio<=0)return;
        departureAnnounced=true;
        String message="«...служебный борт покинул район. Пункт сбора закрыт...» Эфир обрывается.";
        w.note("Последний рейс",message);
        GameIO.out().println(message);
        GameIO.checkpoint();
    }

    boolean visit(Fighter p,WorldStories w,Scanner scanner) {
        if(perimeterEnding){perimeterEnding(scanner);return true;}
        if(evacuated){ending(scanner);return true;}
        announceDeparture(w,p);
        if(w.day()>=31){
            if(!missedFlight){
                missedFlight=true;
                w.note("Тридцать первый день","Последний военный борт покинул район. Приём гражданских прекращён. Дальнейший путь предстоит искать самостоятельно.");
                GameIO.checkpoint();
                ResidentialStories.pause("Пустая площадка","На посадочной площадке пусто. Технические помещения открыты, на земле следы поспешной эвакуации. В диспетчерской осталась запись: «Вылет выполнен. Гражданский приём прекращён».\n\nДжон: «Тридцать дней... И всё ради этого».\n\nОбещанная эвакуация так и не состоялась. Последний военный транспорт покинул город. Джон остался. Завтра ему снова придётся искать еду, воду и безопасное место для ночлега.",scanner);
            } else ResidentialStories.pause("Тридцать первый день","На посадочной площадке по-прежнему пусто. Последний военный борт ушёл.",scanner);
            return false;
        }
        if(nightAssault)return nightAttack(p,w,scanner);
        if(w.isNight()){
            GameIO.storyMenu("Военный склад ночью","Восточный служебный проход открыт. Из-за ограждения доносятся голоса караула.");
            GameIO.action(1,"Проникнуть через технический проход","Не приближаться к ограждению после наступления темноты — предупреждал «Маяк».");
            GameIO.action(2,"Подойти к воротам");GameIO.action(0,"Вернуться домой");
            int approach=SurvivalSystems.choice(scanner);if(approach==0)return false;
            if(approach==1)return nightVisit(p,w,scanner);
        }
        if(!metBelov){
            metBelov=true;GameIO.checkpoint();
            ResidentialStories.pause("У ворот склада","У склада стоят военная техника и контейнеры. Несколько вооружённых людей готовят вертолёт. Джона останавливает капитан.\n\nБелов: «Стоять. Оружие опустить. Кто такой?»\n\nДжон: «Гражданский. Пришёл на эвакуацию. По радио передавали, позывной „Маяк“. Сказали прибыть на военный склад».\n\nБелов смотрит на солдата.\n\nБелов: «Блядь... Опять этот передатчик».\n\nДжон: «Что значит „опять“?»\n\nБелов: «Ты не первый. Часть ушла. Часть осталась за периметром. Остальные... По-разному. Колонну отменили».",scanner);
        }
        while(p.isAlive() && w.day()<31 && !evacuated){
            GameIO.storyMenu("Последний рейс","Белов готов ответить на вопросы. Вертолёт ещё готовят к вылету.");
            GameIO.action(1,"Почему работал «Маяк»?");
            GameIO.action(2,"Зачем продолжали собирать людей?");
            if(w.routeAfterArchive!=null && w.routeAfterArchive.cargoFound)GameIO.action(3,"Показать документы о медикаментах");
            if(w.hospital().registry||w.hospital().patientNotebook||w.hospital().route17)GameIO.action(4,"Спросить о пациентах больницы");
            if(w.schoolyard().busListsRead())GameIO.action(5,"Спросить о школьных группах");
            GameIO.action(6,"Что происходит сейчас?");
            GameIO.action(7,"Договориться об эвакуации");
            GameIO.action(0,"Уйти и вернуться позже");
            int c=SurvivalSystems.choice(scanner);if(c==0)return false;
            if(c==1)ResidentialStories.pause("Маяк","Белов: «Вещание запустили по старому порядку сбора. Приказ об отмене колонны пришёл, а окончательно закрыть пункт вовремя не распорядились. Другой транспорт пытались найти, но никто не гарантировал, что он придёт».",scanner);
            if(c==2)ResidentialStories.pause("Пункт сбора","Белов: «Одни надеялись возобновить эвакуацию. Другие требовали закрыть пункт сразу. Решение так и не принимали, пока связь с командованием окончательно не развалилась».",scanner);
            if(c==3&&w.routeAfterArchive!=null&&w.routeAfterArchive.cargoFound)ResidentialStories.pause("Медицинские поставки","Джон показывает накладные терминала.\n\nБелов: «Я не знаю про каждый контейнер. Подтверждения терялись, грузы задерживали, связь с получателями пропала. От этого лекарства до людей не дошли — знаю».",scanner);
            if(c==4&&(w.hospital().registry||w.hospital().patientNotebook||w.hospital().route17))ResidentialStories.pause("Пациенты","Белов: «Некоторых направляли в другие пункты. Не знаю, что стало с каждым пациентом. Полных списков у меня нет».",scanner);
            if(c==5&&w.schoolyard().busListsRead())ResidentialStories.pause("Школьные группы","Белов: «Я не могу подтвердить, куда попали эти автобусы. Часть групп перенаправляли, по другим сведения потерялись».",scanner);
            if(c==6)ResidentialStories.pause("Последний вылет","Белов: «Через несколько часов мы уходим. Последний вылет. Могу взять на борт двух гражданских».\n\nДжон: «Куда летите?»\n\nБелов: «На запасную площадку. Пилот не получил подтверждения, что она ещё работает. Гарантий не дам».",scanner);
            if(c==7){
                GameIO.storyMenu("Последнее решение","Белов согласен принять Джона на борт. Состояние запасной площадки неизвестно.");
                GameIO.action(1,"Улететь");GameIO.action(0,"Пока остаться");
                if(SurvivalSystems.choice(scanner)==1){evacuated=true;w.note("Цена спасения","Джон покинул город последним военным рейсом. Состояние запасной площадки неизвестно.");GameIO.checkpoint();ending(scanner);return true;}
            }
        }
        return false;
    }
    private boolean nightVisit(Fighter p,WorldStories w,Scanner scanner){
        if(!registryFound){
            registryFound=true;
            String names=(w.schoolyard().busListsRead()?" Среди списков Джон узнаёт обозначения школьных групп А и Б; судьба каждой группы по-прежнему неясна.":"")
                    +(w.hospital().registry||w.hospital().patientNotebook?" В журнале упомянуты пациенты городской больницы; напротив некоторых имён подтверждения нет.":"");
            w.note("Узел регистрации гражданских","После отмены колонны склад продолжал распределять гражданских. Часть направили в убежища и некоторые прибыли; с другими связь потеряна. Отметки: «Принят», «Переведён», «Покинул пункт», «Нет подтверждения»."+names);
            w.note("Убежище №6","«Получен сигнал от гражданского убежища №6. Подтверждено наличие людей. Запрос на эвакуацию отклонён. Координаты переданы в резервный журнал». Джон сохранил координаты; дальнейший маршрут ещё предстоит найти.");
            GameIO.checkpoint();
            ResidentialStories.pause("Узел регистрации","За технической дверью стоят столы, архивные шкафы и пустые медицинские койки. В журналах — гражданские, зарегистрированные после отмены колонны: «Принят», «Переведён», «Покинул пункт», «Нет подтверждения». Часть людей отправили в другие убежища; некоторые добрались, другие пропали со связи."+names+"\n\nВ резервном журнале: «Получен сигнал от гражданского убежища №6. Подтверждено наличие людей. Запрос на эвакуацию отклонён. Координаты переданы в резервный журнал». Джон переписывает координаты.",scanner);
        }
        if(!nightTalk){nightTalk=true;metBelov=true;GameIO.checkpoint();ResidentialStories.pause("После отбоя","Белов: «Предупреждали же не приходить ночью».\n\nДжон: «Вы людей сюда звали, когда уже знали, что забирать их нечем».\n\nБелов: «Мы не знали, что транспорт окончательно не вернётся».\n\nДжон: «А когда узнали?»\n\nБелов молчит.\n\nДжон: «Вот именно».\n\nБелов: «Думаешь, я не понимаю, что здесь произошло? Ты сейчас уедешь, а я останусь со списками людей, которых мы не смогли вывезти».\n\nДжон: «Тогда зачем продолжали передачу?»\n\nБелов: «Потому что каждый раз, когда мы собирались закрыть пункт, кто-нибудь снова выходил на связь и просил помощи». Система продолжала обещать помощь, которую уже не могла гарантировать.",scanner);}
        while(p.isAlive()){
            GameIO.storyMenu("После отбоя","Документы сохранены. Белов и охрана рядом; до вылета остаётся время.");
            GameIO.action(1,"Забрать документы и уйти","Координаты убежища №6 останутся в журнале");
            GameIO.action(2,"Потребовать открыть пункт и сказать людям правду");
            GameIO.action(3,"Попытаться захватить вертолёт","Очень опасно: вооружённая охрана");
            int c=SurvivalSystems.choice(scanner);
            if(c==1)return false;
            if(c==2){
                GameIO.storyMenu("Открыть ворота","Джон требует остановить обманчивое вещание и дать ожидающим людям возможность уйти.");
                GameIO.action(1,"Настоять на открытии ворот");GameIO.action(0,"Вернуться к выбору");
                if(SurvivalSystems.choice(scanner)!=1)continue;
                perimeterEnding=true;w.note("Разрыв периметра","Передатчик «Маяка» замолчал. Людям сообщили правду, ворота открыли. Судьба покинувших пункт неизвестна.");GameIO.checkpoint();perimeterEnding(scanner);return true;
            }
            if(c==3){nightAssault=true;GameIO.checkpoint();return nightAttack(p,w,scanner);}
        }
        return false;
    }
    private boolean nightAttack(Fighter p,WorldStories w,Scanner scanner){
        if(!assaultWon){
            Random random=new Random();List<CombatEnemy> guards=new ArrayList<>();
            for(int i=1;i<=4;i++)guards.add(new HostileSurvivor(i,true,random,true));
            boolean won=TacticalBattle.story(p,scanner,random,guards,NIGHT_BATTLE,List.of(),false,false,true);
            if(!won){if(!p.isAlive())ResidentialStories.pause("Последний выстрел","Джон дошёл до места, где обещали спасение. Но последним препятствием оказались не заражённые. Люди.",scanner);return false;}
            assaultWon=true;GameIO.checkpoint();
        }
        GameIO.storyMenu("Пилот Орлов","Охрана побеждена. Пилот не участвовал в бою и всё ещё готовит вертолёт.");
        GameIO.card("Разговор","Орлов: «Вы хоть понимаете, что натворили?!»\n\nДжон: «Я хочу выбраться».\n\nОрлов: «Запасная площадка не отвечает. Полёт рискованный — гарантий нет». ");
        GameIO.action(1,"Убедить Орлова взлететь");GameIO.action(0,"Уйти");
        if(SurvivalSystems.choice(scanner)!=1)return false;
        evacuated=true;w.note("Ночной вылет","После боя с охраной пилот Орлов согласился взлететь. Состояние запасной площадки неизвестно.");GameIO.checkpoint();ending(scanner);return true;
    }
    private void perimeterEnding(Scanner scanner){ResidentialStories.pause("Разрыв периметра","Передатчик «Маяка» замолчал.\n\nВпервые за долгое время люди услышали не обещание эвакуации, а настоящее сообщение о том, что происходит на складе.\n\nВорота открылись. Никто не обещал им спасения. Но больше никто не приказывал стоять у забора и ждать.",scanner);}
    private void ending(Scanner scanner){ResidentialStories.pause("Цена спасения","Джон покинул город. Он не знает, куда доставит его последний военный рейс. Земля остаётся далеко внизу.",scanner);}
}
