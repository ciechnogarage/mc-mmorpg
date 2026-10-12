plugins {
  java
}

/**
 * Build pluginu MenuKey. Wzorowany na buildzie kreatora postaci: zaleznosci sa JAWNE,
 * a jar pluginu, ktorego nie ma w zadnym publicznym repo (ProtocolLib), jest PRZYPIETY
 * po sciezce — zeby build nie zalezal od tego, co akurat lezy w katalogu serwera.
 */
group = "pl.mcmmorpg"

java {
  toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApi = "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"

val serverPluginsDir = file(providers.gradleProperty("mkServerPlugins").get())
val protocolLib = serverPluginsDir.resolve("ProtocolLib.jar")
if (!protocolLib.isFile) {
  throw GradleException("Brak ${protocolLib.absolutePath} — ustaw -PmkServerPlugins=<katalog z pluginami>.")
}

dependencies {
  compileOnly(paperApi)
  compileOnly(files(protocolLib))
}

version = "${providers.gradleProperty("mkVersion").get()}-${providers.gradleProperty("mkGitRev").orNull ?: "nogit"}"

tasks.processResources {
  val pluginVersion = version.toString()
  inputs.property("version", pluginVersion)
  filesMatching("plugin.yml") { expand("version" to pluginVersion) }
}

tasks.jar { archiveBaseName = "menu-key" }

tasks.compileJava {
  options.encoding = "UTF-8"
  options.release = 21
}
