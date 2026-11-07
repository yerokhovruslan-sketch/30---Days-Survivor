package com.example.survivor;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.ViewCompat;
import com.example.survivor.game.GameMenu;
import com.example.survivor.game.GameSession;
import com.example.survivor.game.GameStatus;

/** Renders explicit screens. Narrative, state and controls never share a text block. */
final class MenuRenderer {
    interface Listener {
        void action(int number);
        void dismiss();
        void restart();
        void close();
        void click();
        default void unavailable(){}
    }
    private static final int INK = Color.rgb(235, 235, 235);
    private static final int MUTED = Color.rgb(175, 175, 175);
    private final Context context;
    private final ItemIcons itemIcons;
    private final View root;
    private final Listener listener;
    private final LinearLayout choices, footer, slot, cards, quantityPanel, tacticalStatus;
    private BattleBoardView boardView;
    private boolean wasBoard;
    private boolean eventsExpanded, gearExpanded;
    private long quantityRevision = -1, renderedRevision = -1;
    private int amount = 1;

    MenuRenderer(View root, Listener listener, Bundle state) {
        this.root = root;
        context = root.getContext();
        itemIcons = new ItemIcons(context);
        this.listener = listener;
        choices = root.findViewById(R.id.section_choices);
        footer = root.findViewById(R.id.footer_actions);
        slot = root.findViewById(R.id.status_slot);
        tacticalStatus = root.findViewById(R.id.tactical_status);
        cards = root.findViewById(R.id.menu_cards);
        quantityPanel = root.findViewById(R.id.quantity_panel);
        if (state != null) {
            eventsExpanded = state.getBoolean("events_expanded");
            gearExpanded = state.getBoolean("gear_expanded");
            quantityRevision = state.getLong("quantity_revision", -1);
            renderedRevision = state.getLong("rendered_revision", -1);
            amount = state.getInt("quantity_amount", 1);
        }
    }

    private int locationArtwork(String key) {
        if (key.startsWith("clinic_") || key.startsWith("pharmacy_") || key.startsWith("hospital_")) {
            int image=context.getResources().getIdentifier(key,"drawable",context.getPackageName());
            if(image!=0)return image;
            return key.equals("clinic_exterior") || key.equals("pharmacy_exterior") || key.equals("hospital_exterior")?R.drawable.location_medical:0;
        }
        return switch (key) {
            case "location_house" -> R.drawable.location_house;
            case "location_food" -> R.drawable.location_food;
            case "location_medical" -> R.drawable.location_medical;
            case "location_forest" -> R.drawable.location_forest;
            case "location_lake" -> R.drawable.location_lake;
            case "location_workshop" -> R.drawable.location_workshop;
            case "location_mine" -> R.drawable.location_mine;
            case "location_railway" -> R.drawable.location_railway;
            case "location_military" -> R.drawable.location_military;
            case "location_laboratory" -> R.drawable.location_laboratory;
            case "location_shelter" -> R.drawable.location_shelter;
            case "location_night" -> R.drawable.location_night;
            default -> MenuArtwork.route(key);
        };
    }
    private int stateArtwork(String key) {
        return switch (key) {
            case "OPEN" -> R.drawable.state_open;
            case "LOOTED" -> R.drawable.state_looted;
            case "LOCKED" -> R.drawable.state_locked;
            case "FLOODED" -> R.drawable.state_flooded;
            case "INFESTED" -> R.drawable.state_infested;
            case "RAIDERS" -> R.drawable.state_raiders;
            default -> 0;
        };
    }

    void save(Bundle state) {
        state.putBoolean("events_expanded", eventsExpanded);
        state.putBoolean("gear_expanded", gearExpanded);
        state.putLong("quantity_revision", quantityRevision);
        state.putLong("rendered_revision", renderedRevision);
        state.putInt("quantity_amount", amount);
    }

    void render(GameSession.Frame frame, String fallbackTitle) {
        TextView title = root.findViewById(R.id.section_title);
        if (frame.mode == GameSession.Mode.BUSY) {
            title.setText(R.string.processing);
            setEnabled(choices, false);
            setEnabled(footer, false);
            setEnabled(quantityPanel, false);
            setEnabled(cards, false);
            return;
        }
        setEnabled(choices, true);
        setEnabled(footer, true);
        setEnabled(quantityPanel, true);
        setEnabled(cards, true);
        title.setText(frame.menu.title.isEmpty() ? fallbackTitle : frame.menu.title);
        TextView hint = root.findViewById(R.id.section_hint);
        hint.setText(DialogueColors.decorate(frame.menu.hint));
        hint.setVisibility(frame.menu.hint.isEmpty() ? View.GONE : View.VISIBLE);
        renderStatus(frame);
        boolean guide = frame.menu.title.equals("Гайд");
        boolean locationPreview = frame.menu.title.equals("Локация");
        if (guide || locationPreview) slot.setVisibility(View.GONE);
        LinearLayout content = (LinearLayout) cards.getParent();
        View eventCard = root.findViewById(R.id.event_card);
        int desiredEventIndex = frame.menu.board != null ? content.getChildCount()-1 : 1;
        if(content.indexOfChild(eventCard)!=desiredEventIndex) {
            content.removeView(eventCard);content.addView(eventCard,desiredEventIndex);
        }
        renderEvents(frame.text);
        if (frame.menu.board != null && !eventsExpanded) ((TextView) root.findViewById(R.id.section_text)).setMaxLines(3);
        cards.removeAllViews();
        if (frame.menu.board != null){
            if(boardView==null)boardView=new BattleBoardView(context,frame.menu.board,number->{listener.click();listener.action(number);});
            else boardView.update(frame.menu.board);
            append(cards,boardView,10);
        }
        java.util.List<GameSession.Choice> tabs=new java.util.ArrayList<>();
        for(GameSession.Choice c:frame.choices)if("shop_tab".equals(c.artwork))tabs.add(c);
        if(!tabs.isEmpty()) {
            LinearLayout tabRow=new LinearLayout(context);tabRow.setOrientation(LinearLayout.HORIZONTAL);
            for(GameSession.Choice tab:tabs) {
                TextView view=text(tab.label,12,INK,true);view.setGravity(Gravity.CENTER);view.setPadding(dp(3),dp(12),dp(3),dp(12));
                view.setBackgroundResource(tab.selected?R.drawable.action_primary:R.drawable.menu_button);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(2),0,dp(2),0);tabRow.addView(view,lp);
                view.setOnClickListener(v->{if(TapFeedback.run(v,()->listener.action(tab.number)))listener.click();});
            }
            append(cards,tabRow,10);
        }
        for (GameMenu.Card card : frame.menu.cards) {
            if (card.image != null && (card.image.startsWith("clinic_art:") || card.image.startsWith("warehouse_art:") || card.image.startsWith("hospital_art:"))) {
                int image=locationArtwork(card.image.substring(card.image.indexOf(':')+1));
                if(image!=0){ImageView picture=new ImageView(context);picture.setImageResource(image);picture.setColorFilter(ArtworkTone.location());picture.setAdjustViewBounds(true);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);picture.setContentDescription(frame.menu.title);append(cards,picture,10);}
                continue;
            }
            if ("__location_metrics".equals(card.title)) {
                append(cards, locationMetrics(card.detail), 10);
                continue;
            }
            if (card.image != null && card.image.startsWith("location_state:")) {
                append(cards, locationStateCard(card.image.substring("location_state:".length())), 10);
                continue;
            }
            LinearLayout body = column();
            body.setBackgroundResource(R.drawable.information_panel);
            body.setPadding(dp(14), dp(14), dp(14), dp(14));
            if (card.image != null && card.image.startsWith("body:")) {
                body.addView(bodyDiagram(card.image.substring("body:".length())),
                        new LinearLayout.LayoutParams(-1, dp(420)));
            } else {
            String imageKey = card.image != null && card.image.startsWith("location_hero:")
                    ? card.image.substring("location_hero:".length()) : card.image;
            int artwork = locationArtwork(imageKey);
            if (artwork != 0) {
                ImageView picture = new ImageView(context);
                picture.setImageResource(artwork);
                picture.setAdjustViewBounds(true);
                picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
                picture.setContentDescription(frame.menu.title);
                picture.setColorFilter(ArtworkTone.location());
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, -2);
                imageParams.bottomMargin = dp(12);
                body.addView(picture, imageParams);
            }
            }
            body.addView(text(card.title, guide ? 21 : 18, guide ? Color.WHITE : INK, true));
            TextView detail = text(card.detail, guide ? 16 : 14, guide ? Color.WHITE : MUTED, false);
            detail.setText(DialogueColors.decorate(detail.getText()));
            if (guide) detail.setLineSpacing(dp(4), 1.12f);
            detail.setPadding(0, dp(8), 0, 0);
            detail.setTextIsSelectable(true);
            body.addView(detail);
            append(cards, body, 12);
        }
        choices.removeAllViews();
        footer.removeAllViews();
        quantityPanel.removeAllViews();
        quantityPanel.setVisibility(frame.quantity ? View.VISIBLE : View.GONE);
        if (frame.mode == GameSession.Mode.MAIN) {
            append(footer, button(context.getString(R.string.back_to_menu), "", false, true, listener::dismiss), 0);
        } else if (frame.mode == GameSession.Mode.CHOICE) {
            if (frame.quantity) renderQuantity(frame);
            else for (GameSession.Choice choice : frame.choices) {
                if ("shop_tab".equals(choice.artwork)) continue;
                if (frame.menu.board != null && choice.number >= 100 && choice.number < 164) continue;
                if (frame.menu.targeting && choice.number > 0) continue;
                if ("location_depart".equals(choice.artwork)) {
                    append(choices, locationDeparture(choice), 10);
                    continue;
                }
                if (locationPreview && choice.number == 0) {
                    // The route actions must stay together; a footer pins "Back" to the screen bottom.
                    append(choices, locationBack(choice), 8);
                    continue;
                }
                if(!choice.priceTag.isEmpty()) {
                    append(choices,shopRow(choice),10);continue;
                }
                boolean difficulty = frame.menu.title.equals("Уровень сложности");
                AppCompatButton action = button(choice.label, difficulty ? "" : choice.detail,
                        choice.selected, choice.enabled, () -> listener.action(choice.number));
                if(choice.selected)action.setAlpha(1f);
                if ("point_searched".equals(choice.artwork)) action.setAlpha(choice.enabled ? .48f : .35f);
                int background=MenuArtwork.route(choice.artwork);
                if(background!=0) {MenuArtwork.apply(action,background);action.setMinHeight(dp(84));}
                if (frame.menu.board != null && choice.number != 0) {
                    LinearLayout row = choices.getChildCount() == 0 ? null : (LinearLayout) choices.getChildAt(choices.getChildCount()-1);
                    if (row == null || row.getChildCount() == 2) {
                        row = new LinearLayout(context); row.setOrientation(LinearLayout.HORIZONTAL);row.setBaselineAligned(false);
                        append(choices, row, 4);
                    }
                    action.setTextSize(14);action.setMinHeight(dp(64));action.setPadding(dp(8),dp(8),dp(8),dp(8));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,-2,1);
                    params.setMargins(dp(3),0,dp(3),0);row.addView(action,params);
                } else append(choice.number == 0 ? footer : choices, action, choice.number == 0 ? 0 : 10);
                if (difficulty && !choice.detail.isEmpty()) {
                    TextView description = text(choice.detail, 14, MUTED, false);
                    description.setPadding(dp(8), dp(4), dp(8), dp(10));
                    description.setLineSpacing(dp(3), 1f);
                    append(choices, description, 0);
                }
            }
        } else if (frame.mode == GameSession.Mode.ENDED) {
            append(choices, button(context.getString(R.string.restart_game), "", true, true, listener::restart), 10);
            append(footer, button(context.getString(R.string.close_game), "", false, true, listener::close), 0);
        }
        root.findViewById(R.id.choice_heading).setVisibility(choices.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        if (renderedRevision != frame.revision && frame.menu.board == null) ((ScrollView) root.findViewById(R.id.section_scroll)).scrollTo(0, 0);
        if(frame.menu.board!=null && !wasBoard)root.findViewById(R.id.section_scroll).post(()->((ScrollView)root.findViewById(R.id.section_scroll)).scrollTo(0,0));
        wasBoard=frame.menu.board!=null;
        renderedRevision = frame.revision;
    }

    private void renderEvents(String message) {
        root.findViewById(R.id.event_card).setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
        TextView content = root.findViewById(R.id.section_text);
        TextView toggle = root.findViewById(R.id.event_toggle);
        content.setText(DialogueColors.decorate(itemIcons.decorate(message)));
        content.setMaxLines(eventsExpanded ? Integer.MAX_VALUE : 6);
        content.setEllipsize(eventsExpanded ? null : TextUtils.TruncateAt.END);
        toggle.setText(eventsExpanded ? R.string.collapse_events : R.string.expand_events);
        toggle.setVisibility(message.length() > 350 || message.split("\n").length > 6 ? View.VISIBLE : View.GONE);
        toggle.setSoundEffectsEnabled(false);
        toggle.setOnClickListener(view -> {listener.click(); eventsExpanded = !eventsExpanded; renderEvents(message); });
    }

    private void renderStatus(GameSession.Frame frame) {
        slot.removeAllViews();
        tacticalStatus.removeAllViews();
        tacticalStatus.setVisibility(frame.hasPlayer && frame.menu.board!=null?View.VISIBLE:View.GONE);
        slot.setVisibility(frame.hasPlayer && frame.menu.board==null ? View.VISIBLE : View.GONE);
        if (!frame.hasPlayer) return;
        if (frame.menu.board != null) {
            LinearLayout john=column();john.setPadding(dp(8),dp(8),dp(8),dp(8));john.setBackgroundResource(R.drawable.status_panel);
            john.addView(text("ДЖОН",14,INK,true));
            john.addView(text(frame.status.combatInjuries,12,INK,false));
            String needs="Силы "+frame.stamina+" · голод "+frame.hunger;
            SpannableString colors=new SpannableString(needs);
            int split=needs.indexOf(" · ");
            colors.setSpan(new ForegroundColorSpan(context.getColor(R.color.stat_stamina_color)),0,split,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            colors.setSpan(new ForegroundColorSpan(context.getColor(R.color.stat_hunger_color)),split+3,needs.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView needLine=text(needs,12,MUTED,false);needLine.setText(colors);john.addView(needLine);
            john.addView(text("Сон "+frame.sleepiness+" · шум "+frame.noise+" · броня "+frame.status.protection+"%",11,MUTED,false));
            LinearLayout.LayoutParams johnParams=new LinearLayout.LayoutParams(0,-2,1);johnParams.setMarginEnd(dp(6));tacticalStatus.addView(john,johnParams);
            LinearLayout target=column();target.setPadding(dp(8),dp(8),dp(8),dp(8));target.setBackgroundResource(R.drawable.status_panel);
            GameStatus.Enemy selected=frame.status.enemies.stream().filter(e->e.selected).findFirst().orElse(null);
            if(selected==null)target.addView(text(frame.status.enemies.isEmpty()?"Врагов не осталось":"Выберите врага на поле",13,INK,true));
            else {
                target.addView(text("ЦЕЛЬ: "+selected.name,13,INK,true));
                target.addView(text("Здоровье: "+selected.health+"/"+selected.maxHealth,13,context.getColor(R.color.stat_health_color),true));
                target.addView(text("Урон "+selected.minDamage+"–"+selected.maxDamage+" · точность "+selected.accuracy+"%",11,MUTED,false));
                TextView details=text(selected.details,11,MUTED,false);details.setMaxLines(2);details.setEllipsize(TextUtils.TruncateAt.END);target.addView(details);
            }
            tacticalStatus.addView(target,new LinearLayout.LayoutParams(0,-2,1));
            return;
        }
        boolean combat = !frame.status.enemies.isEmpty();
        slot.addView(text(context.getString(combat ? R.string.encounter_label : R.string.state_label), 12, MUTED, true));
        TextView time = text(context.getString(R.string.day_money_format, frame.day, frame.balance) + " · " + frame.time + " · " + context.getString(frame.night ? R.string.night_phase : R.string.day_phase), 13, MUTED, false);
        time.setPadding(0, dp(4), 0, dp(10));
        slot.addView(time);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout survivor = column();
        survivor.setBackground(box(0xFF232323, 0xFF515151));
        survivor.setPadding(dp(10), dp(10), dp(10), dp(10));
        survivor.addView(text(context.getString(R.string.character_name), 20, INK, true));
        survivor.addView(text(frame.status.injuries,12,INK,false));
        bar(survivor, context.getString(R.string.stamina), frame.stamina, 100, context.getColor(R.color.stat_stamina_color));
        bar(survivor, context.getString(R.string.hunger), frame.hunger, 100, context.getColor(R.color.stat_hunger_color));
        TextView needs = text(context.getString(R.string.player_needs_format, frame.hunger, frame.sleepiness, frame.noise), 12, MUTED, false);
        needs.setPadding(0, dp(8), 0, dp(8));
        survivor.addView(needs);
        survivor.addView(text(frame.status.weapon, 13, INK, true));
        if (!frame.status.ammo.isEmpty()) survivor.addView(text(frame.status.ammo, 12, MUTED, false));
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
        if (combat) left.setMarginEnd(dp(8));
        row.addView(survivor, left);
        if (combat) {
            LinearLayout opponents = column();
            opponents.addView(text(context.getString(R.string.opponents_label), 11, MUTED, true));
            GameStatus.Enemy selected = frame.status.enemies.stream().filter(enemy -> enemy.selected).findFirst().orElse(frame.status.enemies.get(0));
            append(opponents, enemyCard(frame, selected, true), 0);            row.addView(opponents, new LinearLayout.LayoutParams(0, -2, 1));
        }
        slot.addView(row);
        if (frame.status.enemies.size() > 1 || frame.menu.targeting) {
            android.widget.HorizontalScrollView roster = new android.widget.HorizontalScrollView(context);
            roster.setHorizontalScrollBarEnabled(true);
            LinearLayout strip = new LinearLayout(context);
            strip.setPadding(0, dp(10), 0, dp(6));
            for (GameStatus.Enemy enemy : frame.status.enemies) {
                LinearLayout card = enemyCard(frame, enemy, false);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(132), -2);
                params.setMarginEnd(dp(8));
                strip.addView(card, params);
            }
            roster.addView(strip);
            slot.addView(roster, new LinearLayout.LayoutParams(-1, -2));
        }
        TextView extra = text(context.getString(R.string.gear_format, frame.status.weapon, frame.status.weaponCondition,
                frame.status.armor, frame.status.protection, frame.status.load), 13, MUTED, false);
        extra.setPadding(0, dp(10), 0, 0);
        extra.setVisibility(gearExpanded ? View.VISIBLE : View.GONE);
        slot.addView(extra);
        TextView gear = text(context.getString(gearExpanded ? R.string.hide_gear_details : R.string.gear_details), 13, 0xFFC1C1C1, false);
        gear.setGravity(Gravity.CENTER_VERTICAL);
        gear.setMinHeight(dp(48));
        gear.setSoundEffectsEnabled(false);
        gear.setOnClickListener(view -> {
            listener.click();
            gearExpanded = !gearExpanded;
            extra.setVisibility(gearExpanded ? View.VISIBLE : View.GONE);
            gear.setText(gearExpanded ? R.string.hide_gear_details : R.string.gear_details);
        });
        slot.addView(gear);
    }

    /** Builds the supplied transparent body PNG and stacks one blood overlay per injured zone. */
    private View bodyDiagram(String encoded) {
        FrameLayout diagram = new FrameLayout(context);
        diagram.setBackground(box(0xFF171717, 0xFF494949));
        ImageView body = new ImageView(context);
        body.setImageResource(R.drawable.body_silhouette);
        body.setScaleType(ImageView.ScaleType.FIT_CENTER);
        body.setContentDescription("Схема травм Джона");
        diagram.addView(body, new FrameLayout.LayoutParams(-1, -1));
        if (encoded == null || encoded.isEmpty()) return diagram;

        for (String entry : encoded.split(";")) {
            String[] pair = entry.split(":");
            if (pair.length != 2) continue;
            int severity;
            try { severity = Integer.parseInt(pair[1]); } catch (NumberFormatException ignored) { continue; }
            // The splash source is naturally wide. Limbs use a narrow, tall crop so it sits on
            // the silhouette instead of looking like a red sticker floating beside the body.
            int width = dp(pair[0].equals("TORSO") ? 95 : pair[0].equals("HEAD") ? 72 : 48);
            int height = dp(pair[0].equals("TORSO") ? 95 : pair[0].equals("HEAD") ? 72 : 104);
            ImageView wound = new ImageView(context);
            wound.setImageResource(R.drawable.injury_blood);
            wound.setScaleType(ImageView.ScaleType.FIT_CENTER);
            wound.setAlpha(Math.min(0.92f, 0.32f + severity / 150f));
            int top;
            int shift = 0;
            float rotation = 0f;
            switch (pair[0]) {
                case "HEAD" -> { top = dp(16); rotation = -12f; }
                case "TORSO" -> { top = dp(150); rotation = 5f; }
                case "LEFT_ARM" -> { top = dp(146); shift = -dp(45); rotation = -7f; }
                case "RIGHT_ARM" -> { top = dp(146); shift = dp(48); rotation = 7f; }
                case "LEFT_LEG" -> { top = dp(262); shift = -dp(12); rotation = -5f; }
                case "RIGHT_LEG" -> { top = dp(262); shift = dp(16); rotation = 5f; }
                default -> { continue; }
            }
            wound.setRotation(rotation);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            params.topMargin = top;
            params.leftMargin = shift;
            diagram.addView(wound, params);
        }
        return diagram;
    }

    private View locationMetrics(String encoded) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        String[] values = encoded.split("\\|", -1);
        for (int i = 0; i < 3; i++) {
            LinearLayout chip = new LinearLayout(context);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(8), dp(9), dp(8), dp(9));
            chip.setBackground(box(0xFF202020, 0xFF4B4B4B));
            String[] lines = (i < values.length ? values[i] : "").split("\\n", 2);
            int icon = i == 0 ? R.drawable.location_icon_clock : i == 1 ? R.drawable.location_icon_search
                    : lines[0].contains("низкий") ? R.drawable.location_icon_risk_green
                    : lines[0].contains("средний") ? R.drawable.location_icon_risk_yellow : R.drawable.location_icon_risk_red;
            ImageView picture = locationIcon(icon, 2.15f);
            chip.addView(picture, new LinearLayout.LayoutParams(dp(23), dp(23)));
            LinearLayout words = column();
            words.setPadding(dp(6), 0, 0, 0);
            words.addView(text(lines[0], 13, i == 2 ? (lines[0].contains("низкий") ? 0xFF9AD99A : lines[0].contains("средний") ? 0xFFE7C76B : 0xFFE07168) : INK, true));
            if (lines.length > 1) words.addView(text(lines[1], 10, MUTED, false));
            chip.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            if (i > 0) params.setMarginStart(dp(5));
            row.addView(chip, params);
        }
        return row;
    }

    private View locationStateCard(String state) {
        LinearLayout body = column();
        body.setBackgroundResource(R.drawable.information_panel);
        body.setPadding(dp(2), dp(2), dp(2), dp(2));
        ImageView image = new ImageView(context);
        if ("LOCKED".equals(state)) setCroppedImage(image, R.drawable.state_locked, 24, 25, 1883, 762);
        else if ("OPEN".equals(state)) setCroppedImage(image, R.drawable.state_open, 22, 23, 1870, 767);
        else image.setImageResource(stateArtwork(state));
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("Состояние входа: " + state);
        body.addView(image, new LinearLayout.LayoutParams(-1, -2));
        return body;
    }

    private View locationDeparture(GameSession.Choice choice) {
        FrameLayout button = new FrameLayout(context);
        ImageView background = new ImageView(context);
        setCroppedImage(background, R.drawable.location_button_background, 49, 201, 2074, 318);
        background.setScaleType(ImageView.ScaleType.FIT_XY);
        button.addView(background, new FrameLayout.LayoutParams(-1, -1));
        ImageView john = new ImageView(context);
        setCroppedImage(john, R.drawable.location_button_john, 311, 152, 621, 924);
        john.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams johnParams = new FrameLayout.LayoutParams(dp(33), dp(48), Gravity.START | Gravity.CENTER_VERTICAL);
        johnParams.leftMargin = dp(16); button.addView(john, johnParams);
        TextView label = text(choice.label.toUpperCase(java.util.Locale.ROOT), 15, 0xFF171717, true);
        label.setGravity(Gravity.CENTER); label.setLetterSpacing(.025f);
        label.setSingleLine(true);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(label, 10, 15, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        label.setPadding(dp(58), 0, dp(52), 0); button.addView(label, new FrameLayout.LayoutParams(-1, -1));
        ImageView arrow = new ImageView(context);
        setCroppedImage(arrow, R.drawable.location_arrow_right, 306, 444, 639, 366);
        arrow.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(dp(30), dp(25), Gravity.END | Gravity.CENTER_VERTICAL);
        arrowParams.rightMargin = dp(15); button.addView(arrow, arrowParams);
        button.setContentDescription(choice.label);
        button.setClickable(true); button.setFocusable(true); button.setSoundEffectsEnabled(false);
        button.setOnClickListener(view -> { if (TapFeedback.run(view, () -> listener.action(choice.number))) listener.click(); });
        button.setMinimumHeight(dp(64));
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(64)));
        return button;
    }

    private View locationBack(GameSession.Choice choice) {
        LinearLayout button = new LinearLayout(context);
        button.setGravity(Gravity.CENTER_VERTICAL); button.setPadding(dp(16), dp(4), dp(16), dp(4));
        button.setBackgroundResource(R.drawable.menu_button);
        ImageView arrow = locationIcon(R.drawable.location_arrow_left, 1.7f);
        button.addView(arrow, new LinearLayout.LayoutParams(dp(26), dp(32)));
        TextView label = text(choice.label.toUpperCase(java.util.Locale.ROOT), 15, MUTED, true);
        label.setGravity(Gravity.CENTER_VERTICAL); label.setPadding(dp(4), 0, 0, 0);
        button.addView(label, new LinearLayout.LayoutParams(0, -1, 1));
        button.setContentDescription(choice.label); button.setClickable(true); button.setFocusable(true); button.setSoundEffectsEnabled(false);
        button.setOnClickListener(view -> { if (TapFeedback.run(view, () -> listener.action(choice.number))) listener.click(); });
        button.setMinimumHeight(dp(42));
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(42)));
        return button;
    }

    private void setCroppedImage(ImageView view, int resource, int x, int y, int width, int height) {
        android.graphics.Bitmap source = android.graphics.BitmapFactory.decodeResource(context.getResources(), resource);
        android.graphics.Bitmap cropped = android.graphics.Bitmap.createBitmap(source, x, y, width, height);
        view.setImageBitmap(cropped);
    }

    private ImageView locationIcon(int resource, float scale) {
        ImageView image = new ImageView(context);
        image.setImageResource(resource);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setScaleX(scale); image.setScaleY(scale);
        return image;
    }

    private LinearLayout enemyCard(GameSession.Frame frame, GameStatus.Enemy enemy, boolean detailed) {
        LinearLayout body = column();
        body.setPadding(dp(10), dp(10), dp(10), dp(10));
        body.setBackground(box(enemy.selected ? 0xFF2A2A2A : 0xFF1F1F1F, enemy.selected ? 0xFFA6A6A6 : 0xFF484848));
        body.addView(text(enemy.name, detailed ? 16 : 14, INK, true));
        if (enemy.selected || enemy.health == 0) body.addView(text(context.getString(enemy.health == 0
                ? R.string.defeated_enemy : R.string.selected_enemy), 10, 0xFFA6A6A6, true));
        bar(body, context.getString(R.string.health), enemy.health, enemy.maxHealth, context.getColor(R.color.stat_health_color));
        if (detailed) {
            bar(body, context.getString(R.string.stamina), enemy.stamina, 100, context.getColor(R.color.stat_stamina_color));
            bar(body, context.getString(R.string.hunger), enemy.hunger, 100, context.getColor(R.color.stat_hunger_color));
            body.addView(text(context.getString(R.string.enemy_stats_format, enemy.minDamage, enemy.maxDamage, enemy.accuracy), 12, MUTED, false));
            body.addView(text(enemy.details, 12, MUTED, false));
        }
        if (frame.menu.targeting && enemy.health > 0) {
            body.setClickable(true);
            body.setFocusable(true);
            body.setMinimumHeight(dp(64));
            body.setContentDescription(enemy.name + ", " + enemy.health + "/" + enemy.maxHealth);
            body.setForeground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x558C8C8C), null, null));
            body.setSoundEffectsEnabled(false);
            body.setOnClickListener(view -> {if(TapFeedback.run(view,()->listener.action(enemy.number)))listener.click();});
        }
        body.setAlpha(enemy.health == 0 ? 0.5f : 1f);
        return body;
    }
    private void renderQuantity(GameSession.Frame frame) {
        if (quantityRevision != frame.revision) { quantityRevision = frame.revision; amount = frame.menu.maxQuantity == 0 ? 0 : 1; }
        quantityPanel.addView(text(context.getString(R.string.quantity_label), 16, INK, true));
        LinearLayout stepper = new LinearLayout(context);
        stepper.setGravity(Gravity.CENTER_VERTICAL);
        AppCompatButton minus = button("−", "", false, amount > 1, () -> updateAmount(frame, -1));
        minus.setContentDescription(context.getString(R.string.decrease_quantity));
        TextView value = text(context.getString(R.string.amount_format, amount), 26, INK, true);
        value.setGravity(Gravity.CENTER);
        AppCompatButton plus = button("+", "", false, amount < frame.menu.maxQuantity, () -> updateAmount(frame, 1));
        plus.setContentDescription(context.getString(R.string.increase_quantity));
        stepper.addView(minus, new LinearLayout.LayoutParams(dp(64), -2));
        stepper.addView(value, new LinearLayout.LayoutParams(0, -2, 1));
        stepper.addView(plus, new LinearLayout.LayoutParams(dp(64), -2));
        append(quantityPanel, stepper, 10);
        if (frame.menu.maxQuantity > 0) {
            LinearLayout presets = new LinearLayout(context);
            for (int preset : new int[] {1, 5, 10}) {
                AppCompatButton option = button(Integer.toString(preset), "", false, preset <= frame.menu.maxQuantity,
                        () -> { amount = preset; rerenderQuantity(frame); });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                params.setMargins(dp(2), 0, dp(2), 0);
                presets.addView(option, params);
            }
            append(quantityPanel, presets, 10);
        } else quantityPanel.addView(text(context.getString(R.string.quantity_unavailable), 14, MUTED, false));
        append(quantityPanel, button(context.getString(R.string.buy_quantity_format, amount, (long) amount * frame.menu.unitPrice),
                "", true, amount > 0, () -> listener.action(amount)), 0);
        append(footer, button(context.getString(android.R.string.cancel), "", false, true, () -> listener.action(0)), 0);
    }

    private void updateAmount(GameSession.Frame frame, int delta) {
        amount = Math.max(1, Math.min(frame.menu.maxQuantity, amount + delta));
        rerenderQuantity(frame);
    }
    private void rerenderQuantity(GameSession.Frame frame) {
        quantityPanel.removeAllViews();
        footer.removeAllViews();
        renderQuantity(frame);
    }
    private void bar(LinearLayout parent, String label, int value, int max, int color) {
        TextView caption = text(context.getString(R.string.bar_format, label, value, max), 12, INK, false);
        caption.setPadding(0, dp(7), 0, dp(4));
        parent.addView(caption);
        ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(max);
        progress.setProgress(value);
        progress.setProgressTintList(ColorStateList.valueOf(color));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF444444));
        progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        parent.addView(progress, new LinearLayout.LayoutParams(-1, dp(6)));
    }
    private int shopStatIcon(String line) {
        if(line.startsWith("Энергия:"))return R.drawable.shop_stat_energy;
        if(line.startsWith("Еда:"))return R.drawable.shop_stat_food;
        if(line.startsWith("Шум:"))return R.drawable.shop_stat_noise;
        if(line.startsWith("В наличии:"))return R.drawable.shop_stat_stock;
        if(line.startsWith("Лечение:"))return R.drawable.shop_stat_heal;
        if(line.startsWith("Урон:"))return R.drawable.shop_stat_damage;
        if(line.startsWith("Попадание:"))return R.drawable.shop_stat_accuracy;
        if(line.startsWith("Поставка через:"))return R.drawable.shop_stat_clock;
        return 0;
    }
    private View shopRow(GameSession.Choice choice) {
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(9),dp(10),dp(9),dp(10));row.setBackgroundResource(R.drawable.menu_button);
        int art=itemIcons.resourceFor(choice.label);
        if(art!=0) {ImageView product=new ImageView(context);product.setImageResource(art);product.setScaleType(ImageView.ScaleType.FIT_CENTER);product.setContentDescription(choice.label);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(55),dp(58));ip.setMarginEnd(dp(8));row.addView(product,ip);}
        LinearLayout left=column();left.addView(text(choice.label,16,INK,true));
        for(String line:choice.detail.split("\n")) {
            if(line.isEmpty())continue;
            LinearLayout stat=new LinearLayout(context);stat.setOrientation(LinearLayout.HORIZONTAL);stat.setGravity(Gravity.CENTER_VERTICAL);stat.setPadding(0,dp(2),0,0);
            int icon=shopStatIcon(line);if(icon!=0){ImageView view=new ImageView(context);view.setImageResource(icon);view.setContentDescription(null);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(14),dp(14));iconParams.setMarginEnd(dp(5));stat.addView(view,iconParams);}
            TextView label=text(line,11,MUTED,false);stat.addView(label,new LinearLayout.LayoutParams(0,-2,1));left.addView(stat);
        }
        row.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout buy=column();buy.setGravity(Gravity.CENTER);buy.setPadding(dp(6),0,0,0);
        TextView price=text(choice.priceTag,17,INK,true);price.setGravity(Gravity.CENTER);CharSequence raw=price.getText();SpannableString label=new SpannableString(raw);int line=raw.toString().indexOf('\n');
        if(line>=0){label.setSpan(new AbsoluteSizeSpan(10,true),line+1,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);label.setSpan(new ForegroundColorSpan(MUTED),line+1,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}price.setText(label);buy.addView(price);
        TextView button=text(choice.enabled?"Купить":"Нет в наличии",11,INK,true);button.setGravity(Gravity.CENTER);button.setPadding(dp(7),dp(5),dp(7),dp(5));button.setBackgroundResource(choice.enabled?R.drawable.action_primary:R.drawable.menu_button);buy.addView(button);row.addView(buy,new LinearLayout.LayoutParams(dp(79),-2));
        row.setAlpha(choice.enabled?1f:0.43f);row.setClickable(true);row.setFocusable(true);row.setContentDescription(choice.label+". "+choice.priceTag+". "+choice.detail);
        row.setOnClickListener(v->{if(!choice.enabled){listener.unavailable();return;}if(TapFeedback.run(v,()->listener.action(choice.number)))listener.click();});return row;
    }
    private AppCompatButton button(String title, String detail, boolean primary, boolean enabled, Runnable action) {
        AppCompatButton button = new AppCompatButton(context){
            @Override public boolean onTouchEvent(android.view.MotionEvent event){if(!isEnabled() && event.getAction()==android.view.MotionEvent.ACTION_UP)listener.unavailable();return super.onTouchEvent(event);}
        };
        String combined = detail.isEmpty() ? title : title + "\n" + detail;
        SpannableString label = new SpannableString(combined);
        label.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!detail.isEmpty()) {
            label.setSpan(new AbsoluteSizeSpan(13, true), title.length() + 1, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            label.setSpan(new ForegroundColorSpan(MUTED), title.length() + 1, combined.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        button.setText(itemIcons.decorate(label));
        button.setTextSize(16);
        button.setTextColor(INK);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(dp(14), dp(12), dp(14), dp(12));
        button.setMinHeight(dp(56));
        button.setBackgroundResource(primary ? R.drawable.action_primary : R.drawable.menu_button);
        ViewCompat.setBackgroundTintList(button, null);
        if (title.equals("Положить вещи на склад") || title.equals("Забрать вещи со склада"))
            MenuArtwork.apply(button, R.drawable.menu_art_warehouse);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
        button.setSoundEffectsEnabled(false);
        button.setOnClickListener(view -> {if(TapFeedback.run(view,action))listener.click();});
        return button;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(itemIcons.decorate(value));
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }
    private GradientDrawable box(int fill, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(8));
        shape.setStroke(dp(1), stroke);
        return shape;
    }
    private void append(LinearLayout parent, View child, int bottom) {
        LinearLayout.LayoutParams params = child.getLayoutParams() instanceof LinearLayout.LayoutParams ? (LinearLayout.LayoutParams) child.getLayoutParams() : new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(bottom);
        parent.addView(child, params);
    }
    private void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) setEnabled(group.getChildAt(i), enabled);
        }
    }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}

