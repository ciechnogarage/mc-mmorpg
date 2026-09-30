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
  "MMOItems-6.10.1-20260330.094756-58.jar",
  "MythicLib-dist-1.7.1-20260330.110510-95.jar",
).map { serverPluginsDir.resolve(it) }

val missing = pinnedPluginJars.filterNot { it.isFile }
if (missing.isNotEmpty()) throw GradleException("Brak jarow: " + missing.joinToString())

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  compileOnly(files(pinnedPluginJars))
}

version = "${providers.gradleProperty("abVersion").get()}-${providers.gradleProperty("abRev").orNull ?: "dev"}"

tasks.processResources {
  val v = version.toString()
  inputs.property("version", v)
  filesMatching("plugin.yml") { expand("version" to v) }
}
tasks.jar { archiveBaseName = "animacje-broni" }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release = 21; options.compilerArgs.add("-Xlint:deprecation") }
