package dev.passport.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита раскраски текста в legacy-формат (символ секции {@code §}),
 * совместимый со всеми версиями Minecraft от 1.16.5 до 1.21.x.
 * <p>
 * Поддерживаются:
 * <ul>
 *     <li>обычные коды цвета и стиля — {@code &a}, {@code &c}, {@code &l}, {@code &r} и т.д.;</li>
 *     <li>hex-цвета — {@code &#ff8800} (раскрашивает текст одним цветом);</li>
 *     <li>градиенты — {@code <gradient:#ff0000:#0000ff>текст</gradient>}
 *         (плавный переход между двумя hex-цветами по буквам).</li>
 * </ul>
 * Hex и градиенты отображаются клиентом начиная с версии 1.16, поэтому
 * на всём поддерживаемом диапазоне версий работают корректно.
 */
public final class TextUtil {

    /** Символ секции, которым Minecraft помечает коды форматирования. */
    private static final char COLOR_CHAR = '§';

    /** Стандартные коды цвета и стиля для {@code &}-нотации. */
    private static final String FORMAT_CODES = "0123456789abcdefklmnor";

    /** Коды стиля (без цвета), которые сохраняются внутри градиента. */
    private static final String STYLE_CODES = "klmnor";

    private static final Pattern HEX_PATTERN =
            Pattern.compile("&#([0-9a-fA-F]{6})");

    private static final Pattern GRADIENT_PATTERN =
            Pattern.compile("<gradient:#([0-9a-fA-F]{6}):#([0-9a-fA-F]{6})>(.*?)</gradient>",
                    Pattern.DOTALL);

    private TextUtil() {
    }

    /**
     * Преобразует строку с {@code &}-кодами, hex и градиентами
     * в готовую к отправке legacy-строку с символами {@code §}.
     *
     * @param input исходная строка (может быть {@code null})
     * @return раскрашенная строка, никогда не {@code null}
     */
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String result = applyGradients(input);
        result = applyHex(result);
        result = applyAmpCodes(result);
        return result;
    }

    // ----- Градиенты --------------------------------------------------------

    private static String applyGradients(String input) {
        Matcher matcher = GRADIENT_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int[] from = hexToRgb(matcher.group(1));
            int[] to = hexToRgb(matcher.group(2));
            String content = matcher.group(3);
            String colored = buildGradient(content, from, to);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(colored));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Раскрашивает каждый видимый символ текста интерполированным цветом.
     * Коды стиля ({@code &l}, {@code &o} и т.п.) внутри сохраняются и
     * применяются к последующим символам.
     */
    private static String buildGradient(String text, int[] from, int[] to) {
        int visible = countVisible(text);
        StringBuilder out = new StringBuilder();
        StringBuilder activeStyles = new StringBuilder();
        int index = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Код стиля внутри градиента: запоминаем, но не печатаем как символ.
            if (c == '&' && i + 1 < text.length()
                    && STYLE_CODES.indexOf(Character.toLowerCase(text.charAt(i + 1))) > -1) {
                char style = Character.toLowerCase(text.charAt(i + 1));
                if (style == 'r') {
                    activeStyles.setLength(0);
                } else {
                    activeStyles.append(COLOR_CHAR).append(style);
                }
                i++;
                continue;
            }

            double ratio = visible <= 1 ? 0.0 : (double) index / (visible - 1);
            int r = (int) Math.round(from[0] + (to[0] - from[0]) * ratio);
            int g = (int) Math.round(from[1] + (to[1] - from[1]) * ratio);
            int b = (int) Math.round(from[2] + (to[2] - from[2]) * ratio);

            out.append(hexSequence(r, g, b)).append(activeStyles).append(c);
            index++;
        }
        return out.toString();
    }

    /** Считает количество видимых символов (без учётов кодов стиля). */
    private static int countVisible(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()
                    && STYLE_CODES.indexOf(Character.toLowerCase(text.charAt(i + 1))) > -1) {
                i++;
                continue;
            }
            count++;
        }
        return count;
    }

    // ----- Одиночный hex (&#rrggbb) -----------------------------------------

    private static String applyHex(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int[] rgb = hexToRgb(matcher.group(1));
            matcher.appendReplacement(sb,
                    Matcher.quoteReplacement(hexSequence(rgb[0], rgb[1], rgb[2])));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // ----- Обычные &-коды ---------------------------------------------------

    private static String applyAmpCodes(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&'
                    && FORMAT_CODES.indexOf(Character.toLowerCase(chars[i + 1])) > -1) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    // ----- Вспомогательное --------------------------------------------------

    /**
     * Формирует hex-последовательность клиента: {@code §x§R§R§G§G§B§B}.
     */
    private static String hexSequence(int r, int g, int b) {
        String hex = String.format("%02x%02x%02x", clamp(r), clamp(g), clamp(b));
        StringBuilder sb = new StringBuilder();
        sb.append(COLOR_CHAR).append('x');
        for (int i = 0; i < hex.length(); i++) {
            sb.append(COLOR_CHAR).append(hex.charAt(i));
        }
        return sb.toString();
    }

    private static int[] hexToRgb(String hex) {
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 255);
    }
}
