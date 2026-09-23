package com.ishaan.essentialvoice.whisper;

import com.ibm.icu.text.Transliterator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Personal fork addition, 2026-09-23. Offline Roman Marathi for dictation.
 * Only Devanagari runs are transformed; existing Latin text is preserved.
 * This is a spelling converter, not a translator or an English spell checker.
 * Conversational spellings vary; the small overrides are intentionally explicit.
 */
public final class MarathiRomanizer {
    private MarathiRomanizer() {}

    private static final Pattern DEVANAGARI = Pattern.compile(
            "[\\u0900-\\u0963\\u0966-\\u097F\\u200C\\u200D]+|[\\u0964\\u0965]");
    private static final Transliterator TO_LATIN =
            Transliterator.getInstance("Devanagari-Latin");
    private static final Transliterator TO_ASCII =
            Transliterator.getInstance("Latin-ASCII");
    private static final Map<String, String> COMMON = commonSpellings();

    private static Map<String, String> commonSpellings() {
        Map<String, String> words = new HashMap<>();
        words.put("आहे", "aahe");
        words.put("आहेत", "aahet");
        words.put("आहोत", "aahot");
        words.put("आणि", "aani");
        words.put("नाही", "nahi");
        words.put("काय", "kay");
        words.put("जायचे", "jayche");
        words.put("मध्ये", "madhe");
        words.put("तुमच्या", "tumchya");
        words.put("माझ्या", "majhya");
        words.put("ऑफिस", "office");
        words.put("ऑफिसला", "officela");
        words.put("मीटिंग", "meeting");
        words.put("मीटिंगला", "meetingla");
        return Collections.unmodifiableMap(words);
    }

    /** ICU instances are mutable and are protected by this single lock. */
    public static synchronized String romanize(String text) {
        Matcher matcher = DEVANAGARI.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(word(matcher.group())));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String word(String source) {
        String clean = source.replace("\u200C", "").replace("\u200D", "");
        if (clean.equals("।") || clean.equals("॥")) return ".";
        String known = COMMON.get(clean);
        if (known != null) return known;

        String latin = TO_LATIN.transliterate(clean)
                .replace("jñ", "dny")
                .replace("ś", "sh")
                .replace("ṣ", "sh")
                .replace("r\u0325", "ru")
                .replace("ṛ", "ru")
                .replace("c", "ch");
        // Marathi commonly drops a final implicit 'a'. Keep it after a final
        // conjunct (e.g. mitra / maharashtra) and on single-letter tokens.
        if (clean.length() > 1 && finalConsonant(clean.charAt(clean.length() - 1))
                && clean.charAt(clean.length() - 2) != '\u094D'
                && latin.endsWith("a")) {
            latin = latin.substring(0, latin.length() - 1);
        }
        return TO_ASCII.transliterate(latin);
    }

    private static boolean finalConsonant(char c) {
        return (c >= '\u0915' && c <= '\u0939')
                || (c >= '\u0958' && c <= '\u095F');
    }
}
