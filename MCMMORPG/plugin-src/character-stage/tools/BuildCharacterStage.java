import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class BuildCharacterStage {
  public static void main(String[] args) throws Exception {
    Path root = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
    Path srcRoot = root.resolve("src/main/java");
    Path resourcesRoot = root.resolve("src/main/resources");
    Path buildRoot = root.resolve("build");
    Path classesRoot = buildRoot.resolve("classes");
    Path libsRoot = buildRoot.resolve("libs");
    Path runtimeLibraries = root.resolve("../../libraries").normalize();
    String version = "0.1.0-SNAPSHOT";

    deleteDirectory(buildRoot);
    Files.createDirectories(classesRoot);
    Files.createDirectories(libsRoot);

    compileJava(srcRoot, classesRoot, runtimeLibraries);
    copyResources(resourcesRoot, classesRoot, version);

    Path outJar = libsRoot.resolve("character-stage-" + version + ".jar");
    packageJar(classesRoot, outJar);
    System.out.println("BUILT " + outJar);
  }

  private static void compileJava(Path srcRoot, Path classesRoot, Path runtimeLibraries) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException("System Java compiler not available");
    }

    List<Path> sources;
    try (Stream<Path> stream = Files.walk(srcRoot)) {
      sources = stream.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toList());
    }

    String classpath = buildClasspath(runtimeLibraries);
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      List<String> options = List.of(
          "-classpath", classpath,
          "-d", classesRoot.toString(),
          "-source", "21",
          "-target", "21"
      );
      var compilationUnits = fileManager.getJavaFileObjectsFromFiles(
          sources.stream().map(Path::toFile).collect(Collectors.toList())
      );
      boolean ok = compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();
      if (!ok) {
        throw new IllegalStateException("CharacterStage compilation failed");
      }
    }
  }

  private static String buildClasspath(Path runtimeLibraries) throws IOException {
    try (Stream<Path> stream = Files.walk(runtimeLibraries)) {
      return stream
          .filter(path -> path.toString().endsWith(".jar"))
          .map(path -> path.toAbsolutePath().toString())
          .sorted()
          .collect(Collectors.joining(System.getProperty("path.separator")));
    }
  }

  private static void copyResources(Path resourcesRoot, Path classesRoot, String version) throws IOException {
    try (Stream<Path> stream = Files.walk(resourcesRoot)) {
      for (Path source : stream.toList()) {
        if (Files.isDirectory(source)) {
          continue;
        }
        Path relative = resourcesRoot.relativize(source);
        Path target = classesRoot.resolve(relative.toString());
        Files.createDirectories(target.getParent());
        String text = Files.readString(source, StandardCharsets.UTF_8).replace("${version}", version);
        Files.writeString(target, text, StandardCharsets.UTF_8);
      }
    }
  }

  private static void packageJar(Path classesRoot, Path outJar) throws IOException {
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(outJar))) {
      try (Stream<Path> stream = Files.walk(classesRoot).sorted(Comparator.naturalOrder())) {
        for (Path path : stream.toList()) {
          if (Files.isDirectory(path)) {
            continue;
          }
          String entryName = classesRoot.relativize(path).toString().replace('\\', '/');
          jar.putNextEntry(new JarEntry(entryName));
          try (InputStream in = Files.newInputStream(path)) {
            in.transferTo(jar);
          }
          jar.closeEntry();
        }
      }
    }
  }

  private static void deleteDirectory(Path root) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(root)) {
      for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
  }
}
