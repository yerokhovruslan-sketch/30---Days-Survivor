package com.example.survivor.game;
import org.junit.Test;
import java.nio.file.Files;
import java.io.File;
import java.util.ArrayList;
import static org.junit.Assert.*;
public class ShopTravelTest {
 @Test public void bothShopTripsEmitTravelTransition() throws Exception {
  File dir=Files.createTempDirectory("shop-travel").toFile();ArrayList<String> scenes=new ArrayList<>();
  try(GameSession session=new GameSession(dir,f->{})){
   session.setTravelListener(scenes::add);GameIO.attach(session);
   try{Fighter player=new Fighter("John","",100);Survivor.travel(player,"Путь в магазин",3);Survivor.travel(player,"Возвращение из магазина",3);}
   finally{GameIO.detach();}
  }finally{dir.delete();}
  assertEquals(java.util.List.of("Ты идёшь в магазин","Ты возвращаешься домой"),scenes);
 }
}
