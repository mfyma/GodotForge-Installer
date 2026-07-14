package org.gradle.wrapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Minimal self-contained Gradle launcher used only to bootstrap the configured
 * Gradle distribution. It intentionally accepts HTTPS Gradle distribution
 * hosts only and protects ZIP extraction from path traversal.
 */
public final class GradleWrapperMain {
    private static final List<String> ALLOWED_HOSTS = List.of(
        "services.gradle.org",
        "downloads.gradle.org"
    );

    private GradleWrapperMain() {}

    public static void main(String[] args) throws Exception {
        Path projectDir = locateProjectDirectory();
        Properties properties = loadProperties(projectDir);
        URI distributionUri = validateDistributionUri(properties.getProperty("distributionUrl"));
        String expectedSha256 = properties.getProperty("distributionSha256Sum", "").trim();

        Path wrapperHome = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists",
            "godotforge-minimal", sha256(distributionUri.toString()));
        Files.createDirectories(wrapperHome);

        Path lockPath = wrapperHome.resolve("install.lock");
        try (FileChannel channel = FileChannel.open(lockPath,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            Path gradleHome = findGradleHome(wrapperHome);
            if (gradleHome == null) {
                installDistribution(distributionUri, expectedSha256, wrapperHome);
                gradleHome = findGradleHome(wrapperHome);
            }
            if (gradleHome == null) {
                throw new IOException("Gradle distribution was extracted but no Gradle home was found");
            }
            launchGradle(projectDir, gradleHome, args);
        }
    }

    private static Path locateProjectDirectory() throws Exception {
        Path jar = Paths.get(GradleWrapperMain.class.getProtectionDomain()
            .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        Path wrapperDir = Files.isDirectory(jar) ? jar : jar.getParent();
        if (wrapperDir == null || wrapperDir.getParent() == null || wrapperDir.getParent().getParent() == null) {
            throw new IOException("Unable to locate project directory from wrapper JAR");
        }
        return wrapperDir.getParent().getParent();
    }

    private static Properties loadProperties(Path projectDir) throws IOException {
        Path file = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        if (properties.getProperty("distributionUrl") == null) {
            throw new IOException("distributionUrl is missing from " + file);
        }
        return properties;
    }

    private static URI validateDistributionUri(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTPS Gradle distributions are allowed");
        }
        if (uri.getHost() == null || ALLOWED_HOSTS.stream().noneMatch(it -> it.equalsIgnoreCase(uri.getHost()))) {
            throw new IllegalArgumentException("Untrusted Gradle distribution host: " + uri.getHost());
        }
        return uri;
    }

    private static void installDistribution(URI uri, String expectedSha256, Path wrapperHome) throws Exception {
        Path zip = wrapperHome.resolve("distribution.zip.part");
        Path extract = wrapperHome.resolve("extracting");
        deleteRecursively(zip);
        deleteRecursively(extract);
        Files.createDirectories(extract);

        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(zip));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gradle download failed with HTTP " + response.statusCode());
        }

        if (!expectedSha256.isEmpty()) {
            String actual = sha256(zip);
            if (!actual.equalsIgnoreCase(expectedSha256)) {
                throw new IOException("Gradle distribution checksum mismatch");
            }
        }

        unzipSafely(zip, extract);
        Path extractedHome = findGradleHome(extract);
        if (extractedHome == null) {
            throw new IOException("Downloaded archive does not contain a Gradle distribution");
        }
        Path finalHome = wrapperHome.resolve(extractedHome.getFileName().toString());
        deleteRecursively(finalHome);
        try {
            Files.move(extractedHome, finalHome, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveError) {
            Files.move(extractedHome, finalHome, StandardCopyOption.REPLACE_EXISTING);
        }
        deleteRecursively(extract);
        Files.deleteIfExists(zip);
    }

    private static void unzipSafely(Path zip, Path destination) throws IOException {
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path output = destination.resolve(entry.getName()).normalize();
                if (!output.startsWith(destination)) {
                    throw new IOException("Unsafe ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
                }
                input.closeEntry();
            }
        }
    }

    private static Path findGradleHome(Path parent) throws IOException {
        if (!Files.isDirectory(parent)) return null;
        try (var stream = Files.list(parent)) {
            return stream
                .filter(Files::isDirectory)
                .filter(path -> Files.isRegularFile(path.resolve(gradleCommand(path))))
                .findFirst()
                .orElse(null);
        }
    }

    private static String gradleCommand(Path gradleHome) {
        return isWindows() ? "bin/gradle.bat" : "bin/gradle";
    }

    private static void launchGradle(Path projectDir, Path gradleHome, String[] args) throws Exception {
        Path executable = gradleHome.resolve(gradleCommand(gradleHome));
        if (!isWindows()) executable.toFile().setExecutable(true);

        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("cmd");
            command.add("/c");
        }
        command.add(executable.toString());
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
            .directory(projectDir.toFile())
            .inheritIO()
            .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) System.exit(exitCode);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder()).forEach(item -> {
                    try {
                        Files.deleteIfExists(item);
                    } catch (IOException error) {
                        throw new RuntimeException(error);
                    }
                });
            } catch (RuntimeException error) {
                if (error.getCause() instanceof IOException) throw (IOException) error.getCause();
                throw error;
            }
        } else {
            Files.deleteIfExists(path);
        }
    }
}
