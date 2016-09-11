/*
 * This file is part of WinterWonderland, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2016 Flibio
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.flibio.winterwonderland;

import io.github.flibio.winterwonderland.FileManager.FileType;

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
            Main.access.fileManager.saveFile(FileType.DATA, Main.access.playerData);
            if (value) {
                player.sendMessage(prefix.toBuilder().append(Text.of(TextColors.RED, "Turned off ", TextColors.WHITE, "snow placement!")).build());
            } else {
                player.sendMessage(prefix.toBuilder().append(Text.of(TextColors.GREEN, "Turned on ", TextColors.WHITE, "snow placement!")).build());
            }
        }

        return CommandResult.success();
    }
}
