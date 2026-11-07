package com.example.survivor.game;

import java.util.*;

/** Turn-based encounter model. All mutations run on the session worker, never the UI thread. */
final class TacticalBattle implements java.io.Serializable {
    private static final long serialVersionUID=1L;
    static final class Civilian implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        final SurvivorInfo person;
        int cell;
        boolean safe;
        Civilian(String name,int health,int cell) {person=new SurvivorInfo(name,"Сопровождаемый",health);this.cell=cell;}
    }
    final List<Civilian> civilians=new ArrayList<>();
    boolean escortToExit,allowSurrender;
    static final int N=8, CELL=100;
    enum Obstacle {
        CRATE(true,true), BARREL(true,true), ROCK(false,true), TREE(false,true), WALL(false,true), SANDBAGS(false,true),
        ROADBLOCK(false,true), FENCE(true,false), GATE(true,false), PALLETS(true,true), CRATES(true,true), PIPE(false,true),
        CAR(false,true), BUS(false,true), RUBBLE(true,true), BRICK_WALL(false,true), HEDGEHOG(false,true), BLOCK(false,true),
        FALLEN_TREE(true,true), STUMP(true,true), TRASH(true,true), BARRICADE(true,true), BUSH(true,false), CABLE(true,true),
        CHAIR(true,true), TABLE(true,true), BED(false,true), SOFA(false,true), WARDROBE(false,true), BOOKCASE(false,true), STOVE(false,true), FRIDGE(false,true), SINK(false,true), TOILET(false,true),
        ARMCHAIR(false,true), ROOM_CRATE(true,true), ROOM_BARREL(true,true), LOCKER(false,true), WORKBENCH(false,true), ROOM_SHELF(false,true), ROOM_WALL(false,true);
        final boolean light, solid;
        Obstacle(boolean light, boolean solid) { this.light=light; this.solid=solid; }
    }
    private static final java.util.concurrent.atomic.AtomicInteger VISUAL_ID=new java.util.concurrent.atomic.AtomicInteger(1);
    static final class Unit implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        final int visualId=VISUAL_ID.getAndIncrement();
        final CombatEnemy enemy;
        boolean alerted;int lastKnown=-1;
        int cell, skip, jump=-1, screamAt=-1, shots, bleed, slow;
        boolean fled, dropped;
        Unit(CombatEnemy enemy,int cell) { this.enemy=enemy; this.cell=cell; }
        boolean active() { return enemy.isAlive() && !fled; }
        ZombieType type() { return enemy instanceof Zombie ? ((Zombie)enemy).getType() : null; }
        int sprite() {
            if (enemy instanceof HostileSurvivor) return ((HostileSurvivor)enemy).isRaider()?2:1;
            return switch(type()) { case NORMAL->3; case RUNNER,HUNTER->4; case JUMPER->5; case SCREAMER->6; case BRUTE->7; };
        }
    }
    private long effectId;
    private transient List<BattleBoard.Effect> effects=new ArrayList<>();
    void visual(String kind,int cell,int amount){effects.add(new BattleBoard.Effect(++effectId,cell,amount,kind));if(effects.size()>32)effects.remove(0);}
    final Fighter player;
    transient Random random;
    final Obstacle[] obstacles=new Obstacle[64];
    private int[] obstacleAnchors = new int[64];
    private boolean[] obstacleVertical = new boolean[64];
    final List<Unit> units=new ArrayList<>();
    final Map<Integer,List<ItemInfo>> ground=new HashMap<>();
    final Map<Weapons,Integer> magazines=new IdentityHashMap<>();
    final List<String> log=new ArrayList<>();
    String terrain;
    int playerCell, turn=1, selected, spawned;
    boolean moved, guarded, escaped;
    boolean aimed;
    boolean fixedEncounter,scriptedEncounter,denEncounter;
    boolean woodFloor,grassFloor;
    String layoutProfile="";
    int layoutPrimary=-1;
    boolean layoutHasSecondary;
    int lastActionNoise;
    final Map<Integer,String> markers=new HashMap<>();
    int selectedObstacle=-1;
    final int[] obstacleDamage=new int[64];
    final EnumMap<AmmoType,Integer> spentCases=new EnumMap<>(AmmoType.class);
    private boolean casesDeposited;
    void casingShot(WeaponStyle style) {
        AmmoType type=switch(style){case PISTOL->AmmoType.PISTOL;case SHOTGUN->AmmoType.SHELL;case CARBINE->AmmoType.RIFLE;default->null;};
        if(type!=null)spentCases.merge(type,1,Integer::sum);
    }
    void depositCases() {
        if(casesDeposited || GameIO.clock()==null)return;casesDeposited=true;
        for(Map.Entry<AmmoType,Integer> entry:spentCases.entrySet())GameIO.clock().addCases(GameIO.clock().currentPlace(),entry.getKey(),entry.getValue());
    }

    private void readObject(java.io.ObjectInputStream input) throws java.io.IOException, ClassNotFoundException {
        input.defaultReadObject();random=new Random();effects=new ArrayList<>();
    }
    void prepareReturn() {
        if(casesDeposited){spentCases.clear();casesDeposited=false;}
        int best=-1, distance=-1;
        for(int cell=0;cell<64;cell++){
            if(!edge(cell) || obstacles[cell]!=null || unitAt(cell)!=null)continue;
            int nearest=64;for(Unit unit:units)if(unit.active())nearest=Math.min(nearest,distance(cell,unit.cell));
            if(nearest>distance){distance=nearest;best=cell;}
        }
        if(best>=0)playerCell=best;
        escaped=false;moved=false;guarded=false;aimed=false;selectedObstacle=-1;
        log.add("Джон возвращается к прежней схватке. Противники остались на своих местах.");
    }
    static boolean resumePending(WorldStories world, Fighter player, Scanner scanner) {
        TacticalBattle battle=world.pendingBattle(world.currentPlace());
        if(battle==null)return true;
        battle.prepareReturn();GameIO.out().println("На месте остаются противники, от которых вы отступили.");
        boolean won=false;
        try { won=run(battle,scanner,false,false); return won; }
        finally {
            battle.depositCases();
            if(won){world.clearPendingBattle(world.currentPlace());if(battle.denEncounter)world.monsterDens().complete(world.currentPlace(),battle,battle.random);else if(battle.scriptedEncounter && !battle.terrain.equals("Шум вскрытия"))world.rememberResolvedBattle(world.currentPlace(),battle);}
            else if(!player.isAlive())world.clearPendingBattle(world.currentPlace());
            GameIO.clearBattle();GameIO.checkpoint();
        }
    }
    private static void rememberEscape(TacticalBattle battle) {
        WorldStories world=GameIO.clock();
        if(world!=null && battle.escaped && battle.player.isAlive()) {
            world.rememberBattle(world.currentPlace(),battle);GameIO.checkpoint();
        }
    }

    TacticalBattle(Fighter player,int difficulty,Random random,List<CombatEnemy> enemies) {
        this.player=player; this.random=random;
        int place=GameIO.clock()==null?0:GameIO.clock().currentPlace();
        int area=areaForPlace(place,random.nextInt(6));
        grassFloor=isGrassland(place,"");
        terrain=new String[]{"Жилой квартал","Промышленная зона","Лес","Дорога","Военный периметр","Склад"}[area];
        playerCell=random.nextBoolean()?56+random.nextInt(8):random.nextInt(8)*8;
        if(!isWoodInterior(place,""))BattleLayout.generate(this,area,enemies.size());
        if(isWoodInterior(place,""))buildRoom("");
        for(CombatEnemy enemy:enemies) {
            if(units.size()>=6) break;
            int cell=spawnCell(false); if(cell>=0) units.add(new Unit(enemy,cell));
        }
        int desired=Math.min(6, Math.max(units.size(),1+random.nextInt(Math.max(1,difficulty))+player.getNoise()/20));
        while(units.size()<desired) {
            int cell=spawnCell(false); if(cell<0) break;
            units.add(new Unit(Survivor.createZombie(units.size()+1,random),cell));
        }
        log.add("Нажмите клетку для движения, врага — для выбора цели. После движения выполните действие.");
    }

    static boolean isGrassland(int place,String context){
        String t=context==null?"":context.toLowerCase(Locale.ROOT);
        if(t.contains("лес")||t.contains("озер")||t.contains("озёр")||t.contains("опушк")||t.contains("болот")||t.contains("кемпинг"))return true;
        return place==2||place==19||place>=21&&place<=26;
    }
    static int areaForPlace(int place,int fallback){
        if(place==0)return fallback;
        if(isGrassland(place,""))return 2;
        if(place>=33&&place<=40)return 3;
        if(place>=41&&place<=44)return 1;
        if(place>=45&&place<=50)return 4;
        return 0;
    }
    static boolean isWoodInterior(int place,String context){
        String t=context==null?"":context.toLowerCase(Locale.ROOT);
        if(t.contains("двор")||t.contains("ворот")||t.contains("улиц")||t.contains("площадк")||t.contains("крыш")||t.contains("автомобил")||t.contains("гараж"))return false;
        if(t.contains("квартир")||t.contains("лестнич")||t.contains("жилой дом")||t.contains("избуш")||t.contains("комнат")||t.contains("кухн")||t.contains("учебный класс")||t.contains("мастерск"))return true;
        return place==5||place==20||place==29||place==34||place==46;
    }
    void buildRoom(String context){
        woodFloor=true;grassFloor=false;Arrays.fill(obstacles,null);if(obstacleAnchors!=null)Arrays.fill(obstacleAnchors,0);if(obstacleVertical!=null)Arrays.fill(obstacleVertical,false);
        String t=context==null?"":context.toLowerCase(Locale.ROOT);
        int type=t.contains("кухн")||t.contains("столов")||t.contains("кафе")?2:t.contains("ванн")||t.contains("туалет")?3:t.contains("мастерск")?4:Math.floorMod(random.nextInt(),3);
        Obstacle[][] furnishings={
            {Obstacle.BED,Obstacle.WARDROBE,Obstacle.BOOKCASE,Obstacle.TABLE,Obstacle.CHAIR,Obstacle.ROOM_CRATE},
            {Obstacle.SOFA,Obstacle.BOOKCASE,Obstacle.WARDROBE,Obstacle.TABLE,Obstacle.ARMCHAIR,Obstacle.ROOM_BARREL},
            {Obstacle.FRIDGE,Obstacle.STOVE,Obstacle.SINK,Obstacle.TABLE,Obstacle.CHAIR,Obstacle.ROOM_SHELF},
            {Obstacle.TOILET,Obstacle.SINK,Obstacle.WARDROBE,Obstacle.TABLE,Obstacle.CHAIR,Obstacle.LOCKER},
            {Obstacle.ROOM_SHELF,Obstacle.WORKBENCH,Obstacle.LOCKER,Obstacle.ROOM_CRATE,Obstacle.CHAIR,Obstacle.ROOM_BARREL}
        };
        int[] cells={1,3,5,27,28,15};
        for(int i=0;i<cells.length;i++){
            int cell=cells[i];if(unitAt(cell)!=null||civilianAt(cell)!=null||markers.containsKey(cell))continue;
            if(placeObstacle(cell,furnishings[type][i])&&!connected())removeObstacle(cell);
        }
        // A wall divides the room but leaves a two-cell doorway at 35 and 36.
        for(int cell:new int[]{32,33,34,37,38,39}){
            if(unitAt(cell)!=null||civilianAt(cell)!=null||markers.containsKey(cell))continue;
            if(placeObstacle(cell,Obstacle.ROOM_WALL)&&!connected())removeObstacle(cell);
        }
        int count=0;for(Obstacle obstacle:obstacles)if(obstacle!=null)count++;
        List<Integer> spare=new ArrayList<>();for(int i=0;i<64;i++)spare.add(i);
        Collections.shuffle(spare,random);
        for(int cell:spare){
            if(count>=7)break;
            if(placeObstacle(cell,Obstacle.ROOM_CRATE)){if(connected())count++;else removeObstacle(cell);}
        }
    }

    void clearObstacles(){Arrays.fill(obstacles,null);if(obstacleAnchors!=null)Arrays.fill(obstacleAnchors,0);if(obstacleVertical!=null)Arrays.fill(obstacleVertical,false);}
    boolean obstacleIsVertical(int cell){return obstacleVertical!=null&&obstacleVertical[cell];}
    int obstacleSpan(int cell){
        if(obstacles[cell]==null||obstacleAnchors==null||obstacleAnchors[cell]==0)return 1;
        int anchor=obstacleAnchors[cell]-1,span=obstacles[cell]==Obstacle.CAR?2:obstacles[cell]==Obstacle.BUS?4:1;
        boolean vertical=obstacleIsVertical(cell);int step=vertical?8:1;
        if(anchor<0||anchor+step*(span-1)>=64||!vertical&&anchor/8!=(anchor+span-1)/8)return 1;
        for(int i=0;i<span;i++)if(obstacles[anchor+i*step]!=obstacles[cell]||obstacleAnchors[anchor+i*step]!=anchor+1||obstacleIsVertical(anchor+i*step)!=vertical)return 1;
        return span;
    }
    int obstaclePart(int cell){return obstacleSpan(cell)>1?(cell-(obstacleAnchors[cell]-1))/(obstacleIsVertical(cell)?8:1):0;}
    boolean placeObstacle(int cell,Obstacle obstacle){return placeObstacle(cell,obstacle,false);}
    boolean placeObstacle(int cell,Obstacle obstacle,boolean vertical){
        int span=obstacle==Obstacle.CAR?2:obstacle==Obstacle.BUS?4:1,step=vertical?8:1;
        if(cell<0||cell+step*(span-1)>=64||!vertical&&cell/8!=(cell+span-1)/8)return false;
        for(int i=0;i<span;i++){int c=cell+i*step;if(obstacles[c]!=null||c==playerCell||distance(c,playerCell)<2||unitAt(c)!=null||civilianAt(c)!=null||markers.containsKey(c))return false;}
        if(obstacleAnchors==null)obstacleAnchors=new int[64];if(obstacleVertical==null)obstacleVertical=new boolean[64];
        for(int i=0;i<span;i++){int c=cell+i*step;obstacles[c]=obstacle;obstacleAnchors[c]=span>1?cell+1:0;obstacleVertical[c]=span>1&&vertical;}
        return true;
    }
    void removeObstacle(int cell){
        int span=obstacleSpan(cell),step=obstacleIsVertical(cell)?8:1,start=cell-obstaclePart(cell)*step;
        for(int i=0;i<span;i++){int c=start+i*step;obstacles[c]=null;if(obstacleAnchors!=null)obstacleAnchors[c]=0;if(obstacleVertical!=null)obstacleVertical[c]=false;}
    }

    static TacticalBattle createDenBattle(Fighter player,Random random,MonsterDens.Den den) {
        List<CombatEnemy> enemies=new ArrayList<>();for(int i=1;i<=den.count;i++)enemies.add(new Zombie(i,den.zombieType()));
        player.reduceNoise(60);TacticalBattle battle=new TacticalBattle(player,1,random,enemies);
        battle.denEncounter=true;battle.scriptedEncounter=true;battle.terrain=den.terrain();
        for(int i=battle.units.size();i<enemies.size();i++){
            int cell=battle.spawnCell(false);
            if(cell<0)for(int candidate=0;candidate<64;candidate++)if(battle.free(candidate)){cell=candidate;break;}
            if(cell<0)throw new IllegalStateException("Нет свободной клетки для заражённого в логове");
            battle.units.add(new Unit(enemies.get(i),cell));
        }
        return battle;
    }
    static boolean den(Fighter player,Scanner scanner,Random random,MonsterDens.Den den,WorldStories world,int place){
        TacticalBattle battle=createDenBattle(player,random,den);boolean won=false;
        try{won=run(battle,scanner,false,false);if(won)world.monsterDens().complete(place,battle,random);return won;}
        finally{battle.depositCases();rememberEscape(battle);player.reduceNoise(60);GameIO.clearBattle();}
    }
    static boolean play(Fighter player,Scanner scanner,int difficulty,Random random,List<CombatEnemy> enemies,boolean enemiesFirst) {
        TacticalBattle battle=new TacticalBattle(player,difficulty,random,enemies);
        try{return run(battle,scanner,enemiesFirst,true);}finally{battle.depositCases();rememberEscape(battle);}
    }
    static boolean story(Fighter player,Scanner scanner,Random random,List<CombatEnemy> enemies,String terrain,
                         List<Civilian> civilians,boolean escort,boolean surrender,boolean enemiesFirst) {
        WorldStories savedWorld=GameIO.clock();
        TacticalBattle resolved=savedWorld==null?null:savedWorld.consumeResolvedBattle(savedWorld.currentPlace(),terrain);
        if(resolved!=null){
            for(int i=0;i<Math.min(enemies.size(),resolved.units.size());i++){
                CombatEnemy target=enemies.get(i),old=resolved.units.get(i).enemy;
                if(target.getHealth()>old.getHealth())target.takeDamage(target.getHealth()-old.getHealth());
            }
            for(int i=0;i<Math.min(civilians.size(),resolved.civilians.size());i++){
                Civilian target=civilians.get(i),old=resolved.civilians.get(i);
                if(target.person.getHealth()>old.person.getHealth())target.person.takeDamage(target.person.getHealth()-old.person.getHealth());
                target.cell=old.cell;target.safe=old.safe;
            }
            return true;
        }
        player.reduceNoise(60);
        TacticalBattle battle=new TacticalBattle(player,1,random,enemies);
        battle.scriptedEncounter=true;battle.terrain=terrain;battle.playerCell=56;battle.escortToExit=escort;battle.allowSurrender=surrender;
        battle.civilians.addAll(civilians);
        for(int i=0;i<battle.units.size();i++)battle.units.get(i).cell=new int[]{10,13,22,5,7,15}[i];
        int location=savedWorld==null?0:savedWorld.currentPlace();
        battle.woodFloor=false;battle.grassFloor=isGrassland(location,terrain);
        if(isWoodInterior(location,terrain))battle.buildRoom(terrain);
        else BattleLayout.generate(battle,areaForPlace(location,battle.grassFloor?2:0),battle.units.size());
        try{return run(battle,scanner,enemiesFirst,false);}
        finally {battle.depositCases();rememberEscape(battle);player.reduceNoise(60);GameIO.clearBattle();}
    }
    private static boolean run(TacticalBattle battle,Scanner scanner,boolean enemiesFirst,boolean returnTravel) {
        Fighter player=battle.player;
        if(enemiesFirst) { battle.log.add("Вас застали врасплох: враги ходят первыми."); battle.enemyTurn(); }
        while(player.isAlive() && !battle.escaped) {
            battle.dropDead();
            battle.render();
            if(!scanner.hasNextInt()) { if(scanner.hasNext()) { scanner.next(); continue; } return false; }
            int action=scanner.nextInt();
            if(action==0 && battle.won()) return true;
            battle.act(action,scanner);
        }
        for(String line:battle.log) GameIO.out().println(line);
        if(returnTravel && battle.escaped && player.isAlive()) Survivor.travel(player,"Возвращение после отступления",5);
        return false;
    }

    void act(int action,Scanner scanner) {
            lastActionNoise=0;
            if(action>=CELL && action<CELL+64) {
                int cell=action-CELL;
                Unit target=this.unitAt(cell);
                if(target!=null) { this.selected=this.units.indexOf(target);this.selectedObstacle=-1; return; }
                if(this.obstacles[cell]!=null) {this.selectedObstacle=cell;return;}
                this.move(cell); return;
            }
            boolean used=false;
            switch(action) {
                case 1 -> used=this.attack();
                case 2 -> { this.guarded=true; player.restoreStamina(15); this.log.add("Оборона: урон −60% до следующего хода. Вы переводите дыхание."); used=true; }
                case 3 -> used=this.items(scanner);
                case 4 -> { used=Survivor.chooseEquipment(player,scanner); if(used) {this.aimed=false;this.log.add("Оружие сменено.");} }
                case 5 -> used=this.reload();
                case 6 -> used=this.pickup(scanner);
                case 7 -> used=this.escape();
                case 8 -> { player.restoreStamina(8); used=true; }
                case 10 -> used=this.aim();
                case 11 -> used=this.destroy();
                case 12 -> used=this.extractCases();
                case 9 -> {
                    Unit target=this.target();
                    if(this.allowSurrender && target!=null && target.enemy instanceof HostileSurvivor
                            && target.enemy.getHealth()*100/target.enemy.getMaxHealth()<25) {
                        target.fled=true;this.log.add("Джон опустил оружие. "+target.enemy.getName()+" ушёл живым.");used=true;
                    }
                }
                default -> { }
            }
            if(used) this.endTurn();
    }
    static int distance(int a,int b) { return Math.abs(a/8-b/8)+Math.abs(a%8-b%8); }
    static boolean edge(int cell) { return cell/8==0 || cell/8==7 || cell%8==0 || cell%8==7; }
    static int[] neighbours(int cell) {
        int[] data=new int[4]; int size=0;
        if(cell/8>0)data[size++]=cell-8; if(cell/8<7)data[size++]=cell+8;
        if(cell%8>0)data[size++]=cell-1; if(cell%8<7)data[size++]=cell+1;
        return Arrays.copyOf(data,size);
    }
    Unit unitAt(int cell) { for(Unit u:units) if(u.active() && u.cell==cell)return u; return null; }
    Civilian civilianAt(int cell) {for(Civilian c:civilians)if(c.person.isAlive() && !c.safe && c.cell==cell)return c;return null;}
    boolean free(int cell) { return cell>=0 && cell<64 && cell!=playerCell && obstacles[cell]==null && unitAt(cell)==null && civilianAt(cell)==null; }
    boolean connected() {
        boolean[] seen=new boolean[64]; ArrayDeque<Integer> queue=new ArrayDeque<>(); queue.add(playerCell);seen[playerCell]=true;
        while(!queue.isEmpty()) for(int n:neighbours(queue.remove())) if(!seen[n] && obstacles[n]==null) { seen[n]=true;queue.add(n); }
        for(int i=0;i<64;i++)if(obstacles[i]==null && !seen[i])return false;
        return true;
    }
    int spawnCell(boolean edgeOnly) {
        List<Integer> possible=new ArrayList<>();
        for(int i=0;i<64;i++) if(free(i) && distance(i,playerCell)>=4 && (!edgeOnly || edge(i))) possible.add(i);
        return possible.isEmpty()?-1:possible.get(random.nextInt(possible.size()));
    }
    int moveLimit() {
        return player.getCarryWeight()>Fighter.COMFORT_WEIGHT || player.getHunger()>=60
                || player.getSleepiness()>=60 || player.getStamina()<20 ? 1:2;
    }
    boolean canMove(int cell) {
        if(moved && (!won() || fixedEncounter) || cell<0 || cell>=64 || !free(cell)) return false;
        int d=distance(cell,playerCell);
        if(d<1 || d>moveLimit() || cell/8!=playerCell/8 && cell%8!=playerCell%8) return false;
        int step=cell/8==playerCell/8?Integer.signum(cell-playerCell):8*Integer.signum(cell-playerCell);
        for(int p=playerCell+step;p!=cell;p+=step) if(!free(p))return false;
        return player.getStamina()>=player.staminaCost(d*3);
    }
    boolean move(int cell) {
        if(!canMove(cell))return false;
        int d=distance(cell,playerCell);
        if(!player.spendStamina(d*3))return false;
        playerCell=cell;moved=true;aimed=false;selectedObstacle=-1;
        log.add("Джон → "+coordinate(cell)+". Теперь выберите действие.");
        return true;
    }
    static String coordinate(int cell) { return "АБВГДЕЖЗ".charAt(cell%8)+Integer.toString(cell/8+1); }
    /** Supercover ray: even a corner-touching solid obstacle blocks a shot. */
    List<Integer> ray(int from,int to) {
        List<Integer> result=new ArrayList<>();
        int x=from%8,y=from/8,tx=to%8,ty=to/8,dx=Math.abs(tx-x),dy=Math.abs(ty-y);
        int sx=Integer.signum(tx-x),sy=Integer.signum(ty-y),ix=0,iy=0;
        while(ix<dx || iy<dy) {
            int test=(1+2*ix)*dy-(1+2*iy)*dx;
            if(test==0) {
                if(x+sx>=0 && x+sx<8) result.add(y*8+x+sx);
                if(y+sy>=0 && y+sy<8) result.add((y+sy)*8+x);
                x+=sx;y+=sy;ix++;iy++;
            } else if(test<0) { x+=sx;ix++; } else { y+=sy;iy++; }
            result.add(y*8+x);
        }
        result.removeIf(c->c==from || c==to);
        return result;
    }
    boolean clearShot(int from,int to) {
        for(int c:ray(from,to)) if(obstacles[c]!=null && obstacles[c].solid)return false;
        return true;
    }
    int shotPenalty(int from,int to) {
        int penalty=0;
        for(int c:ray(from,to)) if(obstacles[c]!=null && !obstacles[c].solid)penalty+=20;
        // A nearby obstacle protects from distant fire only, not point-blank attacks.
        if(distance(from,to)>1) for(int c:neighbours(to)) if(obstacles[c]!=null && distance(from,c)<distance(from,to)) {penalty+=20;break;}
        return Math.min(65,penalty);
    }
    Unit target() {
        if(selected>=0 && selected<units.size() && units.get(selected).active()) return units.get(selected);
        for(int i=0;i<units.size();i++)if(units.get(i).active()) {selected=i;return units.get(i);}
        return null;
    }
    int range() {
        Weapons weapon=player.getEquippedWeapon();
        if(weapon==null || weapon.getDurability()==0)return 1;
        return weapon.getStyle().range();
    }
    static int capacity(Weapons weapon) { return weapon.getStyle().capacity(); }
    int loaded() {
        Weapons w=player.getEquippedWeapon();
        if(w==null || w.getAmmoType()==null)return 0;
        return Math.min(magazines.computeIfAbsent(w,key->Math.min(capacity(key)-player.heldCases(key),player.getAmmoCount(key.getAmmoType()))),player.getAmmoCount(w.getAmmoType()));
    }
    boolean canAttack() {
        Unit target=target();
        if(selectedObstacle>=0 || target==null || distance(playerCell,target.cell)>range() || !clearShot(playerCell,target.cell))return false;
        Weapons w=player.getEquippedWeapon();
        return (w==null || w.getDurability()>0) && (w==null || w.getAmmoType()==null || loaded()>0)
                && player.getStamina()>=player.staminaCost(player.getWeaponStyle().staminaCost);
    }
    boolean attack() {
        if(!canAttack()) {log.add("Цель вне досягаемости, линия огня закрыта, оружие сломано или нужна перезарядка/передышка.");return false;}
        Unit target=target(); Weapons weapon=player.getEquippedWeapon();
        boolean ranged=weapon!=null && weapon.getAmmoType()!=null;
        int load=loaded();
        WeaponStyle style=player.getWeaponStyle();
        int d=distance(playerCell,target.cell);
        int penalty=(ranged?shotPenalty(playerCell,target.cell):0)+player.getOverloadPercent()/5;
        if(ranged) {
            penalty+=moved?15:0;
            if(GameIO.clock()!=null && GameIO.clock().isNight())penalty+=player.isFlashlightOn()?8:20;
            if(player.getStamina()<20)penalty+=10;
            if(aimed)penalty-=15;
        }
        int damage=Math.max(60,100-player.getOverloadPercent()/5);
        if(style==WeaponStyle.SHOTGUN)damage=damage*new int[]{100,100,85,65,45}[Math.min(4,d)]/100;
        if(style==WeaponStyle.LONG && d==1)damage=damage*75/100;
        if(style==WeaponStyle.HUNTING_KNIFE && target.enemy.armorProtection()>=20)damage=damage*75/100;
        AttackResult result=Survivor.attack(player,target.enemy,random,penalty,damage);
        if(result.exhausted || result.outOfAmmo || result.brokenWeapon)return false;
        if(result.hits==0)visual("miss",target.cell,0);else visual("damage",target.cell,result.damage);
        lastActionNoise=style.noise();
        if(ranged)magazines.put(weapon,Math.max(0,load-1));
        if(ranged)casingShot(style);
        if(ranged)aimed=false;
        if(result.hits>0 && target.active() && style.bleedChance()>0 && random.nextInt(100)<style.bleedChance()) {
            if(target.enemy instanceof Zombie) {target.slow=2;target.jump=-1;log.add("Повреждена подвижность: заражённый замедлен на 2 хода; кровопотери нет.");}
            else {target.bleed=3;log.add("Кровотечение: 4 HP в течение 3 ходов. Повторное попадание обновляет длительность.");}
        }
        if(result.stunned || result.keptDistance)target.skip=1;
        if(result.stunned)visual("stun",target.cell,0);
        log.add("Джон атакует "+target.enemy.getName()+": "+(result.hits==0?"промах":"врагу −"+result.damage+" HP")
                +(result.stunned?" · оглушён":"")+(result.keptDistance?" · удержан на расстоянии":""));
        dropDead();return true;
    }
    boolean reload() {
        Weapons weapon=player.getEquippedWeapon();
        if(weapon==null || weapon.getAmmoType()==null || player.getAmmoCount(weapon.getAmmoType())<=0)return false;
        int available=Math.min(capacity(weapon),player.getAmmoCount(weapon.getAmmoType()));
        if(loaded()>=available)return false;
        extractCases();magazines.put(weapon,available);GameIO.sound(GameSound.RELOAD);log.add("Перезаряжено: "+available+". Ход потрачен.");return true;
    }
    boolean extractCases() {
        Weapons weapon=player.getEquippedWeapon();if(weapon==null || player.heldCases(weapon)==0)return false;
        int count=player.releaseCases(weapon);
        for(int i=0;i<count;i++) {
            ItemInfo casing=Crafting.component(Crafting.casing(AmmoType.REVOLVER));
            if(!player.addItem(casing))ground.computeIfAbsent(playerCell,key->new ArrayList<>()).add(casing);
        }
        log.add("Из барабана извлечено гильз: "+count+". Не поместившиеся оставлены на клетке Джона.");return true;
    }
    boolean aim() {
        if(player.getEquippedWeapon()==null || player.getEquippedWeapon().getAmmoType()==null || aimed)return false;
        aimed=true;log.add("Джон прицелился: +15 п.п. к следующему выстрелу, максимум 95%. Движение сбросит прицел.");return true;
    }
    boolean canDestroy() {
        if(selectedObstacle<0 || distance(playerCell,selectedObstacle)!=1 || obstacles[selectedObstacle]==null)return false;
        Weapons weapon=player.getEquippedWeapon();if(weapon==null || weapon.getDurability()==0)return false;
        WeaponStyle style=weapon.getStyle();Obstacle obstacle=obstacles[selectedObstacle];
        boolean destructible=style==WeaponStyle.FIRE_AXE && obstacle.light || style==WeaponStyle.SLEDGEHAMMER
                && (obstacle.light || obstacle==Obstacle.BRICK_WALL || obstacle==Obstacle.ROADBLOCK || obstacle==Obstacle.BLOCK);
        return destructible && player.getStamina()>=player.staminaCost(style.staminaCost);
    }
    boolean destroy() {
        if(!canDestroy())return false;
        Weapons weapon=player.getEquippedWeapon();WeaponStyle style=weapon.getStyle();int cell=selectedObstacle;
        if(!player.spendStamina(style.staminaCost))return false;
        obstacleDamage[cell]+=Math.max(1,(style.minDamage+random.nextInt(style.maxDamage-style.minDamage+1))*player.getDamagePercent()/100*weapon.getConditionPercent()/100);
        weapon.wear(3);lastActionNoise=Math.max(lastActionNoise,style.noise());player.addNoise(style.noise());GameIO.sound(GameSound.weapon(style));aimed=false;
        visual("shake",cell,0);
        int durability=obstacles[cell].light?40:80;
        if(obstacleDamage[cell]>=durability) {obstacles[cell]=null;selectedObstacle=-1;log.add("Преграда разрушена. Клетка и линия огня освобождены.");}
        else log.add("Удар по преграде: осталось "+(durability-obstacleDamage[cell])+" прочности.");
        return true;
    }
    boolean items(Scanner scanner) {
        List<ItemInfo> choices=new ArrayList<>();
        for(ItemInfo item:player.getInventory())if(item instanceof MedicalItem || item instanceof Food
                || item.getName().toLowerCase(Locale.ROOT).contains("гранат"))choices.add(item);
        GameIO.menu("Предмет в бою","Применение занимает действие. Отмена не тратит ход.");
        for(int i=0;i<choices.size();i++)GameIO.item(i+1,choices.get(i),"Использовать: ",true);
        GameIO.action(0,"Назад на поле");int answer=SurvivalSystems.choice(scanner);
        if(answer<1 || answer>choices.size())return false;
        ItemInfo item=choices.get(answer-1);
        if(item instanceof MedicalItem) {
            List<BodyPart> parts=player.injuredParts();
            if(parts.isEmpty()){log.add("Нет ран, требующих обработки.");return false;}
            GameIO.menu("Обработать рану", "Выберите повреждённую часть тела. Применение занимает ход.");
            for(int i=0;i<parts.size();i++){BodyPart part=parts.get(i);GameIO.action(i+1,part.label,"Травма "+player.trauma(part)+"/100 · кровь "+player.bleed(part)+"/5");}
            GameIO.action(0,"Отмена");int partChoice=SurvivalSystems.choice(scanner);
            if(partChoice<1 || partChoice>parts.size())return false;
            log.add(player.treat(parts.get(partChoice-1),(MedicalItem)item));
        } else if(item instanceof Food) {
            if(player.getHunger()==0) {log.add("Вы не голодны.");return false;}
            player.eatFood((Food)item);
        } else {
            Unit target=target();
            if(target==null || distance(playerCell,target.cell)>4 || !clearShot(playerCell,target.cell)) {log.add("Нет доступной цели для гранаты.");return false;}
            int centre=target.cell;
            for(Unit unit:units)if(unit.active() && distance(unit.cell,centre)<=1){int hp=unit.enemy.getHealth();unit.enemy.takeDamage(45+random.nextInt(21));visual("damage",unit.cell,hp-unit.enemy.getHealth());}
            if(distance(playerCell,centre)<=1){int hp=player.getHealth();player.takeDamage(45);visual("damage",playerCell,hp-player.getHealth());}
            lastActionNoise=40;player.addNoise(40);log.add("Граната взорвалась. Радиус — соседние клетки; шум +40.");
        }
        player.getInventory().remove(item);log.add("Использовано: "+item.getName());dropDead();return true;
    }
    boolean pickup(Scanner scanner) {
        List<ItemInfo> items=new ArrayList<>();
        for(Map.Entry<Integer,List<ItemInfo>> entry:ground.entrySet())if(distance(entry.getKey(),playerCell)<=1)items.addAll(entry.getValue());
        if(items.isEmpty())return false;
        GameIO.menu("Трофеи на поле","Свою и соседние клетки можно обыскать за одно действие.");
        for(int i=0;i<items.size();i++)GameIO.item(i+1,items.get(i),"Поднять: ",player.canCarry(items.get(i)));
        GameIO.action(0,"Назад на поле");int choice=SurvivalSystems.choice(scanner);
        if(choice<1 || choice>items.size())return false;
        ItemInfo item=items.get(choice-1);if(!player.addItem(item))return false;
        for(List<ItemInfo> pile:ground.values())pile.remove(item);
        log.add("Поднято: "+item.getName());return true;
    }
    boolean escape() {
        if(!edge(playerCell)) {log.add("Для побега сначала дойдите до края поля.");return false;}
        boolean tired=player.getStamina()<player.staminaCost(10);player.spendAvailableStamina(10);
        for(Unit unit:units)if(unit.active() && distance(unit.cell,playerCell)==1 && random.nextInt(100)<40) {
            player.takeDamage(unit.enemy.getMinDamage());log.add("Удар в спину при отходе!");break;
        }
        escaped=player.isAlive() && random.nextInt(100)<(tired?30:65);
        if(escaped)GameIO.musicCue("hunted");
        log.add(escaped?"Вы покинули поле боя.":"Побег не удался. Враги получают ход.");return true;
    }
    boolean won() {
        for(Unit unit:units)if(unit.active())return false;
        if(escortToExit)for(Civilian c:civilians)if(c.person.isAlive() && !c.safe)return false;
        return true;
    }
    void endTurn() {
        GameIO.advanceMinutes(1);player.increaseHunger(1);player.increaseSleepiness(1);
        if(!escaped && !won() && player.isAlive()) {
            moveCivilians();
            enemyTurn();
        }
        if(player.isAlive()) {
            int bleeding=player.sufferBleeding();
            if(bleeding>0) log.add("Кровопотеря ухудшила состояние корпуса на "+bleeding+". Нужен бинт.");
        }
        guarded=false;moved=false;turn++;dropDead();
        if(!fixedEncounter && won() || escaped || !player.isAlive())player.reduceNoise(60);
    }
    void dropDead() {
        for(Unit unit:units)if(!unit.enemy.isAlive() && !unit.dropped) {
            unit.dropped=true;unit.jump=-1;
            if(unit.enemy instanceof HostileSurvivor && ((HostileSurvivor)unit.enemy).soundStyle()==WeaponStyle.REVOLVER && unit.shots>0)
                spentCases.merge(AmmoType.REVOLVER,unit.shots,Integer::sum);
            if(unit.enemy instanceof HostileSurvivor) ground.computeIfAbsent(unit.cell,key->new ArrayList<>()).addAll(((HostileSurvivor)unit.enemy).claimLoot());
            if(unit.enemy instanceof Zombie && !denEncounter) ground.computeIfAbsent(unit.cell,key->new ArrayList<>()).addAll(((Zombie)unit.enemy).claimEars(random));
            log.add(unit.enemy.getName()+" повержен.");
        }
    }
    int nextStep(int from,int target,boolean smash) {
        int[] parent=new int[64];Arrays.fill(parent,-1);parent[from]=from;
        ArrayDeque<Integer> q=new ArrayDeque<>();q.add(from);
        while(!q.isEmpty()) {
            int cell=q.remove();
            if(cell==target)break;
            for(int n:neighbours(cell)) {
                if(parent[n]>=0 || n!=target && (n==playerCell || unitAt(n)!=null || civilianAt(n)!=null))continue;
                if(obstacles[n]!=null && !(smash && obstacles[n].light))continue;
                parent[n]=cell;q.add(n);
            }
        }
        if(parent[target]<0)return from;
        int result=target;while(parent[result]!=from && result!=from)result=parent[result];return result;
    }
    boolean step(Unit unit,int cell) {
        if(cell==playerCell || unitAt(cell)!=null || civilianAt(cell)!=null)return false;
        if(obstacles[cell]!=null) {
            if(unit.type()!=ZombieType.BRUTE || !obstacles[cell].light)return false;
            obstacles[cell]=null;lastActionNoise=Math.max(lastActionNoise,10);player.addNoise(10);log.add("Громила снёс лёгкое препятствие! Шум +10.");
        }
        unit.cell=cell;return true;
    }
    void toward(Unit unit,int target,int steps) {
        if(target==playerCell && !civilians.isEmpty())target=enemyTargetCell(unit);
        for(int i=0;i<steps;i++) {
            int cell=nextStep(unit.cell,target,unit.type()==ZombieType.BRUTE);
            if(cell==unit.cell || !step(unit,cell))break;
        }
    }
    int retreatGoal(Unit unit,boolean cover) {
        int best=unit.cell,score=Integer.MIN_VALUE;
        for(int cell=0;cell<64;cell++)if(cell==unit.cell || free(cell)) {
            int d=distance(cell,playerCell);
            int value=cover?30-Math.abs(d-4)*8:50+d;
            if(!cover && !edge(cell))value-=30;
            if(cover) {
                if(clearShot(cell,playerCell))value+=12;
                for(int n:neighbours(cell))if(obstacles[n]!=null){value+=10;break;}
            }
            value-=distance(unit.cell,cell)*2;
            if(value>score){score=value;best=cell;}
        }
        return best;
    }
    void strike(Unit unit,boolean ranged) {
        int targetCell=enemyTargetCell(unit);
        Civilian civilian=civilianAt(targetCell);
        SurvivorInfo victim=civilian==null?player:civilian.person;
        String victimName=civilian==null?"Джона":victim.getName();
        int penalty=ranged?shotPenalty(unit.cell,targetCell):0;
        int min=unit.enemy.getMinDamage(),max=unit.enemy.getMaxDamage();
        WeaponStyle fired=unit.enemy instanceof HostileSurvivor?((HostileSurvivor)unit.enemy).soundStyle():WeaponStyle.FISTS;
        GameIO.sound(unit.enemy instanceof Zombie?GameSound.ZOMBIE:GameSound.weapon(((HostileSurvivor)unit.enemy).soundStyle()));
        unit.enemy.spendAttackAmmo();
        if(ranged)casingShot(fired);
        if(unit.enemy instanceof HostileSurvivor)player.addNoise(fired.noise());
        if(ranged)unit.shots++;
        if(random.nextInt(100)>=Math.max(5,unit.enemy.getAttackAccuracy()-penalty)) {visual("miss",targetCell,0);log.add(unit.enemy.getName()+" атакует "+victimName+": промах.");return;}
        int damage=min+random.nextInt(max-min+1);
        if(unit.type()==ZombieType.RUNNER)damage=damage*3/2;
        if(guarded && victim==player)damage=Math.max(1,damage*40/100);
        int before=victim.getHealth();victim.takeDamage(damage);
        if(victim==player){visual("damage",targetCell,0);log.add(unit.enemy.getName()+" ранит Джона. Проверьте состояние тела.");}
        else {visual("damage",targetCell,before-victim.getHealth());log.add(unit.enemy.getName()+" атакует "+victimName+": −"+(before-victim.getHealth())+" HP.");}
    }
    int enemyTargetCell(Unit unit) {
        int cell=playerCell;
        for(Civilian c:civilians)if(c.person.isAlive() && !c.safe && distance(unit.cell,c.cell)<distance(unit.cell,cell))cell=c.cell;
        return cell;
    }
    void moveCivilians() {
        for(Civilian c:civilians)if(c.person.isAlive() && !c.safe) {
            if(escortToExit && c.cell/8==0) {c.safe=true;log.add(c.person.getName()+" добрался до выхода.");continue;}
            int goal=playerCell;
            if(escortToExit && playerCell/8==0) {
                for(int i=0;i<8;i++)if(free(i)){goal=i;break;}
            } else if(distance(c.cell,playerCell)<=1)continue;
            int step=nextStep(c.cell,goal,false);
            if(step!=c.cell && free(step))c.cell=step;
            if(escortToExit && c.cell/8==0){c.safe=true;log.add(c.person.getName()+" добрался до выхода.");}
        }
    }
    void enemyTurn() {
        boolean call=false;
        for(Unit unit:new ArrayList<>(units)) {
            if(!unit.active() || !player.isAlive())continue;
            if(unit.bleed>0) {unit.bleed--;int before=unit.enemy.getHealth();unit.enemy.takeBleedingDamage(4);
                log.add(unit.enemy.getName()+": кровотечение −"+(before-unit.enemy.getHealth())+" HP.");if(!unit.active())continue;}
            if(fixedEncounter) {
                if(clearShot(unit.cell,playerCell)){unit.alerted=true;unit.lastKnown=playerCell;}
                else if(lastActionNoise>0){unit.alerted=true;unit.lastKnown=playerCell;}
                if(!unit.alerted)continue;
                if(!clearShot(unit.cell,playerCell)) {
                    if(unit.skip>0){unit.skip--;continue;}
                    if(unit.lastKnown>=0 && unit.cell!=unit.lastKnown)toward(unit,unit.lastKnown,unit.slow>0?0:1);
                    if(unit.slow>0)unit.slow--;
                    if(unit.cell==unit.lastKnown)unit.alerted=false;
                    continue;
                }
            }
            boolean slowed=unit.slow>0;if(slowed)unit.slow--;
            if(unit.skip>0) {unit.skip--;log.add(unit.enemy.getName()+" пропускает действие.");continue;}
            if(unit.enemy instanceof HostileSurvivor) {
                HostileSurvivor human=(HostileSurvivor)unit.enemy;
                if(!human.isRaider() && human.getHealth()*100/human.getMaxHealth()<(allowSurrender?25:30)) {
                    if(allowSurrender && human.getHealth()*100/human.getMaxHealth()<25) {
                        log.add(human.getName()+" опускает оружие и просит дать уйти.");continue;
                    }
                    if(edge(unit.cell)) {unit.fled=true;log.add("Раненый бродяга сбежал.");continue;}
                    toward(unit,retreatGoal(unit,false),2);
                    if(edge(unit.cell)){unit.fled=true;log.add("Бродяга покинул поле.");}
                    continue;
                }
                boolean picked=false;
                for(Map.Entry<Integer,List<ItemInfo>> entry:ground.entrySet())if(distance(entry.getKey(),unit.cell)<=1) {
                    Iterator<ItemInfo> it=entry.getValue().iterator();
                    while(it.hasNext())if(human.takeGroundItem(it.next())) {it.remove();picked=true;}
                }
                if(picked) {log.add(human.getName()+" подобрал снаряжение с земли.");continue;}
                if(human.isRanged()) {
                    if(unit.shots>=human.soundStyle().capacity()) {
                        if(human.soundStyle()==WeaponStyle.REVOLVER)spentCases.merge(AmmoType.REVOLVER,unit.shots,Integer::sum);
                        unit.shots=0;GameIO.sound(GameSound.RELOAD);log.add(human.getName()+" перезаряжает оружие.");continue;
                    }
                    toward(unit,retreatGoal(unit,true),1);
                    if(distance(unit.cell,playerCell)<=6 && clearShot(unit.cell,playerCell))strike(unit,true);
                    else toward(unit,playerCell,1);
                    continue;
                }
            }
            if(unit.type()==ZombieType.JUMPER && !slowed) {
                if(unit.jump>=0) {
                    int landing=unit.jump;unit.jump=-1;
                    if(landing==playerCell) {strike(unit,false);log.add("Прыгун попал в отмеченную клетку!");}
                    else log.add("Прыгун промахнулся — вы ушли из отмеченной клетки.");
                    if(free(landing))unit.cell=landing;
                    else for(int n:neighbours(landing))if(free(n)){unit.cell=n;break;}
                    continue;
                }
                if(distance(unit.cell,playerCell)<=4 && distance(unit.cell,playerCell)>1) {
                    unit.jump=playerCell;log.add("Прыгун готовит прыжок на "+coordinate(playerCell)+"! Уйдите с клетки.");continue;
                }
            }
            if(unit.type()==ZombieType.SCREAMER) {
                if(unit.screamAt>=0 && turn>=unit.screamAt) {call=true;unit.screamAt=-1;}
                int d=distance(unit.cell,playerCell);
                if(!slowed && d<3)toward(unit,retreatGoal(unit,true),1);
                else if(!slowed && d>4)toward(unit,playerCell,1);
                if(unit.screamAt<0) {unit.screamAt=turn+1+random.nextInt(2);GameIO.sound(GameSound.SCREAM);player.addNoise(20);log.add("Крикун зовёт заражённых: подкрепление через 1–2 хода.");}
                continue;
            }
            int steps=unit.type()==ZombieType.RUNNER || unit.type()==ZombieType.JUMPER || unit.type()==ZombieType.HUNTER?2:1;
            toward(unit,playerCell,Math.max(0,steps-(slowed?1:0)));
            if(distance(unit.cell,enemyTargetCell(unit))==1)strike(unit,false);
        }
        if(player.isAlive() && !won() && (call || player.getNoise()>=40 && random.nextInt(100)<25))reinforce();
        dropDead();
    }
    void reinforce() {
        if(fixedEncounter || denEncounter)return;
        if(spawned>=12 || units.stream().filter(Unit::active).count()>=6)return;
        int cell=spawnCell(true);if(cell<0)return;
        units.add(new Unit(Survivor.createZombie(units.size()+1,random),cell));spawned++;
        log.add("На краю поля появился заражённый. Он начнёт действовать со следующего хода.");
    }
    BattleBoard snapshot() {
        List<BattleBoard.Cell> cells=new ArrayList<>();Unit selectedUnit=target();
        for(int i=0;i<64;i++) {
            Unit unit=unitAt(i);boolean john=i==playerCell;boolean hazard=false;
            Civilian civilian=civilianAt(i);
            for(Unit u:units)if(u.active() && u.jump==i)hazard=true;
            boolean loot=ground.containsKey(i) && !ground.get(i).isEmpty();
            cells.add(new BattleBoard.Cell(i,john?0:civilian!=null?8+civilians.indexOf(civilian):unit==null?-1:unit.sprite(),obstacles[i]==null?-1:obstacles[i].ordinal(),
                    john?player.getHealth():civilian!=null?civilian.person.getHealth():unit==null?0:unit.enemy.getHealth(),john?player.getMaxHealth():civilian!=null?civilian.person.getMaxHealth():unit==null?0:unit.enemy.getMaxHealth(),
                    canMove(i),i==selectedObstacle || selectedObstacle<0 && unit!=null && unit==selectedUnit,hazard,loot,
                    markers.containsKey(i)?markers.get(i):john?"Джон":civilian!=null?civilian.person.getName():unit==null?coordinate(i):unit.enemy.getName()));
            BattleBoard.Cell view=cells.get(cells.size()-1);view.identity=john?0:civilian!=null?100000+civilians.indexOf(civilian):unit==null?-1:unit.visualId;
            view.obstacleSpan=obstacleSpan(i);view.obstaclePart=obstaclePart(i);view.obstacleVertical=obstacleIsVertical(i);
            view.stun=unit==null?0:unit.skip;view.slow=unit==null?0:unit.slow;view.aimed=john && aimed;view.obstacleDamage=obstacleDamage[i];
        }
        BattleBoard result=new BattleBoard(cells,turn,playerCell,terrain,!fixedEncounter || units.stream().anyMatch(u->u.active() && u.alerted),fixedEncounter || scriptedEncounter);
        result.woodFloor=woodFloor;result.grassFloor=grassFloor;
        result.epoch=System.identityHashCode(this);result.effects=Collections.unmodifiableList(new ArrayList<>(effects));
        if(selectedUnit!=null && range()>1 && player.getEquippedWeapon()!=null && player.getEquippedWeapon().getAmmoType()!=null){result.shotTarget=selectedUnit.cell;result.shotBlocked=!clearShot(playerCell,selectedUnit.cell) || distance(playerCell,selectedUnit.cell)>range();}
        return result;
    }
    void render() {
        Unit target=target();
        GameIO.player(player);
        GameIO.battle(player,units.stream().filter(Unit::active).map(u->u.enemy).collect(java.util.stream.Collectors.toList()),target==null || selectedObstacle>=0?null:target.enemy);
        GameIO.menu(fixedEncounter?"Архив · ход "+turn:won()?"Поле очищено":"Тактический бой · ход "+turn,
                terrain+" · ходьба: "+moveLimit()+" кл. · "+(moved?"движение выполнено":"движение + действие")
                        +"\n"+player.getWeaponName()+" · дальность "+range()+(player.getEquippedWeapon()!=null && player.getEquippedWeapon().getAmmoType()!=null?" · заряжено "+loaded():""));
        GameIO.board(snapshot());
        if(selectedObstacle>=0 && obstacles[selectedObstacle]!=null)GameIO.card("Выбранная преграда",
                "Клетка "+coordinate(selectedObstacle)+" · прочность "+((obstacles[selectedObstacle].light?40:80)-obstacleDamage[selectedObstacle])
                        +"\nПожарный топор рубит лёгкие преграды. Кувалда также разбивает кирпичную стенку, дорожный и бетонный блок. Машины, скалы и несущие стены не разрушаются.");
        if(aimed)GameIO.card("Прицел подготовлен","Следующий выстрел: +15 п.п. точности, максимум 95%. Движение сбросит бонус.");
        if(!civilians.isEmpty()) {
            StringBuilder info=new StringBuilder(escortToExit?"Доведите спутников до верхнего края поля. Они идут на одну клетку за ход.\n":"Защитите спутника от заражённых.\n");
            for(Civilian c:civilians)info.append(c.person.getName()).append(": ").append(c.safe?"в безопасности":c.person.isAlive()?c.person.getHealth()+"/"+c.person.getMaxHealth()+" HP":"погиб").append('\n');
            GameIO.card("Спутники",info.toString());
        }
        for(int i=0;i<64;i++)if(canMove(i) || unitAt(i)!=null || obstacles[i]!=null)GameIO.action(CELL+i,coordinate(i),"",true);
        if(!won())GameIO.action(1,"Атаковать",target==null?"Выберите врага":target.enemy.getName(),canAttack());
        GameIO.action(2,"Оборона","+выносливость · урон −60%");
        GameIO.action(3,"Предметы","Лечение, еда, гранаты");
        GameIO.action(4,"Оружие","Смена занимает действие");
        GameIO.action(5,"Перезарядить","Одно действие",player.getEquippedWeapon()!=null && player.getEquippedWeapon().getAmmoType()!=null);
        boolean nearLoot=ground.entrySet().stream().anyMatch(e->!e.getValue().isEmpty() && distance(e.getKey(),playerCell)<=1);
        GameIO.action(6,"Поднять трофей","Своя или соседняя клетка",nearLoot);
        if(!won())GameIO.action(7,"Сбежать","С края поля · возможен удар в спину",edge(playerCell));
        GameIO.action(8,"Завершить ход","+выносливость");
        GameIO.action(10,aimed?"Прицел подготовлен":"Прицелиться","+15 п.п. к следующему выстрелу · одно действие",!aimed && player.getEquippedWeapon()!=null && player.getEquippedWeapon().getAmmoType()!=null);
        if(selectedObstacle>=0)GameIO.action(11,"Разрушить преграду","Соседняя клетка · пожарный топор или кувалда",canDestroy());
        if(player.getEquippedWeapon()!=null && player.heldCases(player.getEquippedWeapon())>0)
            GameIO.action(12,"Извлечь револьверные гильзы","Одно действие; можно и без запасных патронов");
        if(allowSurrender && target!=null && target.enemy instanceof HostileSurvivor && target.enemy.getHealth()*100/target.enemy.getMaxHealth()<25)
            GameIO.action(9,"Позволить отступить","Оставить противника в живых");
        if(won())GameIO.action(0,"Продолжить вылазку","Оставленные на поле трофеи будут потеряны");
        if(log.size()>8)log.subList(0,log.size()-8).clear();
        for(String line:log)GameIO.out().println(line);
    }
}
