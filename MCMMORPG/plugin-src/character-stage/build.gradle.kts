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
  compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
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
