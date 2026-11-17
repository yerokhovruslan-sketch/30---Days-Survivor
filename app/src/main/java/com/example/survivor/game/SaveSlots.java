package com.example.survivor.game;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/** Manual snapshots live beside the existing automatic save. */
final class SaveSlots {
 private SaveSlots(){}
 static List<File> all(File current){
  File[] files=current.getParentFile().listFiles((dir,name)->name.matches("survivor-slot-[0-9]+\\.dat(\\.bak)?"));
  List<File> result=new ArrayList<>();if(files!=null)for(File file:files){String path=file.getPath();File slot=path.endsWith(".bak")?new File(path.substring(0,path.length()-4)):file;if(!result.contains(slot))result.add(slot);}
  if(GameSave.exists(current))result.add(current);
  result.sort(Comparator.comparingLong(SaveSlots::modified).reversed());return result;
 }
 private static long modified(File file){File bak=new File(file.getPath()+".bak");return Math.max(file.lastModified(),bak.lastModified());}
 static void snapshot(File current) throws IOException {
  if(!GameSave.exists(current))return;
  // Validate and recover before copying, so a partial file cannot become a slot.
  if(GameSave.load(current).player.difficulty()==3)return;long id=System.currentTimeMillis();File target;
  do{target=new File(current.getParentFile(),"survivor-slot-"+id+++".dat");}while(GameSave.exists(target));
  try(FileInputStream input=new FileInputStream(current);FileOutputStream output=new FileOutputStream(target)){
   byte[] data=new byte[8192];int count;while((count=input.read(data))!=-1)output.write(data,0,count);output.getFD().sync();
  }catch(IOException e){target.delete();throw e;}
 }
 static GameSave choose(File current,Scanner scanner){
  List<File> saves=all(current);if(saves.isEmpty()){GameIO.menu("Загрузить игру","Сохранений пока нет.");GameIO.action(0,"Назад");SurvivalSystems.choice(scanner);return null;}
  GameIO.menu("Загрузить игру","Выберите сохранение. Повреждённый файл можно пропустить.");
  for(int i=0;i<saves.size();i++){File f=saves.get(i);String date=new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(modified(f)));String label=f.equals(current)?"Последний прогресс":"Сохранение "+(i+1);GameIO.action(i+1,label,date);}
  GameIO.action(0,"Назад");int c=SurvivalSystems.choice(scanner);if(c<=0||c>saves.size())return null;
  try{return GameSave.load(saves.get(c-1));}catch(IOException e){GameIO.menu("Не удалось загрузить",e.getMessage());GameIO.action(0,"Назад");SurvivalSystems.choice(scanner);return null;}
 }
 static GameSave latest(File current) throws IOException {List<File> saves=all(current);if(saves.isEmpty())throw new IOException("Сохранений пока нет.");return GameSave.load(saves.get(0));}
}
