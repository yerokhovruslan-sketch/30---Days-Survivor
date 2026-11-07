from pathlib import Path
p=Path('app/src/test/java/com/example/survivor/game/LiveClockTest.java');s=p.read_text(encoding='utf-8');at=s.rfind('}');s=s[:at]+'''    @Test public void loadingAfterDeathRestoresTheSavedClockAndDoesNotOverwriteIt() throws Exception {
        BlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();
        java.io.File directory=Files.createTempDirectory("death-clock").toFile();
        try(GameSession session=new GameSession(directory,frames::offer,()->{
            Locations world=new Locations();Fighter p=new Fighter("John","",100);
            GameIO.mainMenu(p,77,world);GameIO.checkpoint();
            world.getStories().advanceMinutes(4*1440+56);p.takeDamage(1000);GameIO.checkpoint();
            GameIO.scanner().nextInt();
            try {
                GameSave saved=GameSave.load(GameIO.saveFile());
                GameIO.mainMenu(saved.player,saved.balance,saved.locations);
                GameIO.scanner().nextInt();
            }catch(java.io.IOException e){throw new RuntimeException(e);}
        })) {
            session.start();GameSession.Frame dead=frames.poll(3,TimeUnit.SECONDS);assertNotNull(dead);assertEquals(0,dead.health);assertEquals(5,dead.day);
            assertTrue(session.submit(dead.revision,12));
            GameSession.Frame restored;do{restored=frames.poll(3,TimeUnit.SECONDS);assertNotNull(restored);}while(restored.mode==GameSession.Mode.BUSY);
            assertEquals(1,restored.day);assertEquals("08:00",restored.time);assertEquals(100,restored.health);assertEquals(77,restored.balance);
            assertEquals(1,GameSave.load(new java.io.File(directory,"survivor-save.dat")).locations.getStories().day());
        }finally{new java.io.File(directory,"survivor-save.dat").delete();directory.delete();}
    }
''' +s[at:];p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/GameSessionTest.java');s=p.read_text(encoding='utf-8').replace('            assertEquals("Покупка", frame.menu.title);','''            assertEquals("Покупка", frame.menu.title);
            assertTrue(frame.choices.stream().filter(c->c.number>0).allMatch(c->!c.priceTag.isEmpty() && c.enabled));
            assertTrue(frame.choices.stream().noneMatch(c->c.detail.contains("Вес:") || c.detail.contains("прочность: 100")));''');p.write_text(s,encoding='utf-8')
