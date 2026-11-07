from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'GameSession.java';s=p.read_text(encoding='utf-8').replace('        public final boolean night;', '        public final boolean night,home,reading;\n        public final int place;\n        public final String musicCue;').replace('String time, boolean night) {','String time, boolean night, boolean home,int place,boolean reading,String musicCue) {').replace('            this.time = time; this.night = night;', '            this.time = time; this.night = night;this.home=home;this.place=place;this.reading=reading;this.musicCue=musicCue;')
s=s.replace('    private Consumer<GameSound> effects;', '    private String pendingMusicCue="";\n    void musicCue(String cue){pendingMusicCue=cue;}\n    private Consumer<GameSound> effects;')
s=s.replace('clock() != null && clock().isNight());','clock() != null && clock().isNight(),atHome,clock()==null?0:clock().currentPlace(),reading,pendingMusicCue);\n        pendingMusicCue="";');p.write_text(s,encoding='utf-8')
p=b/'GameIO.java';s=p.read_text(encoding='utf-8').replace('    static void sound(GameSound sound)', '    static void musicCue(String cue){if(CURRENT.get()!=null)CURRENT.get().musicCue(cue);}\n    static void sound(GameSound sound)');p.write_text(s,encoding='utf-8')
p=b/'ResidentialStories.java';s=p.read_text(encoding='utf-8').replace('GameIO.storyMenu(title,"");GameIO.card(title,text);', '''GameIO.storyMenu(title,"");
        if(text.contains("Квартира затихла") || text.contains("не выжила") || text.contains("не пережил"))GameIO.musicCue("elegy");
        else if(text.contains("никого нет") || text.contains("покинула дом"))GameIO.musicCue("fading");
        else if(text.contains("Квартира пуста") || text.contains("забыты"))GameIO.musicCue("forgotten");
        GameIO.card(title,text);''');p.write_text(s,encoding='utf-8')
p=b/'ShelterUpgrades.java';s=p.read_text(encoding='utf-8').replace('GameIO.out().println("Улучшение построено. Материалы использованы.");', '{if(choice==3)GameIO.musicCue("digital");GameIO.out().println("Улучшение построено. Материалы использованы.");}');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/GameEffects.java');s=p.read_text(encoding='utf-8').replace('case CLICK->.09f;case TRADE->.25f;case RELOAD->.28f;case SHOT->.4f;case BLUNT,BLADE->.34f;case ZOMBIE->.28f;case WALK->.22f;', 'case CLICK->.24f;case TRADE->.55f;case RELOAD->.65f;case SHOT->.85f;case BLUNT,BLADE->.75f;case ZOMBIE->.65f;case WALK->.5f;');p.write_text(s,encoding='utf-8')
