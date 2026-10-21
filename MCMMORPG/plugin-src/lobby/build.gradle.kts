plugins {
  java
}

/** Plugin lobby: wyspa w pustce + portale na serwery. Zalezy tylko od paper-api. */
group = "pl.mcmmorpg"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

version = "${providers.gradleProperty("lobbyVersion").get()}-${providers.gradleProperty("lobbyGitRev").orNull ?: "nogit"}"

tasks.processResources {
  val pluginVersion = version.toString()
  inputs.property("version", pluginVersion)
  filesMatching("plugin.yml") {
    expand("version" to pluginVersion)
  }
}

tasks.jar {
  archiveBaseName = "lobby"
}

tasks.compileJava {
  options.encoding = "UTF-8"
  options.release = 21
}
