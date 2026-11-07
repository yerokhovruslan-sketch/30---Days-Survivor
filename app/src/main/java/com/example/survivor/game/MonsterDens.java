package com.example.survivor.game;
import java.io.Serializable;
import java.util.*;

/** Stable optional nests: one fixed group, persisted until defeated and looted. */
final class MonsterDens implements Serializable {
    private static final long serialVersionUID=1L;
    static final int NONE=0,NORMAL=1,RUNNER=2,JUMPER=3;
    static final class Den implements Serializable {
        private static final long serialVersionUID=1L;
        final int type,count,valuableProduct;
        boolean cleared;
        final List<ItemInfo> rewards=new ArrayList<>();
        Den(int type,int count,int product){this.type=type;this.count=count;this.valuableProduct=product;}
        boolean active(){return type!=NONE&&!cleared;}
        boolean accessible(){return type!=NONE&&(!cleared||!rewards.isEmpty());}
        ZombieType zombieType(){return type==RUNNER?ZombieType.RUNNER:type==JUMPER?ZombieType.JUMPER:ZombieType.NORMAL;}
        String label(){return type==RUNNER?"Логово бегунов":type==JUMPER?"Логово прыгунов":"Логово гниляков";}
        String terrain(){return "Логово: "+type;}
    }
    private long seed;
    private Map<Integer,Den> dens;
    private Map<Integer,Den> entries(){if(dens==null)dens=new HashMap<>();return dens;}
    private long seed(){if(seed==0){seed=new Random().nextLong();if(seed==0)seed=1;}return seed;}
    static boolean eligible(int number,Location place){return place.getDifficulty()>=2&&(number==14||number==17||number==18||number>=19);}
    static Den roll(Location place,int stage,Random random){
        if(random.nextInt(100)>=30)return new Den(NONE,0,0);
        boolean veryHard=place.getDifficulty()>=3&&(stage>=8||place.isNightOnly());
        int type=veryHard?1+random.nextInt(3):NORMAL;
        int count=type==NORMAL?6+random.nextInt(3):4+random.nextInt(3);
        int[] valuable={8,9,10,29,31,50,51};int product=valuable[random.nextInt(valuable.length)];
        return new Den(type,count,product);
    }
    Den ensure(Locations world,int number){
        Location place=world.getLocation(number);if(!eligible(number,place))return null;
        Map<Integer,Den> map=entries();Den den=map.get(number);
        if(den==null){den=roll(place,world.explorationStage(number),new Random(seed() ^ (0x9e3779b97f4a7c15L*number)));map.put(number,den);GameIO.checkpoint();}
        return den.type==NONE?null:den;
    }
    Den existing(int number){Den den=entries().get(number);return den==null||den.type==NONE?null:den;}
    boolean active(int number){Den den=existing(number);return den!=null&&den.active();}
    boolean accessible(int number){Den den=existing(number);return den!=null&&den.accessible();}
    void complete(int place,TacticalBattle battle,Random random){
        Den den=existing(place);if(den==null||den.cleared)return;
        den.cleared=true;
        for(TacticalBattle.Unit unit:battle.units)if(unit.enemy instanceof Zombie&&!unit.enemy.isAlive())den.rewards.addAll(ZombieEar.roll(((Zombie)unit.enemy).getType(),random));
        ItemInfo valuable=Shop.createItem(den.valuableProduct);if(valuable!=null)den.rewards.add(valuable);
        GameIO.out().println("Логово уничтожено. Усиленные встречи и более ценная случайная добыча здесь прекратились; трофеи остались в логове.");GameIO.checkpoint();
    }
    boolean visit(Fighter p,WorldStories w,int place,Scanner scanner,Random random){
        Den den=existing(place);if(den==null)return true;
        while(p.isAlive()){
            GameIO.menu(den.label(),den.cleared?"Заражённые уничтожены. Оставленные трофеи можно забрать сейчас или позже.":"Внутри "+den.count+" заражённых одного вида. В это логово не приходит новое подкрепление, даже на выстрелы. Можно отступить и вернуться.");
            if(!den.cleared)GameIO.action(1,"Войти в логово","Опасный бой · "+den.count+" заражённых");
            else for(int i=0;i<den.rewards.size();i++)GameIO.item(10+i,den.rewards.get(i),"Забрать: ",true);
            GameIO.action(0,"Отойти");int choice=SurvivalSystems.choice(scanner);if(choice==0)return true;
            if(choice==1&&!den.cleared){if(!TacticalBattle.den(p,scanner,random,den,w,place))return false;}
            if(den.cleared&&choice>=10&&choice<10+den.rewards.size()){
                ItemInfo item=den.rewards.get(choice-10);if(!p.addItem(item)){GameIO.out().println("В рюкзаке нет места. Трофей останется здесь.");continue;}
                den.rewards.remove(choice-10);GameIO.sound(GameSound.BAG);GameIO.out().println("Получено: "+item.getName());GameIO.checkpoint();
            }
        }return false;
    }
}
