// Reference-only: ten workspace nie ma zainstalowanego Gradle. Realny build
// idzie przez tools/BuildCompanionBridge.java (patrz README.md), tak samo
// jak w plugin-src/character-stage. Ten plik dokumentuje deklarowane
// zaleznosci dla ewentualnego przyszlego przejscia na Gradle.
plugins {
  java
}

group = "pl.mcmmorpg"
version = "0.1.0-SNAPSHOT"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  compileOnly("com.google.code.gson:gson:2.13.2")
}

tasks.processResources {
  filesMatching("plugin.yml") {
    expand(
      mapOf(
        "version" to project.version
      )
    )
  }
}
