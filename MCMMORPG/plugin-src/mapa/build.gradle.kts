plugins { java }

group = "pl.mcmmorpg"
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApi = "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"

dependencies {
  compileOnly(paperApi)
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
