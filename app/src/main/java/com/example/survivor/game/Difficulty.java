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
            GameIO.action(3,"Последний шанс","Для тех храбрых, кто готов смотреть смерти в глаза не содрогаясь. Удары больнее, запасы уходят быстрее, полезных находок меньше. Одна жизнь. После вступления игра сохраняет попытку в контрольных точках: после выхода можно продолжить с последней точки, но после смерти попытка закончится. Ручная загрузка и отдельные сохранения недоступны. Гордость за себя загружается отдельно. (Серьёзно? Ты псих?)");
            GameIO.action(0,"Назад в главное меню");
            int selected=SurvivalSystems.choice(scanner);if(selected==0)return 0;if(selected<1 || selected>3)continue;
            return selected;
        }
    }
    static int needsPercent(int level){return level==1?65:level==3?125:110;}
    static int encounterChance(int base,int level){return Math.min(100,(base*(level==1?80:level==3?115:105)+50)/100);}
    static int damage(int amount,int level){return amount<=0?0:Math.max(1,(amount*(level==1?65:level==3?120:110)+99)/100);}
    static int loot(int level){return level==1?15:level==3?-10:-5;}
}
