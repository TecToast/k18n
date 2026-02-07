package de.tectoast.k18n

import com.squareup.kotlinpoet.*
import org.gradle.api.logging.Logger
import java.io.File

class KtGeneration(val logger: Logger) {
    companion object {
        val translatableInterface = ClassName("de.tectoast.k18n.generated", "K18nMessage")
    }

    fun generate(config: Config, outputDir: File, finalMap: Map<String, Map<String, TranslationEntry>>) {
        generateMetaFile(config, outputDir)
        val allFileBuilders = mutableMapOf<String, FileSpec.Builder>()
        for ((packageName, entries) in finalMap.entries.sortedBy { it.key }) {
            if (config.nestedSuffix != null && config.nestedSuffix in packageName) {
                val parentPackage = packageName.substringBeforeLast(".")
                val parentBuilder = allFileBuilders.getOrPut(parentPackage) {
                    generateFileSpec(config, parentPackage)
                }
                parentBuilder.addType(TypeSpec.objectBuilder("K18n_" + packageName.substringAfterLast(".").removeSuffix(config.nestedSuffix)).apply {
                    writeEntries(entries, config, this, withPrefix = false)
                }.build())
            } else {
                val fileSpecBuilder = generateFileSpec(config, packageName)
                writeEntries(entries, config, fileSpecBuilder, withPrefix = true)
                allFileBuilders[packageName] = fileSpecBuilder
            }
        }
        allFileBuilders.values.forEach { it.build().writeTo(outputDir) }

    }

    private fun generateFileSpec(
        config: Config,
        packageName: String
    ): FileSpec.Builder = FileSpec.builder(
        "${if (config.withBasePackage) "de.tectoast.k18n.generated." else ""}$packageName",
        "data"
    )

    private fun writeEntries(
        entries: Map<String, TranslationEntry>,
        config: Config,
        builder: TypeSpecHolder.Builder<*>,
        withPrefix: Boolean
    ) {
        for ((objName, entry) in entries) {
            val finalName = if(withPrefix) "K18n_${objName}" else objName
            val funSpec = FunSpec.builder("translateTo")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("language", ClassName("de.tectoast.k18n.generated", "K18nLanguage"))
                .returns(String::class)
                .addCode(
                    """
                                    return when(language) {
                                        ${
                        entry.translations.entries.joinToString("\n") {
                            val langs =
                                if (it.key == config.defaultLanguage) (config.languages - entry.translations.keys) + config.defaultLanguage else listOf(
                                    it.key
                                )
                            "${langs.joinToString { l -> "K18nLanguage.${l.uppercase()}" }} -> \"${it.value}\""
                        }
                    }
                                    }
                                    """.trimIndent()
                )
                .build()
            if (entry.arguments.isEmpty()) {
                builder.addType(
                    TypeSpec.objectBuilder(finalName)
                        .addSuperinterface(translatableInterface)
                        .addFunction(funSpec).build()
                )
            } else {
                builder.addType(
                    TypeSpec.classBuilder(finalName).addModifiers(KModifier.DATA)
                        .addSuperinterface(translatableInterface)
                        .addConstructorParams(entry.arguments)
                        .addFunction(funSpec)
                        .build()
                )
            }
        }
    }

    fun TypeSpec.Builder.addConstructorParams(arguments: Map<String, String>): TypeSpec.Builder {
        primaryConstructor(FunSpec.constructorBuilder().apply {
            for ((name, type) in arguments) {
                logger.info("Adding parameter $name of type $type")
                addParameter(name, type.identifierToClassName())
            }
        }.build())
        for ((name, type) in arguments) {
            addProperty(
                PropertySpec.builder(name, type.identifierToClassName())
                    .initializer(name)
                    .build()
            )
        }
        return this
    }

    fun String.identifierToClassName() =
        if ("." in this) ClassName(this.substringBeforeLast('.'), this.substringAfterLast('.')) else ClassName(
            "kotlin",
            this
        )

    fun generateMetaFile(config: Config, outputDir: File) {
        FileSpec.builder("de.tectoast.k18n.generated", "meta")
            .addType(TypeSpec.enumBuilder("K18nLanguage").apply {
                for (language in config.languages) {
                    addEnumConstant(language.uppercase())
                }
            }.build())
            .addType(
                TypeSpec.interfaceBuilder("K18nMessage")
                    .addFunction(
                        FunSpec.builder("translateTo")
                            .addParameter("language", ClassName("de.tectoast.k18n.generated", "K18nLanguage"))
                            .returns(String::class)
                            .addModifiers(KModifier.ABSTRACT)
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder(
                    "K18N_DEFAULT_LANGUAGE",
                    ClassName("de.tectoast.k18n.generated", "K18nLanguage")
                ).initializer("K18nLanguage.${config.defaultLanguage.uppercase()}").build()
            )
            .build()
            .writeTo(outputDir)
    }
}