plugins { java }

group = "pl.mcmmorpg"
version = "0.1.0"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

val serverPluginsDir = file(providers.gradleProperty("serverPlugins").get())
val pinnedPluginJars = listOf(
  "MMOInventory-2.0-20260330.113858-32.jar",
  "MMOProfiles-1.2-20260325.193503-28.jar",
  "MMOItems-6.10.1-20260330.094756-58.jar",
  "MythicLib-dist-1.7.1-20260330.110510-95.jar",
  "MMOCore-1.13.1-20260330.110342-58.jar",
).map { serverPluginsDir.resolve(it) }
val missing = pinnedPluginJars.filterNot { it.isFile }
if (missing.isNotEmpty()) throw GradleException("Brak jarow: " + missing.joinToString())

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  compileOnly(files(pinnedPluginJars))
}

tasks.processResources {
  inputs.property("version", version)
  filesMatching("plugin.yml") { expand("version" to version) }
}
tasks.jar { archiveBaseName = "ksiega-postaci" }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release = 21 }
