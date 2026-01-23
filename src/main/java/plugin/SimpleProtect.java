package plugin;

import com.hypixel.hytale.server.core.event.events.ecs.*;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import plugin.commands.ProtectCommand;
import plugin.commands.SetNotifyPlayerCommand;
import plugin.commands.SetProtectedCommand;
import plugin.commands.SetVerboseCommand;
import plugin.events.*;
import plugin.systems.*;

import javax.annotation.Nonnull;
import java.io.File;

public class SimpleProtect extends JavaPlugin {
    public SimpleProtect(@Nonnull JavaPluginInit init) {
        super(init);
        Config<PluginConfig> pluginConfig = withConfig(PluginConfig.CODEC);
        ConfigState.init(pluginConfig);
    }

    @Override
    protected void setup() {
        super.setup();
        ConfigState.get().initializeDefaults();
        ProtectCommand protectCommand = new ProtectCommand("simpleprotect",
                "Simple Protect Commands", false) {{
                    addAliases("spr");
        }};
        protectCommand.addSubCommand(new SetProtectedCommand("protect",
                "Set Simple Protect protection to true/false", false) {{
                    addAliases("p");
        }});
        protectCommand.addSubCommand(new SetNotifyPlayerCommand("notify",
                "Set Simple Protect notifications to true/false", false) {{
                    addAliases("n");
        }});
        protectCommand.addSubCommand(new SetVerboseCommand("verbose",
                "Set Simple Protect verbose logging to true/false", false) {{
            addAliases("v");
        }});

        // Add a command for registering a new world with SimpleProtect by name
        // - Add a GUI for setting up the world
        // Add a command for clearing a world with SimpleProtect by name
        // Add a command for renaming a world with SimpleProtect by name
        // Add a command for modifying each field of a world
        // - Add a GUI for viewing & modifying registered worlds

        // Add a command for modifying each field of DefaultWorld
        // - Add a GUI for viewing & modifying DefaultWorld



        getCommandRegistry().registerCommand(protectCommand);

        getEntityStoreRegistry().registerSystem(new BreakBlockEventSystem(BreakBlockEvent.class));
        getEntityStoreRegistry().registerSystem(new PlaceBlockEventSystem(PlaceBlockEvent.class));
        getEntityStoreRegistry().registerSystem(new UseBlockEventSystem(UseBlockEvent.Pre.class));
        getEntityStoreRegistry().registerSystem(new DamageBlockEventSystem(DamageBlockEvent.class));
        getEntityStoreRegistry().registerSystem(new PlaceFluidEventSystem(PlaceFluidEvent.class));
        getEntityStoreRegistry().registerSystem(new RefillContainerEventSystem(RefillContainerEvent.class));
        getEntityStoreRegistry().registerSystem(new CycleBlockGroupEventSystem(CycleBlockGroupEvent.class));
        getEntityStoreRegistry().registerSystem(new HarvestBlockEventSystem(HarvestBlockEvent.class));
        getEntityStoreRegistry().registerSystem(new GatherBlockEventSystem(GatherBlockEvent.class));

        getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerReadyHandler::HandlePlayerReady);
        getEventRegistry().register(PlayerDisconnectEvent.class, PlayerDisconnectHandler::HandlePlayerDisconnect);

        PacketAdapters.registerInbound(new BuildToolPacketFilter());
    }

    @Nonnull
    public File getDefaultDataFolder() {
        File pluginJar = getFile().toFile(); // Mods/<PluginName>.jar
        File modsFolder = pluginJar.getParentFile(); // Mods/
        return new File(modsFolder + "\\" + getName().replace(":","_")); // Mods_<PluginName>/
    }
}
