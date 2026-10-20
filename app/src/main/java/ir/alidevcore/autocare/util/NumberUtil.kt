package ir.alidevcore.autocare.util

import java.text.NumberFormat
import java.util.Locale

object NumberUtil {
    /**
     * Formats a number with thousand separators in Persian
     * e.g., 1500000 -> "۱٬۵۰۰٬۰۰۰"
     */
    fun formatCurrency(amount: Long): String {
        val formatted = NumberFormat.getNumberInstance(Locale("fa", "IR")).format(amount)
        return formatted
    }

    fun formatKm(km: Long): String {
        val formatted = NumberFormat.getNumberInstance(Locale("fa", "IR")).format(km)
        return "$formatted کیلومتر"
    }

    /**
     * Parse Persian/Arabic numerals to Int
     */
    fun parsePersianNumber(str: String): Long? {
        val normalized = str
            .replace('۰', '0').replace('۱', '1').replace('۲', '2')
            .replace('۳', '3').replace('۴', '4').replace('۵', '5')
            .replace('۶', '6').replace('۷', '7').replace('۸', '8')
            .replace('۹', '9')
            .replace(",", "").replace("٬", "").replace("،", "")
            .trim()
        return normalized.toLongOrNull()
    }

    fun formatNumber(n: Long): String {
        return NumberFormat.getNumberInstance(Locale("fa", "IR")).format(n)
    }
}
