package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrDigammaController.*;

import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.ContaminationUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.sound.StfrFailureSounds;
import fun.aloyenz.stfr.sound.StfrSceneSounds;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Digamma containment damage, radiation and acoustic feedback. */
final class StfrDigammaContainment {
	private StfrDigammaContainment() {}

	static void tickDigammaRadiation(StfrReactor r, World world) {
		int missing = r.missingDigammaGlass(world);
		double rad = 10D + 15D * missing, drx = .001D + .005D * missing;
		for (EntityPlayer player : world.playerEntities) {
			if (player.isSpectator() || player.getDistanceSq(r.center) > 81D) continue;
			ContaminationUtil.contaminate(
					player,
					ContaminationUtil.HazardType.RADIATION,
					ContaminationUtil.ContaminationType.CREATIVE,
					rad / 20D);
			ContaminationUtil.contaminate(
					player,
					ContaminationUtil.HazardType.DIGAMMA,
					ContaminationUtil.ContaminationType.DIGAMMA2,
					drx / 20D);
		}
	}

	static void tickDigammaSoundscape(StfrReactor r, World world) {
		if (r.digammaHumCountdown <= 0) r.digammaHumCountdown = 50 + world.rand.nextInt(31);
		if (--r.digammaHumCountdown == 0)
			world.playSound(null, r.center, STFRSounds.HUMMING_HIGH, SoundCategory.AMBIENT, 8F, 1F);
		if (++r.digammaAmbientTicks < 600) return;
		r.digammaAmbientTicks = 0;
		StfrDigammaSoundscape.Cue cue =
				StfrDigammaSoundscape.select(
						world.rand.nextDouble(), StfrDigammaController.hasBrokenPolaroidNearby(r, world));
		switch (cue) {
			case HUM:
				world.playSound(
						null, r.center, STFRSounds.HUMMING_HIGH, SoundCategory.AMBIENT, 8F, 1F);
				break;
			case PURGE:
				world.playSound(
						null,
						r.center,
						StfrSceneSounds.get("dem.purge"),
						SoundCategory.AMBIENT,
						10F,
						1F);
				break;
			case ROCKET:
				world.playSound(
						null,
						r.center,
						StfrSceneSounds.get("explosion.rocketfly"),
						SoundCategory.AMBIENT,
						10F,
						1F);
				break;
			case POWER_PURGE:
				world.playSound(
						null,
						r.center,
						StfrFailureSounds.get("powerpurge"),
						SoundCategory.AMBIENT,
						10F,
						1F);
				break;
			case SCREAM:
				r.playDigammaScream(world, false);
				break;
			case SECRET_SCREAM:
				r.playDigammaScream(world, true);
				break;
			default:
				break;
		}
	}

	static boolean hasBrokenPolaroidNearby(StfrReactor r, World world) {
		// HBM CE represents the corrupted/broken polaroid as variant 11 of its global polaroid.
		if (MainRegistry.polaroidID != 11) return false;
		for (EntityPlayer player : world.playerEntities)
			if (!player.isSpectator()
					&& player.getDistanceSq(r.center) <= 160D * 160D
					&& player.inventory.hasItemStack(new ItemStack(ModItems.polaroid))) return true;
		return false;
	}

	static void playDigammaScream(StfrReactor r, World world, boolean brokenPolaroid) {
		if (world.isRemote) return;
		world.playSound(
				null,
				r.center,
				brokenPolaroid
						? STFRSounds.DIGAMMA_SCREAM_SECRET
						: STFRSounds.DIGAMMA_SCREAM_NORMAL,
				SoundCategory.AMBIENT,
				12F,
				1F);
	}

	static int missingDigammaGlass(StfrReactor r, World world) {
		int missing = 0;
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (long key : r.destructiblePositions) {
			BlockPos local = BlockPos.fromLong(key), pos = origin.add(local);
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			if (cell != null
					&& !StfrStructureValidator.matchesCell(
							cell, cell.defaultRole, world.getBlockState(pos), world, pos))
				missing++;
		}
		return missing;
	}

	static void destroyDigammaGlass(StfrReactor r, World world) {
		BlockPos origin = r.center.subtract(StfrStructureTemplate.CENTER);
		for (long key : r.destructiblePositions) {
			BlockPos local = BlockPos.fromLong(key), pos = origin.add(local);
			StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
			if (cell == null) continue;
			IBlockState state = world.getBlockState(pos);
			if (state.getBlockHardness(world, pos) >= 0
					&& StfrStructureValidator.matchesCell(
							cell, cell.defaultRole, state, world, pos)) world.setBlockToAir(pos);
		}
	}

	static void finishDigammaShutdown(StfrReactor r, World world) {
		StfrDigammaController.scatterDigammaProduct(r, world,
				ModItems.nugget_polonium,
				(int) ((r.poloniumMb() + r.digammaMatter.legacyPoloniumMb()) / 16D));
		StfrDigammaController.scatterDigammaProduct(r, world,
				STFRItems.YARONITE_DUST_TINY,
				(int)
						((r.yaroniteMb()
										+ r.digammaMatter.legacyYaroniteMb()
										+ r.resources.yaronite().getFill())
								/ 16D));
		r.resources.yaronite().setFill(0);
		r.digammaMatter.clear();
		r.digammaActive = false;
		r.destabilizerStage = 0;
		r.digammaCharge = 0;
		r.digammaSettlingTicks = 0;
	}

	static void scatterDigammaProduct(StfrReactor r, World world, Item item, int count) {
		while (count-- > 0) {
			EntityItem drop =
					new EntityItem(
							world,
							r.center.getX() + .5 + world.rand.nextGaussian() * 2.5,
							r.center.getY() + .5 + world.rand.nextGaussian() * 2,
							r.center.getZ() + .5 + world.rand.nextGaussian() * 2.5,
							new ItemStack(item));
			drop.motionX = world.rand.nextGaussian() * .15;
			drop.motionY = .15 + world.rand.nextDouble() * .2;
			drop.motionZ = world.rand.nextGaussian() * .15;
			world.spawnEntity(drop);
		}
	}
}
