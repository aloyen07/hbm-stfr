package fun.aloyenz.stfr.tile;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.block.BlockFuelSlot;
import fun.aloyenz.stfr.block.BlockSolidInjector;
import fun.aloyenz.stfr.test.StfrDeviceTestWorld;

final class StfrInjectionInterlockTest {
	private static final BlockPos TARGET = new BlockPos(0, 64, 0);

	@BeforeAll static void bootstrap() {
		Bootstrap.register();
		TileEntity.register("hbmstfr:fuel_slot", TileEntityFuelSlot.class);
		TileEntity.register("hbmstfr:solid_injector", TileEntitySolidInjector.class);
	}

	private TileEntityFuelSlot slot(StfrDeviceTestWorld world, BlockPos target, EnumFacing side) {
		TileEntityFuelSlot slot = new TileEntityFuelSlot();
		ItemStack capsule = new ItemStack(Items.STICK);
		NBTTagCompound contents = new NBTTagCompound();
		contents.setInteger("testFuelRemaining", 12345);
		capsule.setTagCompound(contents);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setByte("phase", TileEntityFuelSlot.READY);
		tag.setTag("capsule", capsule.writeToNBT(new NBTTagCompound()));
		slot.readFromNBT(tag);
		world.put(target.offset(side), Blocks.DISPENSER.getDefaultState()
				.withProperty(BlockFuelSlot.FACING, side.getOpposite()), slot);
		return slot;
	}

	private TileEntitySolidInjector injector(StfrDeviceTestWorld world, BlockPos target, EnumFacing side) {
		TileEntitySolidInjector injector = new TileEntitySolidInjector();
		injector.configure(2, 600, TileEntitySolidInjector.RedstoneMode.HIGH.ordinal());
		world.put(target.offset(side), Blocks.FURNACE.getDefaultState()
				.withProperty(BlockSolidInjector.FACING, side.getOpposite()), injector);
		return injector;
	}

	@Test void newlyLockedSlotBreaksAndDropsCapsuleWithItsContents() {
		StfrDeviceTestWorld world = new StfrDeviceTestWorld();
		TileEntitySolidInjector injector = injector(world, TARGET, EnumFacing.EAST);
		world.powered.add(injector.getPos());
		injector.update();
		TileEntityFuelSlot slot = slot(world, TARGET, EnumFacing.NORTH);
		slot.lockForFilling();
		assertEquals(slot.getPos(), world.broken.get(0));
		assertSame(injector, world.getTileEntity(injector.getPos()));
		EntityItem drop = (EntityItem) world.drops.get(0);
		assertEquals(12345, drop.getItem().getTagCompound().getInteger("testFuelRemaining"));
		assertEquals(0, drop.getItem().getItemDamage());
	}

	@Test void newlyPoweredInjectorBreaksWhileLockedSlotSurvives() {
		StfrDeviceTestWorld world = new StfrDeviceTestWorld();
		TileEntityFuelSlot slot = slot(world, TARGET, EnumFacing.UP);
		slot.lockForFilling();
		TileEntitySolidInjector injector = injector(world, TARGET, EnumFacing.WEST);
		injector.update();
		assertTrue(world.broken.isEmpty());
		world.powered.add(injector.getPos());
		injector.update();
		assertEquals(injector.getPos(), world.broken.get(0));
		assertTrue(slot.isLocked());
	}

	@Test void disablingInjectorFreesTheTargetImmediatelyAndDifferentTargetsStayIndependent() {
		StfrDeviceTestWorld world = new StfrDeviceTestWorld();
		TileEntitySolidInjector injector = injector(world, TARGET, EnumFacing.EAST);
		world.powered.add(injector.getPos());
		injector.update();
		TileEntityFuelSlot independent = slot(world, TARGET.down(12), EnumFacing.NORTH);
		independent.lockForFilling();
		assertTrue(world.broken.isEmpty());
		world.powered.clear();
		TileEntityFuelSlot slot = slot(world, TARGET, EnumFacing.NORTH);
		slot.lockForFilling();
		assertTrue(slot.isLocked());
		assertTrue(world.broken.isEmpty());
		world.powered.add(injector.getPos());
		injector.update();
		assertEquals(injector.getPos(), world.broken.get(0));
	}

	@Test void persistedOwnerWinsEvenWhenTheLaterDeviceUpdatesFirst() {
		StfrDeviceTestWorld world = new StfrDeviceTestWorld();
		TileEntityFuelSlot slot = slot(world, TARGET, EnumFacing.NORTH);
		slot.lockForFilling();
		NBTTagCompound saved = slot.writeToNBT(new NBTTagCompound());
		TileEntityFuelSlot reloaded = slot(world, TARGET, EnumFacing.NORTH);
		reloaded.readFromNBT(saved);
		assertTrue(reloaded.isLocked());
		TileEntitySolidInjector injector = injector(world, TARGET, EnumFacing.EAST);
		world.powered.add(injector.getPos());
		injector.update();
		assertEquals(injector.getPos(), world.broken.get(0));
		assertSame(reloaded, world.getTileEntity(reloaded.getPos()));
	}

	@Test void simultaneousSourcesHaveOneSurvivor() {
		StfrDeviceTestWorld world = new StfrDeviceTestWorld();
		TileEntitySolidInjector first = injector(world, TARGET, EnumFacing.EAST);
		TileEntitySolidInjector second = injector(world, TARGET, EnumFacing.WEST);
		world.powered.add(first.getPos());
		world.powered.add(second.getPos());
		first.update();
		second.update();
		assertEquals(1, world.broken.size());
		assertSame(first, world.getTileEntity(first.getPos()));
	}
}
