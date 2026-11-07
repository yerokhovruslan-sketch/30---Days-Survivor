package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class ShopCategoriesTest {
 @Test public void allProductsBelongToFourCategoriesAndStatsUseRealValues() {
  for(int n=1;n<=Shop.LAST_PRODUCT;n++){ItemInfo item=Shop.createItem(n);if(item!=null)assertTrue(Shop.category(item)>=0 && Shop.category(item)<4);}
  assertEquals(0,Shop.category(Shop.createItem(1)));assertEquals(1,Shop.category(Shop.createItem(4)));assertEquals(2,Shop.category(Shop.createItem(12)));assertEquals(3,Shop.category(Shop.createItem(49)));
  String knife=Shop.statDetails(Shop.createItem(4),2,0);assertTrue(knife.contains("Урон: 6–10 ×2"));assertTrue(knife.contains("Энергия: 8"));assertTrue(knife.contains("Попадание: 90%"));assertTrue(knife.contains("Шум: +1"));
  assertTrue(Shop.statDetails(Shop.createItem(1),0,1501).contains("1 д. 1 ч. 1 мин."));
 }
 @SuppressWarnings("unchecked") @Test public void tabsSwitchAndUnavailableGoodsRemainVisibleAndCannotBePurchased() throws Exception {
  Locations l=new Locations();WorldStories world=l.getStories();world.refreshMarket();Field f=WorldStories.class.getDeclaredField("marketStock");f.setAccessible(true);((Map<Integer,Integer>)f.get(world)).put(1,0);
  Fighter p=new Fighter("John","",100);LinkedBlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();File dir=Files.createTempDirectory("shop-tabs").toFile();
  try(GameSession session=new GameSession(dir,frames::offer,()->{GameIO.mainMenu(p,1000,l);Shop.buy(p,GameIO.scanner(),1000);})){session.start();
   GameSession.Frame first=frames.poll(5,TimeUnit.SECONDS);assertNotNull(first);
   GameSession.Choice bandage=first.choices.stream().filter(c->c.number==1).findFirst().orElseThrow();assertFalse(bandage.enabled);assertTrue(bandage.detail.contains("Поставка через:"));assertFalse(session.submit(first.revision,1));
   assertTrue(first.choices.stream().anyMatch(c->c.number==1001&&c.selected&&c.enabled));assertTrue(session.submit(first.revision,1002));
   GameSession.Frame weapon=null;long limit=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(System.nanoTime()<limit){GameSession.Frame next=frames.poll(1,TimeUnit.SECONDS);if(next!=null&&next.mode==GameSession.Mode.CHOICE&&next.choices.stream().anyMatch(c->c.number==1002&&c.selected)){weapon=next;break;}}
   assertNotNull(weapon);assertTrue(weapon.choices.stream().anyMatch(c->c.number==4));assertFalse(weapon.choices.stream().anyMatch(c->c.number==1));
  }finally{for(File file:java.util.Objects.requireNonNull(dir.listFiles()))file.delete();dir.delete();}
 }
}
