from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/GameEffects.java');s=p.read_text(encoding='utf-8').replace('zombies=f.status.enemies.stream()', 'zombies=f.hasPlayer && f.health>0 && f.mode!=GameSession.Mode.ENDED && f.status.enemies.stream()');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MenuRenderer.java');s=p.read_text(encoding='utf-8').replace('        AppCompatButton button = new AppCompatButton(context);', '''        AppCompatButton button = new AppCompatButton(context){
            @Override public boolean onTouchEvent(android.view.MotionEvent event){if(!isEnabled() && event.getAction()==android.view.MotionEvent.ACTION_UP)listener.unavailable();return super.onTouchEvent(event);}
        };''').replace('        if(!enabled)button.setOnTouchListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_UP)listener.unavailable();return true;});\n','');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/game/GameIO.java');s=p.read_text(encoding='utf-8').replace('int place=clock()==null?0:clock().currentPlace();return place==2', 'return footsteps(clock()==null?0:clock().currentPlace());}\n    static GameSound footsteps(int place){return place==2');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/game/Survivor.java');s=p.read_text(encoding='utf-8');s=s.replace('    public static void travel(SurvivorInfo player, String message, int steps) {', '    public static void travel(SurvivorInfo player,String message,int steps){travel(player,message,steps,GameIO.footsteps());}\n    static void travel(SurvivorInfo player, String message, int steps,GameSound surface) {').replace('        showMovement(message, steps);', '        GameIO.sound(surface);GameIO.out().println(message+"…");').replace('travel(selectedSurvivor, "Путь к локации: " + destination.getName(), 5);','travel(selectedSurvivor, "Путь к локации: " + destination.getName(), 5,GameIO.footsteps(locations.getNumber(destination)));');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MainActivity.java');s=p.read_text(encoding='utf-8').replace('    private void dismissResult() {','    private void dismissResult() {\n        if(frame!=null && frame.menu.title.equals("Инвентарь"))game.bagSound();');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/GameViewModel.java');s=p.read_text(encoding='utf-8').replace('    void unavailableSound()', '    void bagSound(){effects.play(com.example.survivor.game.GameSound.BAG);}\n    void unavailableSound()');p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/SoundEventsTest.java');s=p.read_text(encoding='utf-8').replace('Arrays.asList(GameSound.TRADE,GameSound.TRADE)', 'Arrays.asList(GameSound.TRADE,GameSound.ERROR,GameSound.TRADE)').replace('GameSound.ZOMBIE,GameSound.WALK)', 'GameSound.ZOMBIE,GameSound.CONCRETE_STEPS)');at=s.rfind('}');s=s[:at]+'''    @Test public void woundsDeathsAndScreamsAreDiscreteNotRenderingEvents() throws Exception {
        List<GameSound> sounds=events(()->{
            Fighter p=new Fighter("John","",100);GameIO.player(p);p.takeDamage(5);p.takeDamage(5);
            Zombie z=new Zombie(1,ZombieType.NORMAL);z.takeDamage(10000);z.takeDamage(10000);
            TacticalBattle b=new TacticalBattle(p,1,new Random(4),Arrays.asList(new Zombie(1,ZombieType.SCREAMER)));
            Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.get(0).cell=3;b.enemyTurn();
        });
        assertEquals(2,Collections.frequency(sounds,GameSound.HURT));assertEquals(1,Collections.frequency(sounds,GameSound.ZOMBIE_DEATH));assertEquals(1,Collections.frequency(sounds,GameSound.SCREAM));
    }
    @Test public void searchingCollectingAndQuestTransitionsEmitOnce() throws Exception {
        List<GameSound> sounds=events(()->{
            Fighter p=new Fighter("John","",100);SearchMode.CAREFUL.begin(p,new WorldStories());
            SurvivalSystems.collect(p,Arrays.asList(Shop.createItem(16)),new Scanner("1"));
            p.getUpgrades().radio=1;assertTrue(p.getUpgrades().accept());assertFalse(p.getUpgrades().accept());
            p.addItem(Shop.createItem(1));p.addItem(Shop.createItem(1));assertTrue(p.getUpgrades().deliver(p));assertFalse(p.getUpgrades().deliver(p));
        });
        assertEquals(1,Collections.frequency(sounds,GameSound.SEARCH));assertEquals(1,Collections.frequency(sounds,GameSound.BAG));assertEquals(2,Collections.frequency(sounds,GameSound.QUEST));
        assertEquals(GameSound.EARTH_STEPS,GameIO.footsteps(2));assertEquals(GameSound.CONCRETE_STEPS,GameIO.footsteps(15));
    }
''' +s[at:];p.write_text(s,encoding='utf-8')
