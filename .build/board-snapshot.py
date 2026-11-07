from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/game/BattleBoard.java');s=p.read_text(encoding='utf-8').replace('    public final int turn, playerCell;', '    public final int turn, playerCell;\n    public int epoch,shotTarget=-1;\n    public boolean shotBlocked;\n    public List<Effect> effects=Collections.emptyList();\n    public static final class Effect {\n        public final long id;public final int cell,amount;public final String kind;\n        Effect(long id,int cell,int amount,String kind){this.id=id;this.cell=cell;this.amount=amount;this.kind=kind;}\n    }')
s=s.replace('        public final String label;', '        public final String label;\n        public int identity=-1,stun,slow,obstacleDamage;\n        public boolean aimed;')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/game/TacticalBattle.java');s=p.read_text(encoding='utf-8').replace('    static final class Unit {', '    private static final java.util.concurrent.atomic.AtomicInteger VISUAL_ID=new java.util.concurrent.atomic.AtomicInteger(1);\n    static final class Unit {\n        final int visualId=VISUAL_ID.getAndIncrement();')
s=s.replace('    final Fighter player;', '    private long effectId;\n    private final List<BattleBoard.Effect> effects=new ArrayList<>();\n    void visual(String kind,int cell,int amount){effects.add(new BattleBoard.Effect(++effectId,cell,amount,kind));if(effects.size()>32)effects.remove(0);}\n    final Fighter player;')
s=s.replace('        lastActionNoise=style.noise();', '        if(result.hits==0)visual("miss",target.cell,0);else visual("damage",target.cell,result.damage);\n        lastActionNoise=style.noise();',1)
s=s.replace('        int durability=obstacles[cell].light?40:80;', '        visual("shake",cell,0);\n        int durability=obstacles[cell].light?40:80;')
s=s.replace('log.add(unit.enemy.getName()+" атакует "+victimName+": промах.");return;', 'visual("miss",targetCell,0);log.add(unit.enemy.getName()+" атакует "+victimName+": промах.");return;')
s=s.replace('        log.add(unit.enemy.getName()+" атакует "+victimName+": −"', '        visual("damage",targetCell,before-victim.getHealth());\n        log.add(unit.enemy.getName()+" атакует "+victimName+": −"')
s=s.replace('unit.enemy.takeDamage(45+random.nextInt(21));', '{int hp=unit.enemy.getHealth();unit.enemy.takeDamage(45+random.nextInt(21));visual("damage",unit.cell,hp-unit.enemy.getHealth());}')
s=s.replace('if(distance(playerCell,centre)<=1)player.takeDamage(45);', 'if(distance(playerCell,centre)<=1){int hp=player.getHealth();player.takeDamage(45);visual("damage",playerCell,hp-player.getHealth());}')
needle='markers.containsKey(i)?markers.get(i):john?"Джон":civilian!=null?civilian.person.getName():unit==null?coordinate(i):unit.enemy.getName()));'
s=s.replace(needle,needle+'''\n            BattleBoard.Cell view=cells.get(cells.size()-1);view.identity=john?0:civilian!=null?100000+civilians.indexOf(civilian):unit==null?-1:unit.visualId;
            view.stun=unit==null?0:unit.skip;view.slow=unit==null?0:unit.slow;view.aimed=john && aimed;view.obstacleDamage=obstacleDamage[i];''')
s=s.replace('        return new BattleBoard(cells,turn,playerCell,terrain,!fixedEncounter || units.stream().anyMatch(u->u.active() && u.alerted),fixedEncounter || scriptedEncounter);', '''        BattleBoard result=new BattleBoard(cells,turn,playerCell,terrain,!fixedEncounter || units.stream().anyMatch(u->u.active() && u.alerted),fixedEncounter || scriptedEncounter);
        result.epoch=System.identityHashCode(this);result.effects=Collections.unmodifiableList(new ArrayList<>(effects));
        if(selectedUnit!=null && range()>1 && player.getEquippedWeapon()!=null && player.getEquippedWeapon().getAmmoType()!=null){result.shotTarget=selectedUnit.cell;result.shotBlocked=!clearShot(playerCell,selectedUnit.cell) || distance(playerCell,selectedUnit.cell)>range();}
        return result;''')
p.write_text(s,encoding='utf-8')
