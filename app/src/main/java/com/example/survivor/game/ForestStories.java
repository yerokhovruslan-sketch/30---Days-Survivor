package com.example.survivor.game;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/** Repeatable foraging and one small persistent story in the forest region. */
final class ForestStories implements Serializable {
    private static final long serialVersionUID=1L;
    static final String BOX="Зелёный ящик Андрея";
    private Map<Integer,Integer> forageDue;
    boolean campSeen,cacheKnown,cacheOpened,cacheBandage,cacheAmmo;
    // 0 stranger; 1 peaceful; 2 accepted; 3 helped; 4 hostile; 5 left; 6 refused; 7 defeated.
    int andrey,deadline,boxState;
    boolean questStarted,boxOpened,talked,robFood,robWater,robBandage;
    boolean campfireIntro,campfireEnded;
    int campfireTopics,campfireIntroChoice,campfireFinishChoice;
    int[] campfireAnswers;
    boolean boxRequired(){return andrey==2;}
    static boolean supports(int place){return place==2 || place>=19&&place<=26;}
    int availableAt(int place){return forageDue==null?0:forageDue.getOrDefault(place,0);}
    private void setDue(int place,int minute){if(forageDue==null)forageDue=new HashMap<>();forageDue.put(place,minute);}
    private void note(WorldStories w,String title,String text){w.note(title,text);GameIO.out().println(text);GameIO.checkpoint();}
    private boolean take(Fighter p,ItemInfo item){if(!p.addItem(item)){GameIO.out().println("В рюкзаке нет места. Можно вернуться за находкой позже.");return false;}GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;}
    private boolean time(Fighter p,WorldStories w,int minutes,int stamina){if(!p.spendStamina(stamina)){GameIO.out().println("Недостаточно сил. Передохните и попробуйте позже.");return false;}w.advanceMinutes(minutes);return true;}
    private boolean hasBox(Fighter p){return p.getInventory().stream().anyMatch(i->i.getName().equals(BOX));}
    private void update(WorldStories w){if(andrey==2&&w.day()>=deadline){andrey=5;note(w,"Андрей ушёл","«Ждал сколько мог. Пришлось уходить. Если найдёшь ящик, оставь себе». Андрей оставил записку в охотничьей избушке.");}}
    void forage(Fighter p,int place,WorldStories w,Random random){
        int now=w.absoluteMinute();if(now<availableAt(place)){GameIO.out().println("Здесь недавно искали грибы и ягоды. Вернитесь позже.");return;}
        if(p.getCarryWeight()+500>Fighter.MAX_WEIGHT){GameIO.out().println("В рюкзаке нет места для находки.");return;}
        if(!time(p,w,15,4))return;
        setDue(place,w.absoluteMinute()+1440);
        if(random.nextInt(100)>=65){GameIO.out().println("Джон осмотрел заросли, но ничего пригодного не нашёл.");GameIO.checkpoint();return;}
        boolean mushroom=random.nextBoolean();boolean poison=mushroom&&random.nextInt(100)<25;
        take(p,new WildFood(mushroom,poison));GameIO.checkpoint();
    }
    /** Returns -1 to go home, 1 for normal search and 0 to stay in this menu. */
    int menu(Fighter p,Location loc,int place,WorldStories w,Scanner scanner,Random random,int[] wallet){
        update(w);GameIO.menu(loc.getName(),"Можно исследовать локацию или поискать в лесу съестные растения. Незнакомые грибы могут вызвать отравление.");
        GameIO.action(1,"Обыскать точки локации",loc.getRemainingSearches()>0?"Обычный поиск":"Обычные точки уже обысканы",loc.getRemainingSearches()>0);
        int wait=Math.max(0,availableAt(place)-w.absoluteMinute());GameIO.action(2,"Искать грибы и ягоды",wait>0?"Повторный поиск через "+((wait+59)/60)+" ч":"15 минут · находка не гарантирована",wait==0);
        if(place==20){
            GameIO.action(3,andrey==0?"Осмотреть следы у избушки":"Андрей у избушки",andrey==5?"Осталась записка":andrey==7?"Избушка опустела":"Кострище и укрытие лесника");
            if(!cacheKnown)GameIO.action(4,"Изучить старую карту охотника","5 минут · возможная подсказка");
        }
        if(place==21&&!campSeen)GameIO.action(3,"Проверить следы чужого лагеря","Свежие следы и кострище · 5 минут");
        if(place==19&&cacheKnown&&!(cacheBandage&&cacheAmmo))GameIO.action(3,"Найти старый охотничий тайник","Ориентир с карты в избушке");
        if(place==24&&questStarted&&boxState==0)GameIO.action(3,"Проверить рыбацкий домик","Поиск зелёного ящика Андрея");
        MonsterDens.Den den=w.monsterDens().existing(place);
        if(den!=null&&den.accessible())GameIO.action(7,den.label(),den.cleared?"Оставленные трофеи":"Опасный бой · "+den.count+" заражённых");
        GameIO.action(8,"Осмотреть тело","Проверить травмы и лечение");GameIO.action(9,"Передохнуть","Восстановить силы");GameIO.action(0,"Вернуться домой");
        int action=SurvivalSystems.choice(scanner);if(action==0)return -1;if(action==1&&loc.getRemainingSearches()>0)return 1;
        if(action==2&&wait==0)forage(p,place,w,random);
        else if(action==7&&den!=null&&den.accessible()){if(!w.monsterDens().visit(p,w,place,scanner,random))return -1;}
        else if(action==8)Survivor.inspectBody(p,scanner);else if(action==9)Survivor.rest(p,30);
        else if(place==20&&action==3)andrey(p,w,scanner,random,wallet);
        else if(place==20&&action==4&&!cacheKnown&&time(p,w,5,1)){cacheKnown=true;note(w,"Старый охотничий тайник","На старой карте в избушке отмечено дерево у лесной тропы. Под корнями отмечен небольшой охотничий тайник.");}
        else if(place==21&&action==3&&!campSeen&&time(p,w,5,1)){campSeen=true;note(w,"Чужой лагерь","У кострища две жестяные кружки, рядом свежие следы обуви. Пепел ещё хранит тепло. Джон не знает, кто здесь был и собирается ли вернуться.");}
        else if(place==19&&action==3&&cacheKnown)cache(p,w,scanner);
        else if(place==24&&action==3&&questStarted&&boxState==0)box(p,w,scanner);
        GameIO.checkpoint();return 0;
    }
    private void cache(Fighter p,WorldStories w,Scanner scanner){
        while(p.isAlive()){
            GameIO.menu("Охотничий тайник",cacheOpened?"Под корнями открыт старый ящик.":"По отметке на карте Джон находит старый ящик под корнями дерева.");
            if(!cacheOpened)GameIO.action(1,"Открыть тайник","10 минут");
            else{GameIO.action(2,"Взять бинт",cacheBandage?"Уже взят":"Перевязочный материал",!cacheBandage);GameIO.action(3,"Взять патроны",cacheAmmo?"Уже взяты":"Патроны к револьверу",!cacheAmmo);}
            GameIO.action(0,"Уйти");int a=SurvivalSystems.choice(scanner);if(a==0)return;
            if(a==1&&!cacheOpened&&time(p,w,10,2)){cacheOpened=true;note(w,"Охотничий тайник","Тайник под корнями найден. Его оставшиеся вещи можно забрать сейчас или позже.");}
            if(a==2&&cacheOpened&&!cacheBandage&&take(p,Shop.createItem(1))){cacheBandage=true;GameIO.checkpoint();}
            if(a==3&&cacheOpened&&!cacheAmmo&&take(p,new Ammunition(AmmoType.REVOLVER))){cacheAmmo=true;GameIO.checkpoint();}
        }
    }
    private void box(Fighter p,WorldStories w,Scanner scanner){
        GameIO.storyMenu("Рыбацкий домик","На острове домик с разбросанными снастями, порванной сетью и пустыми контейнерами. Под столом зелёный ящик Андрея. Внутри крючки, леска и фотография Андрея с другим мужчиной.");
        GameIO.action(1,"Взять ящик целым","Вернуть хозяину");GameIO.action(2,"Забрать немного снастей и взять ящик","Андрей может заметить пропажу");GameIO.action(0,"Оставить ящик и уйти");
        int a=SurvivalSystems.choice(scanner);if(a!=1&&a!=2)return;
        ItemInfo crate=new Tools(BOX,"Зелёный ящик с рыболовными снастями и фотографией. Андрей просил его вернуть.",1,0);
        ItemInfo line=new Tools("Рыболовная леска","Снасть из зелёного ящика Андрея.",1,2);
        if(p.getCarryWeight()+crate.getWeightGrams()+(a==2?line.getWeightGrams():0)>Fighter.MAX_WEIGHT){GameIO.out().println("В рюкзаке недостаточно места. Ящик остался под столом.");return;}
        if(!time(p,w,10,2))return;
        p.addItem(crate);if(a==2)p.addItem(line);boxState=a;boxOpened=a==2;
        note(w,"Зелёный ящик Андрея",a==1?"Зелёный ящик найден на Рыбацком острове и взят целым. Его можно вернуть Андрею.":"Зелёный ящик найден. Джон забрал часть снастей; Андрей может заметить пропажу.");
    }
    private void andrey(Fighter p,WorldStories w,Scanner scanner,Random random,int[] wallet){
        update(w);
        if(andrey==5){ResidentialStories.pause("Письмо Андрея","«Ждал сколько мог. Пришлось уходить. Если найдёшь ящик, оставь себе».",scanner);return;}
        if(andrey==7){
            while(p.isAlive()){
                GameIO.menu("Рюкзак Андрея","После схватки в укрытии остались чужие припасы.");
                GameIO.action(1,"Взять еду",robFood?"Уже взята":"Галеты",!robFood);
                GameIO.action(2,"Взять воду",robWater?"Уже взята":"Бутылка воды",!robWater);
                GameIO.action(3,"Взять перевязочные материалы",robBandage?"Уже взяты":"Бинт",!robBandage);GameIO.action(0,"Уйти");
                int a=SurvivalSystems.choice(scanner);if(a==0)return;
                if(a==1&&!robFood&&take(p,Shop.createItem(16))){robFood=true;GameIO.checkpoint();}
                if(a==2&&!robWater&&take(p,ResidentialStories.waterBottle())){robWater=true;GameIO.checkpoint();}
                if(a==3&&!robBandage&&take(p,Shop.createItem(1))){robBandage=true;GameIO.checkpoint();}
            }
            return;
        }
        while(p.isAlive()){
            if(andrey==0){
                GameIO.storyMenu("Кто-то здесь есть","У избушки свежие следы и рюкзак с едой, водой и перевязочными материалами. Из-за угла раздаётся голос: «Руки убрал. Я тебя видел ещё у дороги». Мужчина лет сорока держит металлическую трубу. «Это моё место. И вещи тоже мои». Его зовут Андрей, до эпидемии он был лесником.");
                GameIO.action(1,"Попытаться договориться");GameIO.action(2,"Предложить еду или воду за сведения","Один предмет из рюкзака",ResidentialStories.find(p,x->x instanceof Food)!=null);GameIO.action(3,"Извиниться и уйти");GameIO.action(4,"Попытаться забрать рюкзак","Андрей может защищаться");
                int c=SurvivalSystems.choice(scanner);if(c==0||c==3)return;
                if(c==1){andrey=1;note(w,"Андрей в охотничьей избушке","Джон объяснил, что искал припасы, а не чужие вещи. Андрей остаётся осторожным, но согласился поговорить.");continue;}
                if(c==2){ItemInfo item=ResidentialStories.find(p,x->x instanceof Food);if(item==null)continue;p.getInventory().remove(item);andrey=1;note(w,"Обмен с Андреем","За еду или воду Андрей рассказал: к Рыбацкому острову можно добраться через лодочную станцию. О своём лагере он говорит осторожно.");continue;}
                if(c==4){andrey=4;note(w,"Чужое место","Джон попытался забрать рюкзак Андрея. Лесник больше ему не доверяет.");continue;}
            }else if(andrey==4){
                GameIO.storyMenu("Андрей насторожен","Андрей сжимает трубу. Он не отдаст припасы добровольно.");GameIO.action(1,"Напасть и попытаться забрать рюкзак","Опасный бой");GameIO.action(0,"Уйти");int c=SurvivalSystems.choice(scanner);if(c!=1)return;
                HostileSurvivor enemy=new HostileSurvivor("Андрей",new Weapons("Металлическая труба","Труба Андрея",2,15,WeaponStyle.PIPE),55);
                boolean won=TacticalBattle.story(p,scanner,random,List.of(enemy),"Охотничья избушка: Андрей",List.of(),false,false,false);if(!won)return;
                andrey=7;note(w,"Чужое место","Схватка за рюкзак закончилась. Андрей больше не доверит Джону своё укрытие. Припасы остались в избушке.");andrey(p,w,scanner,random,wallet);return;
            }else if(andrey==1){
                GameIO.storyMenu("Просьба Андрея","«У озера есть рыбацкий домик. Раньше я держал там снасти. Проверь его и принеси мой зелёный ящик. Всё остальное, что не моё, можешь забрать себе».");
                GameIO.action(1,"Согласиться проверить домик","Рыбацкий остров · Андрей ждёт несколько дней");GameIO.action(2,"Отказаться от задания");GameIO.action(0,"Уйти и подумать");int c=SurvivalSystems.choice(scanner);if(c==0)return;
                if(c==1){andrey=2;questStarted=true;deadline=w.day()+5;note(w,"Просьба Андрея","Найти зелёный ящик в рыбацком домике на Рыбацком острове и вернуть Андрею. Он будет ждать до дня "+deadline+".");return;}
                if(c==2){andrey=6;note(w,"Просьба Андрея","Джон отказался искать ящик. Андрей остаётся в своём укрытии.");return;}
            }else if(andrey==2){
                GameIO.storyMenu("Андрей ждёт","«Проверь домик на Рыбацком острове. Я подожду до дня "+deadline+"».");
                GameIO.action(1,"Вернуть зелёный ящик",hasBox(p)?(boxOpened?"Ящик вскрыт, часть снастей отсутствует":"Ящик цел"):"Ящика нет в рюкзаке",hasBox(p));GameIO.action(0,"Вернуться позже");int c=SurvivalSystems.choice(scanner);if(c!=1||!hasBox(p))return;
                if(!boxOpened && p.getCarryWeight()+Shop.createItem(16).getWeightGrams()>Fighter.MAX_WEIGHT){GameIO.out().println("Освободите место для награды и вернитесь к Андрею.");return;}
                ItemInfo crate=ResidentialStories.find(p,x->x.getName().equals(BOX));p.getInventory().remove(crate);
                if(boxOpened){andrey=6;note(w,"Ящик вскрыт","Андрей заметил, что часть снастей пропала. Ящик он принял, но доверие потеряно.");return;}
                andrey=3;p.addItem(Shop.createItem(16));wallet[0]=(int)Math.min(Integer.MAX_VALUE,(long)wallet[0]+15);GameIO.balance(wallet[0]);note(w,"Ящик возвращён Андрею","«Думал, всё уже растащили. Спасибо». Андрей отдал Джону галеты и 15 $. За помощь он готов рассказать о лесных маршрутах.");return;
            }else if(andrey==3){
                GameIO.storyMenu("Андрей у избушки","Лесник узнаёт Джона и убирает трубу. Ящик со снастями снова у него.");GameIO.action(1,"Разговор по душам","Поговорить без спешки",AndreyCampfire.available(this));GameIO.action(2,"Спросить о лесных маршрутах");GameIO.action(0,"Уйти");int c=SurvivalSystems.choice(scanner);if(c==0)return;
                if(c==1 && AndreyCampfire.available(this))AndreyCampfire.open(this,p,w,scanner);
                if(c==2)ResidentialStories.pause("Совет Андрея","«К воде безопаснее выходить через лодочную станцию. Если тропа кажется слишком тихой, не спеши — сначала прислушайся».",scanner);
            }else {ResidentialStories.pause("Андрей у избушки",andrey==6?"Андрей держится сдержанно. Совместных дел он пока не предлагает.":"Лесник остался в своём укрытии.",scanner);return;}
        }
    }
}
