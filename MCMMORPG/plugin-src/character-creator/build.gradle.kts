plugins {
  java
}

/**
 * Build kreatora postaci.
 *
 * Zastepuje `tools/BuildCharacterCreator.java`, ktory skladal classpath ze WSZYSTKICH jarow
 * dzialajacego serwera (`libraries/` + `plugins/`, rekurencyjnie) i rozstrzygal konflikty
 * wersji wlasnym resolverem na wyrazeniach regularnych. Skutek: build zalezal od tego, co
 * akurat lezy w katalogu serwera, i nie dalo sie go powtorzyc z czystego klona.
 *
 * Tutaj zaleznosci sa dwojakiego rodzaju i obie sa JAWNE:
 *  - paper-api — z repozytorium PaperMC, po wersji,
 *  - API pluginow, ktorych nie ma w zadnym publicznym repo — z wymienionej nizej,
 *    przypietej listy plikow.
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

val paperApi = "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"

/**
 * Jary pluginow sa PRZYPIETE po nazwie pliku razem z wersja. Podbicie ktoregokolwiek z nich
 * na serwerze wymaga swiadomej zmiany tutaj — zamiast cichego skompilowania sie przeciwko
 * czemus innemu, niz stoi na produkcji.
 */
val serverPluginsDir = file(providers.gradleProperty("ccServerPlugins").get())
val pinnedPluginJars = listOf(
  // Uzywane wprost w kodzie:
  "ModelEngine-4.0.9.jar",
  "MMOProfiles-1.2-20260325.193503-28.jar",
  "LibsDisguises-11.0.15-Free.jar",
  // Potrzebne TYLKO do kompilacji, przez sygnatury powyzszych:
  //  - MythicLib: MMOProfiles.plugin jest typu io.lumine.mythic.lib.module.MMOPlugin,
  //  - packetevents: konstruktor PlayerDisguise odwoluje sie do UserProfile.
  // Zadne z nich nie jest wywolywane z naszego kodu, wiec ZADNE nie ma prawa trafic do
  // 'depend' w plugin.yml — dostarcza je ten plugin, ktory ich faktycznie uzywa.
  "MythicLib-dist-1.7.1-20260330.110510-95.jar",
  "packetevents-spigot-2.11.2.jar",
).map { serverPluginsDir.resolve(it) }

val missingJars = pinnedPluginJars.filterNot { it.isFile }
if (missingJars.isNotEmpty()) {
  throw GradleException(
    "Brak przypietych jarow pluginow:\n" +
      missingJars.joinToString("\n") { "  " + it.absolutePath } +
      "\nUstaw -PccServerPlugins=<katalog z pluginami> albo popraw liste w build.gradle.kts."
  )
}

dependencies {
  compileOnly(paperApi)
  compileOnly(files(pinnedPluginJars))

  testImplementation(paperApi)
  testImplementation(files(pinnedPluginJars))
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/** Wersja = baza z gradle.properties + rewizja gita (przekazana przez build.sh). */
version = "${providers.gradleProperty("ccVersion").get()}-${gitRevision()}"

fun gitRevision(): String {
  providers.gradleProperty("ccGitRev").orNull?.takeIf { it.isNotBlank() }?.let { return it }
  return try {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
      .directory(projectDir)
      .redirectErrorStream(true)
      .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0 && output.isNotEmpty()) output else "nogit"
  } catch (ex: Exception) {
    "nogit"
  }
}

tasks.processResources {
  val pluginVersion = version.toString()
  inputs.property("version", pluginVersion)
  filesMatching("plugin.yml") {
    expand("version" to pluginVersion)
  }
}

tasks.jar {
  archiveBaseName = "character-creator"
}

tasks.test {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
  }
}

tasks.compileJava {
  options.encoding = "UTF-8"
  options.release = 21
}

tasks.compileTestJava {
  options.encoding = "UTF-8"
  options.release = 21
}
