package no.nav.pgi.popp.lagreinntekt


internal fun Map<String, String>.getVal(key: String) =
        this[key] ?: throw MissingEnvironmentVariable("""$key, is not found in environment""")

internal fun Map<String, String>.getVal(key: String, defaultValue: String) =
        this.getOrDefault(key, defaultValue)

internal class MissingEnvironmentVariable(message: String) : RuntimeException(message)