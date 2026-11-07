from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/game/TacticalBattle.java');s=p.read_text(encoding='utf-8').replace('if(result.exhausted || result.outOfAmmo || result.brokenWeapon)return false;', 'if(result.exhausted || result.outOfAmmo || result.brokenWeapon)return false;\n        lastActionNoise=style.noise();').replace('player.addNoise(40);log.add("Граната', 'lastActionNoise=40;player.addNoise(40);log.add("Граната').replace('obstacles[cell]=null;player.addNoise(10);', 'obstacles[cell]=null;lastActionNoise=Math.max(lastActionNoise,10);player.addNoise(10);');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/game/PoliceArchive.java');s=p.read_text(encoding='utf-8').replace('to.guarded=false;room=1-room;', 'to.guarded=false;to.lastActionNoise=0;room=1-room;');p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/PoliceArchiveTest.java');s=p.read_text(encoding='utf-8');pos=s.rfind('}');s=s[:pos]+''' @Test public void actualGunshotCallsExistingEnemiesWithoutSpawning() {
  Fighter p=new Fighter("John","",100);Weapons gun=(Weapons)Shop.createItem(9);p.addItem(gun);p.equipWeapon(gun);p.addItem(new Ammunition(AmmoType.REVOLVER));
  PoliceArchive.Attempt a=new PoliceArchive.Attempt(p,new Random(7));TacticalBattle b=a.rooms[0];Arrays.fill(b.obstacles,null);b.playerCell=16;b.units.get(0).cell=17;b.selected=0;
  assertTrue(b.attack());assertEquals(25,b.lastActionNoise);a.pursue();assertTrue(a.rooms[1].units.get(0).alerted);
  assertEquals(5,a.rooms[0].units.size()+a.rooms[1].units.size());
 }
''' +s[pos:];p.write_text(s,encoding='utf-8')
