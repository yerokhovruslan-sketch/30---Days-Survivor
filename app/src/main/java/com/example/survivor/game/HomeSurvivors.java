package com.example.survivor.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** People explicitly invited to John's shelter and preparation for shelter 6. */
final class HomeSurvivors implements Serializable {
    private static final long serialVersionUID=1L;
    static final int SEMEN=0, ANDREY=1, KIRILL=2, OLGA=3, VIKTOR=4, MARINA=5;
    static final String[] NAMES={"Семён","Андрей","Кирилл","Оля","Виктор Павлович","Марина и Артём"};
    boolean[] home=new boolean[6], travelling=new boolean[6];
    int[] nextTalk=new int[6];
    boolean radioConfirmed,ownLandEnding;

    boolean atHome(int id){return home[id];}
    boolean invite(int id,WorldStories w,Scanner scanner){
        if(home[id])return false;
        GameIO.storyMenu("Приглашение", "Джон: «У меня есть убежище. Можешь пойти со мной, если хочешь».\n\n"+NAMES[id]+": «Хорошо. Вместе будет проще». ");
        GameIO.action(1,"Вернуться вместе домой");GameIO.action(0,"Пока остаться здесь");
        if(SurvivalSystems.choice(scanner)!=1)return false;
        home[id]=true;travelling[id]=true;nextTalk[id]=w.absoluteMinute()+1440;
        w.note("Выжившие в убежище",NAMES[id]+" согласились отправиться с Джоном домой. Поговорить снова можно через сутки.");GameIO.checkpoint();return true;
    }
    int people(){int count=1;for(int i=0;i<home.length;i++)if(home[i]&&travelling[i])count+=i==MARINA?2:1;return count;}
    String group(){List<String> names=new ArrayList<>();for(int i=0;i<home.length;i++)if(home[i]&&travelling[i])names.add(NAMES[i]);return names.isEmpty()?"Джон отправится один":"С Джоном: "+String.join(", ",names);}
    void menu(WorldStories w,Scanner scanner){
        while(true){
            GameIO.menu("Выжившие","Здесь находятся люди, которые согласились жить в убежище. Можно поговорить или решить, кто отправится в убежище №6.");
            boolean any=false;for(int i=0;i<home.length;i++)if(home[i]){any=true;GameIO.action(i+1,NAMES[i],w.absoluteMinute()<nextTalk[i]?"Поговорить можно через "+((nextTalk[i]-w.absoluteMinute()+59)/60)+" ч.":"Поговорить");}
            if(!any)GameIO.card("Пока никого нет","Помогайте встреченным людям и предлагайте пойти с вами, когда заслужите доверие.");
            if(any)GameIO.action(10,"Выбрать спутников для дороги",group());
            GameIO.action(0,"В меню");int c=SurvivalSystems.choice(scanner);if(c==0)return;
            if(c==10&&any){partyMenu(scanner);continue;}
            int id=c-1;if(id<0||id>=home.length||!home[id])continue;
            if(w.absoluteMinute()<nextTalk[id]){GameIO.out().println("Сейчас "+NAMES[id]+" отдыхает. Вернитесь позже.");continue;}
            nextTalk[id]=w.absoluteMinute()+1440;GameIO.checkpoint();
            String[] lines={"Семён: «Если проснёшься раньше меня, проверь, на месте ли крыша. На всякий случай».\n\nДжон: «Проверю».\n\nСемён: «И небо тоже». Осторожная улыбка появляется на его лице.",
                "Андрей: «Странно спать под крышей, когда вокруг не лес. Но сегодня я хотя бы не один».\n\nДжон: «Привыкнем». Ждать Андрей пока не обещает.",
                "Кирилл: «Трубу у двери оставил. Так, на всякий случай».\n\nДжон: «В этом доме запасные планы не помешают».",
                "Оля: «Если опять придёшь с кровоточащей раной и скажешь, что это царапина, я тебя сама перевяжу к стулу».\n\nДжон: «Понял».",
                "Виктор Павлович: «Надо бы пересчитать запасы. Привычка, знаешь ли».\n\nДжон: «Давай вместе».",
                "Марина: «Артём наконец заснул спокойно. Спасибо, что пустил нас».\n\nДжон: «Теперь вы здесь не одни»."};
            ResidentialStories.pause(NAMES[id],lines[id],scanner);
        }
    }
    private void partyMenu(Scanner scanner){while(true){GameIO.menu("Спутники","Выберите, кто пойдёт к убежищу №6. Припасы рассчитываются на каждого человека.");for(int i=0;i<home.length;i++)if(home[i])GameIO.action(i+1,NAMES[i],travelling[i]?"Пойдёт с Джоном":"Останется дома");GameIO.action(0,"Назад");int c=SurvivalSystems.choice(scanner);if(c==0)return;if(c>=1&&c<=home.length&&home[c-1]){travelling[c-1]=!travelling[c-1];GameIO.checkpoint();}}}
    void radio(Fighter p,WorldStories w,Scanner scanner){
        if(!w.warehouseFinale().registryFound || p.getUpgrades().radio<=0)return;
        if(!radioConfirmed){
            radioConfirmed=true;w.note("Связь с убежищем №6","Радист убежища №6 — Алексей. Он подтвердил наличие живых людей, колодца, работающего генератора и места для новых выживших. Транспорта нет. Это другой человек, не водитель Дмитрий; судьба водителя неизвестна.");GameIO.checkpoint();
            ResidentialStories.pause("Убежище №6","Радист: «...принимаем. Кто вызывает?»\n\nДжон: «Джон. Вы из убежища номер шесть?»\n\nРадист: «Если ты это спрашиваешь, значит, наш старый передатчик ещё кому-то нужен».\n\nДжон: «У вас есть транспорт? Можете забрать людей?»\n\nРадист: «Транспорта нет. И забрать мы никого не можем».\n\nДжон: «Заебись. Второй раз за месяц».\n\nРадист: «Зато у нас есть колодец, генератор и место. Не гостиница, конечно. Но люди здесь живут».\n\nДжон: «А заражённые?»\n\nРадист: «Бывают. Мы не на другой планете, дружище».\n\nДжон: «И зачем мне к вам идти?»\n\nРадист: «Не знаю. Я тебя вообще впервые слышу. Решай сам. Меня зовут Алексей».",scanner);
        } else ResidentialStories.pause("Связь с убежищем №6","Алексей подтверждает: люди на связи, вода и место есть, транспорта нет. Дорога остаётся опасной.",scanner);
    }
    private List<ItemInfo> select(Fighter p){
        int n=people(),food=0,water=0,med=0,parts=0;List<ItemInfo> all=new ArrayList<>(p.getInventory());all.addAll(p.getStorage());List<ItemInfo> chosen=new ArrayList<>();
        for(ItemInfo item:all){if(ResidentialStories.water(item)&&water<2*n){water++;chosen.add(item);}else if(ResidentialStories.food(item)&&food<2*n){food++;chosen.add(item);}else if(item instanceof MedicalItem&&med<n){med++;chosen.add(item);}else if(item.getName().equals("Запчасти")&&parts<1){parts++;chosen.add(item);}}
        return food>=2*n&&water>=2*n&&med>=n&&parts>=1?chosen:null;
    }
    String requirements(Fighter p){int n=people();return "Нужно на "+n+" чел.: еда ×"+(2*n)+", вода ×"+(2*n)+", лекарства ×"+n+", запчасти ×1. Учитываются рюкзак и склад убежища. "+group()+".";}
    boolean ready(Fighter p,WorldStories w){return radioConfirmed&&w.warehouseFinale().registryFound&&w.day()<=30&&!w.warehouseFinale().evacuated&&!w.warehouseFinale().perimeterEnding&&select(p)!=null;}
    boolean depart(Fighter p,WorldStories w,Scanner scanner){
        if(!ready(p,w))return false;
        GameIO.storyMenu("Своя земля",requirements(p));GameIO.action(1,"Отказаться от вылета и отправиться к убежищу №6","Припасы будут израсходованы");GameIO.action(0,"Ещё подготовиться");if(SurvivalSystems.choice(scanner)!=1)return false;
        List<ItemInfo> items=select(p);if(items==null)return false;for(ItemInfo item:items){p.getInventory().remove(item);p.getStorage().remove(item);}
        ownLandEnding=true;w.note("Своя земля","Джон добровольно отказался ждать военный рейс и отправился к убежищу №6. "+group()+".");GameIO.checkpoint();
        ending(scanner);return true;
    }
    void ending(Scanner scanner){ResidentialStories.pause("Своя земля","Джон возвращается к складу и сообщает Белову своё решение.\n\nБелов: «Ты понимаешь, что другого рейса не будет?»\n\nДжон: «Понимаю».\n\nБелов: «И всё равно отказываешься? Причина?»\n\nДжон: «Мне надоело идти за голосами, которые обещают, что где-то будет лучше».\n\nБелов: «А ты уверен, что там, куда идёшь, будет лучше?»\n\nДжон: «Нет. Но хотя бы знаю, с кем разговариваю».\n\n"+group()+". Путь нелёгок. Наконец впереди появляются укреплённые ворота.\n\nНеизвестный: «Стой! Кто такие?!»\n\nДжон: «Выжившие. Нам сказали, что у вас есть место».\n\nПосле паузы ворота открываются. За ними нет безопасного мира, но есть живые люди, вода и работа, которую предстоит сделать вместе. Джон сам нашёл дорогу к новому дому.",scanner);}
}
