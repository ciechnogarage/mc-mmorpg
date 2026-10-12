plugins { java }

group = "pl.mcmmorpg"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

val serverPluginsDir = file(providers.gradleProperty("serverPlugins").get())

/** Jary PRZYPIETE po nazwie pliku = dokladnie to, co stoi na world1 (montowane tylko do odczytu). */
val pinnedPluginJars = listOf(
  "MMOCore-1.13.1-20260330.110342-58.jar",
  "MythicLib-dist-1.7.1-20260330.110510-95.jar",
  "MMOItems-6.10.1-20260330.094756-58.jar",
  "MythicMobs-5.11.2.jar",
  "nexo-1.20.jar",
  "packetevents-spigot-2.11.2.jar",
  "PlaceholderAPI-2.12.2.jar",
  "Vault-1.7.4.jar",
).map { serverPluginsDir.resolve(it) }

val missing = pinnedPluginJars.filterNot { it.isFile }
if (missing.isNotEmpty()) throw GradleException("Brak jarow: " + missing.joinToString())

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  compileOnly(files(pinnedPluginJars))
}

version = "${providers.gradleProperty("rzmVersion").get()}-${providers.gradleProperty("rzmRev").orNull ?: "dev"}"

tasks.processResources {
  val v = version.toString()
  inputs.property("version", v)
  filesMatching("plugin.yml") { expand("version" to v) }
}
tasks.jar { archiveBaseName = "rzemioslo" }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release = 21; options.compilerArgs.add("-Xlint:deprecation") }
