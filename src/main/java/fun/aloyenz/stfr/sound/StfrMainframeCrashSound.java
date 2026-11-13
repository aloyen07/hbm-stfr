package fun.aloyenz.stfr.sound;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** One nearby frame per listener and reactor event, irrespective of network size. */
public final class StfrMainframeCrashSound {
	private static final Map<World, StfrEventGate> GATES = new WeakHashMap<>();
	private StfrMainframeCrashSound() {}
	public static void play(World world, TileEntityLogicDevice source, int stage) {
		if (world.isRemote) return;
		BlockPos group = source.crashOrigin();
		if (!GATES.computeIfAbsent(world, w -> new StfrEventGate())
				.accept(group.toLong() + ":" + stage, world.getTotalWorldTime())) return;
		SoundEvent sound = stage == 1300 ? StfrFailureSounds.get("alarms.systemoverheat")
				: stage == 1160 ? STFRSounds.BREAKER_SWITCH : STFRSounds.ALARM_2;
		for (EntityPlayer listener : world.playerEntities) {
			if (!(listener instanceof EntityPlayerMP)) continue;
			BlockPos nearest = null;
			double distance = 64D * 64D;
			for (TileEntity tile : world.loadedTileEntityList) {
				if (!(tile instanceof TileEntityLogicDevice) || tile.isInvalid()) continue;
				TileEntityLogicDevice frame = (TileEntityLogicDevice) tile;
				if (frame.crashTicks() <= 0 || !frame.crashOrigin().equals(group)) continue;
				double d = listener.getDistanceSqToCenter(frame.getPos());
				if (d < distance) { distance = d; nearest = frame.getPos(); }
			}
			if (nearest != null) ((EntityPlayerMP) listener).connection.sendPacket(new SPacketSoundEffect(
					sound, SoundCategory.BLOCKS, nearest.getX()+.5, nearest.getY()+.5, nearest.getZ()+.5,
					.9F, stage == 1160 ? .8F : 1F));
		}
	}
}
