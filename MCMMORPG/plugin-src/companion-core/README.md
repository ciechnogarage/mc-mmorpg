# CompanionCore

First vertical slice: `companion.tank.tier1` summon, FOLLOW, dismiss and quit cleanup.

Build without installing a host JDK:

```bash
docker run --rm -v "$PWD/MCMMORPG:/workspace" -w /workspace eclipse-temurin:21-jdk \
  bash -lc 'mkdir -p plugin-src/companion-core/build/classes && javac -source 21 -target 21 \
  -cp "paper.jar:plugins/MythicMobs-5.11.2.jar" -d plugin-src/companion-core/build/classes \
  $(find plugin-src/companion-core/src/main/java -name "*.java") && \
  cp plugin-src/companion-core/src/main/resources/* plugin-src/companion-core/build/classes/ && \
  jar cf plugin-src/companion-core/build/CompanionCore-0.1.0-SNAPSHOT.jar -C plugin-src/companion-core/build/classes .'
```
