package com.example.survivor.game;

import java.io.Serializable;
import java.util.Scanner;

/** The resident of the blue car and the unfinished story of the red scarf. */
final class SemenStory implements Serializable {
    private static final long serialVersionUID=1L;
    static final String SCARF="Красный шарф Лены", NOTE="Записка Лены";
    boolean semen_met,semen_quest_started,red_scarf_found,lena_note_found,semen_scarf_returned,semen_trust_lost,semen_story_completed;
    boolean wardrobeInspected,noteRead,pendingResponse,returnedNote,bonusTaken;
    boolean rewardFood,rewardWater,rewardParts;
    int heartStage;
    int[] heartAnswers = new int[5];
    int firstReaction,hostileResponses,answer,semenQuestStartDay;
    private transient boolean avoidThisVisit;
    void beginVisit(){avoidThisVisit=false;}
    boolean canApproach(){return !avoidThisVisit;}
    boolean canInspectWardrobe(){return semen_quest_started&&!semen_scarf_returned&&!semen_trust_lost;}
    boolean rewardAvailable(){return semen_story_completed&&(!rewardFood||!rewardWater||!rewardParts);}
    boolean bonusAvailable(){return semen_story_completed&&returnedNote&&!bonusTaken;}
    private void note(WorldStories w,String title,String text){w.note(title,text);GameIO.out().println(text);GameIO.checkpoint();}
    private ItemInfo find(Fighter p,String name){return ResidentialStories.find(p,item->item.getName().equals(name));}
    private boolean take(Fighter p,ItemInfo item){if(!p.addItem(item)){GameIO.out().println("В рюкзаке нет места. Находка останется здесь.");return false;}GameIO.sound(GameSound.BAG);GameIO.out().println("Найдено: "+item.getName());return true;}
    private void angry(WorldStories w,Scanner scanner){
        hostileResponses++;avoidThisVisit=true;
        if(hostileResponses>=2){semen_trust_lost=true;note(w,"Пустая машина Семёна","После новой угрозы Семён ушёл из двора. На стекле осталось: «Если проснёшься раньше меня, не забудь закрыть дверь».");ResidentialStories.pause("Семён уходит",SemenDialogue.LOST,scanner);}
        else note(w,"Ссора у синей машины","Семён прекращает разговор и закрывает дверь. При следующем посещении он ещё может согласиться поговорить.");
    }
    void firstMeeting(Fighter p,WorldStories w,Scanner scanner){
        if(semen_met)return;semen_met=true;GameIO.checkpoint();
        GameIO.storyMenu("Не будить",SemenDialogue.INTRO);
        GameIO.action(1,"Спокойно. Я просто ищу припасы");GameIO.action(2,"Допустим, это сон. И что дальше?");GameIO.action(3,"Ты просто свихнулся от одиночества");GameIO.action(4,"Убери термос, пока я не разозлился");GameIO.action(5,"Почему ты решил, что мы спим?");GameIO.action(0,"Отойти");
        int choice=SurvivalSystems.choice(scanner);if(choice<1||choice>5)return;
        firstReaction=choice;GameIO.checkpoint();
        ResidentialStories.pause("Ответ Семёна",SemenDialogue.FIRST[choice],scanner);
        if(choice==3||choice==4){angry(w,scanner);return;}
        talk(p,w,scanner);
    }
    void talk(Fighter p,WorldStories w,Scanner scanner){
        if(semen_trust_lost){ResidentialStories.pause("Пустая машина","Машина пуста. На стекле: «Если проснёшься раньше меня, не забудь закрыть дверь».",scanner);return;}
        if(!canApproach()){GameIO.out().println("Семён закрыл дверь. Он может согласиться поговорить при следующем посещении двора.");return;}
        if(pendingResponse){response(w,scanner);return;}
        if(semen_story_completed){ResidentialStories.pause("Семён у автомобиля",SemenDialogue.GOOD[Math.floorMod(w.day(),SemenDialogue.GOOD.length)],scanner);return;}
        if(semen_scarf_returned)return;
        if(semen_quest_started){
            GameIO.storyMenu("Красный шарф",SemenDialogue.WAIT[semenQuestStartDay>0 && w.day()-semenQuestStartDay>=3?1:0]);
            boolean scarf=find(p,SCARF)!=null,letter=find(p,NOTE)!=null;
            GameIO.action(1,"Вернуть шарф","Только шарф",scarf);
            GameIO.action(2,"Вернуть шарф и записку Лены","Оба предмета нужны в рюкзаке",scarf&&letter);
            GameIO.action(3,"Пригрозить Семёну","Разговор может закончиться");GameIO.action(0,"Вернуться позже");int c=SurvivalSystems.choice(scanner);
            if(c==3){angry(w,scanner);return;}if(c==1&&scarf||c==2&&scarf&&letter)give(p,w,scanner,c==2);return;
        }
        GameIO.storyMenu("Ты ведь тоже это замечаешь?",SemenDialogue.OBSERVATIONS+"\n\n"+SemenDialogue.REQUEST);
        GameIO.action(1,"Согласиться найти шарф");GameIO.action(2,"Пока отказаться","Можно вернуться позже");GameIO.action(3,"Пригрозить Семёну","Он может уйти из двора");GameIO.action(0,"Уйти");int c=SurvivalSystems.choice(scanner);
        if(c==1){semen_quest_started=true;semenQuestStartDay=w.day();note(w,"Красный шарф","Семён попросил найти красный женский шарф с белой полоской в квартире №24 жилого подъезда. О судьбе его жены Лены ничего не известно.");ResidentialStories.pause("Красный шарф",SemenDialogue.ACCEPT,scanner);}
        else if(c==3)angry(w,scanner);
    }
    void wardrobe(Fighter p,WorldStories w,Scanner scanner){
        if(!canInspectWardrobe())return;
        if(!wardrobeInspected){wardrobeInspected=true;noteRead=true;note(w,"Записка Лены","В шкафу одежда, старые фотографии, красный шарф с белой полоской и записка: «Сёма, если опять забудешь ключи, запасные лежат там же, где всегда. И поешь нормально, а не одними бутербродами». Подпись: «Лена».");}
        while(p.isAlive()){
            GameIO.menu("Шкаф с личными вещами","Красный шарф и записка Лены. Можно взять оба предмета, один из них или оставить вещи здесь.");
            GameIO.action(1,"Взять красный шарф",red_scarf_found?"Уже взят":"Личная вещь Семёна",!red_scarf_found);
            GameIO.action(2,"Взять записку Лены",lena_note_found?"Уже взята":"Можно вернуть вместе с шарфом",!lena_note_found);GameIO.action(0,"Уйти");
            int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1&&!red_scarf_found&&take(p,new Tools(SCARF,"Красный женский шарф с белой полоской. Семён просил его найти.",1,0))){red_scarf_found=true;GameIO.checkpoint();}
            if(c==2&&!lena_note_found&&take(p,new Tools(NOTE,"Личная записка Лены Семёну, найденная рядом с шарфом.",1,0))){lena_note_found=true;GameIO.checkpoint();}
        }
    }
    private void give(Fighter p,WorldStories w,Scanner scanner,boolean withNote){
        ItemInfo scarf=find(p,SCARF),letter=find(p,NOTE);if(scarf==null||withNote&&letter==null)return;
        p.getInventory().remove(scarf);if(withNote)p.getInventory().remove(letter);semen_scarf_returned=true;returnedNote=withNote;
        if(withNote){pendingResponse=true;note(w,"Шарф и записка","Семён получил шарф и записку Лены. Он ещё не знает, что случилось с женой.");response(w,scanner);}
        else {semen_story_completed=true;note(w,"Шарф возвращён","Джон вернул Семёну шарф. В багажнике остались небольшие припасы для Джона.");ResidentialStories.pause("Шарф возвращён",SemenDialogue.RETURN+"\n\n"+SemenDialogue.REWARD,scanner);}
    }
    private void response(WorldStories w,Scanner scanner){
        if(!pendingResponse)return;
        GameIO.storyMenu("Разговор после возвращения шарфа",SemenDialogue.RETURN+"\n\n"+SemenDialogue.NOTE+"\n\n"+SemenDialogue.QUESTION);
        GameIO.action(1,"Может, ты уже проснулся. И это реальность");GameIO.action(2,"Не знаю, что реально. Но шарф сейчас у тебя в руках");GameIO.action(3,"Лена хотела бы, чтобы ты продолжал жить");GameIO.action(4,"Если это сон, почему бы тебе не сделать его лучше?");GameIO.action(5,"Я тоже не знаю, что будет завтра");GameIO.action(6,"Хватит прятаться в машине. Вокруг есть живые люди");GameIO.action(0,"Продолжить позже");int c=SurvivalSystems.choice(scanner);if(c<1||c>6)return;
        answer=c;pendingResponse=false;semen_story_completed=true;
        String reply=SemenDialogue.ANSWERS[c];
        note(w,"Ответ Семёну",reply+" Семён подсказал: за задним колесом красной машины лежит старый ящик. В багажнике синей машины он оставил Джону небольшие припасы.");
        ResidentialStories.pause("Ответ Семёна",reply+"\n\n"+SemenDialogue.REWARD+"\n\n"+SemenDialogue.BONUS,scanner);
    }
    void rewards(Fighter p,Scanner scanner){
        if(!semen_story_completed)return;
        while(p.isAlive()){
            GameIO.menu("Припасы Семёна","Семён оставил для Джона немного припасов в багажнике. Их можно забрать позже.");
            GameIO.action(1,"Взять консерву",rewardFood?"Уже взята":"Тушёнка",!rewardFood);GameIO.action(2,"Взять воду",rewardWater?"Уже взята":"Бутылка воды",!rewardWater);GameIO.action(3,"Взять детали",rewardParts?"Уже взяты":"Запчасти",!rewardParts);GameIO.action(0,"Назад");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==1&&!rewardFood&&take(p,Shop.createItem(17))){rewardFood=true;GameIO.checkpoint();}
            if(c==2&&!rewardWater&&take(p,ResidentialStories.waterBottle())){rewardWater=true;GameIO.checkpoint();}
            if(c==3&&!rewardParts&&take(p,Shop.createItem(19))){rewardParts=true;GameIO.checkpoint();}
        }
    }
    void bonus(Fighter p,WorldStories w){if(!bonusAvailable())return;if(take(p,Shop.createItem(42))){bonusTaken=true;note(w,"Ящик за красной машиной","За задним колесом красной машины по подсказке Семёна найдена изолента.");}}
}
