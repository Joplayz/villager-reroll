package com.joplayx.client.config;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Keeps a damaged or half-written config file from costing the player their saved searches. */
public final class ConfigFiles {
    private ConfigFiles() {}

    /** Writes beside the target first, so a crash during a save leaves the old file intact. */
    public static void replace(Path target, String content) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, content);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Copies an unreadable config aside before defaults overwrite it. Earlier copies are kept. */
    public static Path backup(Path source) throws IOException {
        for (int i = 0; ; i++) {
            Path copy = source.resolveSibling(source.getFileName() + ".broken" + (i == 0 ? "" : "." + i));
            if (!Files.exists(copy)) return Files.copy(source, copy);
        }
    }
}
