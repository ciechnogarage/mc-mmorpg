// Reference-only. The repository has no installed Gradle; use
// tools/build-petcore.sh, which compiles against the local Paper libraries.
plugins { java }

group = "pl.mcmmorpg"
version = "0.1.0-SNAPSHOT"

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies { compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT") }
