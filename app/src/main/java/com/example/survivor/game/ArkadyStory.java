package com.example.survivor.game;
import java.io.Serializable;
import java.util.*;
/** One persistent encounter at the parked cars, independent of the ordinary car loot. */
final class ArkadyStory implements Serializable {
 private static final long serialVersionUID=1L;
 boolean spawned; int stage,spawnPlace,lastRoadVisit; ArkadyEnemy enemy;
 // 0 first contact, 1 history, 2 water decision, 3 threat, 4 battle, 5 helped, 6 refused, 7 dead, 8 fled
 void trySpawn(Random r,int place){if(!spawned && r.nextInt(100)<40){spawned=true;spawnPlace=place;GameIO.checkpoint();}}
 void tryRoad(Random r,WorldStories w,boolean unsearched){int visit=w.visitCount(3);if(!spawned && unsearched && visit>0 && visit!=lastRoadVisit){lastRoadVisit=visit;trySpawn(r,3);}}
 boolean atPlace(int place){return visible() && spawnPlace==place;}
 boolean visible(){return spawned && stage!=7 && stage!=8;}
 boolean finished(){return stage==5||stage==6;}
 static ItemInfo water(Fighter p){for(ItemInfo i:p.getInventory()){String n=i.getName().toLowerCase(Locale.ROOT);if(i instanceof Food && (n.contains("воды")||n.contains("водой")) && n.contains("бутыл"))return i;}return null;}
 boolean interact(Fighter p,WorldStories w,Scanner sc,Random random){
  if(!visible())return true;
  while(p.isAlive() && GameIO.hasInput(sc)){
   if(stage==5&&!rewardTaken)reward(p,sc);
   if(finished()){ResidentialStories.pause("Машина Аркадия","Аркадий остаётся в машине. Пока он внутри, безопасно обыскать её не получится.",sc);return true;}
   if(stage==0){GameIO.storyMenu("Вооружённый незнакомец","Джон подходит к автомобилю. Внутри кто-то шевелится. Раздаётся металлический щелчок.\n\nАркадий: «Стоять, сука. Ещё шаг — и я стреляю».\n\nДжон видит мужчину с револьвером. Руки у него трясутся.\n\nВыберите ответ Джона.");
    GameIO.action(1,"Я не собираюсь тебя трогать.");GameIO.action(2,"Убери револьвер, пока не наделал глупостей.");GameIO.action(3,"Ты ранен? Тебе нужна помощь?");GameIO.action(0,"Отойти");int a=SurvivalSystems.choice(sc);if(a==0)return true;if(a<1||a>3)continue;stage=1;GameIO.checkpoint();}
   if(stage==1){GameIO.storyMenu("Что случилось?", "Аркадий: «Мне помощь нужна была вчера. А теперь отойди от машины».\n\nДжон: «Что случилось?»\n\nАркадий: «А ты не видишь? Всё случилось. Сначала дорогу перекрыли. Потом эти твари набежали. А потом я держал дверь, пока она кричала».\n\nДжон: «Кто?»\n\nАркадий: «Жена, блядь. Кто ещё?»\n\nОн смотрит на пассажирское сиденье. Там лежит женская куртка.\n\nАркадий: «Она говорила, что надо остаться дома. А я всё твердил: поехали, поехали, нас эвакуируют. Ебаный герой нашёлся».");GameIO.action(1,"Продолжить");GameIO.action(0,"Отойти");int a=SurvivalSystems.choice(sc);if(a==0)return true;if(a!=1)continue;stage=2;GameIO.checkpoint();}
   if(stage==2){GameIO.storyMenu("У машины", "Аркадий замечает рюкзак Джона.\n\nАркадий: «Слушай... У тебя вода есть?»\n\nДжон: «Есть немного».\n\nАркадий: «У меня всё закончилось. Уже второй день».\n\nОн опускает револьвер, но не убирает его.");GameIO.action(1,"Дать бутылку воды","Нужна питьевая вода в рюкзаке",water(p)!=null);GameIO.action(2,"Отказаться и уйти");GameIO.action(3,"Потребовать бросить оружие");GameIO.action(0,"Отойти и вернуться позже");int a=SurvivalSystems.choice(sc);if(a==0)return true;
    if(a==1 && water(p)!=null){GameIO.storyMenu("Отдать воду?","Отдать Аркадию 1 бутылку воды?");GameIO.action(1,"Отдать");GameIO.action(0,"Передумать");if(SurvivalSystems.choice(sc)!=1)continue;ItemInfo item=water(p);if(item==null)continue;p.getInventory().remove(item);stage=5;w.note("Аркадий у машины","Джон отдал Аркадию воду. Мужчина жив и остаётся в автомобиле.");GameIO.checkpoint();
      ResidentialStories.pause("Вода для Аркадия", "Аркадий: «Ты серьёзно?»\n\nДжон: «Пей».\n\nАркадий делает несколько глотков.\n\nАркадий: «Я тебя чуть не пристрелил».\n\nДжон: «Заметил».\n\nАркадий: «И ты мне воду дал?»\n\nДжон: «У всех бывают хреновые дни».\n\nАркадий: «У меня, кажется, они теперь до конца жизни».\n\nДжон уже собирается уходить, но Аркадий его окликает.\n\nАркадий: «Стой».\n\nОн достаёт из бардачка небольшой свёрток.\n\nАркадий: «Держи. Мне это уже нахуй не нужно».\n\nВнутри лежат несколько патронов подходящего револьверного калибра.\n\nДжон: «А сам?»\n\nАркадий: «Я останусь здесь. Пока не решу, куда идти».\n\nДжон: «Не сиди слишком долго».\n\nАркадий: «Постараюсь».",sc);
      return reward(p,sc);
    }
    if(a==2){stage=6;w.note("Аркадий у машины","Джон отказался отдать воду и ушёл. Аркадий остался внутри.");GameIO.checkpoint();ResidentialStories.pause("У машины", "Джон: «Извини. У меня самого почти ничего нет».\n\nАркадий: «Понятно».\n\nДжон: «Ты можешь поискать в магазине за перекрёстком».\n\nАркадий: «Думаешь, я не пробовал?»\n\nПауза.\n\nАркадий: «Ладно. Проваливай. И дверь не трогай».\n\nДжон отступает.",sc);return true;}
    if(a==3){stage=3;GameIO.checkpoint();}
   }
   if(stage==3){GameIO.storyMenu("Револьвер", "Джон: «Положи револьвер на землю».\n\nАркадий: «Чего?»\n\nДжон: «Ты едва его держишь. Давай без глупостей».\n\nАркадий: «Значит, и ты такой же...»\n\nДжон: «Какой?»\n\nАркадий: «Сначала разговоры. Потом забираете последнее».\n\nАркадий резко поднимает оружие.\n\nАркадий: «Пошёл нахуй от моей машины!»");GameIO.action(1,"Отступить");GameIO.action(2,"Напасть","Начнётся бой");int a=SurvivalSystems.choice(sc);if(a==1)return true;if(a!=2)return true;stage=4;if(enemy==null)enemy=new ArkadyEnemy(2+random.nextInt(3));GameIO.checkpoint();}
   if(stage==4){boolean resolved=TacticalBattle.story(p,sc,random,List.of(enemy),"Автомобиль у дороги",List.of(),false,false,true,true);if(!resolved)return false;stage=enemy.isAlive()?8:7;w.note("Аркадий у машины",stage==7?"Аркадий погиб в бою у автомобиля.":"Аркадий скрылся во время боя.");GameIO.checkpoint();return true;}
  }
  return p.isAlive();
 }
 private boolean reward(Fighter p,Scanner sc){if(!rewardTaken){if(p.addItem(new Ammunition(AmmoType.REVOLVER,3))){rewardTaken=true;GameIO.out().println("Получено: револьверные патроны ×3.");GameIO.checkpoint();}else GameIO.out().println("Нет места: патроны остались у Аркадия.");}return true;}
 boolean rewardTaken;
}
final class ArkadyEnemy extends HostileSurvivor {
 private static final long serialVersionUID=1L;
 ArkadyEnemy(int rounds){super("Аркадий",new Weapons("Револьвер","Оружие Аркадия",5,350,WeaponStyle.REVOLVER),65,rounds);}
 @Override List<ItemInfo> claimLoot(){List<ItemInfo> items=new ArrayList<>(super.claimLoot());items.removeIf(item->item instanceof Weapons && item.getDurability()<=0);return items;}
}
