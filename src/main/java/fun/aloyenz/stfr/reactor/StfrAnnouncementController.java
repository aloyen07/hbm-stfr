package fun.aloyenz.stfr.reactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.BlockLogicMainframe;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.logic.StfrMusicPacket;
import fun.aloyenz.stfr.logic.StfrVoicePacket;
import fun.aloyenz.stfr.reactor.StfrReactor.LockState;
import fun.aloyenz.stfr.reactor.StfrReactor.RunState;
import fun.aloyenz.stfr.reactor.StfrReactor.Severity;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.sound.StfrFailureSounds;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

final class StfrAnnouncementController {

	private StfrAnnouncementController() {}

	static void announceTransitions(StfrReactor r, World world) {
		if (r.runState == RunState.STARTUP_CONFIRMED) {
			r.startupMusicAudience.removeIf(id -> world.getPlayerEntityByUUID(id) == null);
			for (EntityPlayer player : world.playerEntities)
				if (r.announcementMusic() && player instanceof EntityPlayerMP
						&& player.getDistanceSq(r.center) <= 64D * 64D
						&& r.startupMusicAudience.add(player.getUniqueID()))
					StfrMusicPacket.send((EntityPlayerMP) player, r.center,
							"hbmstfr:reactor.startup_music", 1, true);
		} else r.startupMusicAudience.clear();
		if (r.announcedState != r.runState) {
			SoundEvent voice;
			String event;
			Severity severity = Severity.INFO;
			switch (r.runState) {
				case STARTUP_CONFIRMED:
					voice = STFRSounds.START_CONFIRM;
					event = "start.confirm";
					severity = Severity.NOTICE;
					break;
				case ONLINE:
					voice =
							r.announcedState == RunState.SHUTDOWN
									? STFRSounds.SHUTDOWN_ABORT
									: STFRSounds.START_COMPLETE;
					event =
							r.announcedState == RunState.SHUTDOWN
									? "shutdown.abort"
									: "start.complete";
					if (r.announcedState == RunState.SHUTDOWN) severity = Severity.WARNING;
					break;
				case STARTUP:
					voice = STFRSounds.START_PRIME;
					event = "start.prime";
					severity = Severity.NOTICE;
					break;
				case SHUTDOWN:
					voice = STFRSounds.SHUTDOWN_PRIME;
					event = "shutdown.prime";
					severity = Severity.NOTICE;
					break;
				default:
					if (r.announcedState == RunState.SHUTDOWN && r.shutdownCompletionAnnounced) {
						r.announcedState = r.runState;
						return;
					}
					voice =
							r.announcedState == RunState.STARTUP
											|| r.announcedState == RunState.STARTUP_CONFIRMED
									? STFRSounds.START_ABORT
									: STFRSounds.SHUTDOWN_COMPLETE;
					event = voice == STFRSounds.START_ABORT ? "start.abort" : "shutdown.complete";
					severity = voice == STFRSounds.START_ABORT ? Severity.WARNING : Severity.INFO;
					break;
			}
			StfrAnnouncementController.announce(r, world, event, severity, voice);
			r.announcedState = r.runState;
		}
		if (r.announcedBreakers != r.breakersOn) {
			if (r.runState == RunState.STARTUP) {
				if (r.breakersOn) {
					world.playSound(
							null,
							r.center,
							STFRSounds.BREAKER_SWITCH,
							SoundCategory.BLOCKS,
							8.0F,
							1.0F);
					world.playSound(
							null,
							r.center,
							STFRSounds.BREAKER_SWITCH,
							SoundCategory.BLOCKS,
							8.0F,
							0.8F);
				}
				StfrAnnouncementController.announce(r, world,
						r.breakersOn ? "start.breakers" : "start.breakers_fail",
						r.breakersOn ? Severity.INFO : Severity.WARNING,
						r.breakersOn ? STFRSounds.START_BREAKERS : STFRSounds.START_BREAKERS_FAIL);
			}
			r.announcedBreakers = r.breakersOn;
		}
		if (r.announcedLock != r.lockState) {
			if (r.lockState == LockState.LOCKED || r.lockState == LockState.UNLOCKED)
				world.playSound(
						null,
						r.center,
						SoundEvents.BLOCK_IRON_DOOR_CLOSE,
						SoundCategory.BLOCKS,
						4.0F,
						1.0F);
			r.announcedLock = r.lockState;
		}
	}

	static void announce(
			StfrReactor r, World world, String event, Severity severity, SoundEvent voice) {
		StfrAnnouncementController.announce(r, world, event, severity, voice, true);
	}

	static void announceDigamma(StfrReactor r, World world, String event) {
		StfrAnnouncementController.announce(r, world, event, Severity.CRITICAL, null, false);
		SoundEvent notification = StfrFailureSounds.get("alarms.notification");
		r.eventSound(world, notification, SoundCategory.AMBIENT, 4F, 1F);
	}

	static void announce(
			StfrReactor r,
			World world,
			String event,
			Severity severity,
			SoundEvent voice,
			boolean alarm) {
		if (r.runState == RunState.OFFLINE && !event.startsWith("start.abort")) return;
		if (alarm
				&& voice == null
				&& (severity == Severity.WARNING || severity == Severity.CRITICAL))
			StfrAnnouncementController.playAlarm(r, world, STFRSounds.ALARM_BEEP, SoundCategory.BLOCKS, 4.0F, 0.75F);
		// Notification comes from every alarm; speech is delivered once per listener.
		if (voice != null)
			StfrAnnouncementController.playAlarm(r, world, StfrFailureSounds.get("alarms.notification"),
					SoundCategory.AMBIENT, 4.0F, 1.0F);
		StfrVoicePacket.send(world, r.center, "message.hbmstfr.reactor." + event);
		if (r.announcementMessages() && world instanceof WorldServer)
			for (EntityPlayer player : world.playerEntities)
				if (player.getDistanceSq(r.center) <= 64 * 64) {
					TextFormatting color =
							severity == Severity.CRITICAL
									? TextFormatting.DARK_RED
									: severity == Severity.WARNING
											? TextFormatting.RED
											: severity == Severity.NOTICE
													? TextFormatting.YELLOW
													: TextFormatting.AQUA;
					player.sendMessage(
							new TextComponentTranslation(
									"message.hbmstfr.reactor.notice",
									new TextComponentTranslation(
													"message.hbmstfr.severity."
															+ severity.name()
																	.toLowerCase(Locale.ROOT))
											.setStyle(new Style().setColor(color)),
									new TextComponentTranslation(
											"message.hbmstfr.reactor." + event)));
				}
	}

	static void playRepeated(
			StfrReactor r,
			World world,
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch,
			int count) {
		for (int i = 0; i < count; i++)
			world.playSound(null, r.center, sound, category, volume, pitch);
	}

	static void playAlarmRepeated(
			StfrReactor r,
			World world,
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch,
			int count) {
		for (int i = 0; i < count; i++) StfrAnnouncementController.playAlarm(r, world, sound, category, volume, pitch);
	}

	static void playAlarm(
			StfrReactor r,
			World world,
			SoundEvent sound,
			SoundCategory category,
			float volume,
			float pitch) {
		for(BlockPos source:alarmSources(r,world))
			world.playSound(null,source,sound,category,volume,pitch);
	}

	static boolean alarmNetworkBelongsTo(StfrReactor reactor,World world,StfrLogicNetwork.Snapshot network) {
		if(network.reactor==reactor)return true;
		for(BlockPos pos:network.devices) {
			TileEntity tile=world.getTileEntity(pos);
			if(tile instanceof TileEntityLogicDevice) {
				TileEntityLogicDevice device=(TileEntityLogicDevice)tile;
				if(device.kind()==BlockLogicDevice.Kind.MAINFRAME && device.crashTicks()>0
						&& device.crashOrigin().equals(reactor.center))return true;
			}
		}
		return false;
	}

	static List<BlockPos> alarmSources(StfrReactor r,World world) {
		long now = world.getTotalWorldTime();
		if (r.alarmMainframe != null
				&& (!world.isBlockLoaded(r.alarmMainframe)
						|| !(world.getBlockState(r.alarmMainframe).getBlock()
								instanceof BlockLogicMainframe)))
			r.alarmSourcesChecked = Long.MIN_VALUE;
		if (r.alarmSourcesChecked == Long.MIN_VALUE || now - r.alarmSourcesChecked >= 20) {
			r.alarmSourcesChecked = now;
			r.alarmMainframe = null;
			r.alarmRelays.clear();
			for (TileEntity tile : world.loadedTileEntityList) {
				if (!(tile instanceof TileEntityLogicDevice) || tile.isInvalid()) continue;
				BlockLogicDevice.Kind kind = ((TileEntityLogicDevice) tile).kind();
				if (kind != BlockLogicDevice.Kind.MAINFRAME && kind != BlockLogicDevice.Kind.ALARM)
					continue;
				StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspect(world, tile.getPos());
				if (!alarmNetworkBelongsTo(r,world,net)) continue;
				if (kind == BlockLogicDevice.Kind.MAINFRAME) {
					if (tile.getPos().equals(net.mainframePos))
						r.alarmMainframe = tile.getPos().toImmutable();
				} else r.alarmRelays.add(tile.getPos().toImmutable());
			}
			if (r.alarmMainframe != null)
				r.alarmRelays.sort(
						Comparator.comparingDouble((BlockPos p) -> p.distanceSq(r.alarmMainframe))
								.thenComparingLong(BlockPos::toLong));
		}
		if (r.alarmMainframe == null
				|| !world.isBlockLoaded(r.alarmMainframe)
				|| !(world.getBlockState(r.alarmMainframe).getBlock()
						instanceof BlockLogicMainframe)) {
			return Collections.singletonList(r.center);
		}
		List<BlockPos> active = new ArrayList<>();
		active.add(r.alarmMainframe);
		for (BlockPos candidate : r.alarmRelays) {
			if (!world.isBlockLoaded(candidate)
					|| world.getBlockState(candidate).getBlock() != STFRBlocks.STFR_ALARM) continue;
			StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, candidate);
			if (network.reactor != r && !network.devices.contains(r.alarmMainframe)) continue;
			active.add(candidate);
		}
		return active;
	}

	static void checkOperatingWarnings(StfrReactor r, World world) {
		StfrOperatingWarnings.checkOperatingWarnings(r, world);
	}

	static void warn(
			StfrReactor r,
			World world,
			int bit,
			boolean trigger,
			boolean clear,
			String event,
			Severity severity) {
		StfrOperatingWarnings.warn(r, world, bit, trigger, clear, event, severity);
	}

	static void tickOperatingAlarms(StfrReactor r, World world) {
		StfrOperatingWarnings.tickOperatingAlarms(r, world);
	}
}
