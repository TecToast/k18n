package de.tectoast.k18n

import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.jvm.java

class K18nPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("k18n", K18nExtension::class.java, project)

        project.tasks.register("generateK18nCode", K18nGenerateTask::class.java) { task ->
            task.group = "k18n"
            task.description = "Generates translation code from json files"

            task.inputDirectory.convention(extension.inputDirectory)
            task.outputDirectory.convention(extension.outputDirectory)
        }
    }
}
