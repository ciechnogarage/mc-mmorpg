plugins {
  java
}

/**
 * Build ekranu logowania dla serwera `limbo`.
 *
 * Jedyna zaleznosc to paper-api: plugin nie rozmawia z LibreLogin przez jego API, tylko
 * kaze KLIENTOWI wykonac komende `/login`. Powod jest protokolarny, nie estetyczny —
 * patrz javadoc LimboLoginPlugin.
 */
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

version = "${providers.gradleProperty("llVersion").get()}-${gitRevision()}"

fun gitRevision(): String {
  providers.gradleProperty("llGitRev").orNull?.takeIf { it.isNotBlank() }?.let { return it }
  return "nogit"
}

tasks.processResources {
  val pluginVersion = version.toString()
  inputs.property("version", pluginVersion)
  filesMatching("plugin.yml") {
    expand("version" to pluginVersion)
  }
}

tasks.jar {
  archiveBaseName = "limbo-login"
}

tasks.compileJava {
  options.encoding = "UTF-8"
  options.release = 21
}
