package de.tectoast.k18n

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import javax.inject.Inject

open class K18nExtension @Inject constructor(project: Project) {

    val inputDirectory: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.projectDirectory.dir("k18n"))

    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()
        .convention(project.layout.buildDirectory.dir("generated/k18n"))
}
