package plugin;

import com.hypixel.hytale.server.core.event.events.ecs.*;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import plugin.commands.ProtectCommand;
import plugin.commands.SetNotifyPlayerCommand;
import plugin.commands.SetProtectedCommand;


import javax.annotation.Nonnull;
import java.io.File;

public class SimpleProtect extends JavaPlugin {
    private static SimpleProtect instance;
    public SimpleProtect(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        Config<PluginConfig> pluginConfig = withConfig(PluginConfig.CODEC);
        ConfigState.init(pluginConfig);
    }

    @Override
    protected void setup() {
        super.setup();
        ProtectCommand protectCommand = new ProtectCommand("simpleprotect",
                "Simple Protect Commands", false) {{
                    addAliases("sp");
        }};
        protectCommand.addSubCommand(new SetProtectedCommand("protect",
                "Set Simple Protect protection to true/false", false) {{
                    addAliases("p");
        }});
        protectCommand.addSubCommand(new SetNotifyPlayerCommand("notify",
                "Set Simple Protect notifications to true/false", false) {{
                    addAliases("n");
        }});
        getCommandRegistry().registerCommand(protectCommand);

        getEntityStoreRegistry().registerSystem(new BreakBlockEventSystem(BreakBlockEvent.class));
        getEntityStoreRegistry().registerSystem(new PlaceBlockEventSystem(PlaceBlockEvent.class));
        getEntityStoreRegistry().registerSystem(new UseBlockEventSystem(UseBlockEvent.Pre.class));
        getEntityStoreRegistry().registerSystem(new DamageBlockEventSystem(DamageBlockEvent.class));
        var interactionRegistry = getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("UseBlock", CustomUseBlockInteraction.class, CustomUseBlockInteraction.CODEC);
        interactionRegistry.register("PlaceFluid", CustomPlaceFluidInteraction.class, CustomPlaceFluidInteraction.CODEC);
        interactionRegistry.register("RefillContainer", CustomRefillContainerInteraction.class, CustomRefillContainerInteraction.CODEC);
        interactionRegistry.register("CycleBlockGroup", CustomCycleBlockGroupInteraction.class, CustomCycleBlockGroupInteraction.CODEC);
    }

    public static SimpleProtect get() {
        return instance;
    }

    @Nonnull
    public File getDefaultDataFolder() {
        File pluginJar = getFile().toFile(); // Mods/<PluginName>.jar
        File modsFolder = pluginJar.getParentFile(); // Mods/
        return new File(modsFolder + "\\" + getName().replace(":","_")); // Mods_<PluginName>/
    }
}
