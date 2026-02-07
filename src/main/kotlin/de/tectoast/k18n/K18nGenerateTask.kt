package de.tectoast.k18n

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*

/**
 * Task that generates Kotlin code from text files.
 */
abstract class K18nGenerateTask : DefaultTask() {

    @get:InputDirectory
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDir = outputDirectory.get().asFile
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()
        val inputFile = inputDirectory.get().asFile
        if (!inputFile.exists()) {
            inputFile.mkdirs()
        }
        val inputDir = inputFile.takeIf { it.isDirectory } ?: error("Input must be a directory")
        val configFile = inputDir.resolve("config.json")
        val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        if (!configFile.exists()) {
            configFile.writeText(json.encodeToString(Config()))
            logger.lifecycle("Created default config.json in ${configFile.absolutePath}")
            return
        }
        val config = json.decodeFromString<Config>(configFile.readText())
        if (config.languages.isEmpty()) {
            logger.warn("No languages specified in config.json, skipping code generation.")
            return
        }
        if (config.defaultLanguage !in config.languages) {
            logger.warn("Default language '${config.defaultLanguage}' is not in the list of languages, skipping code generation.")
            return
        }
        val primaryMap =
            parseToFlattenedMap(json.decodeFromString<JsonObject>(inputDir.resolve("${config.defaultLanguage}.json").readText()))
        val secondaryMaps = buildMap {
            for (language in config.languages) {
                if (language == config.defaultLanguage) continue
                val langFile = inputDir.resolve("$language.json")
                if (!langFile.exists()) {
                    error("Language file for '$language' not found at ${langFile.absolutePath}.")
                }
                val langMap = parseToFlattenedMap(json.decodeFromString<JsonObject>(langFile.readText()))
                if (!primaryMap.keys.containsAll(langMap.keys)) {
                    error("Language file for '$language' has keys that are not present in the default language file.")
                }
                put(language, langMap)
            }
        }
        val finalMap = mutableMapOf<String, MutableMap<String, TranslationEntry>>()
        for ((key, primaryValue) in primaryMap) {
            val args = parseArgumentsFromLine(config, primaryValue)
            val translations = mutableMapOf<String, String>()
            translations[config.defaultLanguage] = replaceArgumentsInLine(primaryValue, args)
            secondaryMaps.forEach { (language, langMap) ->
                val secondaryValue = langMap[key] ?: return@forEach
                val secondaryArgs = parseArgumentsFromLine(config, secondaryValue)
                if (args.entries.associate { it.key to it.value.kClassName } != secondaryArgs.entries.associate { it.key to it.value.kClassName }) {
                    error("Arguments for '$key' do not match between default and '$language'")
                }
                translations[language] = replaceArgumentsInLine(secondaryValue, secondaryArgs)
            }
            val entry = TranslationEntry(
                arguments = args.entries.associate { it.value.variableName to it.value.kClassName },
                translations = translations
            )
            val packageName = key.substringBeforeLast('.', "")
            val objName = key.substringAfterLast('.')
            finalMap.getOrPut(packageName) { mutableMapOf() }[objName] = entry
        }
        KtGeneration(logger).generate(config, outputDir, finalMap)
    }

    fun replaceArgumentsInLine(line: String, arguments: Map<String, ArgumentData>): String {
        var result = line
        for ((placeholder, argData) in arguments) {
            result = result.replace(placeholder, $$"${$${argData.formattedString}}")
        }
        return result
    }

    fun parseArgumentsFromLine(config: Config, line: String): Map<String, ArgumentData> {
        val matches = argumentRegex.findAll(line)
        val result = matches.associate { match ->
            val variableName = match.groupValues[1]
            val identifier = match.groupValues[2].ifEmpty { "String" }
            match.groupValues[0] to config.build(variableName, identifier)
        }
        if (result.size != matches.count()) {
            error("Failed to parse arguments from line: '$line'. Parsed ${result.size} arguments but found ${matches.count()} matches. This may lead to incorrect code generation.")
        }
        return result
    }

    fun parseToFlattenedMap(jsonObj: JsonObject, parentKey: String = ""): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for ((keyInObj, value) in jsonObj) {
            val key = keyInObj.replace(".", "").trim()
            val fullKey = if (parentKey.isEmpty()) key else "$parentKey.$key"
            when (value) {
                is JsonObject -> result.putAll(parseToFlattenedMap(value, fullKey))
                else -> result[fullKey] = value.jsonPrimitive.content
            }
        }
        return result
    }

    companion object {
        private val argumentRegex = "\\{(\\w+):?(\\w+)?}".toRegex()
    }
}

data class TranslationEntry(
    val arguments: Map<String, String>,
    val translations: Map<String, String>
)