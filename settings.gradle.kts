import java.util.Locale

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

if (!file(".git").exists()) {
    // ChronaX start - project setup
    val errorText = """

        =====================[ ERROR ]=====================
         The ChronaX project directory is not a properly cloned Git repository.

         In order to build ChronaX from source you must clone
         the ChronaX repository using Git, not download a code
         zip from GitHub.

         See https://github.com/PaperMC/Paper/blob/main/CONTRIBUTING.md
         for further information on building and modifying Paper forks.
        ===================================================
    """.trimIndent()
    // ChronaX end - project setup
    error(errorText)
}

rootProject.name = "chronax"

for (name in listOf("chronax-api", "chronax-server")) {
    val projName = name.lowercase(Locale.ENGLISH)
    include(projName)
}
