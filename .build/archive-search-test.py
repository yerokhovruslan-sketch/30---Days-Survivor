from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/game/PoliceArchive.java');s=p.read_text(encoding='utf-8');at=s.index('        boolean changeRoom()');s=s[:at]+'''        boolean canChangeRoom(){return rooms[room].playerCell==(room==0?UPPER_DOOR:LOWER_DOOR) && rooms[1-room].unitAt(room==0?LOWER_DOOR:UPPER_DOOR)==null;}
        boolean searchCabinet(){
            TacticalBattle b=rooms[room];if(room!=1 || papersCollected || b.playerCell!=DOCUMENTS)return false;
            b.lastActionNoise=0;b.endTurn();pursue();
            if(b.player.isAlive()){searchProgress++;if(searchProgress==3){papersCollected=true;b.log.add("Папка скопирована. Вернитесь к выходу из архива.");}else b.log.add("Поиск в шкафу: "+searchProgress+"/3.");}
            return true;
        }
''' +s[at:]
s=s.replace('if(from.playerCell!=exit || to.unitAt(arrival)!=null)return false;', 'if(!canChangeRoom())return false;')
s=s.replace('b.playerCell==(attempt.room==0?UPPER_DOOR:LOWER_DOOR));', 'attempt.canChangeRoom());')
a=s.index('                    b.lastActionNoise=0;b.endTurn();attempt.pursue();');z=s.index('                    GameIO.checkpoint();continue;',a)
s=s[:a]+'                    attempt.searchCabinet();\n'+s[z:];p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/PoliceArchiveTest.java');s=p.read_text(encoding='utf-8').replace('TacticalBattle.Unit chasing=a.rooms[1].units.get(0);a.pursue();', 'TacticalBattle.Unit chasing=a.rooms[1].units.get(0);chasing.alerted=true;a.pursue();');at=s.rfind('}');s=s[:at]+''' @Test public void cabinetRequiresThreeActionsOnItsCellButDoesNotRequireKilling() {
  PoliceArchive.Attempt a=new PoliceArchive.Attempt(new Fighter("John","",1000),new Random(4));
  a.room=1;assertFalse(a.searchCabinet());a.rooms[1].playerCell=PoliceArchive.DOCUMENTS;
  assertTrue(a.searchCabinet());assertEquals(1,a.searchProgress);assertFalse(a.papersCollected);
  assertTrue(a.searchCabinet());assertFalse(a.papersCollected);assertTrue(a.searchCabinet());assertTrue(a.papersCollected);
  assertFalse(a.cleared());assertFalse(a.searchCabinet());assertEquals(4,a.rooms[1].turn);
 }
 @Test public void enemiesDoNotKnowJohnThroughWallsButShotsAlertExistingNeighbours() {
  PoliceArchive.Attempt a=new PoliceArchive.Attempt(new Fighter("John","",100),new Random(5));
  TacticalBattle b=a.rooms[0];Arrays.fill(b.obstacles,null);b.obstacles[1]=TacticalBattle.Obstacle.WALL;b.playerCell=0;b.units.clear();
  TacticalBattle.Unit u=new TacticalBattle.Unit(new Zombie(1,ZombieType.NORMAL),2);b.units.add(u);b.enemyTurn();assertEquals(2,u.cell);assertFalse(u.alerted);
  b.lastActionNoise=25;b.enemyTurn();assertTrue(u.alerted);assertNotEquals(2,u.cell);
  b.lastActionNoise=0;int before=a.rooms[1].units.get(0).cell;a.pursue();assertEquals(before,a.rooms[1].units.get(0).cell);
  b.lastActionNoise=25;a.pursue();assertTrue(a.rooms[1].units.get(0).alerted);
 }
 @Test public void attemptLivesUntilLeavingPoliceAndDoorCannotOverlapJohnOrEnemy() {
  WorldStories w=new WorldStories();PoliceArchive.Attempt a=new PoliceArchive.Attempt(new Fighter("John","",100),new Random(6));w.archiveAttempt=a;
  a.rooms[0].units.get(0).enemy.takeDamage(10000);assertFalse(w.archiveAttempt.rooms[0].units.get(0).active());
  a.rooms[0].playerCell=PoliceArchive.UPPER_DOOR;a.rooms[1].units.get(0).cell=PoliceArchive.LOWER_DOOR;assertFalse(a.changeRoom());assertEquals(0,a.room);
  a.rooms[1].units.get(0).cell=9;assertTrue(a.changeRoom());assertEquals(PoliceArchive.LOWER_DOOR,a.rooms[1].playerCell);
  w.leavePoliceLocation();assertNull(w.archiveAttempt);assertTrue(new PoliceArchive.Attempt(a.rooms[0].player,new Random(6)).rooms[0].units.get(0).active());
 }
''' +s[at:];p.write_text(s,encoding='utf-8')
