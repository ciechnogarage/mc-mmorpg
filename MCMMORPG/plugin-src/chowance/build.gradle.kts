plugins { java }

group = "pl.mcmmorpg"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApi = "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"
val serverPluginsDir = file(providers.gradleProperty("chwServerPlugins").get())

/** Jary PRZYPIETE po nazwie pliku = dokladnie to, co stoi na world1. */
val pinnedPluginJars = listOf(
  "MythicMobs-5.11.2.jar",
  "MMOProfiles-1.2-20260325.193503-28.jar",
  "MythicLib-dist-1.7.1-20260330.110510-95.jar",
  "MMOCore-1.13.1-20260330.110342-58.jar",
  "PlaceholderAPI-2.12.2.jar",
  "MythicDungeons-2.0.1-SNAPSHOT.jar",
  "ModelEngine-4.0.9.jar",
).map { serverPluginsDir.resolve(it) }

val missing = pinnedPluginJars.filterNot { it.isFile }
if (missing.isNotEmpty()) throw GradleException("Brak jarow: " + missing.joinToString())

dependencies {
  compileOnly(paperApi)
  compileOnly(files(pinnedPluginJars))
  testImplementation(paperApi)
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

version = "${providers.gradleProperty("chwVersion").get()}-${providers.gradleProperty("chwRev").orNull ?: "dev"}"

tasks.processResources {
  val v = version.toString()
  inputs.property("version", v)
  filesMatching("plugin.yml") { expand("version" to v) }
}
tasks.jar { archiveBaseName = "chowance" }
tasks.test { useJUnitPlatform(); testLogging { events("failed"); showStandardStreams = false; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL } }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release = 21; options.compilerArgs.add("-Xlint:deprecation") }
