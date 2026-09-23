package com.ishaan.essentialvoice.whisper;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MarathiRomanizerTest {
    @Test public void keepsEnglishAndFormattingExactly() {
        String text = "Please send Report_v2 to Ash@example.com at 5:30 PM.\nPrice: $10 \\ notes 😊 café";
        assertEquals(text, MarathiRomanizer.romanize(text));
    }
    @Test public void romanizesMarathiWithoutTranslatingMeaning() {
        assertEquals("mala udya officela jayche aahe.",
                MarathiRomanizer.romanize("मला उद्या ऑफिसला जायचे आहे."));
    }
    @Test public void keepsEnglishInsideMixedSentence() {
        assertEquals("mi udya meeting la yenar aahe.",
                MarathiRomanizer.romanize("मी उद्या meeting ला येणार आहे."));
    }
    @Test public void keepsNumbersPunctuationAndEmoji() {
        assertEquals("123, 5:30 PM 😊", MarathiRomanizer.romanize("१२३, 5:30 PM 😊"));
    }
    @Test public void handlesJoinersAndConjuncts() {
        assertEquals("dnyan mitra maharashtra",
                MarathiRomanizer.romanize("ज्ञा\u200Dन मित्र महाराष्ट्र"));
    }
    @Test public void usesReadableChShAndFinalConsonants() {
        assertEquals("chaha chhan shala", MarathiRomanizer.romanize("चहा छान शाळा"));
    }
    @Test public void separatesDandaFromWordOverrides() {
        assertEquals("aahe. aahet.", MarathiRomanizer.romanize("आहे। आहेत॥"));
    }
    @Test public void leavesOtherScriptsAndEmptyInputAlone() {
        assertEquals("", MarathiRomanizer.romanize(""));
        assertEquals("مرحبا 日本語", MarathiRomanizer.romanize("مرحبا 日本語"));
    }
}
