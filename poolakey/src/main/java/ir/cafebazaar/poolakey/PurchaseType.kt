package ir.cafebazaar.poolakey

/**
 * Represents the type of a purchase in the billing system.
 *
 * There are two main types:
 * - [IN_APP] → One-time purchases
 * - [SUBSCRIPTION] → Recurring payments
 */
internal enum class PurchaseType(val type: String) {
    IN_APP("inapp"),
    SUBSCRIPTION("subs");

    companion object {
        /**
         * Returns the [PurchaseType] matching the given [type] string, or `null` if invalid.
         *
         * Example:
         * ```
         * val result = PurchaseType.fromType("subs")  // SUBSCRIPTION
         * ```
         */
        fun fromType(type: String?): PurchaseType? {
            if (type.isNullOrBlank()) return null
            return values().firstOrNull { it.type.equals(type.trim(), ignoreCase = true) }
        }

        /**
         * Checks if the given [type] string is a valid purchase type.
         *
         * Example:
         * ```
         * val isValid = PurchaseType.isValidType("inapp")  // true
         * ```
         */
        fun isValidType(type: String?): Boolean = fromType(type) != null

        /**
         * Returns all available purchase type strings.
         *
         * Example:
         * ```
         * val all = PurchaseType.listAllTypes()  // ["inapp", "subs"]
         * ```
         */
        fun listAllTypes(): List<String> = values().map { it.type }
    }

    /**
     * Returns a friendly display name for the type (for UI or logs).
     *
     * Example:
     * ```
     * PurchaseType.IN_APP.displayName()  // "In-App Purchase"
     * ```
     */
    fun displayName(): String = when (this) {
        IN_APP -> "In-App Purchase"
        SUBSCRIPTION -> "Subscription"
    }

    /**
     * Returns true if this is a subscription type.
     */
    fun isSubscription(): Boolean = this == SUBSCRIPTION

    /**
     * Returns true if this is an in-app (one-time) purchase type.
     */
    fun isInApp(): Boolean = this == IN_APP
}

// ---------------------------------------------------------------------
// Interactive, polished console UI demo (single-file)
// ---------------------------------------------------------------------
fun main() {
    // ANSI colors for nicer terminal UI (works in most terminals)
    val RESET = "\u001B[0m"
    val BOLD = "\u001B[1m"
    val CYAN = "\u001B[36m"
    val GREEN = "\u001B[32m"
    val YELLOW = "\u001B[33m"
    val RED = "\u001B[31m"
    val MAGENTA = "\u001B[35m"

    fun header() {
        println("${CYAN + BOLD}==============================================")
        println("   🛒  Poolakey PurchaseType Utility Demo")
        println("==============================================$RESET")
    }

    fun footer() {
        println()
        println("${CYAN}Thank you for trying the demo — exit with 'q' or Ctrl+C.$RESET")
    }

    header()

    while (true) {
        println()
        println("${YELLOW}Available actions:${RESET}")
        println("  1) List all purchase types")
        println("  2) Convert string -> PurchaseType")
        println("  3) Show display names")
        println("  4) Type checks (isInApp / isSubscription)")
        println("  q) Quit")
        print("\nSelect an action (1-4 or q): ")

        val choice = readLine()?.trim()?.lowercase()
        when (choice) {
            "1" -> {
                val types = PurchaseType.listAllTypes()
                println()
                println("${MAGENTA}🔹 All purchase types:${RESET} ${types.joinToString(", ")}")
            }
            "2" -> {
                print("Enter a type string (e.g. \"inapp\" or \"subs\"): ")
                val input = readLine()?.trim()
                val p = PurchaseType.fromType(input)
                println()
                if (p != null) {
                    println("${GREEN}✅ Parsed successfully:${RESET} ${p.name} (${p.type}) — ${p.displayName()}")
                } else {
                    println("${RED}❌ Invalid purchase type:${RESET} \"${input ?: ""}\"")
                    println("    Valid values: ${PurchaseType.listAllTypes().joinToString(", ")}")
                }
            }
            "3" -> {
                println()
                println("${MAGENTA}🎨 Display names:${RESET}")
                PurchaseType.values().forEach { t ->
                    println("  • ${t.type} → ${t.displayName()}")
                }
            }
            "4" -> {
                println()
                println("Check a type:")
                print("Enter one of (${PurchaseType.listAllTypes().joinToString(", ")}): ")
                val input = readLine()?.trim()
                val p = PurchaseType.fromType(input)
                if (p == null) {
                    println("${RED}❌ Unknown type: ${input ?: "\"\""}${RESET}")
                } else {
                    val checks = buildString {
                        append("${GREEN}✔ ${p.name}${RESET} checks:\n")
                        append("   - isInApp(): ${if (p.isInApp()) "${BOLD}true" else "false"}\n")
                        append("   - isSubscription(): ${if (p.isSubscription()) "${BOLD}true" else "false"}")
                    }
                    println(checks)
                }
            }
            "q", "quit", "exit" -> {
                println()
                println("${CYAN}Exiting demo...$RESET")
                footer()
                return
            }
            else -> {
                println()
                println("${RED}⚠️  Invalid selection. Please choose 1-4 or q.$RESET")
            }
        }
    }
}
 
