from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'WorldStories.java';s=p.read_text(encoding='utf-8');pos=s.index('    int currentPlace()');s=s[:pos]+'''    private boolean radioOpeningSeen,radioInspected,workshopControllerFound;
    private int lastSignalClueSearch=-3;
    void radioOpening(Fighter player,Scanner scanner) {
        if(radioOpeningSeen || player.getUpgrades().radio>0)return;
        radioOpeningSeen=true;
        note("Кто-то ещё говорит","Связь оборвалась до названия точки сбора. Нужно восстановить устойчивый приём. Источник сигнала пока неизвестен.");
        inspectRadio(player,scanner);GameIO.checkpoint();
    }
    void inspectRadio(Fighter player,Scanner scanner) {
        while(true) {
            GameIO.storyMenu("Кто-то ещё говорит","Восстановить устойчивую связь. Чтение не тратит время.");
            GameIO.card("Приёмник",player.getUpgrades().radio>0?"Радиостанция исправна. Теперь можно попробовать поймать сигнал.":"Ты прижимаешь провод большим пальцем. Шипение становится громче. На секунду кажется, что за ним кто-то дышит. Отпускаешь — тишина.\\nРазговорчивый собеседник. Главное — не двигать рукой до конца жизни.");
            GameIO.action(1,"Осмотреть неисправность");GameIO.action(2,"Что нужно для ремонта?");GameIO.action(3,"Разобрать найденную электронику","Верстак I и рабочий инструмент",player.getUpgrades().bench>=1);
            GameIO.action(0,"Отложить. Подготовиться к вылазке");
            int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1){if(!radioInspected){radioInspected=true;note("Неисправность приёмника","Контакты повреждены, приём неустойчив. Нужны два электрочипа, проволока, запчасти и изолента.");}
                ResidentialStories.pause("Неустойчивый контакт","Передача была настоящей. Повреждены контакты и электроника приёмника. Нужны два исправных чипа. Осмотр ничего не расходует.",scanner);}
            if(c==2)ResidentialStories.pause("Несколько путей",player.getUpgrades().description(player,3)+"\\nПервый чип: полицейский участок. Второй: электроподстанция или блок управления в гаражном кооперативе. Блок разбирается дома на верстаке I рабочим инструментом. Чип также продаёт торговец: базовая цена 600 $. Припасы и улучшения можно готовить параллельно.",scanner);
            if(c==3 && player.getUpgrades().bench>=1)Crafting.dismantleMenu(player,this,scanner);
        }
    }
    Discovery signalClue(int place,String name) {
        if(searches-lastSignalClueSearch<3)return null;
        int id;String title,text;
        switch(place) {
            case 3->{id=201;title="Чужая отметка";text="На схеме у остановки обведён промышленный район. Рядом кто-то написал: «Если услышите снова — запишите время». Кто-то ещё ждёт передачу.";}
            case 5->{id=202;title="Собирались в дорогу";text="У лестницы лежит раскрытая сумка. Одежду оставили. Воду и лекарства забрали. На оторванном листе — «МАЯК» и несколько зачёркнутых частот. Ты не первый, кто ищет этот сигнал.";}
            case 4->{id=203;title="Мел на двери";text="На двери магазина мелом: «Если услышите снова — запишите время». Под надписью несколько разных отметок. Здесь ждали не продукты — здесь ждали новостей.";}
            case 1->{id=204;title="Одна частота";text="Среди газет зажат клочок бумаги: «МАЯК». Ниже частоты, почти все зачёркнуты. На обороте: «Не знаю, кто отвечает. Но отвечает». Кто-то уже пытался настроить приёмник.";}
            default->{return null;}
        }
        if(seen.contains(id))return null;lastSignalClueSearch=searches;
        return remember(id,title,text,name,Collections.emptyList());
    }
''' +s[pos:]
s=s.replace('        if (radioPlaying && place == radioPlace', '''        if(completedSearch) {
            Discovery clue=signalClue(place,name);
            if(clue!=null){searched.add(place);return clue;}
        }
        if (radioPlaying && place == radioPlace''',1)
s=s.replace('        return items;\n    }\n    void addCases', '''        if(place==35 && !workshopControllerFound){workshopControllerFound=true;items.add(new Tools("Блок управления радиостанцией","Неисправная электроника. На верстаке I с рабочим инструментом можно извлечь электрочип.",3,0));note("Запасной путь","В гаражном кооперативе найден блок управления. Его можно разобрать на верстаке I рабочим инструментом и получить электрочип.");}
        return items;
    }
    void addCases''')
p.write_text(s,encoding='utf-8')
p=b/'Shelter.java';s=p.read_text(encoding='utf-8').replace('            GameIO.action(5,"Крафтинг"','            GameIO.action(6,"Осмотреть приёмник","Неисправность и способы добычи компонентов");\n            GameIO.action(5,"Крафтинг"').replace('            if(choice==5)Crafting.menu', '            if(choice==6 && GameIO.clock()!=null)GameIO.clock().inspectRadio(player,scanner);\n            if(choice==5)Crafting.menu');p.write_text(s,encoding='utf-8')
p=b/'Survivor.java';s=p.read_text(encoding='utf-8').replace('        while (running) {\n            GameIO.mainMenu', '''        if(loaded==null){GameIO.mainMenu(selectedSurvivor,balance,locations);locations.getStories().radioOpening((Fighter)selectedSurvivor,scanner);GameIO.menu("","");}
        while (running) {
            GameIO.mainMenu''');s=s.replace('"В служебном оборудовании найден исправный электрочип. Он отправлен на склад убежища, чтобы не потерять сюжетный компонент из-за перегруза."', '"Найдено служебное оборудование. Оно отправлено на склад убежища; подробности записаны в журнале."');p.write_text(s,encoding='utf-8')
p=b/'Crafting.java';s=p.read_text(encoding='utf-8').replace('        int budget=item.getSalePrice();', '''        if(item.getName().equals("Блок управления радиостанцией")) {
            if(possessions(p).stream().anyMatch(i->i.isRepairTool() && i.getDurability()>0))result.add(Shop.createItem(49));
            return result;
        }
        int budget=item.getSalePrice();''');p.write_text(s,encoding='utf-8')
p=b/'ShelterUpgrades.java';s=p.read_text(encoding='utf-8').replace('        String goal = "Точка эвакуации: Военный склад.\\n";', '        String goal = signalReceived?"Точка сбора по радиозаписи: Военный склад.\\n":"Главная цель: восстановить устойчивую связь и узнать точку сбора.\\n";').replace('или найти по одному в полицейском участке и на электроподстанции.', 'или добыть в полицейском участке, на электроподстанции; блок из гаражного кооператива разбирается на верстаке I.');p.write_text(s,encoding='utf-8')
