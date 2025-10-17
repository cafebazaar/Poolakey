package ir.cafebazaar.poolakey

/**
 * An inline extension that behaves like [takeIf], but also executes [andIfNot]
 * when the predicate condition fails.
 *
 * Example:
 * ```
 * val number = 5.takeIfOrElse(
 *     thisIsTrue = { it > 10 },
 *     andIfNot = { println("Number is too small!") }
 * )
 * // prints: Number is too small!
 * // returns: null
 * ```
 *
 * @param thisIsTrue Predicate to test on the receiver [T].
 * @param andIfNot Lambda to execute if [thisIsTrue] evaluates to false.
 * @return The receiver [T] if the predicate is true, otherwise `null`.
 */
internal inline fun <T> T.takeIfOrElse(
    thisIsTrue: (T) -> Boolean,
    andIfNot: () -> Unit
): T? = if (thisIsTrue(this)) this else {
    andIfNot()
    null
}

/**
 * Simple demo console app to show how [takeIfOrElse] works.
 * It prints results in a user-friendly, colored format.
 */
fun main() {
    // Terminal color codes for style
    val reset = "\u001B[0m"
    val blue = "\u001B[36m"
    val green = "\u001B[32m"
    val yellow = "\u001B[33m"
    val red = "\u001B[31m"
    val bold = "\u001B[1m"

    // Header UI
    println("$blue$bold==============================")
    println("   🧠 Poolakey takeIfOrElse Demo")
    println("==============================$reset\n")

    val input = "Hello"

    println("${yellow}Input Value:$reset \"$input\"")
    println("${yellow}Condition:$reset length > 10\n")

    val result = input.takeIfOrElse(
        thisIsTrue = { it.length > 10 },
        andIfNot = {
            println("${red}❌ Condition failed: String too short!$reset\n")
        }
    )

    if (result != null) {
        println("${green}✅ Success!$reset Value passed the test.")
        println("${yellow}Result:$reset \"$result\"")
    } else {
        println("${blue}ℹ️  Returned:$reset null (predicate not satisfied)")
    }

    println("\n$blue$bold==============================")
    println("         End of Demo")
    println("==============================$reset")
}
==============================
   🧠 Poolakey takeIfOrElse Demo
==============================

Input Value: "Hello"
Condition: length > 10

❌ Condition failed: String too short!

ℹ️  Returned: null (predicate not satisfied)

==============================
         End of Demo
==============================
 
