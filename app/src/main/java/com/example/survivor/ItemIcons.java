package com.example.survivor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import androidx.core.content.ContextCompat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adds small item illustrations without changing game commands or saved item names. */
final class ItemIcons {
    private final Context context;
    private final Map<String, Integer> resources = new HashMap<>();
    private final Pattern names;
    private final int size;

    ItemIcons(Context context) {
        this.context = context;
        size = Math.round(22 * context.getResources().getDisplayMetrics().density);
        TypedArray images = context.getResources().obtainTypedArray(R.array.item_icon_drawables);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("item_icons.tsv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if(line.trim().isEmpty())continue;
                String[] cells = line.split("\t");
                int id = images.getResourceId(Integer.parseInt(cells[0]) - 1, 0);
                for (int i = 1; i < cells.length; i++) resources.put(normalize(cells[i]), id);
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Missing packaged item icon catalogue", e);
        } finally { images.recycle(); }
        // Individually reviewed crops; more specific item names take priority.
        resources.put(normalize("Тестер кабелей"), R.drawable.item_reviewed_123);
        resources.put(normalize("Зарядное устройство"), R.drawable.item_reviewed_124);
        resources.put(normalize("Комплект деталей для патронов"), R.drawable.item_reviewed_125);
        resources.put(normalize("Монитор"), R.drawable.item_reviewed_126);
        resources.put(normalize("Холодильник"), R.drawable.item_reviewed_127);
        resources.put(normalize("Аптечный холодильник"), R.drawable.item_reviewed_127);
        resources.put(normalize("Аэрозольный баллон"), R.drawable.item_reviewed_128);
        resources.put(normalize("Баллончик"), R.drawable.item_reviewed_128);
        resources.put(normalize("Ультрафиолетовый стерилизатор"), R.drawable.item_reviewed_129);
        resources.put(normalize("Порох"), R.drawable.item_reviewed_130);
        resources.put(normalize("Набор инструментов"), R.drawable.item_reviewed_131);
        resources.put(normalize("Слесарные тиски"), R.drawable.item_reviewed_132);
        resources.put(normalize("Домкрат"), R.drawable.item_reviewed_133);
        resources.put(normalize("Сварочный аппарат"), R.drawable.item_reviewed_134);
        resources.put(normalize("Электрическая дрель"), R.drawable.item_reviewed_135);
        resources.put(normalize("Ручная дрель"), R.drawable.item_reviewed_135);
        resources.put(normalize("Шуруповёрт"), R.drawable.item_reviewed_136);
        resources.put(normalize("Аккумуляторный шуруповёрт"), R.drawable.item_reviewed_136);
        resources.put(normalize("Угловая шлифмашина"), R.drawable.item_reviewed_137);
        resources.put(normalize("Электрический лобзик"), R.drawable.item_reviewed_138);
        resources.put(normalize("Складной верстак"), R.drawable.item_reviewed_139);
        resources.put(normalize("Струбцина"), R.drawable.item_reviewed_140);
        resources.put(normalize("Рулетка"), R.drawable.item_reviewed_141);
        resources.put(normalize("Ручной генератор"), R.drawable.item_reviewed_142);
        resources.put(normalize("Револьвер"), R.drawable.item_reviewed_143);
        resources.put(normalize("Пистолет"), R.drawable.item_reviewed_144);
        resources.put(normalize("Револьверный патрон"), R.drawable.item_reviewed_145);
        resources.put(normalize("Пистолетный патрон"), R.drawable.item_reviewed_146);
        resources.put(normalize("Карабинный патрон"), R.drawable.item_reviewed_153);
        resources.put(normalize("Рогатка"), R.drawable.item_reviewed_155);
        resources.put(normalize("Метательная рогатка"), R.drawable.item_reviewed_155);
        resources.put(normalize("Стрелы"), R.drawable.item_reviewed_156);
        resources.put(normalize("Арбалетный болт"), R.drawable.item_reviewed_157);
        resources.put(normalize("Бита"), R.drawable.item_reviewed_158);
        resources.put(normalize("Алюминиевая бита"), R.drawable.item_reviewed_158);
        resources.put(normalize("Деревянная бита"), R.drawable.item_reviewed_158);
        resources.put(normalize("Бронежилет"), R.drawable.item_reviewed_163);
        resources.put(normalize("Защитный жилет"), R.drawable.item_reviewed_163);
        resources.put(normalize("Бронежилет рейдера"), R.drawable.item_reviewed_163);
        resources.put(normalize("Металлолом"), R.drawable.item_reviewed_164);
        resources.put(normalize("Наждачная бумага"), R.drawable.item_reviewed_165);
        // Reviewed crops from the new 1–70 sheets.
        resources.put(normalize("Мука"), R.drawable.item_reviewed_001);
        resources.put(normalize("Соль"), R.drawable.item_reviewed_002);
        resources.put(normalize("Сахар"), R.drawable.item_reviewed_003);
        resources.put(normalize("Энергетический батончик"), R.drawable.item_reviewed_004);
        resources.put(normalize("Протеиновый батончик"), R.drawable.item_reviewed_004);
        resources.put(normalize("Шоколад"), R.drawable.item_reviewed_005);
        resources.put(normalize("Шоколад из шкафчика"), R.drawable.item_reviewed_005);
        resources.put(normalize("Растворимый кофе"), R.drawable.item_reviewed_006);
        resources.put(normalize("Растворимый какао"), R.drawable.item_reviewed_007);
        resources.put(normalize("Галеты"), R.drawable.item_reviewed_008);
        resources.put(normalize("Пачка галет"), R.drawable.item_reviewed_008);
        resources.put(normalize("Галеты из детского рюкзака"), R.drawable.item_reviewed_008);
        resources.put(normalize("Крекеры"), R.drawable.item_reviewed_009);
        resources.put(normalize("Сухари"), R.drawable.item_reviewed_010);
        resources.put(normalize("Пакет сухарей"), R.drawable.item_reviewed_010);
        resources.put(normalize("Аварийный пищевой рацион"), R.drawable.item_reviewed_011);
        resources.put(normalize("Аварийный паёк"), R.drawable.item_reviewed_011);
        resources.put(normalize("Армейский сухпаёк"), R.drawable.item_reviewed_012);
        resources.put(normalize("Армейский рацион"), R.drawable.item_reviewed_012);
        resources.put(normalize("Сухпаёк"), R.drawable.item_reviewed_012);
        resources.put(normalize("Мясные консервы"), R.drawable.item_reviewed_013);
        resources.put(normalize("Тушенка"), R.drawable.item_reviewed_013);
        resources.put(normalize("Тушёнка"), R.drawable.item_reviewed_013);
        resources.put(normalize("Кукурузные хлопья"), R.drawable.item_reviewed_014);
        resources.put(normalize("Овсяные хлопья"), R.drawable.item_reviewed_015);
        resources.put(normalize("Консервы"), R.drawable.item_reviewed_016);
        resources.put(normalize("Консервы из кладовой"), R.drawable.item_reviewed_016);
        resources.put(normalize("Бинт"), R.drawable.item_reviewed_017);
        resources.put(normalize("Бинты"), R.drawable.item_reviewed_017);
        resources.put(normalize("Марлевые салфетки"), R.drawable.item_reviewed_018);
        resources.put(normalize("Носки"), R.drawable.item_reviewed_019);
        resources.put(normalize("Запасные носки"), R.drawable.item_reviewed_019);
        resources.put(normalize("Швейный набор"), R.drawable.item_reviewed_020);
        resources.put(normalize("Набор швейных принадлежностей"), R.drawable.item_reviewed_020);
        resources.put(normalize("Швейный набор для ремонта обуви"), R.drawable.item_reviewed_020);
        resources.put(normalize("Антисептик"), R.drawable.item_reviewed_021);
        resources.put(normalize("Зубная паста"), R.drawable.item_reviewed_022);
        resources.put(normalize("Зубная щётка"), R.drawable.item_reviewed_023);
        resources.put(normalize("Мыло"), R.drawable.item_reviewed_024);
        resources.put(normalize("Пластырь"), R.drawable.item_reviewed_025);
        resources.put(normalize("Набор пластырей"), R.drawable.item_reviewed_025);
        resources.put(normalize("Обезболивающее"), R.drawable.item_reviewed_026);
        resources.put(normalize("Таблетки"), R.drawable.item_reviewed_027);
        resources.put(normalize("Медицинский фонарик"), R.drawable.item_reviewed_028);
        resources.put(normalize("Шприц"), R.drawable.item_reviewed_029);
        resources.put(normalize("Шприц одноразовый"), R.drawable.item_reviewed_029);
        resources.put(normalize("Физиологический раствор"), R.drawable.item_reviewed_030);
        resources.put(normalize("Медицинская сумка"), R.drawable.item_reviewed_032);
        resources.put(normalize("Контейнер для медикаментов"), R.drawable.item_reviewed_032);
        resources.put(normalize("Хирургический набор"), R.drawable.item_reviewed_034);
        resources.put(normalize("Медицинский жгут"), R.drawable.item_reviewed_035);
        resources.put(normalize("Стетоскоп"), R.drawable.item_reviewed_036);
        resources.put(normalize("Пульсоксиметр"), R.drawable.item_reviewed_037);
        resources.put(normalize("Тонометр"), R.drawable.item_reviewed_038);
        resources.put(normalize("Дефибриллятор"), R.drawable.item_reviewed_039);
        resources.put(normalize("Аптечка"), R.drawable.item_reviewed_040);
        resources.put(normalize("Полевая аптечка"), R.drawable.item_reviewed_040);
        resources.put(normalize("Медицинский набор"), R.drawable.item_reviewed_040);
        resources.put(normalize("Стерильная повязка"), R.drawable.item_reviewed_041);
        resources.put(normalize("Перевязочный пакет"), R.drawable.item_reviewed_041);
        resources.put(normalize("Перевязочный комплект"), R.drawable.item_reviewed_041);
        resources.put(normalize("Термопокрывало"), R.drawable.item_reviewed_042);
        resources.put(normalize("Складные носилки"), R.drawable.item_reviewed_045);
        resources.put(normalize("Медицинская кушетка"), R.drawable.item_reviewed_046);
        resources.put(normalize("Складная медицинская кушетка"), R.drawable.item_reviewed_046);
        resources.put(normalize("Шлем"), R.drawable.item_reviewed_047);
        resources.put(normalize("Боевой шлем"), R.drawable.item_reviewed_047);
        resources.put(normalize("Рюкзак"), R.drawable.item_reviewed_048);
        resources.put(normalize("Тактический рюкзак"), R.drawable.item_reviewed_048);
        resources.put(normalize("Котелок"), R.drawable.item_reviewed_049);
        resources.put(normalize("Походный котелок"), R.drawable.item_reviewed_049);
        resources.put(normalize("Аккумулятор 18650"), R.drawable.item_reviewed_056);
        resources.put(normalize("Зажигалка"), R.drawable.item_reviewed_057);
        resources.put(normalize("Огниво"), R.drawable.item_reviewed_058);
        resources.put(normalize("Рация"), R.drawable.item_reviewed_059);
        resources.put(normalize("Радиостанция"), R.drawable.item_reviewed_059);
        resources.put(normalize("Верёвка"), R.drawable.item_reviewed_060);
        resources.put(normalize("Буксировочный трос"), R.drawable.item_reviewed_061);
        resources.put(normalize("Верёвочная лестница"), R.drawable.item_reviewed_062);
        resources.put(normalize("Изолента"), R.drawable.item_reviewed_063);
        resources.put(normalize("Клейкая лента"), R.drawable.item_reviewed_063);
        resources.put(normalize("Скотч"), R.drawable.item_reviewed_064);
        resources.put(normalize("Разводной ключ"), R.drawable.item_reviewed_065);
        resources.put(normalize("Динамометрический ключ"), R.drawable.item_reviewed_067);
        resources.put(normalize("Трубный ключ"), R.drawable.item_reviewed_068);
        resources.put(normalize("Торцевой набор"), R.drawable.item_reviewed_069);
        resources.put(normalize("Молоток"), R.drawable.item_reviewed_070);
        resources.put(normalize("Столярный молоток"), R.drawable.item_reviewed_070);
        List<String> keys = new ArrayList<>(resources.keySet());
        keys.sort(Comparator.comparingInt(String::length).reversed());
        StringBuilder expression = new StringBuilder("(?<![\\p{L}\\p{N}])(?:");
        for (String key : keys) {
            if (expression.charAt(expression.length() - 1) != ':') expression.append('|');
            expression.append(Pattern.quote(key));
        }
        expression.append(")(?![\\p{L}\\p{N}])");
        names = Pattern.compile(expression.toString());
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    int resourceFor(String text) {
        if(com.example.survivor.game.JunkLoot.isJunkText(text))return 0;
        Matcher matcher=names.matcher(normalize(text));
        return matcher.find()?resources.getOrDefault(matcher.group(),0):0;
    }
    CharSequence decorate(CharSequence text) {
        if(com.example.survivor.game.JunkLoot.isJunkText(text.toString()))return text;
        Matcher matcher = names.matcher(normalize(text.toString()));
        List<int[]> matches = new ArrayList<>();
        while (matcher.find()) matches.add(new int[] {matcher.start(), resources.get(matcher.group())});
        if (matches.isEmpty()) return text;
        SpannableStringBuilder result = new SpannableStringBuilder(text);
        // Work backwards to preserve title/detail text styling and all match offsets.
        for (int i = matches.size() - 1; i >= 0; i--) {
            int start = matches.get(i)[0];
            Drawable drawable = ContextCompat.getDrawable(context, matches.get(i)[1]);
            if (drawable == null) continue;
            if (matches.get(i)[1] == R.drawable.item_icon_chip) {
                android.graphics.ColorMatrix monochrome = new android.graphics.ColorMatrix();
                monochrome.setSaturation(0);
                drawable = drawable.mutate();
                drawable.setColorFilter(new android.graphics.ColorMatrixColorFilter(monochrome));
            }
            drawable.setBounds(0, 0, size, size);
            result.insert(start, "\uFFFC ");
            result.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), start, start + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return result;
    }
}
