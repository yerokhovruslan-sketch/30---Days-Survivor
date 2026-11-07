from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'Item.java';s=p.read_text(encoding='utf-8');a=s.index('        List<ItemInfo> items = new ArrayList<>();',s.index('public static ItemInfo createItems(int difficulty, LootProfile'));z=s.index('        int roll = random.nextInt(100) + 1;',a);catalog=s[a:z];s=s[:a]+'        List<ItemInfo> items = lootCatalogue();\n'+s[z:];pos=s.index('     public static ItemInfo createItems(int difficulty, LootProfile');s=s[:pos]+'     static List<ItemInfo> lootCatalogue() {\n'+catalog+'        return items;\n     }\n\n'+s[pos:];p.write_text(s,encoding='utf-8')
p=b/'Economy.java';s=p.read_text(encoding='utf-8').replace('    static {','    static {\n        for(ItemInfo item:Item.lootCatalogue())PRICES.put(key(item.getName()),item.rawPrice());',1);p.write_text(s,encoding='utf-8')
p=b/'Shop.java';s=p.read_text(encoding='utf-8').replace('            int quantity = SurvivalSystems.choice(scanner);','            int quotedPrice=price(number);\n            int quantity = SurvivalSystems.choice(scanner);\n            if(price(number)!=quotedPrice){GameIO.out().println("Цена изменилась с новой поставкой. Проверьте предложение ещё раз; деньги не списаны.");continue;}');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/MainActivity.java');s=p.read_text(encoding='utf-8').replace('        game = new ViewModelProvider(this).get(GameViewModel.class);','''        game = new ViewModelProvider(this).get(GameViewModel.class);
        findViewById(R.id.character_day).setOnClickListener(v->{
            game.pauseWorld(true);
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Пауза")
                .setMessage("Игровые часы остановлены. Скорость ×24: сутки за реальный час. Дорога, сон и изготовление дополнительно продвигают те же часы.")
                .setPositiveButton("Продолжить",(d,w)->{}).setOnDismissListener(d->game.pauseWorld(false)).show();
        });''').replace('current.time + " · "','current.time + " · ×24 · "');p.write_text(s,encoding='utf-8')
