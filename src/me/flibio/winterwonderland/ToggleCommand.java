package me.flibio.winterwonderland;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class ToggleCommand implements CommandExecutor {

    private Text prefix = Text.of(TextColors.AQUA, "Winter Wonderland: ", TextColors.WHITE);

    @Override
    public CommandResult execute(CommandSource source, CommandContext args)
            throws CommandException {

        if (!(source instanceof Player)) {
            source.sendMessage(Text.builder("You must be a player to use /winter!").color(TextColors.RED).build());
            return CommandResult.success();
        }

        Player player = (Player) source;
        String uuid = player.getUniqueId().toString();
        if (Main.access.playerData.getNode(uuid) == null) {
            source.sendMessage(Text.builder("An error has occurred!").color(TextColors.RED).build());
            return CommandResult.success();
        } else {
            boolean value = Main.access.playerData.getNode(uuid).getBoolean();
            Main.access.playerData.getNode(uuid).setValue(!value);
            if (value) {
                player.sendMessage(prefix.toBuilder().append(Text.of(TextColors.RED, "Turned off ", TextColors.WHITE, "snow placement!")).build());
            } else {
                player.sendMessage(prefix.toBuilder().append(Text.of(TextColors.GREEN, "Turned on ", TextColors.WHITE, "snow placement!")).build());
            }
        }

        return CommandResult.success();
    }
}
