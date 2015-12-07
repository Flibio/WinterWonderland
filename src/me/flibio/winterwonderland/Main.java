package me.flibio.winterwonderland;

import org.slf4j.Logger;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.data.property.block.SolidCubeProperty;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Plugin(id = "WinterWonderland", name = "Winter Wonderland", version = "1.0.3")
public class Main {
	
	@Inject
	Logger logger;
	
	private GameRegistry registery;
	private Scheduler scheduler;
	
	private CopyOnWriteArrayList<Location<World>> snowLocs = new CopyOnWriteArrayList<Location<World>>();

	private ArrayList<BlockType> blockedTypes = new ArrayList<>(Arrays.asList(BlockTypes.WOODEN_SLAB,BlockTypes.STONE_SLAB,BlockTypes.STONE_SLAB2));
	
	private boolean enabled = false;
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		registery = event.getGame().getRegistry();
		scheduler = event.getGame().getScheduler();

		scheduler.createTaskBuilder().execute(r -> {
			enabled = isEnabled();
		}).interval(10, TimeUnit.MINUTES).submit(this);
	}
	
	@Listener
	public void onPlayerMove(DisplaceEntityEvent.Move.TargetPlayer event) {
		if(enabled) {
			Player player = event.getTargetEntity();
			Location<World> loc = player.getLocation();
			if(loc.getBlockType().equals(BlockTypes.AIR)&&!loc.add(0, -1, 0).getBlockType().equals(BlockTypes.AIR)) {
				Optional<PassableProperty> passableOptional = loc.add(0, 0, 0).getProperty(PassableProperty.class);
				Optional<SolidCubeProperty> solidOptional = loc.add(0, -1, 0).getProperty(SolidCubeProperty.class);
				if(solidOptional.isPresent()) {
					if(!solidOptional.get().getValue()) {
						return;
					}
				}
				if(passableOptional.isPresent()) {
					if(!passableOptional.get().getValue()) {
						return;
					}
				}
				if(blockedTypes.contains(loc.add(0, -1, 0).getBlockType())||blockedTypes.contains(loc.getBlockType())) {
					return;
				}
				if(loc.add(0, -1, 0).get(Keys.STAIR_SHAPE).isPresent()||loc.get(Keys.STAIR_SHAPE).isPresent()) {
					return;
				}
				loc.setBlockType(BlockTypes.SNOW_LAYER);
				Location<World> rounded = new Location<World>(loc.getExtent(),loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
				snowLocs.add(rounded);
				ParticleEffect effect = registery.createBuilder(ParticleEffect.Builder.class).type(ParticleTypes.SNOWBALL).count(50).build();
				player.spawnParticles(effect, loc.getPosition(), 32);	
				scheduler.createTaskBuilder().execute(r -> {
					snowLocs.remove(loc);
					if(loc.getBlockType().equals(BlockTypes.SNOW_LAYER)) {
						loc.setBlockType(BlockTypes.AIR);
					}
				}).delayTicks(50).submit(this);
			}
		}
	}
	
	@Listener
	public void onItemDrop(DropItemEvent.Pre event) {
		if(enabled) {
			Optional<BlockSnapshot> blockOptional = event.getCause().first(BlockSnapshot.class);
			if(blockOptional.isPresent()) {
				BlockSnapshot block = blockOptional.get();
				if(block.getState().getType().equals(BlockTypes.SNOW_LAYER)) {
					if(block.getLocation().isPresent()) {
						if(snowLocs.contains(block.getLocation().get())) {
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
		logger.info("Current Date: "+(currentMonth+1)+"-"+currentDay);
		if(currentMonth==11||currentMonth==0) {
			if(currentMonth==11) {
				if(currentDay>=18) {
					logger.info("Winter Wonderland is enabled! The last day it will be enabled is 1-1.");
					return true;
				}
			} else if(currentMonth==0) {
				if(currentDay<=1) {
					logger.info("Winter Wonderland is enabled! The last day it will be enabled is 1-1.");
					return true;
				}
			}
		}
		logger.info("Winter Wonderland is disabled! It will be enabled on 12-18.");
		return false;
	}
}
