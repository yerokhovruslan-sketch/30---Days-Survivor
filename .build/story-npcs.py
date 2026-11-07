from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/game/ResidentialStories.java');s=p.read_text(encoding='utf-8');at=s.index('    static boolean water(');s=s[:at]+'''    private boolean marinaSignalTalk,kirillSignalTalk,viktorSignalTalk;
    void signalTalk(int who,WorldStories world,Scanner scanner) {
        String title,text;
        if(who==1){marinaSignalTalk=true;title="Списки больницы";text="Марина поправляет одеяло на сыне. «В больнице тоже составляли списки. Отдельно тех, кто может идти. Отдельно остальных». Она молчит. «Я тогда решила, что это просто для транспорта». Что случилось дальше, Марина не знает.";}
        else if(who==2){kirillSignalTalk=true;title="Машина с красным крестом";text="«В сторону склада ещё ездили машины. Недавно. На одной был красный крест». — «Людей вывозили?» — «Я видел машину, Джон. Про людей придумывать не буду». След можно искать на станции скорой помощи, но это пока только слова Кирилла.";}
        else{viktorSignalTalk=true;title="Люди с бумагами";text="«Воду со склада магазина забрали заранее. И консервы. По ведомости». — «Кто?» — «Люди с бумагами. В те дни бумаги ещё действовали». Похоже, к сбору людей действительно готовились. Был ли транспорт — Виктор не знает.";}
        world.note(title,text);pause(title,text,scanner);
    }
''' +s[at:]
s=s.replace('            GameIO.action(1,"Расспросить о первых днях эпидемии");', '            if(!marinaSignalTalk)GameIO.action(8,"Спросить о списках эвакуации");\n            GameIO.action(1,"Расспросить о первых днях эпидемии");')
s=s.replace('            if(SurvivalSystems.choice(scanner)==1)pause("Марина о поликлинике"', '            int talk=SurvivalSystems.choice(scanner);if(talk==8 && !marinaSignalTalk)signalTalk(1,world,scanner);\n            if(talk==1)pause("Марина о поликлинике"')
s=s.replace('            GameIO.action(1,"Попросить медикаменты");', '            if(!marinaSignalTalk)GameIO.action(8,"Спросить о списках эвакуации");\n            GameIO.action(1,"Попросить медикаменты");')
s=s.replace('int c=SurvivalSystems.choice(scanner);if(c!=1 && c!=2)return;', 'int c=SurvivalSystems.choice(scanner);if(c==8 && !marinaSignalTalk){signalTalk(1,world,scanner);return;}if(c!=1 && c!=2)return;')
s=s.replace('            GameIO.action(1,"Обменять еду на запчасти"', '            if(kirill==2 && !kirillSignalTalk)GameIO.action(8,"Спросить о машинах у склада");\n            GameIO.action(1,"Обменять еду на запчасти"')
s=s.replace('GameIO.action(0,"Уйти");if(SurvivalSystems.choice(scanner)==1 && kirill==2', 'GameIO.action(0,"Уйти");int talk=SurvivalSystems.choice(scanner);if(talk==8 && kirill==2 && !kirillSignalTalk){signalTalk(2,world,scanner);return;}if(talk==1 && kirill==2')
s=s.replace('            GameIO.action(5,"Осмотреть подвал"', '            if((viktor==2 || viktor==3 || viktor==6) && !viktorSignalTalk)GameIO.action(8,"Куда забрали припасы магазина?");\n            GameIO.action(5,"Осмотреть подвал"')
s=s.replace('            if(c==1 || c==2) {if(helpViktor', '            if(c==8 && (viktor==2 || viktor==3 || viktor==6) && !viktorSignalTalk){signalTalk(3,world,scanner);continue;}\n            if(c==1 || c==2) {if(helpViktor')
p.write_text(s,encoding='utf-8')
