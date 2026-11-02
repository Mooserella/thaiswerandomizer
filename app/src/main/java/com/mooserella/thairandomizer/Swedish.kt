package com.mooserella.thairandomizer

/** A letter of the Swedish alphabet, for learners who read Thai. */
data class SvLetter(
    val id: String,
    val ch: String,
    val name: String,
    val kind: String,
    val word: String,
    val meaning: String,
) {
    /** Capital and small letter together, e.g. "Bb". */
    val glyph get() = ch + ch.lowercase()
}

object Swedish {
    val ALL = listOf(
        SvLetter("s01", "A", "a", "vowel", "apa", "monkey"),
        SvLetter("s02", "B", "be", "consonant", "bil", "car"),
        SvLetter("s03", "C", "se", "consonant", "citron", "lemon"),
        SvLetter("s04", "D", "de", "consonant", "dörr", "door"),
        SvLetter("s05", "E", "e", "vowel", "ek", "oak"),
        SvLetter("s06", "F", "eff", "consonant", "fisk", "fish"),
        SvLetter("s07", "G", "ge", "consonant", "gata", "street"),
        SvLetter("s08", "H", "hå", "consonant", "hus", "house"),
        SvLetter("s09", "I", "i", "vowel", "is", "ice"),
        SvLetter("s10", "J", "ji", "consonant", "jul", "Christmas"),
        SvLetter("s11", "K", "kå", "consonant", "katt", "cat"),
        SvLetter("s12", "L", "ell", "consonant", "lampa", "lamp"),
        SvLetter("s13", "M", "emm", "consonant", "mjölk", "milk"),
        SvLetter("s14", "N", "enn", "consonant", "nyckel", "key"),
        SvLetter("s15", "O", "o", "vowel", "orm", "snake"),
        SvLetter("s16", "P", "pe", "consonant", "pojke", "boy"),
        SvLetter("s17", "Q", "ku", "consonant", "quiz", "quiz"),
        SvLetter("s18", "R", "err", "consonant", "ros", "rose"),
        SvLetter("s19", "S", "ess", "consonant", "sol", "sun"),
        SvLetter("s20", "T", "te", "consonant", "tåg", "train"),
        SvLetter("s21", "U", "u", "vowel", "uggla", "owl"),
        SvLetter("s22", "V", "ve", "consonant", "vatten", "water"),
        SvLetter("s23", "W", "dubbel-ve", "consonant", "webb", "web"),
        SvLetter("s24", "X", "ex", "consonant", "xylofon", "xylophone"),
        SvLetter("s25", "Y", "y", "vowel", "yxa", "axe"),
        SvLetter("s26", "Z", "säta", "consonant", "zebra", "zebra"),
        SvLetter("s27", "Å", "å", "vowel", "åska", "thunder"),
        SvLetter("s28", "Ä", "ä", "vowel", "äpple", "apple"),
        SvLetter("s29", "Ö", "ö", "vowel", "öga", "eye"),
    )

    fun byId(id: String?): SvLetter? = ALL.firstOrNull { it.id == id }
}
