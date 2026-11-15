package com.example.survivor.game;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Decisions before the first radio transmission; regular progress is not overwritten here. */
final class OpeningChapter {
 private static void screen(String id,String title,String text){GameIO.menu(title,text);GameIO.scene("opening_"+id);}
 static boolean play(Fighter p,Locations locations,Scanner in,Random random){
  WorldStories w=locations.getStories();GameIO.preparePrologue(p,locations);
  screen("room","Комната","Джон приходит в себя. Рядом рюкзак и металлическая труба. Дверь ходит ходуном: времени хватит только на одно.");
  GameIO.action(1,"Схватить рюкзак","Вода и галеты");GameIO.action(2,"Взять металлическую трубу","Оружие ближнего боя");
  int first=SurvivalSystems.choice(in);boolean backpack=first==1;
  if(backpack){p.getInventory().remove(p.getEquippedWeapon());Weapons club=new Weapons("Дубинка", "Найденное простое оружие",1,10,WeaponStyle.CLUB);p.addItem(club);p.equipWeapon(club);p.addItem(new Tools("Рюкзак","Рюкзак, взятый во время побега",1,0));p.addItem(ResidentialStories.waterBottle());p.addItem(Shop.createItem(16));}
  screen("door","Они прорвались","Дверь вылетает из петель. В комнату входят трое заражённых. Решать нужно сейчас.");
  GameIO.action(1,"Вступить в бой",p.difficulty()==3?"Опасный бой: три заражённых · смерть завершит попытку":"Опасный бой: три заражённых · можно уйти и продолжить игру");GameIO.action(0,"Уйти","Оставить вещи в комнате");
  int door=SurvivalSystems.choice(in);
  if(door==1){
   boolean won=TacticalBattle.story(p,in,random,List.of(new Zombie(1,ZombieType.NORMAL),new Zombie(2,ZombieType.NORMAL),new Zombie(3,ZombieType.NORMAL)),"Начало: комната",List.of(),false,false,false);
   w.clearPendingBattle(w.currentPlace());
   if(!p.isAlive())return false;
   if(won){boolean food=false,bag=backpack,pistol=false;
    while(true){screen("room","После боя","Заражённые повержены. На полу остались вещи. Забери нужное и уходи.");
     if(!food)GameIO.action(1,"Взять консервы");if(!bag)GameIO.action(2,"Взять рюкзак","Внутри вода и галеты");if(!pistol)GameIO.action(3,"Взять пистолет","Прочность 50% · без патронов");GameIO.action(0,"Уйти");
     int c=SurvivalSystems.choice(in);if(c==0)break;
     if(c==1&&!food){food=p.addItem(Shop.createItem(17));}
     if(c==2&&!bag){bag=true;p.addItem(new Tools("Рюкзак","Рюкзак, взятый во время побега",1,0));p.addItem(ResidentialStories.waterBottle());p.addItem(Shop.createItem(16));}
     if(c==3&&!pistol){Weapons gun=new Weapons("Пистолет","Пустой пистолет, найденный в комнате",4,300,WeaponStyle.PISTOL);gun.wear(50);pistol=p.addItem(gun);}
    }
   }
  }
  screen("wounded","Лестница","На лестнице сидит раненый выживший. Рядом раскрытая аптечка. Снизу слышны шаги.\n\nВыживший: «Подожди... Помоги подняться. Я сам не дойду».");
  GameIO.action(1,"Помочь и провести его","Потратить силы");GameIO.action(2,"Оставить ему аптечку","Показать путь к выходу");GameIO.action(3,"Уйти одному");GameIO.action(0,"Не успеть ответить");
  int response=SurvivalSystems.choice(in);
  if(response==1){w.residential().changeHumanity(2);p.spendAvailableStamina(15);p.addItem(Shop.createItem(1));screen("help","К выходу","Джон помогает мужчине спуститься к боковому выходу. Тот отдаёт ему бинт: «Ты мог меня там оставить». Дальнейшая судьба выжившего неизвестна.");}
  else if(response==2){w.residential().changeHumanity(1);screen("text","На лестнице","Джон пододвигает аптечку к раненому и показывает путь к выходу. Успел ли тот выбраться, неизвестно.");}
  else{if(response==3)w.residential().changeHumanity(-1);screen("text","На лестнице",response==0?"Шаги становятся ближе. Джон отступает к выходу, не успев принять решение.":"Джон уходит. За спиной остаётся просьба о помощи. Судьба раненого неизвестна.");}
  w.openingChoice=response;w.openingChoiceRecorded=true;w.note("Путь к убежищу",response==1?"Джон помог раненому человеку выбраться из подъезда. Его дальнейшая судьба неизвестна.":response==2?"Джон оставил раненому аптечку и показал выход. Его дальнейшая судьба неизвестна.":response==3?"Джон оставил раненого на лестнице. Его дальнейшая судьба неизвестна.":"Джон ушёл, не успев принять решение о раненом. Его дальнейшая судьба неизвестна.");
  GameIO.action(1,"Продолжить");SurvivalSystems.choice(in);
  screen("street","На улице","Сирена обрывается. Между разбитыми машинами Джон замечает дверь убежища и бежит к ней.");GameIO.action(1,"Добраться до убежища");SurvivalSystems.choice(in);
  screen("shelter_door","У двери убежища","Джон добегает до входа. Дверь закрыта, за спиной слышны шаги. Он дёргает ручку — безуспешно. Наконец замок поддаётся, и Джон проскальзывает внутрь.");GameIO.action(1,"Закрыть дверь");SurvivalSystems.choice(in);
  screen("cry","","За дверью слышны детские крики.");GameIO.action(1,"Продолжить");SurvivalSystems.choice(in);
  screen("shelter","Убежище","Дверь захлопнулась. Джон замирает, прислушиваясь к звукам снаружи.\n\nНа столе он замечает старый приёмник и поворачивает ручку. Сквозь помехи пробивается голос...");GameIO.action(1,"Включить приёмник");SurvivalSystems.choice(in);
  GameIO.menu("Первое сообщение","Сквозь помехи слышно сообщение об эвакуации через тридцать дней. Название точки сбора тонет в шуме.");GameIO.scene("opening_radio");GameIO.action(1,"Продолжить");SurvivalSystems.choice(in);
  String supplies=p.getInventory().isEmpty()?"ничего":p.getInventory().stream().map(ItemInfo::getName).collect(java.util.stream.Collectors.joining(", "));
  screen("text","Ты добрался до убежища","Припасы и снаряжение: "+supplies+".\n\nВыносливость: "+p.getStamina()+"/100.\n"+p.injurySummary()+"\n\n"+(response==1?"Ты помог раненому выжившему выбраться из подъезда.":response==2?"Ты оставил раненому аптечку и показал выход.":response==3?"Ты оставил раненого на лестнице.":"Ты ушёл, не успев принять решение о раненом."));GameIO.action(1,"Продолжить");SurvivalSystems.choice(in);
  GameIO.finishPrologue();GameIO.checkpoint();return true;
 }
}
