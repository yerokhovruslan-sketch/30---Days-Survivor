package com.example.survivor;

/** Map the in-game clock to the icon shown in the home HUD. */
final class HudTime {
    private HudTime() {}
    static int phase(String time) {
        if(time==null)return 0;
        String[] parts=time.split(":",2);
        if(parts.length!=2)return 0;
        try {
            int hour=Integer.parseInt(parts[0]), minute=Integer.parseInt(parts[1]);
            if(hour<0 || hour>23 || minute<0 || minute>59)return 0;
            if(hour<6 || hour>=20)return 2;
            return hour>=18?1:0;
        } catch(NumberFormatException ignored) { return 0; }
    }
    static String label(int phase){return phase==2?"Ночь":phase==1?"Вечер":"День";}
}
