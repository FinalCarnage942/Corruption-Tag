package carnage.corruptionTag.managers;

import carnage.corruptionTag.CorruptionTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class WorldManager {

    public static World createGameWorld(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        File dataFolder = new File(worldFolder, "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("{\"layers\":[],\"biome\":\"minecraft:plains\",\"structures\":{\"structures\":{}}}");
        creator.generateStructures(false);
        creator.environment(World.Environment.NORMAL);

        return creator.createWorld();
    }

    public static Location pasteSchematic(World bukkitWorld, String schematicName) throws IOException {
        Plugin plugin = CorruptionTag.getInstance();
        Path schematicPath = plugin.getDataFolder().toPath().resolve("schematics").resolve(schematicName);
        Files.createDirectories(schematicPath.getParent());

        if (!Files.exists(schematicPath)) {
            try (InputStream in = plugin.getResource("schematics/" + schematicName)) {
                if (in != null) {
                    Files.copy(in, schematicPath);
                } else {
                    throw new IOException("Schematic not found in resources: " + schematicName);
                }
            }
        }

        if (!Files.isReadable(schematicPath)) {
            throw new IOException("Schematic is not readable: " + schematicPath);
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicPath.toFile());
        if (format == null) {
            throw new IOException("Unsupported schematic format: " + schematicName);
        }

        try (ClipboardReader reader = format.getReader(Files.newInputStream(schematicPath))) {
            Clipboard clipboard = reader.read();
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);

            int width = clipboard.getDimensions().getX();
            int height = clipboard.getDimensions().getY();
            int length = clipboard.getDimensions().getZ();

            // Paste schematic centered at (0, 64, 0)
            int originX = -width / 2;
            int originY = 64;
            int originZ = -length / 2;

            BlockVector3 pasteOrigin = BlockVector3.at(originX, originY, originZ);

            try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(session)
                        .to(pasteOrigin)
                        .ignoreAirBlocks(true)
                        .build();

                Operations.complete(operation);
            }

            // Calculate center of schematic in world coordinates
            int centerX = originX + width / 2;
            int centerY = originY + 1; // Y+1 to spawn on the surface
            int centerZ = originZ + length / 2;

            return new Location(bukkitWorld, centerX + 0.5, centerY, centerZ + 0.5);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to paste schematic: " + e.getMessage(), e);
            throw new IOException("Failed to paste schematic", e);
        }
    }

    public static void deleteWorld(World world) {
        String worldName = world.getName();
        Bukkit.unloadWorld(world, false);
        File folder = new File(Bukkit.getWorldContainer(), worldName);
        deleteFolder(folder);
    }

    private static void deleteFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
            folder.delete();
        }
    }
}
