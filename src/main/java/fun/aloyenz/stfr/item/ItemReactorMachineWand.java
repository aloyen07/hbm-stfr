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
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.logic.StfrSignal;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;
import fun.aloyenz.stfr.tile.TileEntityMultiFluidPort;

public final class ItemReactorMachineWand extends Item {

	private final boolean reset;

	public ItemReactorMachineWand(boolean reset) {
		this.reset = reset;
		String name = reset ? "reactor_reset_wand" : "reactor_machine_debug_wand";
		setRegistryName(Tags.MODID, name);
		setTranslationKey(Tags.MODID + "." + name);
		setMaxStackSize(1);
		setCreativeTab(STFRBlocks.TAB);
	}

	@Override
	public EnumActionResult onItemUse(
			EntityPlayer player,
			World world,
			BlockPos pos,
			EnumHand hand,
			EnumFacing face,
			float x,
			float y,
			float z) {
		if (world.isRemote) return EnumActionResult.SUCCESS;
		StfrLogicNetwork.Snapshot snapshot = StfrLogicNetwork.inspect(world, pos);
		StfrReactor reactor = snapshot.reactor;
		if (reactor == null && world.getBlockState(pos).getBlock() == STFRBlocks.MULTI_FLUID_PORT)
			reactor = StfrManager.get(world).forPort(pos);
		if (reset) {
			if (reactor == null)
				player.sendMessage(
						new TextComponentTranslation(
								"message.hbmstfr.machine_wand.reset_no_reactor"));
			else if (!player.isSneaking())
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.machine_wand.reset_help"));
			else {
				reactor.emergency().stopMusic(world);
				reactor.emergency().reset();
				reactor.debugReset();
				StfrManager.get(world).markDirty();
				player.sendMessage(
						new TextComponentTranslation("message.hbmstfr.machine_wand.reset_done"));
			}
			return EnumActionResult.SUCCESS;
		}
		player.sendMessage(
				new TextComponentTranslation(
						"message.hbmstfr.machine_wand.network",
						snapshot.issue,
						snapshot.mainframes,
						snapshot.breakers,
						snapshot.ports));
		if (world.getTileEntity(pos) instanceof TileEntityLogicDevice) {
			TileEntityLogicDevice device = (TileEntityLogicDevice) world.getTileEntity(pos);
			StringBuilder faces = new StringBuilder();
			for (EnumFacing side : EnumFacing.VALUES) {
				if (device.signal(side) != StfrSignal.NONE)
					faces.append(' ').append(side.name()).append('=').append(device.signal(side));
			}
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.machine_wand.device",
							device.kind().name(),
							device.kind() == BlockLogicDevice.Kind.BREAKER
									? Boolean.toString(device.breakerClosed())
									: faces.toString()));
		}
		if (world.getTileEntity(pos) instanceof TileEntityMultiFluidPort) {
			TileEntityMultiFluidPort port = (TileEntityMultiFluidPort) world.getTileEntity(pos);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.port.status",
							port.waterAmount(),
							port.lubricantAmount(),
							port.steamAmount(),
							port.productsAmount(),
							port.getPower()));
		}
		if (reactor != null) {
			reactor.refresh(world);
			player.sendMessage(
					new TextComponentTranslation(
							"message.hbmstfr.machine_wand.reactor",
							reactor.id.toString(),
							reactor.runState().name(),
							reactor.stateTicks(),
							reactor.lockState().name(),
							reactor.workingTurbines(),
							reactor.sealFailures(),
							reactor.keyOn(),
							reactor.breakersOn(),
							reactor.fuelRemaining(),
							reactor.waterAmount(),
							reactor.steamAmount(),
							Math.round(reactor.temperature())));
		}
		return EnumActionResult.SUCCESS;
	}
}
