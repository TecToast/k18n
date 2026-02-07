package de.tectoast.k18n

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Config(
    val languages: List<String> = listOf("de", "en"),
    val defaultLanguage: String = "de",
    val withBasePackage: Boolean = true,
    val nestedSuffix: String? = "_nested",
    val customClasses: Map<String, CustomArgumentType> = mapOf("Mention" to CustomArgumentType("Long", "<@{var}>"))
) {
    @Transient
    val nativeClasses = setOf(
        "String",
        "Int",
        "Long",
        "Float",
        "Double",
        "Boolean",
        "Char"
    )

    fun build(variableName: String, identifier: String): ArgumentData {
        customClasses[identifier]?.let { customClass ->
            return ArgumentData(
                kClassName = customClass.kClassName,
                formattedString = "\"${customClass.formatStringTemplate.replace("{var}", "$$variableName")}\"",
                variableName = variableName
            )
        }
        if (identifier in nativeClasses) {
            return ArgumentData(
                kClassName = identifier,
                formattedString = variableName,
                variableName = variableName
            )
        }
        error("No transformation found for type $identifier (used in variable ${variableName})")
    }
}

@Serializable
data class CustomArgumentType(
    val kClassName: String,
    val formatStringTemplate: String
)

@Serializable
data class ArgumentData(
    val kClassName: String,
    val variableName: String,
    val formattedString: String
)