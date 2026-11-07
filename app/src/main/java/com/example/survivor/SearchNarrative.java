package com.example.survivor;

import java.util.*;

/** Selects ordinary search text most often and limits atmospheric lines to suitable surroundings. */
final class SearchNarrative {
 static final List<String> ORDINARY=Arrays.asList(
  "Джон внимательно осматривает окрестности в поисках полезных вещей.",
  "Обыск продолжается. Джон проверяет всё, что может представлять интерес.",
  "Джон перебирает оставленные вещи, стараясь ничего не пропустить.",
  "Поиск занимает время. Пока ничего необычного не происходит.",
  "Джон осматривает доступные места, проверяя, не осталось ли здесь припасов.");
 private final Map<String,List<String>> groups=new HashMap<>();
 private String previous="";
 SearchNarrative(List<String> lines){
  for(String line:lines){int tab=line.indexOf('\t');if(tab<=0)continue;
   String value=line.substring(tab+1).trim().replace("|","\n");
   if(!value.isEmpty())groups.computeIfAbsent(line.substring(0,tab),k->new ArrayList<>()).add(value);
  }
 }
 static boolean outdoor(int place,String title){
  String t=title.toLowerCase(Locale.ROOT);
  if(t.matches(".*(подвал|кладов|кабинет|комнат|квартир|коридор|архив|шкаф|салон|багажник|склад|шахт|лаборатор|убежищ|домик|избушк|здани|казарм|госпитал|бункер).*"))return false;
  return place==1||place==2||place==3||place==6||place==7||place==19||place==21||place==22||place==23||place==24||place==25||place==27||place==28||place==32||place==38||place==40||place==42||place==43||place==45;
 }
 List<String> atmospheric(int place,String title){
  List<String> list=new ArrayList<>();add(list,"general");
  boolean outside=outdoor(place,title);add(list,outside?"outdoor":"indoor");
  if(place==1||place==3||place==4||place==5||place==7||place==8||place==29||place==34) add(list,"residential");
  if(place==10||place==11||place==12||place==13||place==48||place==53)add(list,"medical");
  if(!outside&&(place==9||place==14||place>=27&&place<=32||place>=35&&place<=47||place>=49&&place<=51))add(list,"industrial");
  return list;
 }
 private void add(List<String> list,String key){List<String> source=groups.get(key);if(source!=null)list.addAll(source);}
 String choose(Random random,int place,String title){
  boolean ordinary=random.nextInt(100)<70;
  List<String> pool=ordinary?ORDINARY:atmospheric(place,title==null?"":title);
  if(pool.isEmpty())pool=ORDINARY;
  int index=random.nextInt(pool.size());String selected=pool.get(index);
  if(pool.size()>1&&selected.equals(previous)){
   for(int offset=1;offset<pool.size();offset++){String candidate=pool.get((index+offset)%pool.size());if(!candidate.equals(previous)){selected=candidate;break;}}
  }
  previous=selected;return selected;
 }
}
