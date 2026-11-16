plugins { java }

group = "pl.mcmmorpg"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://repo.minceraft.dev/releases/") { name = "minceraft" }
}

val paperApi = "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"
val serverPluginsDir = file(providers.gradleProperty("mapaServerPlugins").orElse("/serverplugins").get())
val pinnedPluginJars = listOf(
  "MMOProfiles-1.2-20260325.193503-28.jar",
  "MythicLib-dist-1.7.1-20260330.110510-95.jar",
  "MMOCore-1.13.1-20260330.110342-58.jar",
  "packetevents-spigot-2.11.2.jar",
).map { serverPluginsDir.resolve(it) }
val missing = pinnedPluginJars.filterNot { it.isFile }
if (missing.isNotEmpty()) throw GradleException("Brak jarow: " + missing.joinToString())

dependencies {
  compileOnly(paperApi)
  compileOnly("de.pianoman911:mapengine-api:1.8.12")
  compileOnly(files("libs/NMinimap-1.0.9.jar", "libs/AnvilORM-1.0.2.jar"))
  compileOnly(files(pinnedPluginJars))
  testImplementation(paperApi)
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

version = "0.1.0-${providers.gradleProperty("mapaRev").orNull ?: "dev"}"

tasks.processResources {
  val v = version.toString()
  inputs.property("version", v)
  filesMatching("plugin.yml") { expand("version" to v) }
}
tasks.jar { archiveBaseName = "mapa" }
tasks.test { useJUnitPlatform(); testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL } }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release = 21; options.compilerArgs.add("-Xlint:deprecation") }
