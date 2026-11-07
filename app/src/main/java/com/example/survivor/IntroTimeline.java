package com.example.survivor;

/** The shared timeline for audio, picture, lettering and the continue gate. */
final class IntroTimeline {
    static final int PICTURE_AT = 15_000;
    static final int END = 30_000;
    static final String TEXT = "Связь оборвалась.\n\n"
            + "Ты записал каждое слово. Потом ещё час сидел у приёмника.\n\n"
            + "Возможно, это старая запись. Возможно, там уже никто не ждёт.\n\n"
            + "Но утром ты впервые собираешь рюкзак не только ради еды.\n\n"
            + "У тебя тридцать дней, чтобы найти источник сигнала.\n\n"
            + "И лучше вернуться до темноты.";

    static int characters(int elapsed) {
        return (int) (TEXT.length() * (long) Math.max(0, Math.min(15_000, elapsed - PICTURE_AT)) / 15_000);
    }
    static float pictureAlpha(int elapsed) {
        return Math.max(0f, Math.min(1f, (elapsed - PICTURE_AT) / 1800f));
    }
    static boolean ready(int elapsed) { return elapsed >= END; }
}
