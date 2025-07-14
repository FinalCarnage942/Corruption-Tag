package carnage.corruptionTag.managers;

import carnage.corruptionTag.CorruptionTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


import static org.apache.logging.log4j.LogManager.getLogger;

public class WorldManager {

    public static World createGameWorld(String worldName) {
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        File dataFolder = new File(worldFolder, "data");

        // Create data folder if it doesn't exist
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

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        File dataFolder = new File(world.getWorldFolder(), "data");
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            getLogger().info("Created data folder for world " + world.getName() + ": " + created);
        }
    }


    public static void pasteSchematic(org.bukkit.World bukkitWorld, File schematicFile, int x, int y, int z) throws IOException {
        ClipboardReader reader = ClipboardFormats.findByFile(schematicFile).getReader(new FileInputStream(schematicFile));
        Clipboard clipboard = reader.read();

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(x, y, z))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
        }
    }

    public static void buildStonePlatform(org.bukkit.World bukkitWorld, int centerX, int centerY, int centerZ) {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);

        try (EditSession session = WorldEdit.getInstance().newEditSession(weWorld)) {
            int radius = 2; // 5x5 area

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockVector3 pos = BlockVector3.at(centerX + x, centerY, centerZ + z);
                    session.setBlock(pos, BukkitAdapter.asBlockType(org.bukkit.Material.STONE).getDefaultState());
                }
            }
        }
    }


    public static void deleteWorld(org.bukkit.World world) {
        String worldName = world.getName();
        Bukkit.unloadWorld(world, false);

        File folder = new File(Bukkit.getWorldContainer(), worldName);
        deleteFolder(folder);
    }

    private static void deleteFolder(File folder) {
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) deleteFolder(file);
                else file.delete();
            }
            folder.delete();
        }
    }
}
