from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'ActiveClock.java';s=p.read_text(encoding='utf-8').replace('2_500_000_000L','5_000_000_000L').replace('    int tick(', '    void reset(){remainder=0;}\n    int tick(');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MainActivity.java');s=p.read_text(encoding='utf-8').replace('×24','×12').replace('сутки за реальный час', 'сутки за два реальных часа');p.write_text(s,encoding='utf-8')
p=b/'GameSession.java';s=p.read_text(encoding='utf-8').replace('private final ActiveClock activeClock=new ActiveClock();','private final ActiveClock activeClock=new ActiveClock();')
s=s.replace('if(!foreground || manualPause', 'if(player==null || !player.isAlive() || !foreground || manualPause')
s=s.replace('    void mainMenu(SurvivorInfo player, int balance, Locations world) {','''    synchronized void mainMenu(SurvivorInfo player, int balance, Locations world) {
        if(this.world!=world){activeClock.reset();realtimeStamp=System.nanoTime();reading=false;pendingNarrative=false;menu=new GameMenu.Builder("","");}
''')
s=s.replace('        closed = true;', '        foreground=false;activeClock.reset();\n        closed = true;')
# Explicit presentation data instead of parsing price out of prose.
s=s.replace('public final String label, detail, artwork;', 'public final String label, detail, artwork;\n        public String priceTag="";')
s=s.replace('    void action(int number, String label, String detail, boolean enabled) {','''    void shopAction(int number,String label,String detail,String priceTag) {
        Choice choice=new Choice(number,label,detail,true);choice.priceTag=priceTag;menu.choices.add(choice);
    }
    void action(int number, String label, String detail, boolean enabled) {''')
p.write_text(s,encoding='utf-8')
p=b/'GameIO.java';s=p.read_text(encoding='utf-8').replace('    static void action(int number, String label) {','    static void shopAction(int number,String label,String detail,String priceTag){if(CURRENT.get()!=null)CURRENT.get().shopAction(number,label,detail,priceTag);}\n    static void action(int number, String label) {');p.write_text(s,encoding='utf-8')
p=b/'Shop.java';s=p.read_text(encoding='utf-8');a=s.index('            String details = price(i)');z=s.index('\n        }\n        GameIO.action(7',a)
s=s[:a]+'''            int stock=clock()==null?Integer.MAX_VALUE:clock().craftStock(i);
            if(stock<=0)continue;
            String details;
            if(item instanceof Weapons)details=((Weapons)item).combatStats();
            else if(item instanceof MedicalItem)details="Здоровье +"+((MedicalItem)item).getHealing();
            else if(item instanceof Food)details="Голод −"+((Food)item).getNutrition();
            else if(item.getArmorProtection()>0)details="Защита "+item.getArmorProtection()+"%";
            else details=item.getName().equals("Электрочип")?"Для восстановления радиостанции":item.getName().equals("Фонарик")?"Помогает замечать врагов ночью":"";
            int count=item instanceof Ammunition?((Ammunition)item).getQuantity():1;
            String tag=count==1?price(i)+" $\\nза 1 шт.":String.format(java.util.Locale.ROOT,"%.2f $\\nза 1 шт.\\n%d шт. — %d $",price(i)/(double)count,count,price(i));
            GameIO.shopAction(i,item.getName()+(count>1?" ×"+count:""),details+(details.isEmpty()?"":"\\n")+"В наличии: "+(stock==Integer.MAX_VALUE?"—":stock),tag);''' +s[z:]
s=s.replace('        GameIO.action(7, "Лечиться");\n','')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MenuRenderer.java');s=p.read_text(encoding='utf-8').replace('                AppCompatButton action = button(choice.label', '''                if(!choice.priceTag.isEmpty()) {
                    append(choices,shopRow(choice),10);continue;
                }
                AppCompatButton action = button(choice.label''');at=s.index('    private AppCompatButton button(')
s=s[:at]+'''    private View shopRow(GameSession.Choice choice) {
        LinearLayout row=new LinearLayout(context);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14),dp(14),dp(14),dp(14));row.setBackgroundResource(R.drawable.menu_button);
        LinearLayout left=column();left.addView(text(choice.label,17,INK,true));
        if(!choice.detail.isEmpty()){TextView detail=text(choice.detail,13,MUTED,false);detail.setPadding(0,dp(6),0,0);left.addView(detail);}
        row.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        TextView price=text(choice.priceTag,22,INK,true);price.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        CharSequence raw=price.getText();SpannableString label=new SpannableString(raw);int line=raw.toString().indexOf('\\n');
        if(line>=0){label.setSpan(new AbsoluteSizeSpan(11,true),line+1,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);label.setSpan(new ForegroundColorSpan(MUTED),line+1,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        price.setText(label);price.setPadding(dp(12),0,0,0);row.addView(price,new LinearLayout.LayoutParams(-2,-2));
        row.setClickable(true);row.setFocusable(true);row.setContentDescription(choice.label+". "+choice.priceTag+". "+choice.detail);
        row.setOnClickListener(v->{listener.click();listener.action(choice.number);});return row;
    }
''' +s[at:];p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/GameViewModel.java');s=p.read_text(encoding='utf-8').replace('    private GameSession session;', '    private GameSession session;\n    private int sessionGeneration;').replace('        effects.clear();effectsHandler.removeCallbacksAndMessages(null);', '        final int generation=++sessionGeneration;\n        effects.clear();effectsHandler.removeCallbacksAndMessages(null);',1).replace('new GameSession(getApplication().getFilesDir(), frames::postValue)', 'new GameSession(getApplication().getFilesDir(), frame->effectsHandler.post(()->{if(generation==sessionGeneration)frames.setValue(frame);}))');p.write_text(s,encoding='utf-8')
