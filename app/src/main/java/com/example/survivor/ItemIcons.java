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
        Matcher matcher=names.matcher(normalize(text));
        return matcher.find()?resources.getOrDefault(matcher.group(),0):0;
    }
    CharSequence decorate(CharSequence text) {
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
