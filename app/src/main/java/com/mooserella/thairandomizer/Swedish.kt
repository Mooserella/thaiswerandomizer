package com.mooserella.thairandomizer

/** A letter of the Swedish alphabet, for learners who read Thai. */
data class SvLetter(
    val id: String,
    val ch: String,
    val name: String,
    val kind: String,
    val word: String,
    /** The meaning of the example word in Thai. */
    val thai: String,
) {
    /** Capital and small letter together, e.g. "Bb". */
    val glyph get() = ch + ch.lowercase()
}

object Swedish {
    val ALL = listOf(
        SvLetter("s01", "A", "a", "vokal", "apa", "ลิง"),
        SvLetter("s02", "B", "be", "konsonant", "bil", "รถยนต์"),
        SvLetter("s03", "C", "se", "konsonant", "citron", "เลมอน"),
        SvLetter("s04", "D", "de", "konsonant", "dörr", "ประตู"),
        SvLetter("s05", "E", "e", "vokal", "ek", "ต้นโอ๊ก"),
        SvLetter("s06", "F", "eff", "konsonant", "fisk", "ปลา"),
        SvLetter("s07", "G", "ge", "konsonant", "gata", "ถนน"),
        SvLetter("s08", "H", "hå", "konsonant", "hus", "บ้าน"),
        SvLetter("s09", "I", "i", "vokal", "is", "น้ำแข็ง"),
        SvLetter("s10", "J", "ji", "konsonant", "jul", "คริสต์มาส"),
        SvLetter("s11", "K", "kå", "konsonant", "katt", "แมว"),
        SvLetter("s12", "L", "ell", "konsonant", "lampa", "โคมไฟ"),
        SvLetter("s13", "M", "emm", "konsonant", "mjölk", "นม"),
        SvLetter("s14", "N", "enn", "konsonant", "nyckel", "กุญแจ"),
        SvLetter("s15", "O", "o", "vokal", "orm", "งู"),
        SvLetter("s16", "P", "pe", "konsonant", "pojke", "เด็กผู้ชาย"),
        SvLetter("s17", "Q", "ku", "konsonant", "quiz", "แบบทดสอบ"),
        SvLetter("s18", "R", "err", "konsonant", "ros", "กุหลาบ"),
        SvLetter("s19", "S", "ess", "konsonant", "sol", "ดวงอาทิตย์"),
        SvLetter("s20", "T", "te", "konsonant", "tåg", "รถไฟ"),
        SvLetter("s21", "U", "u", "vokal", "uggla", "นกฮูก"),
        SvLetter("s22", "V", "ve", "konsonant", "vatten", "น้ำ"),
        SvLetter("s23", "W", "dubbel-ve", "konsonant", "webb", "เว็บ"),
        SvLetter("s24", "X", "ex", "konsonant", "xylofon", "ไซโลโฟน"),
        SvLetter("s25", "Y", "y", "vokal", "yxa", "ขวาน"),
        SvLetter("s26", "Z", "säta", "konsonant", "zebra", "ม้าลาย"),
        SvLetter("s27", "Å", "å", "vokal", "åska", "ฟ้าร้อง"),
        SvLetter("s28", "Ä", "ä", "vokal", "äpple", "แอปเปิล"),
        SvLetter("s29", "Ö", "ö", "vokal", "öga", "ตา"),
    )

    fun byId(id: String?): SvLetter? = ALL.firstOrNull { it.id == id }
}
