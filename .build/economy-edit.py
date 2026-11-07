from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/game/Item.java');s=p.read_text(encoding='utf-8')
s=s.replace('public int getSalePrice() {return getPrice();}','public int getSalePrice() {return Economy.sale(this);}')
s=s.replace('public int getPrice() {return isDurable() ? price * (20 + durability * 80 / 100) / 100 : price; }','int rawPrice() {return price;}\n    public int getPrice() {return Economy.base(this);}')
s=s.replace('@Override public int getSalePrice() {return crafted?getPrice()/4:getPrice();}','@Override public int getSalePrice() {return Economy.sale(this);}')
s=s.replace('foundItem.multiplyPrice(difficulty);','// Difficulty changes rarity, never the value of an identical object.')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/game/WorldStories.java');s=p.read_text(encoding='utf-8');a=s.index('    int craftStock(int product)');b=s.index('    void addCases',a)
s=s[:a]+'''    private Map<Integer,Integer> marketStock,marketDue;
    private long marketSeed;
    private int marketEventUntil,marketEventId,marketSaleCategory,eventDue;
    private boolean chipPolice,chipPower;
    int absoluteMinute(){return (day()-1)*1440+minuteOfDay();}
    void refreshMarket() {
        if(marketStock==null){marketStock=new HashMap<>();marketDue=new HashMap<>();marketSeed=new Random().nextLong();eventDue=absoluteMinute()+2880;}
        int now=absoluteMinute();
        if(now>=eventDue) {
            int cycle=now/2880;Random r=new Random(marketSeed+cycle*7919L);
            marketEventId=r.nextInt(100)<23?1+r.nextInt(6):0;
            marketSaleCategory=r.nextInt(6);marketEventUntil=now+2880;eventDue=(cycle+1)*2880;
            if(marketEventId!=0)note("Торговые вести",marketEventText());
        }
        for(int n=1;n<=Shop.LAST_PRODUCT;n++)if(Shop.createItem(n)!=null && now>=marketDue.getOrDefault(n,0)) {
            int period=Economy.period(n),cycle=now/period;
            Random r=new Random(marketSeed+n*104729L+cycle*15485863L);
            int target=Economy.target(n,day,r);
            if(marketEvent()==2 && Economy.category(n)==1)target=Math.min(1,target);
            if(marketEvent()==3 && Economy.category(n)==3)target+=1;
            // Delayed essentials keep their current stock, never repeatedly postpone.
            boolean delayed=marketEvent()==1 && Economy.category(n)<=3;
            if(!delayed)marketStock.put(n,Math.max(marketStock.getOrDefault(n,0),target));
            marketDue.put(n,delayed?now+2880:(cycle+1)*period);
        }
    }
    int marketEvent(){return absoluteMinute()<marketEventUntil?marketEventId:0;}
    int saleCategory(){return marketSaleCategory;}
    String marketEventText(){return switch(marketEvent()) {
        case 1->"Поставка задержана: караван обходит перекрытую дорогу. Основные товары ожидаются примерно через 2 игровых дня. Оставшиеся товары продаются.";
        case 2->"Дефицит медикаментов: цены +30%, поставка сокращена. До конца события: "+Math.max(0,marketEventUntil-absoluteMinute())+" мин.";
        case 3->"Доставлены армейские припасы: дополнительная партия патронов на 2 дня.";
        case 4->"Перебои с энергией: чипы и медная проволока дороже на 25% на 2 дня.";
        case 5->"Распродажа одной категории: скидка 20% на 2 дня.";
        case 6->"Прибыла группа выживших: еда +15%, инструменты принимают дороже на 2 дня.";
        default->"Поставки идут по расписанию.";};}
    String marketStatus(){refreshMarket();int now=absoluteMinute(),due=Integer.MAX_VALUE;for(int n:marketDue.keySet())if(Economy.category(n)<=3)due=Math.min(due,marketDue.get(n));int left=Math.max(0,due-now);return marketEventText()+"\\nОсновное пополнение примерно через "+left/1440+" д. "+left%1440/60+" ч. "+left%60+" мин. Оружие и инструменты: раз в 4–6 дней. После дня 14 ассортимент постепенно редеет.";}
    int craftStock(int product){refreshMarket();return marketStock.getOrDefault(product,0);}
    void buyCraftStock(int product,int count){refreshMarket();marketStock.put(product,Math.max(0,craftStock(product)-count));}
    List<ItemInfo> guaranteedChips(int place) {
        List<ItemInfo> items=new ArrayList<>();
        if(place==15 && !chipPolice){chipPolice=true;items.add(Shop.createItem(49));note("Чип дежурной рации","В полицейском участке найден исправный электрочип. Второй можно снять с оборудования электроподстанции.");}
        if(place==43 && !chipPower){chipPower=true;items.add(Shop.createItem(49));note("Чип подстанции","Из блока управления извлечён электрочип для радиостанции.");}
        return items;
    }
''' +s[b:]
p.write_text(s,encoding='utf-8')
