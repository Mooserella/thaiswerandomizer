package com.mooserella.thairandomizer

/** Thai vowels, shown on the dummy consonant อ. Same shape as the consonants. */
object Vowels {
    val ALL = listOf(
        Letter("v01", "อะ", "จะ", "sara a", "ska (framtid)", "a", "kort"),
        Letter("v02", "อา", "หมา", "sara aa", "hund", "aa", "lång"),
        Letter("v03", "อิ", "ดิน", "sara i", "jord", "i", "kort"),
        Letter("v04", "อี", "สี", "sara ii", "färg", "ii", "lång"),
        Letter("v05", "อึ", "ตึก", "sara ue", "byggnad", "ue", "kort"),
        Letter("v06", "อื", "มือ", "sara uue", "hand", "uue", "lång"),
        Letter("v07", "อุ", "ลุง", "sara u", "morbror/farbror", "u", "kort"),
        Letter("v08", "อู", "ปู", "sara uu", "krabba", "uu", "lång"),
        Letter("v09", "เอะ", "เตะ", "sara e", "sparka", "e", "kort"),
        Letter("v10", "เอ", "เมฆ", "sara ee", "moln", "ee", "lång"),
        Letter("v11", "แอะ", "แกะ", "sara ae", "får", "ae", "kort"),
        Letter("v12", "แอ", "แม่", "sara aae", "mamma", "aae", "lång"),
        Letter("v13", "โอะ", "โต๊ะ", "sara o", "bord", "o", "kort"),
        Letter("v14", "โอ", "โลก", "sara oo", "jorden", "oo", "lång"),
        Letter("v15", "เอาะ", "เกาะ", "sara aw", "ö", "aw", "kort"),
        Letter("v16", "ออ", "ช้อน", "sara aaw", "sked", "aaw", "lång"),
        Letter("v17", "เออะ", "เลอะ", "sara oe", "smutsig", "oe", "kort"),
        Letter("v18", "เออ", "เธอ", "sara ooe", "du/hon", "ooe", "lång"),
        Letter("v19", "เอียะ", "เกี๊ยะ", "sara ia", "träsko", "ia", "kort"),
        Letter("v20", "เอีย", "เมีย", "sara iia", "hustru", "iia", "lång"),
        Letter("v21", "เอือ", "เรือ", "sara uea", "båt", "uea", "lång"),
        Letter("v22", "อัว", "ตัว", "sara ua", "kropp", "ua", "lång"),
        Letter("v23", "อำ", "น้ำ", "sara am", "vatten", "am", "kort"),
        Letter("v24", "ไอ", "ไม้", "sara ai (mai malai)", "trä", "ai", "diftong"),
        Letter("v25", "ใอ", "ใจ", "sara ai (mai muan)", "hjärta", "ai", "diftong"),
        Letter("v26", "เอา", "เขา", "sara ao", "berg", "ao", "diftong"),
        Letter("v27", "ฤ", "ฤดู", "rue", "årstid", "rue", "kort"),
        Letter("v28", "ฤๅ", "ฤๅษี", "ruue", "eremit", "ruue", "lång"),
    )
}
