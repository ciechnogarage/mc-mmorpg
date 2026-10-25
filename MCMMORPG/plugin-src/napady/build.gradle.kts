plugins { java }

group = "pl.mcmmorpg"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  // jary z serwera (libs/ poza gitem, kopiuje build.sh): MythicMobs 5.11.2, LibsDisguises 11.0.15, packetevents 2.11.2
  compileOnly(fileTree("libs") { include("*.jar") })
}

version = "${providers.gradleProperty("pmVersion").get()}-${providers.gradleProperty("pmRev").orNull ?: "dev"}"

tasks.processResources {
  val v = version.toString()
  inputs.property("version", v)
  filesMatching("plugin.yml") { expand("version" to v) }
}
tasks.jar { archiveBaseName = "napady" }
tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release = 21; options.compilerArgs.add("-Xlint:deprecation") }
