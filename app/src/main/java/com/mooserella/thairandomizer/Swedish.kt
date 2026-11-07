package com.mooserella.thairandomizer

/** A letter of the Swedish alphabet, for learners who read Thai. */
data class SvLetter(
    val id: String,
    val ch: String,
    val name: String,
    val kind: String,
    val word: String,
) {
    /** Capital and small letter together, e.g. "Bb". */
    val glyph get() = ch + ch.lowercase()
}

object Swedish {
    val ALL = listOf(
        SvLetter("s01", "A", "a", "vokal", "apa"),
        SvLetter("s02", "B", "be", "konsonant", "bil"),
        SvLetter("s03", "C", "se", "konsonant", "citron"),
        SvLetter("s04", "D", "de", "konsonant", "dörr"),
        SvLetter("s05", "E", "e", "vokal", "ek"),
        SvLetter("s06", "F", "eff", "konsonant", "fisk"),
        SvLetter("s07", "G", "ge", "konsonant", "gata"),
        SvLetter("s08", "H", "hå", "konsonant", "hus"),
        SvLetter("s09", "I", "i", "vokal", "is"),
        SvLetter("s10", "J", "ji", "konsonant", "jul"),
        SvLetter("s11", "K", "kå", "konsonant", "katt"),
        SvLetter("s12", "L", "ell", "konsonant", "lampa"),
        SvLetter("s13", "M", "emm", "konsonant", "mjölk"),
        SvLetter("s14", "N", "enn", "konsonant", "nyckel"),
        SvLetter("s15", "O", "o", "vokal", "orm"),
        SvLetter("s16", "P", "pe", "konsonant", "pojke"),
        SvLetter("s17", "Q", "ku", "konsonant", "quiz"),
        SvLetter("s18", "R", "err", "konsonant", "ros"),
        SvLetter("s19", "S", "ess", "konsonant", "sol"),
        SvLetter("s20", "T", "te", "konsonant", "tåg"),
        SvLetter("s21", "U", "u", "vokal", "uggla"),
        SvLetter("s22", "V", "ve", "konsonant", "vatten"),
        SvLetter("s23", "W", "dubbel-ve", "konsonant", "webb"),
        SvLetter("s24", "X", "ex", "konsonant", "xylofon"),
        SvLetter("s25", "Y", "y", "vokal", "yxa"),
        SvLetter("s26", "Z", "säta", "konsonant", "zebra"),
        SvLetter("s27", "Å", "å", "vokal", "åska"),
        SvLetter("s28", "Ä", "ä", "vokal", "äpple"),
        SvLetter("s29", "Ö", "ö", "vokal", "öga"),
    )

    fun byId(id: String?): SvLetter? = ALL.firstOrNull { it.id == id }
}
