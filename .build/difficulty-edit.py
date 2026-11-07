from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'Survivor.java';s=p.read_text(encoding='utf-8').replace('    private int noise = 0;', '''    private int noise = 0;
    private int campaignDifficulty;
    int difficulty(){return campaignDifficulty==0?2:campaignDifficulty;}
    void chooseDifficulty(int level){if(level<1 || level>3 || campaignDifficulty!=0)throw new IllegalStateException("Сложность уже выбрана");campaignDifficulty=level;}
''')
s=s.replace('Math.max(0, amount)); }\n    public boolean canSleep()', 'Math.max(0, amount)); }\n    public boolean canSleep()')
s=s.replace('sleepiness + Math.max(0, amount)', 'sleepiness + Difficulty.needs(amount,difficulty())').replace('hunger + Math.max(0, amount)', 'hunger + Difficulty.needs(amount,difficulty())')
s=s.replace('super.takeDamage(Math.max(1, (damage * (100 - reduction) + 99) / 100));', 'super.takeDamage(Difficulty.damage(Math.max(1, (damage * (100 - reduction) + 99) / 100),difficulty()));')
s=s.replace('        return john;\n    }\n\n    public static void main', '        john.chooseDifficulty(saved.difficulty());\n        return john;\n    }\n\n    public static void main')
s=s.replace('        if(loaded==null) {\n            Fighter beginner', '        if(loaded==null) {\n            selectedSurvivor.chooseDifficulty(Difficulty.choose(scanner));\n            Fighter beginner')
p.write_text(s,encoding='utf-8')
p=b/'GameSession.java';s=p.read_text(encoding='utf-8');a=s.index('                Integer number=null;');z=s.index('                if (closed)',a);s=s[:a]+'                int number=input.take();\n'+s[z:]
s=s.replace('    WorldStories clock() {', '    int difficulty(){return player==null?2:player.difficulty();}\n    WorldStories clock() {');p.write_text(s,encoding='utf-8')
p=b/'GameIO.java';s=p.read_text(encoding='utf-8').replace('    static WorldStories clock()', '    static int difficulty(){return CURRENT.get()==null?2:CURRENT.get().difficulty();}\n    static WorldStories clock()');p.write_text(s,encoding='utf-8')
p=b/'Item.java';s=p.read_text(encoding='utf-8').replace('location.searchSupplies(random, bonus)', 'location.searchSupplies(random, bonus+Difficulty.loot(GameIO.difficulty()))');p.write_text(s,encoding='utf-8')
p=b/'GameSave.java';s=p.read_text(encoding='utf-8').replace('if (version != 1 || player == null', 'if (version != 1 || player == null').replace('|| player.getName() == null', '|| player.difficulty()<1 || player.difficulty()>3 || player.getName() == null');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MainActivity.java');s=p.read_text(encoding='utf-8').replace('Игровые часы остановлены. Скорость ×12: сутки за два реальных часа. Дорога, сон и изготовление дополнительно продвигают те же часы.', 'Время расходуется только на действия: дорогу, поиск, бой, отдых и изготовление. Чтение и меню не продвигают часы.').replace(' + " · ×12 · "', ' + " · "').replace('+" · ×12 · Пауза"', '+" · Время действий"');p.write_text(s,encoding='utf-8')
