package com.example.survivor.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A screen is data, not a numbered paragraph that the Android UI has to parse. */
public final class GameMenu {
    public final String title;
    public final String hint;
    public final List<GameSession.Choice> choices;
    public final List<Card> cards;
    public final boolean quantity;
    public final boolean targeting;
    public final boolean story;
    public final int maxQuantity;
    public final int unitPrice;
    public final BattleBoard board;
    public final String scene;

    public static final class Card {
        public final String title, detail, image;
        Card(String title, String detail) { this(title, detail, ""); }
        Card(String title, String detail, String image) {
            this.title = title; this.detail = detail; this.image = image;
        }
    }

    GameMenu(String title, String hint, List<GameSession.Choice> choices, List<Card> cards, boolean quantity, int maxQuantity, int unitPrice, boolean targeting, BattleBoard board,String scene,boolean story) {
        this.title = title;
        this.hint = hint;
        this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
        this.cards = Collections.unmodifiableList(new ArrayList<>(cards));
        this.quantity = quantity;
        this.maxQuantity = maxQuantity;
        this.unitPrice = unitPrice;
        this.targeting = targeting;
        this.board = board;
        this.scene=scene;
        this.story=story;
    }

    static final class Builder {
        final String title, hint;
        final List<GameSession.Choice> choices = new ArrayList<>();
        final List<Card> cards = new ArrayList<>();
        boolean quantity;
        boolean targeting;
        boolean story;
        BattleBoard board;
        String scene="";
        int maxQuantity;
        int unitPrice;
        Builder(String title, String hint) { this.title = title; this.hint = hint; }
        GameMenu build() { return new GameMenu(title, hint, choices, cards, quantity, maxQuantity, unitPrice, targeting, board,scene,story); }
    }
}
