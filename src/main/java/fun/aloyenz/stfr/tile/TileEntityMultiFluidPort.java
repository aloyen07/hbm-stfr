package fun.aloyenz.stfr.tile;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import fun.aloyenz.stfr.api.data.IDataMachine;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.reactor.StfrFluids;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.StfrResourceNetwork;
import fun.aloyenz.stfr.reactor.StfrStructureValidator;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

/** A port is an access point to reactor-wide storage. Legacy local tanks migrate once. */
public final class TileEntityMultiFluidPort extends TileEntity
		implements ITickable,
				IDataMachine,
				IFluidStandardTransceiverMK2,
				IEnergyReceiverMK2,
				IEnergyProviderMK2 {
	private final FluidTankNTM oldWater = new FluidTankNTM(Fluids.WATER, 600_000).withOwner(this);
	private final FluidTankNTM oldLube = new FluidTankNTM(Fluids.LUBRICANT, 1_000).withOwner(this);
	private final FluidTankNTM oldSteam =
			new FluidTankNTM(Fluids.SUPERHOTSTEAM, 14_000_000).withOwner(this);
	private final FluidTankNTM oldProducts =
			new FluidTankNTM(Fluids.HELIUM4, 100_000).withOwner(this);
	private long oldPower;
	private transient boolean migrated;
	private transient long networkTick = Long.MIN_VALUE;
	private transient StfrResourceNetwork cachedNetwork;

	private StfrReactor reactor() {
		return world == null ? null : StfrManager.get(world).forPort(pos);
	}

	private StfrResourceNetwork network() {
		if (world == null) return null;
		long tick = world.getTotalWorldTime();
		if (networkTick == tick) return cachedNetwork;
		networkTick = tick;
		cachedNetwork = null;
		StfrReactor r = reactor();
		if (r == null || !StfrLogicNetwork.portAccess(world, r, pos)) return null;
		StfrResourceNetwork n = r.resources();
		if (!migrated && !world.isRemote) {
			n.add(n.water(), oldWater.getFill());
			n.add(n.lubricant(), oldLube.getFill());
			n.add(n.steam(), oldSteam.getFill());
			n.add(n.helium4(), oldProducts.getFill());
			n.setEnergy(n.energy() + oldPower);
			oldWater.setFill(0);
			oldLube.setFill(0);
			oldSteam.setFill(0);
			oldProducts.setFill(0);
			oldPower = 0;
			migrated = true;
			dirty();
		}
		cachedNetwork = n;
		return n;
	}

	private boolean isolated() {
		return world == null || !world.isRemote && (reactor() == null || network() == null);
	}

	private FluidTankNTM water() {
		StfrResourceNetwork n = network();
		return n == null ? oldWater : n.water();
	}

	private FluidTankNTM lube() {
		StfrResourceNetwork n = network();
		return n == null ? oldLube : n.lubricant();
	}

	private FluidTankNTM steam() {
		StfrResourceNetwork n = network();
		return n == null ? oldSteam : n.steam();
	}

	private FluidTankNTM products() {
		StfrResourceNetwork n = network();
		return n == null ? oldProducts : n.helium4();
	}

	private FluidTankNTM yaronite() {
		StfrResourceNetwork n = network();
		return n == null ? null : n.yaronite();
	}

	private void dirty() {
		markDirty();
		if (world != null && !world.isRemote) StfrManager.get(world).markDirty();
	}

	private boolean steamOutputAllowed() {
		StfrReactor r = reactor();
		return r == null || !r.catastrophic();
	}

	private final IFluidHandler forgeFluids =
			new IFluidHandler() {
				@Override
				public IFluidTankProperties[] getTankProperties() {
					return isolated()
							? new IFluidTankProperties[0]
							: new IFluidTankProperties[] {
								property(water(), true, false), property(lube(), true, false),
								property(steam(), false, steamOutputAllowed()),
										property(products(), false, true),
								property(yaronite(), false, true)
							};
				}

				@Override
				public int fill(FluidStack fluid, boolean doFill) {
					if (isolated() || fluid == null || fluid.amount <= 0) return 0;
					FluidTankNTM tank =
							fluid.getFluid() == Fluids.WATER.getFF()
									? water()
									: fluid.getFluid() == Fluids.LUBRICANT.getFF() ? lube() : null;
					if (tank == null) return 0;
					int amount = Math.min(fluid.amount, tank.getMaxFill() - tank.getFill());
					if (doFill && amount > 0) {
						tank.setFill(tank.getFill() + amount);
						dirty();
					}
					return amount;
				}

				@Override
				public FluidStack drain(FluidStack fluid, boolean doDrain) {
					if (isolated() || fluid == null || fluid.amount <= 0) return null;
					return drainTank(
							fluid.getFluid() == Fluids.STEAM.getFF()
									? steam()
									: fluid.getFluid() == Fluids.HELIUM4.getFF()
											? products()
											: fluid.getFluid() == StfrFluids.YARONITE.getFF()
													? yaronite()
													: null,
							fluid.amount,
							doDrain);
				}

				@Override
				public FluidStack drain(int amount, boolean doDrain) {
					return isolated()
							? null
							: drainTank(
									steam().getFill() > 0
											? steam()
											: products().getFill() > 0 ? products() : yaronite(),
									amount,
									doDrain);
				}
			};

	private static IFluidTankProperties property(FluidTankNTM tank, boolean fill, boolean drain) {
		if (tank == null) return new FluidTankProperties(null, 0, false, false);
		return new FluidTankProperties(
				new FluidStack(tank.getTankType().getFF(), tank.getFill()),
				tank.getMaxFill(),
				fill,
				drain);
	}

	private FluidStack drainTank(FluidTankNTM tank, int requested, boolean doDrain) {
		if (tank == steam() && !steamOutputAllowed()) return null;
		if (tank == null || requested <= 0 || tank.getFill() <= 0) return null;
		int amount = Math.min(requested, tank.getFill());
		if (doDrain) {
			tank.setFill(tank.getFill() - amount);
			dirty();
		}
		return new FluidStack(tank.getTankType().getFF(), amount);
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) return;
		if (!isolated())
			for (EnumFacing side : EnumFacing.VALUES) {
				BlockPos neighbour = pos.offset(side);
				ForgeDirection direction = ForgeDirection.getOrientation(side);
				trySubscribe(Fluids.WATER, world, neighbour, direction);
				trySubscribe(Fluids.LUBRICANT, world, neighbour, direction);
				if (reactor() == null || !reactor().catastrophic())
					tryProvide(steam(), world, neighbour, direction);
				tryProvide(products(), world, neighbour, direction);
				tryProvide(yaronite(), world, neighbour, direction);
				StfrReactor reactor = reactor();
				if (reactor != null
						&& reactor.runState() == StfrReactor.RunState.ONLINE
						&& !reactor.destabilizerAcceptsPower())
					tryProvide(world, neighbour, direction);
				else trySubscribe(world, neighbour, direction);
			}
		if (world.getTotalWorldTime() % 20 == 0 && network() != null)
			StfrManager.get(world).markDirty();
		if ((world.getTotalWorldTime() + pos.toLong()) % 100 != 0) return;
		StfrManager manager = StfrManager.get(world);
		for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells()) {
			if (cell.defaultRole != StfrBlockRole.PORT) continue;
			BlockPos center = pos.subtract(cell.localPos.subtract(StfrStructureTemplate.CENTER));
			if (manager.get(center) != null) return;
			BlockPos origin = center.subtract(StfrStructureTemplate.CENTER);
			if (world.isAreaLoaded(origin, origin.add(28, 28, 28))
					&& StfrStructureValidator.inspect(world, center).workingTurbines >= 2) {
				manager.getOrCreate(center).tick(world);
				return;
			}
		}
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection direction) {
		return !isolated()
				&& direction != ForgeDirection.UNKNOWN
				&& (type == Fluids.WATER
						|| type == Fluids.STEAM
						|| type == Fluids.LUBRICANT
						|| type == Fluids.HELIUM4
						|| type == StfrFluids.YARONITE);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
				|| super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
				? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(forgeFluids)
				: super.getCapability(capability, facing);
	}

	@Override
	public boolean isLoaded() {
		return world != null && world.isBlockLoaded(pos);
	}

	private static FluidTankNTM[] tanks(FluidTankNTM... values) {
		return Arrays.stream(values).filter(Objects::nonNull).toArray(FluidTankNTM[]::new);
	}

	@Override
	public FluidTankNTM[] getAllTanks() {
		return isolated()
				? new FluidTankNTM[0]
				: tanks(water(), lube(), steam(), products(), yaronite());
	}

	@Override
	public FluidTankNTM[] getReceivingTanks() {
		return isolated() ? new FluidTankNTM[0] : new FluidTankNTM[] {water(), lube()};
	}

	@Override
	public FluidTankNTM[] getSendingTanks() {
		return isolated()
				? new FluidTankNTM[0]
				: steamOutputAllowed()
						? tanks(steam(), products(), yaronite())
						: tanks(products(), yaronite());
	}

	@Override
	public long getPower() {
		if (isolated()) return 0;
		StfrResourceNetwork n = network();
		return n == null ? oldPower : n.energy();
	}

	@Override
	public void setPower(long value) {
		StfrResourceNetwork n = network();
		if (isolated()) return;
		StfrReactor reactor = reactor();
		if (reactor != null && reactor.energyLocked()) return;
		if (n != null) n.setEnergy(value);
		else if (reactor() == null) oldPower = Math.max(0, Math.min(getMaxPower(), value));
		dirty();
	}

	@Override
	public long transferPower(long amount, boolean simulate) {
		if (isolated()) return amount;
		return IEnergyReceiverMK2.super.transferPower(amount, simulate);
	}

	@Override
	public long getReceiverSpeed() {
		return isolated() ? 0 : getMaxPower();
	}

	@Override
	public long getMaxPower() {
		if (isolated()) return 0;
		StfrResourceNetwork n = network();
		return n == null ? StfrResourceNetwork.STARTUP_ENERGY_CAPACITY : n.energyCapacity();
	}

	@Override
	public long getProviderSpeed() {
		StfrResourceNetwork n = network();
		return n == null ? 0 : n.outputBudget();
	}

	@Override
	public boolean allowDirectProvision() {
		if (isolated()) return false;
		StfrReactor reactor = reactor();
		return reactor == null
				|| reactor.runState() != StfrReactor.RunState.ONLINE
				|| reactor.destabilizerAcceptsPower();
	}

	@Override
	public void usePower(long amount) {
		StfrResourceNetwork n = network();
		if (n != null) n.useOutput(amount);
		dirty();
	}

	public int waterAmount() {
		return isolated() ? 0 : water().getFill();
	}

	public int steamAmount() {
		return isolated() ? 0 : steam().getFill();
	}

	public int steamSpace() {
		return isolated() ? 0 : steam().getMaxFill() - steam().getFill();
	}

	public int productsAmount() {
		return isolated() ? 0 : products().getFill();
	}

	public int productsSpace() {
		return isolated() ? 0 : products().getMaxFill() - products().getFill();
	}

	public int lubricantAmount() {
		return isolated() ? 0 : lube().getFill();
	}

	public int takeWater(int amount) {
		return take(water(), amount);
	}

	public int takeLubricant(int amount) {
		return take(lube(), amount);
	}

	public int addSteam(int amount) {
		return add(steam(), amount);
	}

	public int addProducts(int amount) {
		return add(products(), amount);
	}

	private int take(FluidTankNTM tank, int amount) {
		if (isolated()) return 0;
		int n = Math.min(Math.max(0, amount), tank.getFill());
		if (n > 0) {
			tank.setFill(tank.getFill() - n);
			dirty();
		}
		return n;
	}

	private int add(FluidTankNTM tank, int amount) {
		if (isolated()) return 0;
		int n = Math.min(Math.max(0, amount), tank.getMaxFill() - tank.getFill());
		if (n > 0) {
			tank.setFill(tank.getFill() + n);
			dirty();
		}
		return n;
	}

	public long takePower(long amount) {
		if (isolated()) return 0;
		long n = Math.min(Math.max(0, amount), getPower());
		setPower(getPower() - n);
		return n;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		oldWater.writeToNBT(tag, "stfrWater");
		oldLube.writeToNBT(tag, "stfrLubricant");
		oldSteam.writeToNBT(tag, "stfrSteam");
		oldProducts.writeToNBT(tag, "stfrProducts");
		tag.setLong("stfrPower", oldPower);
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		networkTick = Long.MIN_VALUE;
		cachedNetwork = null;
		oldWater.readFromNBT(tag, "stfrWater");
		oldLube.readFromNBT(tag, "stfrLubricant");
		oldSteam.readFromNBT(tag, "stfrSteam");
		oldProducts.readFromNBT(tag, "stfrProducts");
		oldWater.setTankType(Fluids.WATER);
		oldLube.setTankType(Fluids.LUBRICANT);
		oldSteam.setTankType(Fluids.SUPERHOTSTEAM);
		oldProducts.setTankType(Fluids.HELIUM4);
		oldPower =
				Math.max(
						0,
						Math.min(
								StfrResourceNetwork.STARTUP_ENERGY_CAPACITY,
								tag.getLong("stfrPower")));
		migrated = false;
	}
}
