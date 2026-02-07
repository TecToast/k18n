plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "de.tectoast"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    implementation("com.squareup:kotlinpoet:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}

gradlePlugin {
    plugins {
        create("k18nPlugin") {
            id = "de.tectoast.k18n"
            implementationClass = "de.tectoast.k18n.K18nPlugin"
            displayName = "K18n Code Generator"
            description = "A Gradle plugin that generates code from text files"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "tectoast"
            url = uri("https://maven.tectoast.de/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}


kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}