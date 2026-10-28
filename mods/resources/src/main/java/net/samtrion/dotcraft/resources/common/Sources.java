package net.samtrion.dotcraft.resources.common;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.io.CharStreams;

import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.samtrion.dotcraft.resources.CraftResources;

/** Reads the {@code *.json} source files of a data folder, from the mod jar or from a directory. */
final class Sources {
    /** Not instantiable. */
    private Sources() {}

    /**
     * Reads the sources of a folder of this mod on the classpath.
     *
     * @param directory the folder, e.g. {@code materials}
     * @return the file contents by file name, sorted by name
     */
    static Map<String, String> classpath(String directory) {
        if (ModList.get() == null) return unitTestClasspath(directory);
        Map<String, String> sources = new TreeMap<>();
        JarContents contents = ModList.get().getModFileById(CraftResources.MODID).getFile().getContents();
        contents.visitContent(directory, (path, resource) -> {
            if (path.endsWith(".json")) {
                try (Reader reader = resource.bufferedReader()) {
                    sources.put(path.substring(path.lastIndexOf('/') + 1), CharStreams.toString(reader));
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot read " + path, e);
                }
            }
        });
        return sources;
    }

    /**
     * Reads the sources from the class path folder when no mod loader runs, which is the case in unit tests (the
     * resources are a directory there, not a jar), so that the config can list the materials without a game.
     */
    private static Map<String, String> unitTestClasspath(String directory) {
        try {
            return directory(Path.of(Sources.class.getResource("/" + directory).toURI()));
        } catch (IOException | java.net.URISyntaxException e) {
            throw new IllegalStateException("Cannot read " + directory, e);
        }
    }

    /**
     * Reads the sources of a directory.
     *
     * @param directory the folder with the {@code *.json} sources
     * @return the file contents by file name, sorted by name
     * @throws IOException if a file cannot be read
     */
    static Map<String, String> directory(Path directory) throws IOException {
        Map<String, String> sources = new TreeMap<>();
        try (var list = Files.list(directory)) {
            for (Path file : list.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                sources.put(file.getFileName().toString(), Files.readString(file));
            }
        }
        return sources;
    }
}
