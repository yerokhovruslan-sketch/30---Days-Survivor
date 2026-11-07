from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
for name in ['Shop.java','ShelterUpgrades.java']:
 p=b/name;s=p.read_text(encoding='utf-8').replace('5, 1200); break;', '5, 600); break;').replace('1200 $ за штуку', '600 $ за штуку');p.write_text(s,encoding='utf-8')
p=b/'Survivor.java';s=p.read_text(encoding='utf-8');needle='        if (loaded == null) GameIO.out().println("Ты — Джон.';at=s.index(needle);s=s[:at]+'''        if(loaded==null) {
            Fighter beginner=(Fighter)selectedSurvivor;
            beginner.addItem(Shop.createItem(16));beginner.addItem(Shop.createItem(16));beginner.addItem(Shop.createItem(1));
        }
'''+s[at:];p.write_text(s,encoding='utf-8')
