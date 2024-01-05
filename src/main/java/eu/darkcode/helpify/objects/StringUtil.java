package eu.darkcode.helpify.objects;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

public class StringUtil {

    private static final Pattern REMOVE_UNWANTED = Pattern.compile("[^\\p{ASCII}]+");
    private static final Pattern HYPHEN = Pattern.compile("[\\W\\s+]+");
    private static final Pattern TRIM_DASH = Pattern.compile("^-|-$");

    public static String slug(@Nullable String string){
        return Optional.ofNullable(string)
                .filter((s) -> !s.isEmpty())
                .map((s) -> REMOVE_UNWANTED.matcher(s).replaceAll(""))
                .map((s) -> HYPHEN.matcher(s).replaceAll("-"))
                .map(String::trim)
                .map((s) -> TRIM_DASH.matcher(s).replaceAll(""))
                .map(String::toLowerCase)
                .orElse("");
    }

    public static String lastPartOfSlug(String url) {
        String[] split = url.split("-");
        return split.length == 0 ? "" : split[split.length-1];
    }

    public static String limit(String text, int length) {
        if(text.length() > length - 3){
            return text.substring(0, length-3) + "...";
        }
        return text;
    }
}