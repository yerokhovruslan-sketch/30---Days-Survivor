package com.example.survivor.game;

import java.util.Scanner;

final class Difficulty {
    static final String WARNING="После начала игры изменить сложность нельзя. Для другого уровня потребуется новая игра.";
    static String name(int level){return level==1?"История":level==3?"Последний шанс":"Выживание";}
    static int choose(Scanner scanner) {
        while(true) {
            GameIO.menu("Уровень сложности",WARNING+"\nВремя идёт только от действий. Чтение и меню не тратят игровые минуты.");
            GameIO.action(1,"История","Если хочешь разобраться в происходящем и сильно не волноваться. Входящий урон ниже, потребности растут медленнее, добычу находить легче.");
            GameIO.action(2,"Выживание","Обычная жизнь после конца обычной жизни. Нужно готовиться, считать припасы и иногда признавать, что ты не герой… не сегодня и не завтра. (Для крепких мужчин)");
            GameIO.action(3,"Последний шанс","Для тех храбрых, кто готов смотреть смерти в глаза не содрогаясь. Удары больнее, запасы уходят быстрее, полезных находок меньше. 1 жизнь — и ты не Джон Уик.\nСохранение, автосохранение и загрузка недоступны. Выход завершает прохождение. Гордость за себя загружается отдельно. (Серьёзно? Ты псих?)");
            int selected=SurvivalSystems.choice(scanner);if(selected<1 || selected>3)continue;
            GameIO.menu("Начать: "+name(selected),WARNING+(selected==3?"\nОдна жизнь. Сохранений и загрузки нет. После выхода начать придётся заново.":""));GameIO.action(1,"Начать игру");GameIO.action(0,"Выбрать другой уровень");
            if(SurvivalSystems.choice(scanner)==1)return selected;
        }
    }
    static int needs(int amount,int level){return amount<=0?0:level==1?(amount+1)/2:level==3?(amount*3+1)/2:amount;}
    static int damage(int amount,int level){return amount<=0?0:Math.max(1,level==1?(amount*65+99)/100:level==3?(amount*125+99)/100:amount);}
    static int loot(int level){return level==1?15:level==3?-10:0;}
}
