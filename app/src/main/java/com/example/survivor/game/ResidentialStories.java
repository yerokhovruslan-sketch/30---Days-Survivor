package com.example.survivor.game;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;

/** Only chapter I events 1–3. Persistent decisions, NPC relationships and delayed consequences. */
final class ResidentialStories implements Serializable {
    private static final long serialVersionUID=1L;
    int humanity,reputation,kirillSuspicion;
    // Family: 0 unknown, 1 waiting, 2 supplied, 3 escorted, 4 robbed, 5 killed, 6 gone, 7 child saved, 8 escort failed.
    int family,familyDay,familyDue,nightmareUntil;
    boolean familyCleared,familyDeparted,familyApartmentSeen,familyFollowup,clinicInfo,clinicLoot,dreamSeen;
    // Kirill: 0 unknown, 1 first meeting, 2 friendly, 3 neutral, 4 escaped robbery, 5 dead, 6 abandoned, 7 revenge ended.
    int kirill,kirillDue,kirillTradeStock=3;
    boolean toolInfo,toolCache,kirillSupport;
    // Viktor: 0 unknown, 1 waiting, 2 helped, 3 traded, 4 robbed, 5 dead, 6 robbers driven away.
    int viktor,viktorDue;
    boolean basementKnown,basementKey,basementOpen,basementLoot,ambushUsed;
    transient boolean aborted;

    static final String[] TITLES={"","Окно на третьем этаже","Чужая добыча","Последняя банка"};
    void remember(WorldStories world,int id,String text) {world.note(TITLES[id],text);}
    void changeHumanity(int value) {humanity=Math.max(-100,Math.min(100,humanity+value));}
    void changeReputation(int value) {reputation=Math.max(-100,Math.min(100,reputation+value));}
    void update(WorldStories world) {
        if(family==1 && world.day()>=familyDue) {
            family=6;familyDeparted=true;remember(world,1,"Срок ожидания семьи истёк. У окна больше никого нет; стоит проверить квартиру.");
        }
        if((family==2 || family==4) && world.day()>=familyDue && !familyDeparted) {
            familyDeparted=true;remember(world,1,"Семья покинула квартиру. При возвращении во двор можно найти следы её ухода.");
        }
        if(viktor==4 && world.day()>=viktorDue) {viktor=5;remember(world,3,"После ограбления Виктор так и не получил помощи. При следующем визите подсобку можно проверить.");}
    }
    int discover(int place,SearchMode mode,WorldStories world,Random random) {
        update(world);
        if(place==1 && family==0 && world.day()>=3 && random.nextInt(100)<(mode==SearchMode.FAST?15:mode==SearchMode.CAREFUL?35:55)) {
            family=1;familyDay=world.day();familyDue=world.day()+3;
            remember(world,1,"В окне третьего этажа женщина показывает: «НЕ ШУМИ. ОНИ В ПОДЪЕЗДЕ». Затем — «ПОМОГИ». Окно отмечено как точка интереса.");GameIO.sound(GameSound.QUEST);return 1;
        }
        if(place==1 && kirill==0 && random.nextInt(100)<(mode==SearchMode.FAST?20:mode==SearchMode.CAREFUL?35:50)) {
            kirill=1;remember(world,2,"У открытого гаража найден рюкзак. Его хозяин — раненый Кирилл с металлической трубой — требует вернуть вещи.");GameIO.sound(GameSound.QUEST);return 2;
        }
        if(place==4 && viktor==0 && random.nextInt(100)<(mode==SearchMode.FAST?20:mode==SearchMode.CAREFUL?35:55)) {
            viktor=1;remember(world,3,"В подсобке магазина скрывается пожилой мужчина. У него осталась последняя банка, а грабители обещали вернуться.");GameIO.sound(GameSound.QUEST);return 3;
        }
        return 0;
    }
    List<Integer> points(int place,WorldStories world) {
        update(world);List<Integer> result=new ArrayList<>();
        if(place==1 && (family==1 || (family==2 || family==4 || family==6) && (!familyDeparted || !familyApartmentSeen)))result.add(1);
        if((place==3 || place==13) && (family==2 && familyDeparted || family==3 || family==4 && familyDeparted)
                && !familyFollowup)result.add(1);
        if(place==10 && clinicInfo && !clinicLoot)result.add(1);
        if(place==1 && kirill>0 && kirill!=5 && kirill!=7)result.add(2);
        if(place==35 && (kirill==2 || toolInfo && !toolCache))result.add(2);
        if(place==4 && viktor>0)result.add(3);
        return result;
    }
    void pointsMenu(Fighter player,int place,WorldStories world,Scanner scanner,Random random) {
        while(player.isAlive() && !aborted) {
            List<Integer> points=points(place,world);
            if(points.isEmpty())return;
            GameIO.storyMenu("Точки интереса","Известные истории в этой локации. Чтение не тратит время.");
            for(int id:points)GameIO.action(id,TITLES[id]);GameIO.action(0,"Назад к вылазке");
            int choice=SurvivalSystems.choice(scanner);if(choice==0)return;
            if(points.contains(choice))run(choice,player,place,world,scanner,random);
        }
    }
    void visitFamilyApartment(WorldStories world, Fighter player, Scanner scanner, Random random) {
        update(world);
        if (family == 0) { family=1; familyDay=world.day(); familyDue=world.day()+3; remember(world,1,"В квартире №31 обнаружены Марина и Артём. Им нужна вода и перевязка."); }
        if (family != 1) { family(player,1,world,scanner,random); return; }
        while (player.isAlive() && family==1) {
            GameIO.storyMenu("Квартира №31", "Из-за двери отвечает Марина: с ней сын Артём. В подъезде были заражённые; им нужна вода и медицинские припасы. Ждать они смогут до дня "+familyDue+".");
            GameIO.action(1,"Передать воду и медикаменты","По одному предмету из рюкзака",find(player,ResidentialStories::water)!=null && find(player,ResidentialStories::medical)!=null);
            GameIO.action(2,"Предложить сопровождение","Провести семью через подъезд");
            GameIO.action(3,"Поговорить о поликлинике");
            GameIO.action(0,"Вернуться позже");
            int c=SurvivalSystems.choice(scanner); if(c==0)return;
            if(c==1 && helpFamily(player,world,random)){familyCleared=true;world.advanceMinutes(15);GameIO.checkpoint();return;}
            if(c==2) {
                GameIO.storyMenu("Вывести семью", "Марина помогает Артёму подняться. Можно выйти по лестнице или выбрать более долгий обход.");
                GameIO.action(1,"Провести по лестнице","20 минут"); GameIO.action(2,"Провести боковым проходом","35 минут"); GameIO.action(0,"Пока не уходить");
                int route=SurvivalSystems.choice(scanner); if(route==0)continue; if(route!=1 && route!=2)continue;
                world.advanceMinutes(route==1?20:35);family=3;familyCleared=true;changeHumanity(5);clinicInfo=true;
                remember(world,1,"Марина и Артём выведены из подъезда. Марина рассказала о закрытом шкафе с лекарствами в поликлинике."); GameIO.checkpoint(); return;
            }
            if(c==3) pause("Марина о поликлинике", "Марина работала в поликлинике. За регистратурой остался закрытый служебный шкаф, где могли сохраниться лекарства.",scanner);
        }
    }
    void visitWindow(WorldStories world, Fighter player, Scanner scanner, Random random) {
        update(world);
        if (family == 0 && world.day() >= 3) {
            family=1; familyDay=world.day(); familyDue=world.day()+3;
            remember(world,1,"В окне третьего этажа женщина показывает: «НЕ ШУМИ. ОНИ В ПОДЪЕЗДЕ». Затем — «ПОМОГИ».");
        }
        if (family > 0) run(1,player,1,world,scanner,random);
    }
    void run(int id,Fighter player,int place,WorldStories world,Scanner scanner,Random random) {
        update(world);
        try {
            if(id==1)family(player,place,world,scanner,random);
            if(id==2)kirill(player,place,world,scanner,random);
            if(id==3)viktor(player,world,scanner,random);
        } finally {GameIO.checkpoint();}
    }
    private boolean marinaSignalTalk,kirillSignalTalk,viktorSignalTalk;
    void signalTalk(int who,WorldStories world,Scanner scanner) {
        String title,text;
        if(who==1){marinaSignalTalk=true;title="Списки больницы";text="Марина ненадолго замолкает. «В больнице тоже составляли списки. Отдельно тех, кто может идти. Отдельно остальных». Она молчит. «Я тогда решила, что это просто для транспорта». Что случилось дальше, Марина не знает.";}
        else if(who==2){kirillSignalTalk=true;title="Машина с красным крестом";text="«В сторону склада ещё ездили машины. Недавно. На одной был красный крест». — «Людей вывозили?» — «Я видел машину, Джон. Про людей придумывать не буду». След можно искать на станции скорой помощи, но это пока только слова Кирилла.";}
        else{viktorSignalTalk=true;title="Люди с бумагами";text="«Воду со склада магазина забрали заранее. И консервы. По ведомости». — «Кто?» — «Люди с бумагами. В те дни бумаги ещё действовали». Возможно, припасы собирали для размещения людей. Куда они попали и был ли транспорт — Виктор не знает.";}
        world.note(title,text);pause(title,text,scanner);
    }
    static boolean water(ItemInfo item) {String n=item.getName().toLowerCase(Locale.ROOT);return n.contains("воды") || n.contains("водой");}
    static boolean food(ItemInfo item) {return item instanceof Food && !water(item);}
    static boolean medical(ItemInfo item) {return item instanceof MedicalItem || item.getName().matches(".*(Марлевые салфетки|Перевязочный комплект|Медицинский набор).*" );}
    static ItemInfo find(Fighter player,Predicate<ItemInfo> predicate) {return player.getInventory().stream().filter(predicate).findFirst().orElse(null);}
    static boolean consume(Fighter player,Predicate<ItemInfo> first,Predicate<ItemInfo> second) {
        ItemInfo a=find(player,first),b=second==null?null:player.getInventory().stream().filter(i->i!=a && second.test(i)).findFirst().orElse(null);
        if(a==null || second!=null && b==null)return false;
        player.getInventory().remove(a);if(b!=null)player.getInventory().remove(b);return true;
    }
    static boolean lever(Fighter player) {
        return player.getInventory().stream().anyMatch(i->i.getDurability()>0 && (i.isRepairTool() || i.getName().toLowerCase(Locale.ROOT).matches(".*(лом|монтиров|топор).*")));
    }
    static void reward(Fighter player,Scanner scanner,ItemInfo... items) {SurvivalSystems.collect(player,Arrays.asList(items),scanner);}
    static Food waterBottle() {return new Food("Бутылка минеральной воды","Чистая питьевая вода",1,4,0);}
    static Tools battery() {return new Tools("Батарейки AA","Комплект элементов питания",1,6);}
    static void pause(String title,String text,Scanner scanner) {
        GameIO.storyMenu(title,"");
        if(text.contains("Квартира затихла") || text.contains("не выжила") || text.contains("не пережил"))GameIO.musicCue("elegy");
        else if(text.contains("никого нет") || text.contains("покинула дом"))GameIO.musicCue("fading");
        else if(text.contains("Квартира пуста") || text.contains("забыты"))GameIO.musicCue("forgotten");
        GameIO.card(title,text);GameIO.action(0,"Продолжить");SurvivalSystems.choice(scanner);
    }
    List<CombatEnemy> infected(int count) {List<CombatEnemy> list=new ArrayList<>();for(int i=1;i<=count;i++)list.add(new Zombie(i,ZombieType.NORMAL));return list;}
    boolean fight(Fighter player,Scanner scanner,Random random,List<CombatEnemy> enemies,String terrain,
                  List<TacticalBattle.Civilian> people,boolean escort,boolean surrender,boolean first) {
        boolean won=TacticalBattle.story(player,scanner,random,enemies,terrain,people,escort,surrender,first);
        if(!won)aborted=true;return won;
    }
    boolean helpFamily(Fighter player,WorldStories world,Random random) {
        if(family!=1 || !consume(player,ResidentialStories::water,ResidentialStories::medical))return false;
        GameIO.sound(GameSound.QUEST);family=2;familyDue=world.day()+3+random.nextInt(3);changeHumanity(3);
        remember(world,1,"Марина и Артём получили воду и медицинские припасы. Марина дала ключ от кладовки и рассказала, что работала в районной поликлинике.");return true;
    }
    void family(Fighter player,int place,WorldStories world,Scanner scanner,Random random) {
        if(place==10 && clinicInfo && !clinicLoot) {
            GameIO.sound(GameSound.METAL_CONTAINER);clinicLoot=true;remember(world,1,"По указанию Марины найден закрытый шкаф в поликлинике.");
            reward(player,scanner,new MedicalItem("Аптечка",40,50),new MedicalItem("Бинт",15,20),Shop.createItem(42));return;
        }
        if(place==3 || place==13) {
            if(family==4) {
                familyFollowup=true;changeReputation(-2);remember(world,1,"Марина узнала грабителя возле других выживших и предупредила их.");
                pause(TITLES[1],"«Я помню тебя. Ты забрал еду у моего сына. Не подходи к нам». Марина отворачивается. Торговли и помощи не будет.",scanner);return;
            }
            GameIO.storyMenu("Встреча с Мариной","Марина и Артём живы. Она узнаёт Джона и предлагает помощь.");
            if(!marinaSignalTalk)GameIO.action(8,"В больнице говорили об эвакуации?");
            GameIO.action(1,"Попросить медикаменты");GameIO.action(2,"Узнать о закрытом помещении поликлиники");GameIO.action(0,"Поговорим позже");
            int c=SurvivalSystems.choice(scanner);if(c==8 && !marinaSignalTalk){signalTalk(1,world,scanner);return;}if(c!=1 && c!=2)return;
            familyFollowup=true;changeReputation(2);remember(world,1,"Спасённая Марина встретила Джона и рассказала о его помощи другим выжившим.");
            if(c==1)reward(player,scanner,new MedicalItem("Аптечка",40,50),new MedicalItem("Бинт",15,20));
            else {clinicInfo=true;pause("Совет Марины","В поликлинике остался запертый шкаф за регистратурой. Марина объясняет, как открыть служебную дверь.",scanner);}return;
        }
        if(familyDeparted) {
            familyApartmentSeen=true;
            pause(TITLES[1],family==2?"Квартира пуста. На столе записка: «Спасибо за помощь. Мы пошли на восток, к старой станции. Если доберёмся, оставим знак»."
                    :family==4?"В квартире беспорядок и пустые полки. Семья ушла поспешно. Свежие следы ведут к автобусной остановке."
                    :"Окно пусто. В квартире никто не отвечает. Семья не смогла ждать бесконечно и покинула дом.",scanner);return;
        }
        if(family==2) {
            GameIO.storyMenu("Разговор с Мариной","Артём спит. Семья готовится уйти на восток, к старой станции.");
            if(!marinaSignalTalk)GameIO.action(8,"В больнице говорили об эвакуации?");
            GameIO.action(1,"Расспросить о первых днях эпидемии");GameIO.action(0,"Попрощаться");
            int talk=SurvivalSystems.choice(scanner);if(talk==8 && !marinaSignalTalk)signalTalk(1,world,scanner);
            if(talk==1)pause("Марина о поликлинике","«Сначала привозили людей с температурой. Потом перестали отвечать отделения. Нас просили не покидать кабинеты и ждать транспорта. Но транспорт не приехал. За регистратурой оставался закрытый служебный шкаф — там могли уцелеть лекарства». Подробный путь Марина обещает показать, когда доберётся до станции.",scanner);
            return;
        }
        if(family!=1) {pause(TITLES[1],family==2?"Марина благодарит за воду. Артём спит. Они готовятся покинуть район. В ближайшие дни их можно встретить у старой станции."
                :family==3 || family==7?"Спасённые добрались до безопасного места. Эта квартира больше не требует помощи.":"Квартира пуста.",scanner);return;}
        while(player.isAlive() && family==1 && !aborted) {
            update(world);if(family!=1) {pause(TITLES[1],"Пока Джон готовился, семья покинула квартиру. У окна больше никого нет.",scanner);return;}
            GameIO.storyMenu(TITLES[1],"Марина ждёт до дня "+familyDue+". Припасы нужны в рюкзаке.");
            GameIO.card("Третий этаж","Ветер гоняет газеты между брошенными автомобилями. За стеклом появляется женское лицо.\n«НЕ ШУМИ. ОНИ В ПОДЪЕЗДЕ». Через несколько секунд — «ПОМОГИ». Из приоткрытой двери подъезда доносятся удары.");
            GameIO.action(1,familyCleared?"Поговорить с Мариной":"Подняться в квартиру","В подъезде два заражённых");
            GameIO.action(2,"Поговорить с улицы");
            GameIO.action(3,"Отдать воду и медицинские припасы","1 вода + 1 медицинский предмет",familyCleared && find(player,ResidentialStories::water)!=null && find(player,ResidentialStories::medical)!=null);
            GameIO.action(4,"Сопроводить семью","Марина и Артём появятся на поле боя",familyCleared);
            GameIO.action(5,"Потребовать все припасы");GameIO.action(6,"Напасть на жильцов","Безоружная женщина с ребёнком",familyCleared);
            GameIO.action(0,"Уйти. Вернуться позже");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==2) {pause(TITLES[1],"«Я Марина. Со мной сын, Артём. Нам нужна вода и перевязка. Я работала в поликлинике, но выйти из подъезда мы не можем». Можно вернуться с припасами.",scanner);continue;}
            if((c==1 || c==5) && !familyCleared) {
                if(!fight(player,scanner,random,infected(2),"Заражённая лестничная площадка",Collections.emptyList(),false,false,false))return;
                GameIO.sound(GameSound.WOOD_DOOR);familyCleared=true;world.advanceMinutes(10);
            }
            if(c==1)continue;
            if(c==3 && familyCleared && helpFamily(player,world,random)) {
                world.advanceMinutes(15);reward(player,scanner,Shop.createItem(17),battery(),Shop.createItem(42));return;
            }
            if(c==4 && familyCleared) {
                TacticalBattle.Civilian marina=new TacticalBattle.Civilian("Марина",50,57),artem=new TacticalBattle.Civilian("Артём",35,58);
                boolean survived=fight(player,scanner,random,infected(2),"Лестница: сопровождение семьи",Arrays.asList(marina,artem),true,false,false);
                if(!survived) {if(player.isAlive() && world.pendingBattle(world.currentPlace())!=null)return;family=8;remember(world,1,"Сопровождение сорвалось. Джон отступил или погиб; доверие семьи потеряно.");return;}
                if(marina.person.isAlive() && artem.person.isAlive()) {
                    family=3;GameIO.sound(GameSound.QUEST);GameIO.musicCue("ashfall");changeHumanity(5);remember(world,1,"Марина и Артём выведены через заражённый подъезд. Они доверяют Джону и обещают встретиться у станции.");
                    reward(player,scanner,Shop.createItem(17),new MedicalItem("Бинт",15,20));
                } else if(artem.person.isAlive()) {
                    GameIO.storyMenu("После сопровождения","Марина не выжила. Артём добрался до выхода и не может идти дальше один.");
                    GameIO.action(1,"Отвести Артёма в безопасное убежище","60 минут");GameIO.action(2,"Оставить у выхода");int choice=SurvivalSystems.choice(scanner);
                    family=choice==1?7:8;if(choice==1){world.advanceMinutes(60);changeHumanity(2);}
                    remember(world,1,choice==1?"Артём передан под присмотр выживших в безопасном убежище.":"Джон оставил Артёма у выхода. История семьи закончилась разлукой.");
                } else {family=8;remember(world,1,"Семью не удалось спасти. Подробностей Джон не записывает.");}return;
            }
            if(c==5 && familyCleared) {
                family=4;familyDue=world.day()+3;changeHumanity(-5);
                remember(world,1,"Джон забрал у Марины и Артёма еду и воду. Марина запомнила его лицо.");
                reward(player,scanner,Shop.createItem(17),waterBottle(),new MedicalItem("Бинт",15,20));return;
            }
            if(c==6 && familyCleared) {
                GameIO.storyMenu("Опустить оружие?","Перед тобой безоружная женщина с ребёнком. Они не представляют угрозы. Ты действительно собираешься это сделать?");
                GameIO.action(0,"Опустить оружие");GameIO.action(1,"Продолжить нападение");if(SurvivalSystems.choice(scanner)!=1)continue;
                family=5;changeHumanity(-20);nightmareUntil=world.day()+3;
                remember(world,1,"Джон напал на безоружных жильцов. Линия Марины и Артёма закрыта. Свидетелей, которые могли бы рассказать другим, нет.");
                pause(TITLES[1],"Квартира затихла. Теперь здесь никто не попросит воды. Ты получил то, за чем пришёл.\n\n«Ты изверг. Даже этот мёртвый город не заставлял тебя делать подобное», — мысль, которую Джон пытается подавить.",scanner);
                reward(player,scanner,Shop.createItem(17),waterBottle(),new MedicalItem("Бинт",15,20));return;
            }
        }
    }
    HostileSurvivor kirillEnemy() {return new HostileSurvivor("Кирилл",new Weapons("Металлическая труба","Труба Кирилла",2,15,WeaponStyle.PIPE),38);}
    void kirill(Fighter player,int place,WorldStories world,Scanner scanner,Random random) {
        if(place==35 && toolInfo && !toolCache) {
            GameIO.sound(GameSound.WOOD_CONTAINER);toolCache=true;remember(world,2,"Найден указанный Кириллом склад инструментов в гаражном кооперативе.");
            reward(player,scanner,Shop.createItem(20),Shop.createItem(19),Shop.createItem(19),Shop.createItem(42));return;
        }
        if(kirill==4) {
            if(world.day()<kirillDue) {pause(TITLES[2],"Кирилл ушёл. В гараже остались следы спешки. Вряд ли он забудет отнятые припасы.",scanner);return;}
            GameIO.storyMenu("Кирилл вернулся","Кирилл стоит рядом с другим выжившим. «Теперь поговорим о моём рюкзаке». ");
            GameIO.action(1,"Вернуть припасы","Две порции еды",player.getInventory().stream().filter(ResidentialStories::food).count()>=2);
            GameIO.action(2,"Принять бой","Двое противников");GameIO.action(0,"Отступить");int c=SurvivalSystems.choice(scanner);
            if(c==0){aborted=true;return;}
            if(c==1 && consume(player,ResidentialStories::food,ResidentialStories::food)){kirill=3;remember(world,2,"Припасы возвращены Кириллу. Он прекратил преследование, но дружбы не будет.");return;}
            if(c==2) {
                HostileSurvivor k=kirillEnemy(),friend=new HostileSurvivor(2,false,random);
                boolean win=fight(player,scanner,random,Arrays.asList(k,friend),"Проход между гаражами",Collections.emptyList(),false,true,true);
                if(win){kirill=k.isAlive()?7:5;if(k.isAlive() || friend.isAlive())changeReputation(-1);
                    remember(world,2,"Повторная встреча с Кириллом закончилась боем. "+(k.isAlive() || friend.isAlive()?"Выживший свидетель знает Джона.":"Свидетелей не осталось; слухи сами собой не появились."));}return;
            }
        }
        if(kirill==2 || kirill==3 || kirill==6) {
            GameIO.storyMenu("Кирилл",kirill==2?"Кирилл узнаёт Джона. Можно обменять припасы; запас ограничен.":"Кирилл держится настороженно. Совместных дел больше не предлагает.");
            if(kirill==2 && !kirillSignalTalk)GameIO.action(8,"Видел, как люди покидали район?");
            GameIO.action(1,"Обменять еду на запчасти","1 порция еды · осталось обменов: "+kirillTradeStock,kirill==2 && kirillTradeStock>0 && find(player,ResidentialStories::food)!=null);
            GameIO.action(0,"Уйти");int talk=SurvivalSystems.choice(scanner);if(talk==8 && kirill==2 && !kirillSignalTalk){signalTalk(2,world,scanner);return;}if(talk==1 && kirill==2 && kirillTradeStock>0 && consume(player,ResidentialStories::food,null)) {
                kirillTradeStock--;reward(player,scanner,Shop.createItem(19));
            }return;
        }
        if(kirill!=1)return;
        while(kirill==1 && !aborted) {
            GameIO.storyMenu(TITLES[2],"Раненый хозяин рюкзака ждёт ответа.");
            GameIO.card("У гаража","Возле открытой двери лежит рюкзак: внутри консервы, вода и коробка патронов. Рядом — лом и свежие следы.\nМужчина с перевязанной рукой выходит из-за гаража, сжимая металлическую трубу. «Эй. Положи на место. Это моё». ");
            GameIO.action(1,"Вернуть рюкзак");GameIO.action(2,"Потребовать плату за возврат");GameIO.action(3,"Забрать вещи силой","Кирилл атакует первым");
            GameIO.action(4,"Предложить сотрудничество","Только если не угрожали Кириллу",kirillSuspicion==0);GameIO.action(0,"Не трогать вещи и уйти");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1) {
                kirill=2;changeHumanity(1);GameIO.storyMenu("Предложение Кирилла","«Спасибо. Я оставил рюкзак, пока открывал соседний гараж. Выбирай, чем помочь». ");
                GameIO.action(1,"Сведения о складе инструментов");GameIO.action(2,"Две батарейки");GameIO.action(3,"Помощь при следующем визите");
                int choice=SurvivalSystems.choice(scanner);
                if(choice==2)reward(player,scanner,battery(),battery());else if(choice==3)kirillSupport=true;else toolInfo=true;
                remember(world,2,"Рюкзак возвращён Кириллу. Он стал дружественным и предложил награду"+(toolInfo?": склад инструментов в гаражном кооперативе.":kirillSupport?": помощь при следующем визите.":": две батарейки."));return;
            }
            if(c==2) {
                kirillSuspicion=Math.min(3,kirillSuspicion+1);
                GameIO.storyMenu("Плата за находку","Кирилл раздражён. Он готов отдать одну консерву или батарейки, но не весь рюкзак.");
                GameIO.action(1,"Взять консерву");GameIO.action(2,"Взять батарейки");GameIO.action(3,"Вернуть бесплатно");GameIO.action(4,"Требовать всё. Угрожать");GameIO.action(0,"Отказаться от требования");
                int choice=SurvivalSystems.choice(scanner);if(choice==0)continue;
                if(choice==3){kirill=2;changeHumanity(1);remember(world,2,"Джон отказался от платы и вернул рюкзак.");return;}
                if(choice==1 || choice==2) {kirill=3;reward(player,scanner,choice==1?Shop.createItem(17):battery());remember(world,2,"За возврат рюкзака получена небольшая плата. Кирилл остался нейтральным.");return;}
                if(choice!=4)continue;c=3;
            }
            if(c==3) {
                HostileSurvivor k=kirillEnemy();if(!fight(player,scanner,random,Arrays.asList(k),"Узкий проход между гаражами",Collections.emptyList(),false,true,true))return;
                kirill=k.isAlive()?4:5;kirillDue=world.day()+3;changeHumanity(k.isAlive()?-3:-6);
                remember(world,2,k.isAlive()?"Джон отнял рюкзак. Кирилл ушёл живым и может вернуться с товарищем.":"Кирилл погиб в драке за рюкзак. Будущая торговля с ним невозможна.");
                reward(player,scanner,Shop.createItem(17),waterBottle(),new Ammunition(AmmoType.REVOLVER));return;
            }
            if(c==4 && kirillSuspicion==0) {
                world.advanceMinutes(20);GameIO.storyMenu("Автомобильная сигнализация","В соседнем гараже стоят машина и ящик с инструментами. При открытии ящика срабатывает сигнализация.");
                GameIO.action(1,"Немедленно уйти");GameIO.action(2,"Отключить сигнализацию","Нужен рабочий ремонтный инструмент",player.getInventory().stream().anyMatch(i->i.isRepairTool() && i.getDurability()>0));
                GameIO.action(3,"Остаться и защищать Кирилла","2–3 заражённых");int choice=SurvivalSystems.choice(scanner);
                if(choice==1 || choice==0){kirill=6;remember(world,2,"Джон ушёл, оставив Кирилла у гаража с работающей сигнализацией.");return;}
                if(choice==2 && player.getInventory().stream().anyMatch(i->i.isRepairTool() && i.getDurability()>0)) {
                    world.advanceMinutes(5);kirill=2;toolInfo=true;remember(world,2,"Сигнализация отключена. Джон и Кирилл разделили инструменты.");
                    reward(player,scanner,Shop.createItem(19),Shop.createItem(42));return;
                }
                TacticalBattle.Civilian k=new TacticalBattle.Civilian("Кирилл",38,57);
                if(!fight(player,scanner,random,infected(2+random.nextInt(2)),"Защита Кирилла у гаража",Arrays.asList(k),false,false,false))return;
                kirill=k.person.isAlive()?2:5;
                remember(world,2,k.person.isAlive()?"Кирилл защищён от заражённых. Он доверяет Джону и готов обмениваться припасами.":"Кирилл не пережил нападение заражённых.");
                if(k.person.isAlive()) {toolInfo=true;reward(player,scanner,Shop.createItem(19),Shop.createItem(17));}return;
            }
            if(c==4)pause(TITLES[2],"После угроз Кирилл отказывается от совместных дел. Сначала нужно решить, кому достанется рюкзак.",scanner);
        }
    }
    void visitShopkeeper(Fighter player, WorldStories world, Scanner scanner, Random random) {
        update(world);
        if (viktor == 0) { viktor=1; remember(world,3,"За дверью подсобки скрывается Виктор Павлович. Он просит не шуметь и рассказывает о грабителях."); }
        viktor(player,world,scanner,random);
    }
    boolean helpViktor(Fighter player,WorldStories world,boolean trade) {
        if(viktor!=1 && viktor!=4)return false;
        if(!consume(player,ResidentialStories::food,trade?null:ResidentialStories::water))return false;
        GameIO.sound(GameSound.QUEST);viktor=trade?3:2;basementKnown=true;changeHumanity(trade?0:2);
        remember(world,3,trade?"Еда обменяна на сведения о подвале. Виктор остался нейтральным.":"Виктор Павлович получил воду и еду. Он рассказал о складе под магазином; дверь заклинило.");return true;
    }
    void basement(Fighter player,WorldStories world,Scanner scanner) {
        if(basementLoot) {pause("Подвал магазина","Подвал уже обыскан. Новых запасов здесь нет.",scanner);return;}
        GameIO.storyMenu("Подвал магазина",basementKey?"Ключ открывает служебный вход.":"Дверь за холодильным оборудованием заклинило. Нужен лом или подходящий инструмент.");
        GameIO.action(1,"Открыть подвал","15 минут",basementOpen || basementKey || viktor==2 || lever(player));GameIO.action(0,"Вернуться позже");
        if(SurvivalSystems.choice(scanner)!=1 || !basementOpen && !basementKey && viktor!=2 && !lever(player))return;
        GameIO.sound(GameSound.METAL_DOOR);basementOpen=true;basementLoot=true;world.advanceMinutes(15);remember(world,3,"Подвал открыт и обыскан. Повторно запасы не появятся.");
        reward(player,scanner,Shop.createItem(17),Shop.createItem(17),Shop.createItem(17),Shop.createItem(20),Shop.createItem(19),Shop.createItem(42));
    }
    void viktor(Fighter player,WorldStories world,Scanner scanner,Random random) {
        while(player.isAlive() && !aborted) {
            update(world);
            if(viktor==5 && !basementKey) {
                GameIO.storyMenu(TITLES[3],"Подсобка затихла. Виктор не пережил истощение. Рядом с ним лежит ключ от служебного входа в подвал.");
                GameIO.action(1,"Забрать ключ");GameIO.action(0,"Уйти");if(SurvivalSystems.choice(scanner)!=1)return;
                basementKey=true;basementKnown=true;remember(world,3,"После смерти Виктора найден ключ от подвала. Помощи человека уже не будет.");
            }
            GameIO.storyMenu(TITLES[3],"Виктор Павлович — бывший заведующий магазином.");
            GameIO.card("Подсобка",viktor==5?"Пустая подсобка. На полу осталось место от последней банки.":"За перевёрнутым стеллажом сидит худой пожилой мужчина с дрожащими руками. Рядом — последняя банка.\n«Ты не из тех, кто вчера приходил? Трое забрали всё. Сказали, что вернутся». ");
            boolean canHelp=viktor==1 || viktor==4;
            GameIO.action(1,"Отдать воду и еду","По одному предмету из рюкзака",canHelp && find(player,ResidentialStories::water)!=null && find(player,ResidentialStories::food)!=null);
            GameIO.action(2,"Обменять еду на информацию","1 порция еды",canHelp && find(player,ResidentialStories::food)!=null);
            GameIO.action(3,"Забрать последнюю банку","Виктор не может сопротивляться",viktor==1);
            GameIO.action(4,"Дождаться грабителей","3 игровых часа · возможен тяжёлый бой",viktor!=5 && !ambushUsed);
            if((viktor==2 || viktor==3 || viktor==6) && !viktorSignalTalk)GameIO.action(8,"Куда забрали припасы магазина?");

            GameIO.action(0,"Уйти");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==8 && (viktor==2 || viktor==3 || viktor==6) && !viktorSignalTalk){signalTalk(3,world,scanner);continue;}
            if(c==1 || c==2) {if(helpViktor(player,world,c==2))world.advanceMinutes(10);continue;}
            if(c==3 && viktor==1) {
                viktor=4;viktorDue=world.day()+3;changeHumanity(-3);remember(world,3,"Джон забрал последнюю банку. Виктор остался без еды; ещё можно принести ему помощь.");
                pause(TITLES[3],"Старик смотрит на пустое место перед собой. Он ничего не говорит.",scanner);reward(player,scanner,Shop.createItem(17));continue;
            }

            if(c==4 && !ambushUsed && viktor!=5) {
                ambushUsed=true;world.advanceMinutes(180);int roll=random.nextInt(3);
                if(roll==0){remember(world,3,"Три часа ожидания прошли впустую. Грабители не появились.");pause(TITLES[3],"За три часа никто не пришёл. Джон теряет время, но Виктор всё ещё рядом.",scanner);continue;}
                int count=roll==1?2:3;GameIO.storyMenu("Грабители вернулись",count==2?"У входа двое выживших. Один требует, чтобы Джон отошёл от подсобки.":"Трое вооружённых людей перекрыли выход. Это очень опасные противники.");
                GameIO.action(1,"Потребовать оставить старика","Переговоры",count==2);
                GameIO.action(2,"Предложить две порции еды","Откупиться",player.getInventory().stream().filter(ResidentialStories::food).count()>=2);
                GameIO.action(3,"Атаковать");GameIO.action(0,"Отступить из магазина");int choice=SurvivalSystems.choice(scanner);
                if(choice==0){aborted=true;return;}
                if(choice==2 && consume(player,ResidentialStories::food,ResidentialStories::food) || choice==1 && count==2 && random.nextInt(100)<60) {
                    viktor=6;basementKnown=true;remember(world,3,"Грабители ушли без боя. Виктор указал скрытый подвал.");continue;
                }
                List<CombatEnemy> enemies=new ArrayList<>();for(int i=1;i<=count;i++)enemies.add(new HostileSurvivor(i,count==3,random));
                if(!fight(player,scanner,random,enemies,"Стеллажи продуктового магазина",Collections.emptyList(),false,false,false))return;
                viktor=6;basementKnown=true;changeReputation(1);remember(world,3,"Мародёры побеждены. Виктор видел, как Джон защитил магазин, и раскрыл проход в подвал.");
            }
        }
    }
    void onVisit(int place,WorldStories world,Fighter player) {
        update(world);
        if(place==1 && kirillSupport) {
            kirillSupport=false;player.heal(15);player.restoreStamina(20);
            remember(world,2,"Кирилл встретил Джона у двора: помог перевязать раны и отдохнуть (+до 15 здоровья и до 20 сил). Обещанная помощь получена.");
        }
    }
    int sleepPenalty(WorldStories world) {
        if(nightmareUntil<world.day() || family!=5)return 0;
        if(!dreamSeen) {dreamSeen=true;remember(world,1,"«Мне приснился третий этаж. Я снова стоял перед дверью. На этот раз никто не открывал». Воспоминание ухудшает отдых на несколько ночей.");}
        return 2;
    }
    boolean valid() {return humanity>=-100 && humanity<=100 && reputation>=-100 && reputation<=100
            && family>=0 && family<=8 && kirill>=0 && kirill<=7 && viktor>=0 && viktor<=6
            && kirillSuspicion>=0 && kirillSuspicion<=3 && kirillTradeStock>=0 && kirillTradeStock<=3
            && familyDay>=0 && familyDue>=0 && kirillDue>=0 && viktorDue>=0 && nightmareUntil>=0;}
}
