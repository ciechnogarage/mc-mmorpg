plugins { java }

group = "pl.mcmmorpg"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

val serverPluginsDir = file(providers.gradleProperty("trServerPlugins").get())
/** Jar ModelEngine PRZYPIETY po nazwie = dokladnie ten z world1. */
val meJar = serverPluginsDir.resolve("ModelEngine-4.0.9.jar")
if (!meJar.isFile) throw GradleException("Brak $meJar")
/** MythicLib z world1 (AttackMetadata: zaklęcie czy cios bronią, żywioły; NBTItem: typ MMOItems). */
val mlJar = serverPluginsDir.listFiles()!!.first { it.name.startsWith("MythicLib-dist-") && it.name.endsWith(".jar") }

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  compileOnly(files(meJar))
  compileOnly(files(mlJar))
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testImplementation("org.joml:joml:1.10.8")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

version = "${providers.gradleProperty("trVersion").get()}-${providers.gradleProperty("trRev").orNull ?: "dev"}"

tasks.processResources {
  val v = version.toString()
  inputs.property("version", v)
  filesMatching("plugin.yml") { expand("version" to v) }
}
tasks.jar { archiveBaseName = "trafienia" }
tasks.test { useJUnitPlatform(); testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL } }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release = 21; options.compilerArgs.add("-Xlint:deprecation") }
