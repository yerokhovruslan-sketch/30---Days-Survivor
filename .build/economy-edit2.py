from pathlib import Path
base=Path('app/src/main/java/com/example/survivor/game')
p=base/'Shop.java';s=p.read_text(encoding='utf-8')
s=s.replace('static void showCatalogue() {','static WorldStories clock(){return GameIO.world()==null?null:GameIO.world().getStories();}\n    static int price(int number){return Economy.buyPrice(clock(),number);}\n    static void showCatalogue() {\n        if(clock()!=null)GameIO.card("Поставки",clock().marketStatus());')
s=s.replace('item.getPrice() + " $ · "','price(i) + " $ · "')
s=s.replace('details+(i==54 || i==59?"\\nОстаток на сегодня: "+stock:"")','details+"\\nВ наличии: "+(stock==Integer.MAX_VALUE?"—":stock)')
s=s.replace('sample.getPrice()', 'price(number)')
s=s.replace('item.getSalePrice() + " $ · "','Economy.salePrice(clock(),item) + " $ · "')
s=s.replace('(long) balance + item.getSalePrice()', '(long) balance + Economy.salePrice(clock(),item)')
s=s.replace('GameIO.sound(GameSound.TRADE);','GameIO.balance(balance);GameIO.checkpoint();\n        GameIO.sound(GameSound.TRADE);')
p.write_text(s,encoding='utf-8')
p=base/'Survivor.java';s=p.read_text(encoding='utf-8');needle='        int event=world.getStories().residential().discover(world.getNumber(location),mode,world.getStories(),random);'
s=s.replace(needle,'''        List<ItemInfo> chips=world.getStories().guaranteedChips(world.getNumber(location));
        if(!chips.isEmpty()) {
            location.searchSupplies(random);
            GameIO.out().println("В служебном оборудовании найден исправный электрочип. Он отправлен на склад убежища, чтобы не потерять сюжетный компонент из-за перегруза.");
            player.getStorage().addAll(chips);GameIO.checkpoint();return;
        }
'''+needle)
p.write_text(s,encoding='utf-8')
p=base/'ShelterUpgrades.java';s=p.read_text(encoding='utf-8').replace('/2 · 1200 $ за штуку в магазине.', '/2 · купить у торговца или найти по одному в полицейском участке и на электроподстанции.').replace('Магазин: Электрочип × ', 'Электрочип: полицейский участок, электроподстанция или магазин × ')
p.write_text(s,encoding='utf-8')
p=base/'GameIO.java';s=p.read_text(encoding='utf-8').replace('    static void menu(String title, String hint) {','    static void storyMenu(String title,String hint) {menu(title,hint);if(CURRENT.get()!=null)CURRENT.get().pauseReading();}\n    static void menu(String title, String hint) {')
p.write_text(s,encoding='utf-8')
p=base/'ResidentialStories.java';s=p.read_text(encoding='utf-8').replace('GameIO.menu(', 'GameIO.storyMenu(');p.write_text(s,encoding='utf-8')
p=base/'GameSession.java';s=p.read_text(encoding='utf-8')
s=s.replace('    private boolean waiting;','''    private boolean waiting;
    private boolean foreground,manualPause,reading;
    private long realtimeStamp=System.nanoTime();
    private long realtimeRemainder;
    public synchronized void setForeground(boolean value){foreground=value;realtimeStamp=System.nanoTime();}
    public synchronized void setPaused(boolean value){manualPause=value;realtimeStamp=System.nanoTime();}
    void pauseReading(){reading=true;}
    private synchronized void realtimeTick() {
        long now=System.nanoTime(),elapsed=now-realtimeStamp;realtimeStamp=now;
        if(!foreground || manualPause || reading || !menu.scene.isEmpty() || enemies!=null || menu.board!=null || clock()==null || !waiting)return;
        realtimeRemainder+=Math.max(0,elapsed);
        int minutes=(int)(realtimeRemainder/2500000000L);realtimeRemainder%=2500000000L;
        if(minutes>0){clock().advanceMinutes(minutes);publish(atMain?Mode.MAIN:Mode.CHOICE);if(clock().minuteOfDay()%5==0)checkpoint();}
    }''')
s=s.replace('        atMain = true;','        reading = false;\n        atMain = true;')
s=s.replace('        atMain = false;\n        menu = new GameMenu.Builder(title, hint);','        atMain = false;\n        reading = false;\n        menu = new GameMenu.Builder(title, hint);')
s=s.replace('                int number = input.take();','''                Integer number=null;
                synchronized(this){realtimeStamp=System.nanoTime();}
                while(number==null){number=input.poll(250,java.util.concurrent.TimeUnit.MILLISECONDS);realtimeTick();}
''')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/GameViewModel.java');s=p.read_text(encoding='utf-8').replace('        session.start();','        session.setForeground(foreground);\n        session.start();').replace('        foreground = true;', '        foreground = true;\n        if(session!=null)session.setForeground(true);').replace('void pauseMusic() { foreground = false;', 'void pauseMusic() { foreground = false;if(session!=null)session.setForeground(false);')
s=s.replace('    void clickSound()', '    void pauseWorld(boolean value){if(session!=null)session.setPaused(value);}\n    void clickSound()')
p.write_text(s,encoding='utf-8')
