package fun.aloyenz.stfr.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.reactor.StfrComponentStatus;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** Right-click a port for status; sneak-right-click to lock or unlock. */
public final class ItemReactorPortWand extends Item {

	public ItemReactorPortWand() {
		setRegistryName(Tags.MODID, "reactor_port_wand");
		setTranslationKey(Tags.MODID + ".reactor_port_wand");
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public EnumActionResult onItemUse(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumHand hand,
			EnumFacing facing,
			float hitX,
			float hitY,
			float hitZ) {
		if (world.getBlockState(pos).getBlock() != STFRBlocks.MULTI_FLUID_PORT)
			return EnumActionResult.PASS;
		if (world.isRemote) return EnumActionResult.SUCCESS;
		StfrManager manager = StfrManager.get(world);
		StfrReactor reactor = null;
		EnumFacing turbine = null;
		search:
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT) continue;
			EnumFacing direction = StfrRadialSymmetry.sourceDirection(cell.localPos);
			for (int depth = 0; depth <= 1; depth++) {
				BlockPos center =
						pos.subtract(cell.localPos.subtract(StfrStructureTemplate.CENTER))
								.offset(direction.getOpposite(), depth);
				reactor = manager.get(center);
				if (reactor != null) {
					turbine = direction;
					break search;
				}
				BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
				if (!world.isAreaLoaded(origin, origin.add(28, 28, 28))) continue;
				StfrStructureValidator.Result result =
						StfrStructureValidator.inspect(world, center);
				if (result.workingTurbines >= 2) {
					reactor = manager.getOrCreate(center);
					turbine = direction;
					break search;
				}
			}
		}
		if (reactor == null) {
			player.sendMessage(
					new TextComponentTranslation("message.hbmstfr.port_wand.no_reactor"));
			return EnumActionResult.SUCCESS;
		}
		reactor.refresh(world);
		if (player.isSneaking()) {
			if (reactor.lockState() == StfrReactor.LockState.UNLOCKED) {
				if (!reactor.canLock())
					player.sendMessage(
							new TextComponentTranslation(
									"message.hbmstfr.port_wand.cannot_lock",
									reactor.workingTurbines(),
									reactor.blockedChamberAir()));
				else if (!manager.ensureTicket(world, reactor.center))
					player.sendMessage(
							new TextComponentTranslation("message.hbmstfr.port_wand.no_ticket"));
				else if (reactor.requestLock(world))
					player.sendMessage(
							new TextComponentTranslation("message.hbmstfr.port_wand.locking"));
			} else if (reactor.requestUnlock(world)) {
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.port_wand.unlocking"));
			}
			manager.markDirty();
		}
		player.sendMessage(
				new TextComponentTranslation(
						"message.hbmstfr.port_wand.status",
						reactor.id.toString(),
						reactor.lockState().name(),
						reactor.workingTurbines(),
						reactor.wrongBlocks(),
						reactor.blockedAir(),
						reactor.fixedHatchesWrong()));
		player.sendMessage(
				new TextComponentTranslation(
						"message.hbmstfr.port_wand.run_status",
						reactor.runState().name(),
						reactor.stateTicks(),
						reactor.sealFailures(),
						reactor.keyOn(),
						reactor.sstbOn(),
						reactor.breakersOn()));
		player.sendMessage(
				new TextComponentTranslation(
						"message.hbmstfr.port_wand.turbine",
						turbine.name(),
						reactor.turbineState(turbine)));
		StfrComponentStatus component = reactor.componentStatus(turbine);
		if (component != null) {
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.port_wand.components",
							reactor.mainElectrodeEnabled(turbine),
							component.secondaryElectrodesEnabled(),
							(int) Math.round(component.efficiency() * 100D),
							component.sealFailures,
							component.shieldFailures,
							component.lightningRodFailures,
							component.electrodeShieldDegraded()));
			int shown = 0;
			for (StfrComponentStatus.Defect defect : component.defects) {
				if (shown++ >= 4) break;
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.port_wand.defect",
								defect.localPos.getX(),
								defect.localPos.getY(),
								defect.localPos.getZ(),
								defect.radialDepth,
								defect.role.name(),
								defect.reason,
								defect.expected,
								defect.actual));
			}
		}
		if (!reactor.globalDefects().isEmpty()) {
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.port_wand.global_defects",
							reactor.globalDefects().size()));
			int shown = 0;
			for (StfrComponentStatus.Defect defect : reactor.globalDefects()) {
				if (shown++ >= 2) break;
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.port_wand.defect",
								defect.localPos.getX(),
								defect.localPos.getY(),
								defect.localPos.getZ(),
								defect.radialDepth,
								defect.role.name(),
								defect.reason,
								defect.expected,
								defect.actual));
			}
		}
		return EnumActionResult.SUCCESS;
	}
}
