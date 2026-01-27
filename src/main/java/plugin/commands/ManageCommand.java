package plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.gui.pages.SimpleProtectUI;

import javax.annotation.Nonnull;

public class ManageCommand extends AbstractPlayerCommand {
    public ManageCommand(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
    }

    @Override
    protected void execute(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        CommandSender sender = commandContext.sender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.raw("Only players can run this command!"));
            return;
        }

        CustomUIPage page = player.getPageManager().getCustomPage();
        if (page == null) {
            page = new SimpleProtectUI(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
            player.getPageManager().openCustomPage(ref, store, page);
        }

        playerRef.sendMessage(Message.raw("UI PAGE SHOWN"));
    }
}
