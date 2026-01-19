package plugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.Argument;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import plugin.BooleanArgumentType;
import plugin.ConfigState;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil.getLogger;

public class SetProtectedCommand extends AbstractPlayerCommand {
    private final Argument<?, Boolean> toggleArg;

    public SetProtectedCommand(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        this.toggleArg = this.withRequiredArg(
                "protect",
                "Boolean value to set protection true/false",
                new BooleanArgumentType()
        );
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
        boolean toggleValue = commandContext.get(toggleArg);
        if (!(sender instanceof Player player)) {
            getLogger().info(
                    String.format("Console set Simple Protection to : %s", toggleValue));
            return;
        }

        UUID playerId = sender.getUuid();
        getLogger().info(
                String.format("%s set Simple Protection to : %s", playerId, toggleValue));
        ConfigState.get().setProtected(toggleValue);

        player.sendMessage(Message.raw(
                String.format("Set Simple Protection to : %s", toggleValue)));
    }
}
