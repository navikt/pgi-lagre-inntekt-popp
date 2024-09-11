package no.nav.pgi.popp.lagreinntekt.util

fun Map<String, String>.getVal(key: String) =
    this[key] ?: throw MissingEnvironmentVariable("""$key, is not found in environment""")

fun Map<String, String>.getVal(key: String, defaultValue: String) = this.getOrDefault(key, defaultValue)

fun Map<String, String>.verifyEnvironmentVariables(keys: List<String>) {
    val missingKeys = keys.filter { this[it] == null }
    if (missingKeys.isNotEmpty()) throw MissingEnvironmentVariables(
        missingKeys.joinToString(", ", "Missing env keys: ")
    )
}

class MissingEnvironmentVariable(message: String) : RuntimeException(message)
class MissingEnvironmentVariables(message: String) : RuntimeException(message)