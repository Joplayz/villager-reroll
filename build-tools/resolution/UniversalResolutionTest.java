package com.joplayx.testing;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.discovery.ModResolutionException;
import net.fabricmc.loader.impl.discovery.ModResolver;
import net.fabricmc.loader.impl.game.GameProvider.BuiltinMod;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.ModMetadataParser;
import net.fabricmc.loader.impl.metadata.VersionOverrides;

/** Uses the actual Loader solver and packaged metadata; no game, graphics or user config. */
public final class UniversalResolutionTest {
    private static final List<String> LIBRARIES = List.of("fabric-api", "yet_another_config_lib_v3", "modmenu");
    private static final VersionOverrides VERSIONS = new VersionOverrides();
    private static DependencyOverrides overrides;
    private record Variant(String path, byte[] metadata) {}
    private record Bundle(byte[] metadata, List<Variant> variants) {}

    public static void main(String[] args) throws Exception {
        Path report = Path.of(args[2]);
        // A fresh empty directory prevents a developer's dependency overrides affecting the test.
        Path config = Files.createTempDirectory("reroller-resolver-");
        overrides = new DependencyOverrides(config);
        try {
            Bundle bundle = read(Path.of(args[0]));
            String loaderVersion = args[1];
            String release = parse(bundle.metadata()).getVersion().getFriendlyString();
            List<String> results = new ArrayList<>();
            for (Variant variant : bundle.variants()) {
                LoaderModMetadata metadata = parse(variant.metadata());
                String game = dependency(metadata, "minecraft").getVersionRequirements().iterator().next().toString().replaceFirst("^=", "");
                boolean compatibleLoader = dependency(metadata, "fabricloader").matches(Version.parse(loaderVersion));
                run(bundle, game, loaderVersion, "none", compatibleLoader, release, results);
                if (compatibleLoader) {
                    for (String id : LIBRARIES) run(bundle, game, loaderVersion, id, false, release, results);
                    run(bundle, game, loaderVersion, "older-copy", true, release, results);
                    run(bundle, game, loaderVersion, "newer-copy", true, release, results);
                    if (metadata.getDependencies().stream().anyMatch(d -> d.getModId().equals("placeholder-api")))
                        run(bundle, game, loaderVersion, "placeholder-api", false, release, results);
                }
            }
            run(bundle, "1.21.9", loaderVersion, "unsupported", false, release, results);
            Files.createDirectories(report.getParent());
            Files.write(report, results, StandardCharsets.UTF_8);
            System.out.println("Fabric Loader " + loaderVersion + ": " + results.size() + " resolution checks passed.");
        } finally {
            Files.deleteIfExists(config);
        }
    }

    private static void run(Bundle bundle, String game, String loader, String scenario,
                            boolean shouldLoad, String release, List<String> results) throws Exception {
        List<ModCandidateImpl> candidates = new ArrayList<>();
        List<ModCandidateImpl> nested = new ArrayList<>();
        for (Variant v : bundle.variants())
            nested.add(invoke("createNested", v.path(), 0L, parse(v.metadata()), false, List.of()));
        ModCandidateImpl outer = invoke("createPlain", List.of(Path.of("universal.jar")), parse(bundle.metadata()), false, nested);
        candidates.add(outer);
        var addParent = ModCandidateImpl.class.getDeclaredMethod("addParent", ModCandidateImpl.class);
        addParent.setAccessible(true);
        for (var child : nested) { addParent.invoke(child, outer); candidates.add(child); }
        candidates.add(builtin("minecraft", game));
        candidates.add(builtin("java", game.equals("1.20.1") ? "17" : game.startsWith("1.") ? "21" : "25"));
        candidates.add(builtin("fabricloader", loader));
        for (String id : LIBRARIES) if (!id.equals(scenario)) candidates.add(plain(id, "99.0.0"));
        if (!scenario.equals("placeholder-api")) candidates.add(plain("placeholder-api", "99.0.0"));
        // Deliberately neutral standalone metadata: it isolates the wrapper's release constraint.
        if (scenario.equals("older-copy")) candidates.add(plain("villager-reroll", "0.1.0"));
        if (scenario.equals("newer-copy")) candidates.add(plain("villager-reroll", "999.0.0"));
        try {
            var selected = ModResolver.resolve(candidates, EnvType.CLIENT, new HashMap<>());
            if (!shouldLoad) throw new AssertionError(game + "/" + scenario + " unexpectedly loaded");
            var matches = selected.stream().filter(m -> m.getId().equals("villager-reroll")).toList();
            if (matches.size() != 1 || !matches.get(0).getVersion().getFriendlyString().equals(release + "+mc" + game)
                    || !matches.get(0).getParentMods().contains(outer))
                throw new AssertionError("Wrong implementation selected: " + matches);
        } catch (ModResolutionException failure) {
            if (shouldLoad) throw new AssertionError(game + " should load on " + loader, failure);
            if (LIBRARIES.contains(scenario) && !failure.getMessage().contains("Install " + scenario))
                throw new AssertionError("Missing library must be named in the suggested fix", failure);
        }
        results.add("PASS " + game + " / " + scenario + " / " + (shouldLoad ? "correct nested implementation" : "rejected"));
    }

    private static ModDependency dependency(LoaderModMetadata metadata, String id) {
        return metadata.getDependencies().stream().filter(d -> d.getModId().equals(id)).findFirst().orElseThrow();
    }

    private static Bundle read(Path path) throws Exception {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            byte[] metadata = zip.getInputStream(zip.getEntry("fabric.mod.json")).readAllBytes();
            List<Variant> variants = new ArrayList<>();
            // Follow the wrapper's declared nested JAR list, as Loader discovery does.
            for (var entry : parse(metadata).getJars()) {
                String nestedPath = entry.getFile();
                try (ZipInputStream nested = new ZipInputStream(zip.getInputStream(zip.getEntry(nestedPath)))) {
                    boolean found = false;
                    for (var file = nested.getNextEntry(); file != null; file = nested.getNextEntry()) {
                        if (file.getName().equals("fabric.mod.json")) {
                            variants.add(new Variant(nestedPath, nested.readAllBytes())); found = true; break;
                        }
                    }
                    if (!found) throw new AssertionError("Missing nested metadata: " + nestedPath);
                }
            }
            return new Bundle(metadata, variants);
        }
    }

    private static LoaderModMetadata parse(byte[] bytes) throws Exception {
        return ModMetadataParser.parseMetadata(new ByteArrayInputStream(bytes), "universal.jar", List.of(), VERSIONS, overrides, false);
    }
    private static ModCandidateImpl builtin(String id, String version) throws Exception {
        return invoke("createBuiltin", new BuiltinMod(List.of(Path.of(id)), new BuiltinModMetadata.Builder(id, version).build()), VERSIONS, overrides);
    }
    private static ModCandidateImpl plain(String id, String version) throws Exception {
        String json = "{\"schemaVersion\":1,\"id\":\"" + id + "\",\"version\":\"" + version + "\"}";
        return invoke("createPlain", List.of(Path.of(id)), parse(json.getBytes(StandardCharsets.UTF_8)), false, List.of());
    }
    // Loader intentionally keeps candidate construction package-private. Reflection avoids modifying
    // its signed JAR or putting our unsigned test class in its package. This is test-only code.
    private static ModCandidateImpl invoke(String name, Object... args) throws Exception {
        for (var method : ModCandidateImpl.class.getDeclaredMethods()) if (method.getName().equals(name)) {
            method.setAccessible(true); return (ModCandidateImpl) method.invoke(null, args);
        }
        throw new NoSuchMethodException(name);
    }
}
