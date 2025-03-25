package io.github.mdsadiqueinam.qamus.util


val arabicToUrduMap = mapOf(
    'ا' to 'ا', // Arabic Alef to Urdu Alef
    'ب' to 'ب', // Arabic Beh to Urdu Beh
    'ت' to 'ت', // Arabic Teh to Urdu Teh
    'ث' to 'ث', // Arabic Theh to Urdu Theh
    'ج' to 'ج', // Arabic Jeem to Urdu Jeem
    'ح' to 'ح', // Arabic Hah to Urdu Hah
    'خ' to 'خ', // Arabic Khah to Urdu Khah
    'د' to 'د', // Arabic Dal to Urdu Dal
    'ذ' to 'ذ', // Arabic Thal to Urdu Thal
    'ر' to 'ر', // Arabic Reh to Urdu Reh
    'ز' to 'ز', // Arabic Zain to Urdu Zain
    'س' to 'س', // Arabic Seen to Urdu Seen
    'ش' to 'ش', // Arabic Sheen to Urdu Sheen
    'ص' to 'ص', // Arabic Sad to Urdu Sad
    'ض' to 'ض', // Arabic Dad to Urdu Dad
    'ط' to 'ط', // Arabic Tah to Urdu Tah
    'ظ' to 'ظ', // Arabic Zah to Urdu Zah
    'ع' to 'ع', // Arabic Ain to Urdu Ain
    'غ' to 'غ', // Arabic Ghain to Urdu Ghain
    'ف' to 'ف', // Arabic Feh to Urdu Feh
    'ق' to 'ق', // Arabic Qaf to Urdu Qaf
    'ك' to 'ک', // Arabic Kaf to Urdu Kaf
    'ل' to 'ل', // Arabic Lam to Urdu Lam
    'م' to 'م', // Arabic Meem to Urdu Meem
    'ن' to 'ن', // Arabic Noon to Urdu Noon
    'ه' to 'ہ', // Arabic Heh to Urdu Heh
    'و' to 'و', // Arabic Waw to Urdu Waw
    'ي' to 'ی', // Arabic Yeh to Urdu Yeh
    'ء' to 'ء', // Arabic Hamza to Urdu Hamza
    'ئ' to 'ئ', // Arabic Yeh with Hamza Above to Urdu Yeh with Hamza Above
    'ؤ' to 'ؤ', // Arabic Waw with Hamza Above to Urdu Waw with Hamza Above
    'إ' to 'ا', // Arabic Alef with Hamza Below to Urdu Alef
    'أ' to 'ا', // Arabic Alef with Hamza Above to Urdu Alef
    'آ' to 'آ', // Arabic Alef with Madda Above to Urdu Alef with Madda Above
    'ة' to 'ۃ', // Arabic Teh Marbuta to Urdu Teh Marbuta
    'ى' to 'ی'  // Arabic Alef Maksura to Urdu Yeh
)

fun mapArabicToUrdu(input: String): String {
    val output = StringBuilder()
    for (char in input) {
        output.append(arabicToUrduMap[char] ?: char)
    }
    return output.toString()
}