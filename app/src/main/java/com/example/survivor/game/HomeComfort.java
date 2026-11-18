package com.example.survivor.game;
import java.util.*;
/** Small persistent comforts using the existing shelter and inventory. */
final class HomeComfort {
 static List<ItemInfo> all(Fighter p){List<ItemInfo> a=new ArrayList<>(p.getInventory());a.addAll(p.getStorage());return a;}
 static ItemInfo find(Fighter p,String... names){for(ItemInfo i:all(p))for(String n:names)if(i.getName().equalsIgnoreCase(n))return i;return null;}
 static void clothing(Fighter p,Scanner sc){while(GameIO.hasInput(sc)){List<ItemInfo> items=new ArrayList<>();for(ItemInfo i:p.getInventory())if(p.clothingSlot(i)>0)items.add(i);GameIO.menu("Одежда","Одежду можно носить одновременно с бронёй. Повторное нажатие снимает вещь.");for(int i=0;i<items.size();i++){ItemInfo it=items.get(i);boolean on=p.worn(p.clothingSlot(it))==it;GameIO.item(i+1,it,on?"Надето: ":"Надеть: ",true,on);}GameIO.action(0,"Назад");int n=SurvivalSystems.choice(sc);if(n==0)return;if(n>0&&n<=items.size()){p.wearClothing(items.get(n-1));GameIO.checkpoint();}}}
 static void menu(Fighter p,WorldStories w,Scanner sc){while(GameIO.hasInput(sc)){ShelterUpgrades u=p.getUpgrades();long fire=Math.max(0,u.fireUntil-ShelterUpgrades.minute(w));GameIO.menu("Отдых","Температура: "+p.coldState()+" · костёр: "+(fire>0?"горит ещё "+fire+" мин":"не горит"));GameIO.action(1,"Спать","8 часов · сонливость выше 40",p.canSleep());GameIO.action(2,"Костёр","Древесина: одна порция горит 60 минут");GameIO.action(3,"Кровать","Подушка, одеяло и простыня");GameIO.action(4,"Гигиена","Умыться или почистить зубы");GameIO.action(5,"Передохнуть","Восстановить силы");GameIO.action(0,"Назад");int a=SurvivalSystems.choice(sc);if(a==0)return;if(a==1){if(ShelterUpgrades.sleep(p,w)<0)GameIO.out().println("Спать можно при сонливости выше 40.");else {if(u.sheet!=null)p.recoverInjuries(60);GameIO.out().println("Джон поспал восемь часов.");GameIO.checkpoint();}}else if(a==2)fire(p,w,sc);else if(a==3)bed(p,sc);else if(a==4)hygiene(p,w,sc);else if(a==5){Survivor.rest(p,30);if(u.sheet!=null)p.restoreStamina(5);}}
 }
 static void fire(Fighter p,WorldStories w,Scanner sc){while(GameIO.hasInput(sc)){ShelterUpgrades u=p.getUpgrades();long remain=Math.max(0,u.fireUntil-ShelterUpgrades.minute(w));ItemInfo wood=find(p,Crafting.WOOD,"Ветки","Доски","Доска","Деревянные доски");GameIO.menu("Костёр",remain>0?"Горит ещё "+remain+" минут игрового времени.":"Костёр погас.");GameIO.action(1,"Добавить древесину","Одна порция продлит горение на 60 минут",wood!=null);GameIO.action(0,"Назад");int n=SurvivalSystems.choice(sc);if(n==0)return;if(n==1&&wood!=null){p.getInventory().remove(wood);p.getStorage().remove(wood);u.fireUntil=Math.max(u.fireUntil,ShelterUpgrades.minute(w))+60;GameIO.checkpoint();}}}
 private static ItemInfo bedding(Fighter p,int slot){
  return slot==1?find(p,"Старая подушка","Чистая подушка")
   :slot==2?find(p,"Тонкое одеяло","Порванный плед"):find(p,"Простыня");
 }
 private static ItemInfo installed(ShelterUpgrades u,int slot){return slot==1?u.pillow:slot==2?u.blanket:u.sheet;}
 static void bed(Fighter p,Scanner sc){
  String[] labels={"Подушка","Одеяло","Простыня"};
  while(GameIO.hasInput(sc)){
   ShelterUpgrades u=p.getUpgrades();
   GameIO.menu("Кровать","По одному предмету каждого типа. Вещи берутся из рюкзака или со склада.");
   GameIO.illustratedCard("Кровать","Выберите место для постельных принадлежностей.","bed_comfort");
   for(int slot=1;slot<=3;slot++){
    ItemInfo current=installed(u,slot),available=bedding(p,slot);
    String detail=current!=null?"Уже положено: "+current.getName():available!=null?"Положить: "+available.getName():"Нет в рюкзаке и на складе";
    GameIO.action(slot,labels[slot-1],detail,current==null&&available!=null);
   }
   GameIO.action(0,"Назад");int a=SurvivalSystems.choice(sc);
   if(a==0)return;
   if(a<1||a>3)continue;
   if(installed(u,a)!=null||bedding(p,a)==null){GameIO.sound(GameSound.ERROR);GameIO.out().println(installed(u,a)!=null?"Это место уже занято.":"Подходящего предмета нет в рюкзаке или на складе.");continue;}
   GameIO.menu(labels[a-1],"Положить на кровать?");GameIO.action(1,"Положить");GameIO.action(0,"Отмена");
   if(SurvivalSystems.choice(sc)!=1)continue;
   ItemInfo item=bedding(p,a);
   if(item==null||installed(u,a)!=null){GameIO.sound(GameSound.ERROR);continue;}
   p.getInventory().remove(item);p.getStorage().remove(item);
   if(a==1)u.pillow=item;else if(a==2)u.blanket=item;else u.sheet=item;
   GameIO.sound(GameSound.BAG);GameIO.out().println("На кровать положено: "+item.getName()+".");GameIO.checkpoint();
  }
 }
 static void hygiene(Fighter p,WorldStories w,Scanner sc){while(GameIO.hasInput(sc)){boolean brush=find(p,"Зубная щетка","Зубная щётка","Запечатанная зубная щётка","Старая зубная щётка")!=null;boolean paste=find(p,"Зубная паста","Початый тюбик зубной пасты","Запечатанный тюбик зубной пасты")!=null;GameIO.menu("Гигиена","Бытовые действия для атмосферы, без штрафов и обязательного повторения.");GameIO.action(1,"Умыться","5 минут");GameIO.action(2,"Почистить зубы","5 минут · нужна щётка и паста",brush&&paste);GameIO.action(0,"Назад");int a=SurvivalSystems.choice(sc);if(a==0)return;if(a==1||a==2&&brush&&paste){w.advanceMinutes(5);GameIO.sound(a==1?GameSound.WASH:GameSound.BRUSH_TEETH);GameIO.out().println(a==1?"Джон умылся.":"Джон почистил зубы.");GameIO.checkpoint();}}}
}
