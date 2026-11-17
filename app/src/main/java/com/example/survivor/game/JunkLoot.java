package com.example.survivor.game;
import java.util.*;
public final class JunkLoot {
 public static boolean isJunkText(String text){String lower=text.toLowerCase(java.util.Locale.ROOT).replace('ё','е');for(Entry[] group:CATEGORIES)for(Entry e:group)if(lower.contains(e.name.toLowerCase(java.util.Locale.ROOT).replace('ё','е')))return true;return false;}
 static final class Entry {final String name;final int price,weight;Entry(String n,int p,int w){name=n;price=p;weight=w;}}
 static final Entry[][] CATEGORIES={
    {
        new Entry("Керамическая кружка",1,7),
        new Entry("Треснувшая тарелка",0,7),
        new Entry("Стеклянный стакан",1,7),
        new Entry("Алюминиевая миска",3,7),
        new Entry("Пластиковый контейнер с крышкой",2,7),
        new Entry("Пластиковая крышка от контейнера",0,7),
        new Entry("Деревянная разделочная доска",2,7),
        new Entry("Кухонная прихватка",1,7),
        new Entry("Потёртый кухонный фартук",1,7),
        new Entry("Губка для посуды",1,7),
        new Entry("Почти пустой флакон моющего средства",1,7),
        new Entry("Рулон пищевой плёнки",3,7),
        new Entry("Алюминиевая фольга",3,7),
        new Entry("Пластиковый половник",1,7),
        new Entry("Металлический дуршлаг",3,7),
        new Entry("Ручная тёрка",3,7),
        new Entry("Силиконовая лопатка",1,7),
        new Entry("Стеклянная банка с крышкой",2,7),
        new Entry("Старая кофемолка без ручки",1,7),
        new Entry("Металлический термос",8,1)
    },
    {
        new Entry("Старая зубная щётка",0,7),
        new Entry("Запечатанная зубная щётка",3,7),
        new Entry("Початый тюбик зубной пасты",2,7),
        new Entry("Запечатанный тюбик зубной пасты",5,3),
        new Entry("Пустой флакон шампуня",0,7),
        new Entry("Остатки шампуня",1,7),
        new Entry("Кусок туалетного мыла",3,7),
        new Entry("Пластиковая мыльница",1,7),
        new Entry("Расчёска с поломанными зубцами",0,7),
        new Entry("Целая расчёска",1,7),
        new Entry("Мужская бритва со старым лезвием",1,7),
        new Entry("Запечатанный одноразовый станок",3,7),
        new Entry("Маленькое полотенце",3,7),
        new Entry("Старое банное полотенце",2,7),
        new Entry("Резиновая пробка для ванны",0,7),
        new Entry("Пластиковый стаканчик для щёток",1,7),
        new Entry("Ватные диски, маленькая упаковка",2,7),
        new Entry("Пустая упаковка влажных салфеток",0,7),
        new Entry("Пакет влажных салфеток",4,7),
        new Entry("Рулон туалетной бумаги",2,7)
    },
    {
        new Entry("Старая подушка",1,7),
        new Entry("Чистая подушка",5,3),
        new Entry("Наволочка",2,7),
        new Entry("Простыня",3,7),
        new Entry("Тонкое одеяло",5,3),
        new Entry("Порванный плед",1,7),
        new Entry("Домашние тапочки",2,7),
        new Entry("Одинокий носок",0,7),
        new Entry("Пара чистых носков",2,7),
        new Entry("Пластиковая вешалка",1,7),
        new Entry("Маленькое настенное зеркало",4,7),
        new Entry("Фотография семьи",0,7),
        new Entry("Детский рисунок",0,7),
        new Entry("Настольный календарь",1,7),
        new Entry("Пульт от телевизора",3,7),
        new Entry("Сломанный будильник",0,7),
        new Entry("Работающий механический будильник",8,3),
        new Entry("Швейный набор",6,3),
        new Entry("Коробка разноцветных пуговиц",2,7),
        new Entry("Настольная лампа без лампочки",2,7)
    },
    {
        new Entry("Пустая картонная коробка",0,7),
        new Entry("Пакет с ручками",1,7),
        new Entry("Пластиковая корзинка",2,7),
        new Entry("Ценникодержатель",1,7),
        new Entry("Рулон бумажных чеков",1,7),
        new Entry("Кассовая лента",1,7),
        new Entry("Старые весы без батареек",3,7),
        new Entry("Маленький совок для сыпучих продуктов",2,7),
        new Entry("Пластиковый поднос",1,7),
        new Entry("Металлический поднос",3,7),
        new Entry("Пустая банка из-под кофе",1,7),
        new Entry("Бумажные стаканчики, упаковка",2,7),
        new Entry("Пластиковые трубочки, упаковка",1,7),
        new Entry("Набор одноразовых ложек",2,7),
        new Entry("Коробка деревянных зубочисток",1,7),
        new Entry("Скатерть из клеёнки",2,7),
        new Entry("Пластиковая мерная кружка",2,7),
        new Entry("Резинки для упаковки, связка",1,7),
        new Entry("Канцелярский скотч",2,7),
        new Entry("Диспенсер для скотча",3,7)
    },
    {
        new Entry("Шариковая ручка",1,7),
        new Entry("Простой карандаш",1,7),
        new Entry("Ластик",1,7),
        new Entry("Точилка",1,7),
        new Entry("Школьная линейка",1,7),
        new Entry("Тетрадь в клетку",2,7),
        new Entry("Блокнот с чистыми страницами",3,7),
        new Entry("Папка для документов",2,7),
        new Entry("Пластиковый файл",1,7),
        new Entry("Старая географическая карта",4,7),
        new Entry("Сломанный степлер",0,7),
        new Entry("Рабочий степлер",4,7),
        new Entry("Скрепки, коробочка",2,7),
        new Entry("Кнопки канцелярские",1,7),
        new Entry("Настольный органайзер",3,7),
        new Entry("Дырокол",4,7),
        new Entry("Калькулятор с работающей батарейкой",8,3),
        new Entry("Пачка бумаги",4,7),
        new Entry("Старая школьная медаль",2,7),
        new Entry("Настольная табличка с фамилией",1,7)
    },
    {
        new Entry("Пустой пластиковый шприц без иглы",0,7),
        new Entry("Запечатанный шприц",3,7),
        new Entry("Медицинская маска",1,7),
        new Entry("Запечатанная пара медицинских перчаток",3,7),
        new Entry("Пустой пузырёк от лекарства",0,7),
        new Entry("Пластиковая таблетница",2,7),
        new Entry("Медицинский лоток",3,7),
        new Entry("Одноразовая простыня в упаковке",2,7),
        new Entry("Бумажный медицинский халат",1,7),
        new Entry("Клеёнка для медицинской кушетки",3,7),
        new Entry("Пустой контейнер для анализов",0,7),
        new Entry("Чистая медицинская пипетка",2,7),
        new Entry("Бумажные бахилы, пара",0,7),
        new Entry("Шпатель для осмотра горла, упаковка",2,7),
        new Entry("Резиновая грелка",5,3),
        new Entry("Пластиковый холодовый пакет, неиспользованный",4,7),
        new Entry("Медицинская измерительная лента",2,7),
        new Entry("Пустая аптечная коробочка",0,7),
        new Entry("Запечатанный медицинский пластырь, небольшой рулон",4,7),
        new Entry("Кровоостанавливающий жгут, исправный",12,1)
    },
    {
        new Entry("Старая автомобильная тряпка",0,7),
        new Entry("Пластиковая воронка",2,7),
        new Entry("Пустая бутылка моторного масла",0,7),
        new Entry("Остатки смазки в тюбике",3,7),
        new Entry("Комплект ржавых гаек",1,7),
        new Entry("Коробка шайб",2,7),
        new Entry("Набор шплинтов",2,7),
        new Entry("Старый автомобильный предохранитель, непроверенный",1,7),
        new Entry("Кусок резинового шланга",3,7),
        new Entry("Пластиковые кабельные стяжки",3,7),
        new Entry("Защитные рабочие очки",4,7),
        new Entry("Промасленная рабочая перчатка",0,7),
        new Entry("Пара рабочих перчаток",4,7),
        new Entry("Небольшая щётка по металлу",3,7),
        new Entry("Изолирующая прокладка",1,7),
        new Entry("Металлический хомут",2,7),
        new Entry("Пластиковая ручка от инструмента",0,7),
        new Entry("Магнитный держатель для винтов",3,7),
        new Entry("Комплект мебельных уголков",2,7),
        new Entry("Рабочая рулетка",6,3)
    },
    {
        new Entry("Сухая сосновая шишка",0,7),
        new Entry("Пучок сухой травы",0,7),
        new Entry("Кусок берёзовой коры",0,7),
        new Entry("Птичье перо",0,7),
        new Entry("Красивый речной камень",0,7),
        new Entry("Пучок сухого мха",0,7),
        new Entry("Пустая коробочка от рыболовных крючков",0,7),
        new Entry("Рыболовный поплавок",2,7),
        new Entry("Небольшой моток лески",4,7),
        new Entry("Рыболовное грузило",1,7),
        new Entry("Сломанная катушка от удочки",0,7),
        new Entry("Пластиковая коробочка для наживки",1,7),
        new Entry("Старый походный коврик",3,7),
        new Entry("Пустой тканевый мешочек",1,7),
        new Entry("Металлическая походная кружка",4,7),
        new Entry("Кусок брезента",5,3),
        new Entry("Компас с повреждённым корпусом, исправный",7,3),
        new Entry("Моток тонкой верёвки",6,3),
        new Entry("Палаточный колышек",1,7),
        new Entry("Старый складной табурет",3,7)
    },
    {
        new Entry("Обломок известняка",0,7),
        new Entry("Кусок гранита",0,7),
        new Entry("Образец кварца",2,7),
        new Entry("Кусок каменного угля",1,7),
        new Entry("Ржавый железнодорожный костыль",1,7),
        new Entry("Сломанный шахтёрский жетон",0,7),
        new Entry("Пустой мешок из-под цемента",0,7),
        new Entry("Кусок абразивного камня",2,7),
        new Entry("Старая пылезащитная маска",1,7),
        new Entry("Рабочие беруши в упаковке",2,7),
        new Entry("Изношенный светоотражающий жилет",2,7),
        new Entry("Кусок транспортерной ленты",2,7),
        new Entry("Небольшой деревянный клин",0,7),
        new Entry("Стальной крепёжный уголок",2,7),
        new Entry("Упаковка строительных дюбелей",2,7),
        new Entry("Плотницкий карандаш",0,7),
        new Entry("Пузырьковый уровень",5,3),
        new Entry("Старая маслёнка",3,7),
        new Entry("Металлическая цепь, короткий отрезок",5,3),
        new Entry("Изношенный защитный шлем",3,7)
    },
    {
        new Entry("Пустая пластиковая канистра малого объёма",4,7),
        new Entry("Ржавая металлическая табличка",0,7),
        new Entry("Кусок медной проволоки",4,7),
        new Entry("Сломанная радиодеталь",1,7),
        new Entry("Небольшой изолятор",2,7),
        new Entry("Пластиковый защитный колпачок",0,7),
        new Entry("Старая промышленная прокладка",1,7),
        new Entry("Небольшой фильтрующий картридж, использованный",1,7),
        new Entry("Неповреждённая защитная маска от пыли",4,7),
        new Entry("Пустой металлический футляр",2,7),
        new Entry("Ржавый карабин для крепления",2,7),
        new Entry("Неисправный фонарик",1,7),
        new Entry("Рабочий налобный фонарик без батареек",8,3),
        new Entry("Пустая армейская кружка-котелок",5,3),
        new Entry("Армейский ремень без пряжки",1,7),
        new Entry("Старая нашивка подразделения",0,7),
        new Entry("Пустая сигнальная упаковка",0,7),
        new Entry("Набор пластиковых маркировочных бирок",1,7),
        new Entry("Запечатанная пара химически стойких перчаток",7,3),
        new Entry("Рабочий маленький мультиметр",20,1)
    }
 };
 static final class JunkItem extends ItemInfo {
  private static final long serialVersionUID=1L;
  JunkItem(Entry e){super(e.name,"Обычная находка",1,e.price);}
  @Override public int getPrice(){return rawPrice();}
  int priceCents(){return fractional(getName())?50:getPrice()*100;}
  String shownPrice(){int c=priceCents();return c%100==0?Integer.toString(c/100):String.format(java.util.Locale.ROOT,"%d,%02d",c/100,c%100).replaceAll("0$","");}
  @Override public int getWeightGrams(){return weightFor(getName());}
 }
 static boolean fractional(String name){return java.util.Arrays.asList("Пустой пластиковый шприц без иглы","Пустой пузырёк от лекарства","Пустой контейнер для анализов","Бумажные бахилы, пара","Старая автомобильная тряпка","Промасленная рабочая перчатка","Пластиковая ручка от инструмента","Кусок берёзовой коры","Пучок сухого мха","Сломанная катушка от удочки","Кусок гранита","Пустой мешок из-под цемента","Небольшой деревянный клин","Плотницкий карандаш","Ржавая металлическая табличка","Пластиковый защитный колпачок","Резиновая пробка для ванны").contains(name);}
 static int weightFor(String name){String n=name.toLowerCase(java.util.Locale.ROOT);
  if(n.contains("гранита")||n.contains("известняка"))return 2500;
  if(n.contains("цепь")||n.contains("табурет")||n.contains("одеяло")||n.contains("весы"))return 1200;
  if(n.contains("подушк")||n.contains("шлем")||n.contains("плед")||n.contains("коврик")||n.contains("брезент"))return 700;
  if(n.contains("камень")||n.contains("термос")||n.contains("доска")||n.contains("миска")||n.contains("кружка")||n.contains("крепёж")||n.contains("металлический поднос"))return 400;
  if(n.contains("ручк")||n.contains("перо")||n.contains("шприц")||n.contains("карандаш")||n.contains("маска")||n.contains("носк")||n.contains("пакет"))return 50;
  if(n.contains("полотенце")||n.contains("тарелка")||n.contains("бутылка")||n.contains("коробка")||n.contains("банка"))return 250;
  return 150;}
 static int category(String name,String description){
  String text=((name==null?"":name)+" "+(description==null?"":description)).toLowerCase(java.util.Locale.ROOT);
  if(text.matches(".*(медицин|аптек|больниц|поликлиник|скорой помощи).*"))return 5;
  if(text.matches(".*(гараж|автомастер|терминал|автомобил|контейнер).*"))return 6;
  if(text.matches(".*(лес|озер|кемпинг|охотнич|рыбац|палатк).*"))return 7;
  if(text.matches(".*(карьер|шахт|лесопил|горн).*"))return 8;
  if(text.matches(".*(промышлен|завод|подстанц|железн|блокпост|казарм).*"))return 9;
  if(text.matches(".*(туалет|ванн|душев|моечн|сануз|умываль).*"))return 1;
  if(text.matches(".*(кухн|столов|кафе|посуд|холодильник|кухон).*"))return 0;
  if(text.matches(".*(спальн|жилой блок|комнат[аы] отдых|постел|койк|общежити|квартир).*"))return 2;
  if(text.matches(".*(касс|торгов|прилав|магазин|продукт|супермаркет|ресторан).*"))return 3;
  if(text.matches(".*(кабинет|учитель|школ|канцеляр|парт[аы]|архив|диспетчер).*"))return 4;
  return -1;
 }
 static int count(Random r,boolean kitchen){
  int v=r.nextInt(100);if(v<15)return 0;
  if(kitchen){if(v<50)return 2;if(v<80)return 3;if(v<95)return 4;return 5;}
  if(v<50)return 1;if(v<80)return 2;if(v<95)return 3;return 4;
 }
 static ItemInfo pick(int category,Random r){
  Entry[] list=CATEGORIES[category];int total=0;for(Entry e:list)total+=e.weight;
  int n=r.nextInt(total);for(Entry e:list){n-=e.weight;if(n<0)return new JunkItem(e);}
  throw new IllegalStateException();
 }
 static List<ItemInfo> apartmentItems(int category,Random r){
  List<ItemInfo> finds=new ArrayList<>();int amount=count(r,category==0);
  for(int i=0;i<amount;i++){
   int group=category;if(r.nextInt(100)>=85){group=r.nextInt(CATEGORIES.length-1);if(group>=category)group++;}
   finds.add(pick(group,r));
  }
  return finds;
 }
 static List<ItemInfo> items(Location location,Location.SearchPoint point,ItemInfo first,Random r){
  String name=point==null?location.getName():point.name,desc=point==null?"":point.description;
  int cat=category(name,desc);if(cat<0)cat=category(location.getName(),"");
  int amount=count(r,cat==0),chosen=cat;List<ItemInfo> result=new ArrayList<>();
  for(int i=0;i<amount;i++){
   if(chosen<0){result.add(i==0?first:Item.createLootForLocation(location,r));continue;}
   int roll=r.nextInt(100);
   if(roll<42)result.add(chosen==7 && !((name+" "+desc).toLowerCase(java.util.Locale.ROOT).matches(".*(палат|лагер|стоянк|рыба|лодк|избуш|домик).*")) ? new JunkItem(CATEGORIES[7][r.nextInt(6)]) : pick(chosen,r));
   else if(roll<50){int other=r.nextInt(CATEGORIES.length-1);if(other>=chosen)other++;result.add(pick(other,r));}
   else result.add(i==0?first:Item.createLootForLocation(location,r));
  }
  return result;
 }
}
