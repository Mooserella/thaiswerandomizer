package com.mooserella.thairandomizer

data class Letter(
    val id: String,
    val glyph: String,
    val word: String,
    val roman: String,
    val meaning: String,
    val sound: String,
    val cls: String,
) {
    val title get() = "$glyph  $roman ($meaning)"
    val detail get() = "$sound sound, $cls class\n$word  $meaning"
}

object Letters {
    val ALL = listOf(
    Letter("c01", "ก", "ไก่", "gor gài", "chicken", "g / k", "middle"),
    Letter("c02", "ข", "ไข่", "khǒr khài", "egg", "kh / k", "high"),
    Letter("c03", "ค", "ควาย", "khor khwaai", "buffalo", "kh / k", "low"),
    Letter("c04", "ฆ", "ระฆัง", "khor rá-khang", "bell", "kh / k", "low"),
    Letter("c05", "ง", "งู", "ngor nguu", "snake", "ng / ng", "low"),
    Letter("c06", "จ", "จาน", "jor jaan", "plate", "j / t", "middle"),
    Letter("c07", "ฉ", "ฉิ่ง", "chǒr chìng", "cymbals", "ch / -", "high"),
    Letter("c08", "ช", "ช้าง", "chor cháang", "elephant", "ch / t", "low"),
    Letter("c09", "ซ", "โซ่", "sor sôh", "chain", "s / t", "low"),
    Letter("c10", "ฌ", "เฌอ", "chor choe", "tree", "ch / -", "low"),
    Letter("c11", "ญ", "หญิง", "yor yǐng", "woman", "y / n", "low"),
    Letter("c12", "ฎ", "ชฎา", "dor chá-daa", "headdress", "d / t", "middle"),
    Letter("c13", "ฏ", "ปฏัก", "dtor bpà-dtàk", "goad", "dt / t", "middle"),
    Letter("c14", "ฐ", "ฐาน", "thǒr thǎan", "pedestal", "th / t", "high"),
    Letter("c15", "ฑ", "มณโฑ", "thor mon-toh", "Montho (queen)", "th / t", "low"),
    Letter("c16", "ฒ", "ผู้เฒ่า", "thor pûu-tâo", "elder", "th / t", "low"),
    Letter("c17", "ณ", "เณร", "nor neen", "novice monk", "n / n", "low"),
    Letter("c18", "ด", "เด็ก", "dor dèk", "child", "d / t", "middle"),
    Letter("c19", "ต", "เต่า", "dtor dtào", "turtle", "dt / t", "middle"),
    Letter("c20", "ถ", "ถุง", "thǒr thǔng", "bag", "th / t", "high"),
    Letter("c21", "ท", "ทหาร", "thor tá-hǎan", "soldier", "th / t", "low"),
    Letter("c22", "ธ", "ธง", "thor thong", "flag", "th / t", "low"),
    Letter("c23", "น", "หนู", "nor nǔu", "mouse", "n / n", "low"),
    Letter("c24", "บ", "ใบไม้", "bor bai-máai", "leaf", "b / p", "middle"),
    Letter("c25", "ป", "ปลา", "bpor bplaa", "fish", "bp / p", "middle"),
    Letter("c26", "ผ", "ผึ้ง", "phǒr phûeng", "bee", "ph / -", "high"),
    Letter("c27", "ฝ", "ฝา", "fǒr fǎa", "lid", "f / -", "high"),
    Letter("c28", "พ", "พาน", "phor phaan", "tray", "ph / p", "low"),
    Letter("c29", "ฟ", "ฟัน", "for fan", "tooth", "f / p", "low"),
    Letter("c30", "ภ", "สำเภา", "phor sǎm-phao", "sailing ship", "ph / p", "low"),
    Letter("c31", "ม", "ม้า", "mor máa", "horse", "m / m", "low"),
    Letter("c32", "ย", "ยักษ์", "yor yák", "giant", "y / y", "low"),
    Letter("c33", "ร", "เรือ", "ror rua", "boat", "r / n", "low"),
    Letter("c34", "ล", "ลิง", "lor ling", "monkey", "l / n", "low"),
    Letter("c35", "ว", "แหวน", "wor wǎen", "ring", "w / w", "low"),
    Letter("c36", "ศ", "ศาลา", "sǒr sǎa-laa", "pavilion", "s / t", "high"),
    Letter("c37", "ษ", "ฤๅษี", "sǒr rue-sǐi", "hermit", "s / t", "high"),
    Letter("c38", "ส", "เสือ", "sǒr sǔea", "tiger", "s / t", "high"),
    Letter("c39", "ห", "หีบ", "hǒr hìip", "chest", "h / -", "high"),
    Letter("c40", "ฬ", "จุฬา", "lor jù-laa", "kite", "l / n", "low"),
    Letter("c41", "อ", "อ่าง", "or àang", "basin", "vowel carrier / -", "middle"),
    Letter("c42", "ฮ", "นกฮูก", "hor nók-hûuk", "owl", "h / -", "low"),
    )

    fun byId(id: String?): Letter? = ALL.firstOrNull { it.id == id }
}
