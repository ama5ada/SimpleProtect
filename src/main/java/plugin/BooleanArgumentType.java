package plugin;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BooleanArgumentType extends SingleArgumentType<Boolean> {

    public BooleanArgumentType() {
        super("boolean", "true/false");
    }

    @Override
    @Nullable
    public Boolean parse(@Nonnull String input, @Nonnull ParseResult parseResult) {
        String value = input.trim().toLowerCase();

        return switch (value) {
            case "true", "t", "yes", "y", "on", "1" -> true;
            case "false", "f", "no", "n", "off", "0" -> false;
            default -> {
                // Message to player as a result of improper argument for a boolean command
                parseResult.fail(Message.raw("The wrong type of required argument was provided.\n" +
                        "Expected : Boolean \nTrue : 'true, yes, on, 1'\nFalse: 'false, no, off, 0'\n" +
                        "Provided : " + value));
                yield null;
            }
        };
    }
}
