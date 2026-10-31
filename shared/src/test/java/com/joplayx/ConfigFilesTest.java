package com.joplayx;
import com.joplayx.client.config.ConfigFiles;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class ConfigFilesTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();
    @Test public void replaceOverwritesAndLeavesNoTemporaryFile() throws Exception {
        var config=folder.getRoot().toPath().resolve("villager-reroll.json");
        ConfigFiles.replace(config,"first");ConfigFiles.replace(config,"second");
        assertEquals("second",Files.readString(config));
        assertFalse(Files.exists(config.resolveSibling("villager-reroll.json.tmp")));
    }
    @Test public void backupKeepsEveryUnreadableCopy() throws Exception {
        var config=folder.getRoot().toPath().resolve("villager-reroll.json");
        Files.writeString(config,"{broken");
        var first=ConfigFiles.backup(config);
        Files.writeString(config,"{broken again");
        var second=ConfigFiles.backup(config);
        assertEquals("{broken",Files.readString(first));
        assertEquals("{broken again",Files.readString(second));
    }
}
