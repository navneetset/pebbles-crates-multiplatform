import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    java
    kotlin("jvm") version "1.8.22"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.2-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

architectury {
    minecraft = rootProject.property("minecraft_version").toString()
}

subprojects {
    apply(plugin = "dev.architectury.loom")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")


    dependencies {
        "minecraft"("com.mojang:minecraft:${project.property("minecraft_version")}")
         "mappings"("net.fabricmc:yarn:1.20.1+build.10:v2")
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    base.archivesName.set(rootProject.property("archives_base_name").toString())
    version = rootProject.property("mod_version").toString()
    group = rootProject.property("maven_group").toString()

    repositories {

    }

    dependencies {
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib")

        implementation("net.kyori:adventure-api:${property("minimessage_version")}")
        implementation("net.kyori:adventure-text-minimessage:${property("minimessage_version")}")
        implementation("net.kyori:adventure-text-serializer-gson:${property("minimessage_version")}")
        implementation("net.kyori:adventure-text-serializer-legacy:${property("minimessage_version")}")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    kotlin.target.compilations.all {
        kotlinOptions.jvmTarget = "17"
    }

    java {
        withSourcesJar()
    }
}