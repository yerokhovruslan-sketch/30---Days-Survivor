package com.example.survivor.game;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WorldStoriesTest {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static class Fixed extends Random {
        final int value;
        Fixed(int value) { this.value = value; }
        @Override public int nextInt(int bound) { return Math.min(value, bound - 1); }
    }
    static int rollFor(WorldStories world, int place, int id) {
        int result = 0;
        for (WorldStories.Story story : world.candidates(place)) {
            if (story.id() == id) return result;
            result += story.weight();
        }
        throw new AssertionError("Unavailable story " + id + " at " + place);
    }
    static WorldStories.Discovery pick(WorldStories world, int place, int id) {
        world.sleep();
        world.beginVisit(place, "Место " + place, new Fixed(0));
        WorldStories.Discovery result = world.discover(place, "Место " + place, new Fixed(rollFor(world, place, id)), true);
        check(result != null && result.id() == id, "Selected event " + id);
        return result;
    }
    static void catalogueAndPlacement() {
        check(WorldStories.catalogue().size() == 90, "All 90 entries exist");
        Set<Integer> ids = new HashSet<>();
        int notes = 0;
        for (WorldStories.Story story : WorldStories.catalogue()) {
            check(ids.add(story.id()), "Unique catalogue ID");
            check(!story.title().isBlank() && story.text().length() > 40, "Nonempty narrative");
            check(story.places().length > 0 && Arrays.stream(story.places()).allMatch(n -> n >= 1 && n <= 50), "Valid locations");
            if (story.id() >= 51 && story.id() <= 75) {
                notes++;
                check(story.title().startsWith("Записка:"), "Notes have journal titles");
            }
        }
        check(notes == 25 && ids.size() == 90, "Exactly 25 notes");
        WorldStories world = new WorldStories();
        check(world.candidates(12).stream().noneMatch(s -> s.id() == 1 || s.id() == 9), "No bedroom or classroom in hospital");
        check(world.candidates(12).stream().anyMatch(s -> s.id() == 24), "Hospital scenes in hospital");
        check(world.candidates(8).stream().anyMatch(s -> s.id() == 9), "Classroom in school");
        check(world.candidates(2).stream().noneMatch(s -> s.id() == 46), "No office computer in forest");
        check(WorldStories.catalogue().get(0).weight() < WorldStories.catalogue().get(87).weight(), "Tragedies rarer than ordinary rooms");
    }
    static void pacingAndRepetition() {
        WorldStories world = new WorldStories();
        pick(world, 5, 2);
        check(world.candidates(5).stream().noneMatch(s -> s.id() == 2), "Story is not rolled again");
        check(world.discover(5, "Дом", new Fixed(0), true) == null, "One independent story per visit");
        world.beginVisit(28, "Посёлок", new Fixed(0));
        check(world.discover(28, "Посёлок", new Fixed(0), true) == null, "Cooldown between visits");
        Random random = new Random(713);
        int ordinary = 0;
        for (int i = 0; i < 500; i++) {
            WorldStories fresh = new WorldStories();
            fresh.beginVisit(5, "Дом", random);
            if (fresh.discover(5, "Дом", random, true) == null) ordinary++;
        }
        check(ordinary > 230 && ordinary < 320, "Higher story frequency still leaves ordinary visits");
    }
    static void chainsAndReturns() {
        WorldStories world = new WorldStories();
        pick(world, 1, 76);
        check(world.candidates(1).stream().noneMatch(s -> s.id() == 76), "Chalk needs a new place");
        WorldStories.Discovery second = pick(world, 3, 76);
        check(second.text().contains("снова"), "Chalk continuation");
        WorldStories.Discovery third = pick(world, 5, 76);
        check(third.text().contains("ночлег"), "Chalk explanation");
        check(world.candidates(7).stream().noneMatch(s -> s.id() == 76), "Chalk chain ends");
        pick(world, 8, 86); pick(world, 10, 86);
        check(pick(world, 12, 86).text().contains("помоги"), "Rescuer chain progresses");
        WorldStories rooms = new WorldStories();
        rooms.beginVisit(5, "Дом", new Fixed(0));
        check(rooms.candidates(5).stream().noneMatch(s -> s.id() == 89), "No return story on first visit");
        rooms.discover(5, "Дом", new Fixed(rollFor(rooms, 5, 88)), true);
        check(pick(rooms, 5, 89).text().contains("которую ты уже обыскивал"), "Return remembers previous search");
        WorldStories radio = new WorldStories();
        pick(radio, 5, 78);
        WorldStories.Discovery silence = radio.discover(5, "Дом", new Fixed(0), true);
        check(silence != null && silence.title().equals("Радио замолчало"), "Radio battery runs out");
        check(radio.discover(5, "Дом", new Fixed(0), true) == null, "Radio stops only once");
    }
    static void photograph() {
        WorldStories world = new WorldStories();
        pick(world, 5, 77);
        world.beginVisit(35, "Гараж", new Fixed(0));
        world.observePeople(new Fixed(0));
        check(world.journal().size() == 1, "Recognition is delayed");
        world.beginVisit(2, "Лес", new Fixed(99));
        for (int i = 0; i < 4; i++) world.discover(2, "Лес", new Fixed(0), true);
        world.sleep(); world.sleep();
        world.beginVisit(35, "Гараж", new Fixed(0));
        world.observePeople(new Fixed(0));
        check(world.journal().size() == 2 && world.journal().get(1).contains("Лицо с фотографии"), "Recognizes a real hostile encounter later");
        world.observePeople(new Fixed(0));
        check(world.journal().size() == 2, "Recognition is one-time");
    }
    static void donationsAndPersistence() throws Exception {
        Locations locations = new Locations();
        WorldStories world = locations.getStories();
        Fighter player = new Fighter("John", "", 100);
        Food food = new Food("Галеты", "", 1, 3);
        player.addItem(food);
        world.beginVisit(5, "Дом", new Fixed(0));
        world.leaveSupplies(player, new Scanner("2"));
        check(!player.getInventory().contains(food), "Donation consumes real supplies");
        check(world.candidates(5).stream().noneMatch(s -> s.id() == 90), "No immediate thank-you");
        world.sleep();
        check(world.candidates(5).stream().noneMatch(s -> s.id() == 90), "Wait several days");
        Path path = Path.of(".build/world-stories-test.dat");
        GameSave.save(path.toFile(), player, 15, locations);
        WorldStories restored = GameSave.load(path.toFile()).locations.getStories();
        restored.sleep();
        WorldStories.Discovery thanks = pick(restored, 5, 90);
        check(thanks.items().isEmpty() && thanks.text().contains("Спасибо"), "Thank-you is a story, not a reward");
        check(restored.candidates(5).stream().noneMatch(s -> s.id() == 90), "No repeated thank-you");
        pick(restored, 1, 76);
        // Serialize the world holding the discovered event and in-progress marker chain.
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(Path.of(".build/stories-state.dat")))) {
            output.writeObject(restored);
        }
        WorldStories copy;
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(Path.of(".build/stories-state.dat")))) {
            copy = (WorldStories) input.readObject();
        }
        check(copy.hasSeen(90) && copy.journal().size() == restored.journal().size(), "Journal and seen state persist");
        check(pick(copy, 3, 76).text().contains("снова"), "Chain continues after loading");
        pick(world, 5, 25);
        GameSave.save(path.toFile(), player, 15, locations);
        check(GameSave.load(path.toFile()).locations.getStories().hasSeen(25), "GameSave includes journal and memories");
    }
    static void searchIntegration() {
        Locations world = new Locations();
        Location location = world.getLocation(5);
        WorldStories stories = world.getStories();
        Fighter player = new Fighter("John", "", 100);
        stories.beginVisit(5, location.getName(), new Fixed(0));
        int roll = rollFor(stories, 5, 2);
        Survivor.searchLocation(player, location, world, new Scanner(""), new Fixed(roll));
        check(location.getRemainingSearches() == location.getSearchCapacity() - 1, "Narrative spends one search site");
        check(player.getInventory().size() == 1, "Narrative does not also generate ordinary loot");
        stories.beginVisit(5, location.getName(), new Fixed(99));
        Survivor.searchLocation(player, location, world, new Scanner("0"), new Fixed(0));
        check(location.getRemainingSearches() == location.getSearchCapacity() - 2, "Ordinary search still works");
        WorldStories reward = new WorldStories();
        check(pick(reward, 1, 4).items().get(0) instanceof Food, "Backpack scene offers actual food");
        stories.showJournal(new Scanner("1 0"));
        check(stories.journal().size() == 1, "Reading journal does not duplicate entries");
    }
    @org.junit.Test public void originalRegression() throws Exception { java.nio.file.Files.createDirectories(java.nio.file.Path.of(".build")); main(new String[0]); }
    public static void main(String[] args) throws Exception {
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            catalogueAndPlacement(); pacingAndRepetition(); chainsAndReturns(); photograph();
            donationsAndPersistence(); searchIntegration();
        } finally { System.setOut(original); }
        System.out.println("World stories tests passed");
    }
}
