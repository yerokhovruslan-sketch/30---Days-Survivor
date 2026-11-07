package com.example.survivor.game;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.util.ArrayList;

import java.util.List;

import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;



/** Owns the original game loop on one worker; the UI only sends numbered choices. */
public final class GameSession implements AutoCloseable {
    public enum Mode { MAIN, CHOICE, BUSY, ENDED }
    public static final class Choice {
        public final int number;
        public final String label, detail, artwork;
        public String priceTag="";
        public final boolean enabled;
        public final boolean selected;
        Choice(int number, String label, String detail, boolean enabled) { this(number,label,detail,enabled,""); }
        Choice(int number, String label, String detail, boolean enabled,String artwork) {
            this(number,label,detail,enabled,artwork,false);
        }
        Choice(int number,String label,String detail,boolean enabled,String artwork,boolean selected) {
            this.number=number;this.label=label;this.detail=detail;this.enabled=enabled;this.artwork=artwork;
            this.selected=selected;
        }
    }
    public static final class Frame {
        public final long revision;
        public final Mode mode;
        public final String text;
        public final List<Choice> choices;
        public final int health, maxHealth, stamina, hunger, sleepiness, noise, balance, day;
        public final boolean quantity;
        public final GameMenu menu;
        public final GameStatus status;
        public final boolean hasPlayer;
        public final boolean savesAllowed;
        public final String time;
        public final String difficulty;
        public final boolean night,home,reading;
        public final int place;
        public final String musicCue;
        Frame(long revision, Mode mode, String text, SurvivorInfo player, int balance, int day, GameMenu menu, GameStatus status, String time, boolean night, boolean home,int place,boolean reading,String musicCue) {
            this.revision = revision;
            this.mode = mode;
            this.text = text;
            this.balance = balance;
            this.day = day;
            this.difficulty=player==null?"":Difficulty.name(player.difficulty());
            this.time = time; this.night = night;this.home=home;this.place=place;this.reading=reading;this.musicCue=musicCue;
            health = player == null ? 100 : player.getHealth();
            maxHealth = player == null ? 100 : player.getMaxHealth();
            stamina = player == null ? 100 : player.getStamina();
            hunger = player == null ? 0 : player.getHunger();
            sleepiness = player == null ? 0 : player.getSleepiness();
            noise = player == null ? 0 : player.getNoise();
            this.menu = menu;
            this.status = status;
            hasPlayer = player != null;
            savesAllowed = player == null || player.difficulty() != 3;
            quantity = menu.quantity;
            choices = mode == Mode.CHOICE ? menu.choices : java.util.Collections.emptyList();
        }
    }

    final File saveFile;
    final PrintStream output;
    final Scanner scanner;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final ArrayBlockingQueue<Integer> input = new ArrayBlockingQueue<>(1);
    private final Consumer<Frame> listener;
    private final Runnable program;
    private Thread worker;
    private boolean waiting;
    private boolean foreground,manualPause,reading,pendingNarrative;
    private long realtimeStamp=System.nanoTime();
    private final ActiveClock activeClock=new ActiveClock();
    public synchronized void setForeground(boolean value){foreground=value;realtimeStamp=System.nanoTime();}
    public synchronized void setPaused(boolean value){manualPause=value;realtimeStamp=System.nanoTime();}
    void pauseReading(){reading=true;menu.story=true;}
    void narrative(){pendingNarrative=true;}
    private synchronized void realtimeTick() {
        long now=System.nanoTime(),elapsed=now-realtimeStamp;realtimeStamp=now;
        if(player==null || !player.isAlive() || !foreground || manualPause || reading || !menu.scene.isEmpty() || enemies!=null || menu.board!=null || clock()==null || !waiting)return;
        int minutes=activeClock.tick(elapsed,true);
        if(minutes>0){int before=clock().absoluteMinute();clock().advanceMinutes(minutes);
            if(player!=null){player.increaseHunger(clock().absoluteMinute()/60-before/60);player.increaseSleepiness(clock().absoluteMinute()/45-before/45);}
            clock().refreshMarket();
            if(menu.title.equals("Покупка") && !menu.quantity){menu=new GameMenu.Builder("Покупка","Остатки и цены обновляются по игровым часам.");Shop.showCatalogue();}
            publish(atMain?Mode.MAIN:Mode.CHOICE,false);if(clock().minuteOfDay()%5==0)checkpoint();}
    }
    private volatile boolean closed;
    private boolean atMain;
    boolean atHome;
    private long revision;
    private String pendingInput = "";
    private SurvivorInfo player;
    private int balance;
    private Locations world;
    private GameMenu.Builder menu = new GameMenu.Builder("", "");
    private List<? extends CombatEnemy> enemies;
    private CombatEnemy target;
    private Frame lastWaiting;
    private String pendingMusicCue="";
    void musicCue(String cue){pendingMusicCue=cue;}
    private Consumer<String> travelListener;
    public void setTravelListener(Consumer<String> listener){travelListener=listener;}
    void travelTransition(String text){if(!closed&&travelListener!=null)travelListener.accept(text);}
    private Consumer<GameSound> effects;
    public void setEffectsListener(Consumer<GameSound> effects) {this.effects=effects;}
    boolean isCurrentPlayer(SurvivorInfo p){return player==p;}
    void sound(GameSound sound) {if(!closed && effects!=null)effects.accept(sound);}

    public GameSession(File directory, Consumer<Frame> listener) {
        this(directory, listener, null);
    }

    GameSession(File directory, Consumer<Frame> listener, Runnable program) {
        saveFile = new File(directory, "survivor-save.dat");
        this.listener = listener;
        this.program = program;
        try { output = new PrintStream(buffer, true, "UTF-8"); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
        scanner = new Scanner((Readable) this::read);
    }

    public synchronized void start() {
        if (worker != null) return;
        worker = new Thread(() -> {
            GameIO.attach(this);
            try {
                if (program == null) Survivor.main(new String[0]);
                else program.run();
                publish(Mode.ENDED);
            } catch (CancellationException cancelled) {
                // Activity's ViewModel was cleared; do not change the user's explicit save.
            } catch (Exception error) {
                output.println("Не удалось продолжить игру: " + error.getMessage());
                publish(Mode.ENDED);
            } finally {
                GameIO.detach();
                scanner.close();
                output.close();
            }
        }, "survivor-game");
        worker.start();
    }

    synchronized void mainMenu(SurvivorInfo player, int balance, Locations world) {
        if(this.world!=world){activeClock.reset();realtimeStamp=System.nanoTime();reading=false;pendingNarrative=false;menu=new GameMenu.Builder("","");}

        if(world!=null){world.getStories().leavePoliceLocation();world.getStories().bindRecovery(player instanceof Fighter?(Fighter)player:null);}
        player.settleAtHome();
        this.player = player;
        this.balance = balance;
        this.world = world;
        reading = false;
        atMain = true;
        atHome = true;
        enemies = null;
        target = null;
        menu.choices.clear();
        menu.quantity = false;
    }

    void player(SurvivorInfo player) { this.player = player; }
    int difficulty(){return player==null?2:player.difficulty();}
    WorldStories clock() { return world == null ? null : world.getStories(); }
    Locations world() { return world; }
    void balance(int balance) { this.balance = balance; }
    void battle(SurvivorInfo player, List<? extends CombatEnemy> enemies, CombatEnemy target) {
        if (player != null) this.player = player;
        this.enemies = enemies;
        this.target = target;
    }
    void menu(String title, String hint) {
        atMain = false;
        reading = false;
        menu = new GameMenu.Builder(title, hint);
    }
    void shopTab(int number,String label,boolean selected) {menu.choices.add(new Choice(number,label,"",true,"shop_tab",selected));}
    void shopAction(int number,String label,String detail,String priceTag,boolean enabled) {
        Choice choice=new Choice(number,label,detail,enabled);choice.priceTag=priceTag;menu.choices.add(choice);
    }
    void removeActions(int... numbers){for(int n:numbers)menu.choices.removeIf(c->c.number==n);}
    void action(int number, String label, String detail, boolean enabled) {
        menu.choices.add(new Choice(number, label, detail, enabled));
    }
    void pointAction(int number,String label,String detail,boolean completed,boolean enabled) {
        menu.choices.add(new Choice(number,label,completed?"Полностью обыскано · "+detail:detail,enabled,completed?"point_searched":""));
    }
    void selectedAction(int number,String label,String detail,boolean enabled) {
        menu.choices.add(new Choice(number,label,detail,enabled,"",true));
    }
    void illustratedAction(int number,String label,String detail,String artwork) {
        menu.choices.add(new Choice(number,label,detail,true,artwork));
    }
    void card(String title, String detail) { menu.cards.add(new GameMenu.Card(title, detail)); }
    void illustratedCard(String title, String detail, String image) {
        menu.cards.add(new GameMenu.Card(title, detail, image));
    }
    void quantity(int max, int unitPrice) { menu.quantity = true; menu.maxQuantity = Math.max(0, max); menu.unitPrice = unitPrice; }
    void targeting() { menu.targeting = true; }
    void board(BattleBoard board) { menu.board = board; }
    void scene(String scene) {menu.scene=scene;}
    void checkpoint() {
        if(world==null || player==null || !player.isAlive() || player.difficulty()==3)return;
        try {GameSave.save(saveFile,player,balance,world);}
        catch(IOException e) {output.println("Не удалось сохранить попытку приёма: "+e.getMessage());}
    }

    private int read(CharBuffer target) throws IOException {
        if (closed) throw new CancellationException();
        if (!target.hasRemaining()) return 0;
        if (pendingInput.isEmpty()) {
            synchronized (this) {
                if(pendingNarrative){reading=true;pendingNarrative=false;}
                waiting = true;
                publish(atMain ? Mode.MAIN : Mode.CHOICE);
            }
            try {
                int number=input.take();
                if (closed) throw new CancellationException();
                buffer.reset();
                menu = new GameMenu.Builder("", "");
                atMain = false;
                pendingInput = number + "\n";
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new CancellationException();
            }
        }
        int length = Math.min(target.remaining(), pendingInput.length());
        target.put(pendingInput, 0, length);
        pendingInput = pendingInput.substring(length);
        return length;
    }

    public synchronized boolean submit(long expectedRevision, int number) {
        if (!waiting || closed || revision != expectedRevision) return false;
        if (lastWaiting.mode == Mode.CHOICE) {
            if (lastWaiting.quantity) {
                if (number < 0 || number > lastWaiting.menu.maxQuantity) return false;
            } else if (lastWaiting.choices.stream().noneMatch(choice -> choice.number == number && choice.enabled)) return false;
        } else if (number < 0 || number > 17) return false;
        waiting = false;
        publish(Mode.BUSY);
        return input.offer(number);
    }

    private void publish(Mode mode) {publish(mode,true);}
    private void publish(Mode mode,boolean newRevision) {
        if (closed) return;
        String text;
        try { text = buffer.toString("UTF-8").trim(); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
        if(newRevision)revision++;
        Frame frame = new Frame(revision, mode, text, player, balance, world == null ? 1 : world.getStories().day(),
                menu.build(), new GameStatus(player, enemies, target), clock() == null ? "08:00" : clock().time(), clock() != null && clock().isNight(),atHome,clock()==null?0:clock().currentPlace(),reading,pendingMusicCue);
        pendingMusicCue="";
        if (mode == Mode.MAIN || mode == Mode.CHOICE) lastWaiting = frame;
        listener.accept(frame);
    }

    @Override public synchronized void close() {
        foreground=false;activeClock.reset();
        closed = true;
        waiting = false;
        if (worker != null) worker.interrupt();
    }
}
