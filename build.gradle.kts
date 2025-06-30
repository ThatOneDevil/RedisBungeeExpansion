plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.thatonedevil"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation("org.spigotmc:spigot-api:1.19-R0.1-SNAPSHOT")
    implementation("me.clip:placeholderapi:2.11.6")
}

tasks.build {
    dependsOn("shadowJar")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
