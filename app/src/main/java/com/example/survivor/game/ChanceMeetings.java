package com.example.survivor.game;
import java.io.Serializable;
import java.util.*;
/** Brief one-off meetings; they never join the evacuation roster. */
final class ChanceMeetings implements Serializable {
 private static final long serialVersionUID=1L;
 private int completed; private int pending,phase,place;
 private static final String[] TITLES={"","За сколько продашь совесть?","Я тебя знаю","Один патрон","Не трогай пакет"};
 static boolean allowed(int id,int p){return switch(id){case 1->p>=10&&p<=13;case 2->p==1||p==3||p==5||p==19||p>=33&&p<=40;case 3->p==3||p>=33&&p<=47;case 4->p==1||p==4||p==5;default->false;};}
 void maybe(Fighter player,WorldStories world,int location,Scanner scanner,Random random){
  if(pending!=0 && place==location){run(player,world,scanner,random);return;}
  if(pending!=0 || random.nextInt(100)>=8)return;
  List<Integer> candidates=new ArrayList<>();for(int id=1;id<=4;id++)if(allowed(id,location) && (completed&(1<<id))==0)candidates.add(id);
  if(candidates.isEmpty())return;pending=candidates.get(random.nextInt(candidates.size()));place=location;phase=0;GameIO.checkpoint();run(player,world,scanner,random);
 }
 void startForTest(int id,int location){pending=id;place=location;phase=0;}
 boolean completed(int id){return (completed&(1<<id))!=0;}
 void finish(WorldStories world){completed|=1<<pending;world.note(TITLES[pending],"Короткая встреча завершена. Новых спутников или задач нет.");pending=0;phase=0;GameIO.checkpoint();}
 void run(Fighter player,WorldStories world,Scanner scanner,Random random){
  while(pending!=0 && player.isAlive() && GameIO.hasInput(scanner)){
   String text="";String[] answers=null;
   switch(pending){
    case 1->{if(phase==0){text="В коридоре сидит мужчина в грязном медицинском халате. Он перебирает упаковки таблеток.\n\nМужчина: «Эй. У тебя бинты есть?»";answers=new String[]{"А тебе зачем?","Сначала скажи, кто ты.","Смотря сколько предложишь"};}else if(phase==1){text="Мужчина: «Я санитар. Был санитаром. Теперь, видимо, просто долбоёб с дипломом».\n\nОн показывает ладонь, покрытую старыми порезами.\n\nМужчина: «Мне одному помогли. Десятерых пришлось оставить. Знаешь, как это называется?»";answers=new String[]{"Выживание","Ты сделал всё, что смог","Называется, тебе не повезло"};}else text="Мужчина: «Нет. Это называется: однажды перестаёшь считать людей».\n\nМужчина собирает лекарства и уходит через служебный выход.";}
    case 2->{if(phase==0){text="Навстречу Джону выходит женщина лет тридцати пяти. Увидев его, она внезапно останавливается.\n\nЖенщина: «Нет... Только не ты».";answers=new String[]{"Мы знакомы?","Ты ошиблась","Что я тебе сделал?"};}else if(phase==1){text="Женщина: «Я видела тебя. Возле больницы. Ты стоял и смотрел, как они умирали».\n\nОна отступает на шаг.\n\nЖенщина: «На тебе даже куртка та же».";answers=new String[]{"Я вообще там не был","Ты кого-то со мной перепутала","Расскажи, что случилось"};}else text="Женщина: «Не подходи ко мне. Не хочу снова тебя видеть».\n\nЖенщина убегает. Доказательств её слов нет.";}
    case 3->{if(phase==0){text="Возле разбитого грузовика сидит молодой мужчина в военной куртке. В руке у него револьвер.\n\nМужчина: «Не приближайся. У меня один патрон остался».";answers=new String[]{"Тогда лучше не трать его на меня","Я не собираюсь нападать","И кого ты им напугать решил?"};}else if(phase==1){text="Мужчина: «Я не пугаю. Просто предупреждаю».\n\nОн смотрит на дорогу.\n\nМужчина: «Знаешь, что самое смешное? Я всю жизнь боялся умереть. А теперь боюсь не успеть».";answers=new String[]{"Не успеть куда?","Ты вообще понимаешь, что говоришь?","Давай без загадок"};}else text="Мужчина: «У меня сестра в городе. Я обещал вернуться».\n\nОн встаёт и начинает уходить по дороге.";}
    case 4->{if(phase==0){text="У мусорного контейнера пожилая женщина складывает продукты в большой пакет. Часть еды явно испорчена.\n\nЖенщина: «Не смей трогать! Моё!»";answers=new String[]{"Да забирайте, мне не нужно","Там половина продуктов испорчена","Я вообще-то просто мимо проходил"};}else if(phase==1){text="Женщина: «Все так говорят. А потом отворачиваешься, и уже ничего нет!»\n\nОна пытается поднять тяжёлый пакет.\n\nЖенщина: «Вот раньше люди друг другу помогали. А теперь только зубы показывают».";answers=new String[]{"Помочь донести?","Вы же сами на меня наорали","Лучше оставьте испорченное"};}else text="Женщина: «Не надо мне помогать. Сама донесу».\n\nОна поднимает пакет и медленно уходит. Через несколько секунд за углом слышен звук упавшей бутылки. Что произошло, не видно.";}
   }
   GameIO.storyMenu(TITLES[pending],text);
   if(answers!=null){for(int i=0;i<answers.length;i++)GameIO.action(i+1,answers[i]);GameIO.action(0,"Отойти");int a=SurvivalSystems.choice(scanner);if(a==0)return;if(a<1||a>answers.length)continue;phase++;GameIO.checkpoint();continue;}
   if(pending==1)GameIO.action(2,"Последовать за ним","Служебный выход");if(pending==3)GameIO.action(2,"Подойти вплотную","Он может выстрелить");GameIO.action(1,"Уйти");int a=SurvivalSystems.choice(scanner);if(a!=1&&a!=2)return;
   if(a==2&&pending==1)ResidentialStories.pause("Служебный выход","Проход закрыт. Мужчина ушёл.",scanner);
   if(a==2&&pending==3){if(random.nextInt(100)<40){GameIO.sound(GameSound.SHOT);player.takeDamage(8);GameIO.out().println("Незнакомец выстрелил и скрылся. Джон ранен.");}else GameIO.out().println("Незнакомец поднимает револьвер. Джон останавливается; тот уходит.");}
   finish(world);
  }
 }
}
