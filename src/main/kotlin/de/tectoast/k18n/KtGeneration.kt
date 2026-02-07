package de.tectoast.k18n

import com.squareup.kotlinpoet.*
import org.gradle.api.logging.Logger
import java.io.File

class KtGeneration(val logger: Logger) {
    fun generate(config: Config, outputDir: File, finalMap: Map<String, Map<String, TranslationEntry>>) {
        val translatableInterface = ClassName("de.tectoast.k18n.generated", "K18nMessage")
        generateMetaFile(config, outputDir)
        for ((packageName, entries) in finalMap) {
            val fileSpecBuilder = FileSpec.builder("${if(config.withBasePackage) "de.tectoast.k18n.generated." else ""}$packageName", "data")
            for ((objName, entry) in entries) {
                val finalName = "K18n_${objName}"
                val funSpec = FunSpec.builder("translateTo")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("language", ClassName("de.tectoast.k18n.generated", "K18nLanguage"))
                    .returns(String::class)
                    .addCode(
                        """
                                return when(language) {
                                    ${entry.translations.entries.joinToString("\n") {
                                        val langs = if(it.key == config.defaultLanguage) (config.languages - entry.translations.keys) + config.defaultLanguage else listOf(it.key)
                                        "${langs.joinToString { l -> "K18nLanguage.${l.uppercase()}"}} -> \"${it.value}\"" 
                                    }}
                                }
                                """.trimIndent()
                    )
                    .build()
                if (entry.arguments.isEmpty()) {
                    fileSpecBuilder.addType(
                        TypeSpec.objectBuilder(finalName)
                            .addSuperinterface(translatableInterface)
                            .addFunction(funSpec).build()
                    )
                } else {
                    fileSpecBuilder.addType(
                        TypeSpec.classBuilder(finalName).addModifiers(KModifier.DATA)
                            .addSuperinterface(translatableInterface)
                            .addConstructorParams(entry.arguments)
                            .addFunction(funSpec)
                            .build()
                    )
                }
            }
            fileSpecBuilder.build().writeTo(outputDir)
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
            .build()
            .writeTo(outputDir)
    }
}