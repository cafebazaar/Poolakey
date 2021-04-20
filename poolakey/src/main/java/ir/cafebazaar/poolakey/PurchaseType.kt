package ir.cafebazaar.poolakey

enum class PurchaseType(val type: String) {
    IN_APP("inapp"),
    SUBSCRIPTION("subs");

    companion object {
        internal fun fromValue(type: String): PurchaseType {
            return values().firstOrNull { it.type == type }
                ?: throw IllegalArgumentException("invalid purchase type")
        }
    }
}
