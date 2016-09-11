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

import com.google.inject.Inject;
import io.github.flibio.winterwonderland.FileManager.FileType;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.data.property.block.SolidCubeProperty;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(id = "winterwonderland", name = "Winter Wonderland", version = "1.2.1")
public class Main {

    public static Main access;

    @Inject Logger logger;

    @Inject Game game;

    private GameRegistry registery;
    protected FileManager fileManager;
    private Scheduler scheduler;
    protected ConfigurationNode playerData;

    private ArrayList<Location<World>> snowLocs = new ArrayList<Location<World>>();

    private ArrayList<BlockType> blockedTypes = new ArrayList<>(Arrays.asList(BlockTypes.WOODEN_SLAB, BlockTypes.STONE_SLAB, BlockTypes.STONE_SLAB2,
            BlockTypes.STANDING_SIGN, BlockTypes.WALL_SIGN));

    private boolean enabled = false;
    private boolean defaultTrailValue;

    @Listener
    public void onServerStart(GameInitializationEvent event) {
        registery = game.getRegistry();
        scheduler = game.getScheduler();
        fileManager = new FileManager(logger);

        access = this;

        // File generation
        fileManager.generateFolder("config/WinterWonderland");
        fileManager.generateFile("config/WinterWonderland/config.conf");
        fileManager.generateFile("config/WinterWonderland/data.conf");
        fileManager.loadFile(FileType.DATA);
        fileManager.loadFile(FileType.CONFIGURATION);
        fileManager.testDefault("ignore-date", "disabled");
        fileManager.testDefault("default-trail-value", "enabled");

        fileManager.loadFile(FileType.CONFIGURATION);
        if (fileManager.getConfigValue("ignore-date").equalsIgnoreCase("enabled")) {
            enabled = true;
        }
        defaultTrailValue = fileManager.getConfigValue("default-trail-value").equalsIgnoreCase("enabled");

        CommandSpec toggleCommand = CommandSpec.builder()
                .description(Text.of("Toggle snow placement"))
                .permission("winter.toggle")
                .executor(new ToggleCommand())
                .build();
        game.getCommandManager().register(this, toggleCommand, "winter");

        playerData = fileManager.getFile(FileType.DATA);

        // Check for the correct date
        if (!enabled) {
            scheduler.createTaskBuilder().execute(r -> {
                enabled = isEnabled();
            }).interval(10, TimeUnit.MINUTES).submit(this);
        }
    }

    @Listener
    public void onServerStop(GameStoppingServerEvent event) {
        fileManager.saveFile(FileType.DATA, playerData);
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Auth event) {
        String uuid = event.getProfile().getUniqueId().toString();
        if (playerData.getNode(uuid).isVirtual()) {
            playerData.getNode(uuid).setValue(defaultTrailValue);
            fileManager.saveFile(FileType.DATA, playerData);
        }
    }

    @Listener
    public void onPlayerMove(MoveEntityEvent event) {
        if (enabled) {
            if (!(event.getTargetEntity() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getTargetEntity();
            String uuid = player.getUniqueId().toString();
            if (!playerData.getNode(uuid).isVirtual() && playerData.getNode(uuid).getBoolean()) {
                Location<World> loc = player.getLocation();
                if (loc.getBlockType().equals(BlockTypes.AIR) && !loc.add(0, -1, 0).getBlockType().equals(BlockTypes.AIR)) {
                    Optional<PassableProperty> passableOptional = loc.add(0, 0, 0).getProperty(PassableProperty.class);
                    Optional<SolidCubeProperty> solidOptional = loc.add(0, -1, 0).getProperty(SolidCubeProperty.class);
                    if (solidOptional.isPresent()) {
                        if (!solidOptional.get().getValue()) {
                            return;
                        }
                    }
                    if (passableOptional.isPresent()) {
                        if (!passableOptional.get().getValue()) {
                            return;
                        }
                    }
                    if (blockedTypes.contains(loc.add(0, -1, 0).getBlockType()) || blockedTypes.contains(loc.getBlockType())) {
                        return;
                    }
                    if (loc.add(0, -1, 0).get(Keys.STAIR_SHAPE).isPresent() || loc.get(Keys.STAIR_SHAPE).isPresent()) {
                        return;
                    }
                    loc.setBlockType(BlockTypes.SNOW_LAYER, Cause.of(NamedCause.owner(this)));
                    Location<World> rounded = new Location<World>(loc.getExtent(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    snowLocs.add(rounded);
                    ParticleEffect effect = registery.createBuilder(ParticleEffect.Builder.class).type(ParticleTypes.SNOWBALL).count(50).build();
                    player.spawnParticles(effect, loc.getPosition(), 32);
                    scheduler.createTaskBuilder().execute(r -> {
                        snowLocs.remove(loc);
                        if (loc.getBlockType().equals(BlockTypes.SNOW_LAYER)) {
                            loc.setBlockType(BlockTypes.AIR, Cause.of(NamedCause.owner(this)));
                        }
                    }).delayTicks(50).submit(this);
                }
            }
        }
    }

    @Listener
    public void onItemDrop(DropItemEvent.Pre event) {
        if (enabled) {
            Optional<BlockSnapshot> blockOptional = event.getCause().first(BlockSnapshot.class);
            if (blockOptional.isPresent()) {
                BlockSnapshot block = blockOptional.get();
                if (block.getState().getType().equals(BlockTypes.SNOW_LAYER)) {
                    if (block.getLocation().isPresent()) {
                        if (snowLocs.contains(block.getLocation().get())) {
                            event.setCancelled(true);
                            snowLocs.remove(block.getLocation().get());
                        }
                    }
                }
            }
        }
    }

    private boolean isEnabled() {
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        logger.info("Current Date: " + (currentMonth + 1) + "-" + currentDay);
        if (currentMonth == 11 || currentMonth == 0) {
            if (currentMonth == 11) {
                if (currentDay >= 18) {
                    logger.info("Winter Wonderland is enabled! The last day it will be enabled is 1-1.");
                    return true;
                }
            } else if (currentMonth == 0) {
                if (currentDay <= 1) {
                    logger.info("Winter Wonderland is enabled! The last day it will be enabled is 1-1.");
                    return true;
                }
            }
        }
        logger.info("Winter Wonderland is disabled! It will be enabled on 12-18.");
        return false;
    }
}
