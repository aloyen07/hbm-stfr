package fun.aloyenz.stfr.tile;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import fun.aloyenz.stfr.block.STFRBlocks;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrMiscRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;
import fun.aloyenz.stfr.structure.StfrRotorTemplate;
import fun.aloyenz.stfr.structure.StfrStructureTemplate;

public final class TileEntityStructureAnalyzer extends TileEntity implements ITickable {

	int heightOffset = 1;
	boolean projectionEnabled = true;
	final Map<Long, StfrMiscRole> miscRoles = new HashMap<>();
	final Map<Long, IBlockState> injectorStates = new HashMap<>();
	final Map<Long, StfrBlockRole> roles = new HashMap<>();
	final Set<Long> caseBreach = new HashSet<>();
	// Destruction annotations are deliberately independent from validation roles.
	final Set<Long> destructible = new HashSet<>();
	@Nullable BlockPos liftBoxFirst, liftBoxSecond;
	int liftReleaseY = Integer.MIN_VALUE;
	boolean caseBreachExplicit;

	public boolean isCaseBreach(BlockPos local) {
		if (caseBreachExplicit) return caseBreach.contains(local.toLong());
		StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
		return cell != null && cell.caseBreach;
	}

	public Set<Long> caseBreachBlocks() {
		Set<Long> result = new HashSet<>(caseBreach);
		if (!caseBreachExplicit)
			for (StfrStructureTemplate.Cell cell : StfrStructureTemplate.cells())
				if (cell.caseBreach) result.add(cell.localPos.toLong());
		return result;
	}

	void editCaseBreach(BlockPos local, boolean value) {
		if (!caseBreachExplicit) caseBreach.addAll(caseBreachBlocks());
		caseBreachExplicit = true;
		if (value) caseBreach.add(local.toLong());
		else caseBreach.remove(local.toLong());
	}

	public enum DestructionMark {
		DESTRUCTIBLE,
		UNSIGNED,
		LIFT_BOX_FIRST,
		LIFT_BOX_SECOND,
		RELEASE_HEIGHT
	}

	public boolean isDestructible(BlockPos local) {
		return destructible.contains(local.toLong());
	}

	public Set<Long> destructibleBlocks() {
		return new HashSet<>(destructible);
	}

	public void markDestruction(BlockPos local, DestructionMark mark) {
		if (!containsLocalPos(local)) return;
		switch (mark) {
			case DESTRUCTIBLE:
				destructible.add(local.toLong());
				break;
			case UNSIGNED:
				destructible.remove(local.toLong());
				break;
			case LIFT_BOX_FIRST:
				liftBoxFirst = local;
				break;
			case LIFT_BOX_SECOND:
				liftBoxSecond = local;
				break;
			case RELEASE_HEIGHT:
				liftReleaseY = local.getY();
				break;
			default:
				break;
		}
		sync();
	}

	public boolean lifts(BlockPos local) {
		if (liftBoxFirst == null || liftBoxSecond == null) return false;
		return local.getX() >= Math.min(liftBoxFirst.getX(), liftBoxSecond.getX())
				&& local.getX() <= Math.max(liftBoxFirst.getX(), liftBoxSecond.getX())
				&& local.getY() >= Math.min(liftBoxFirst.getY(), liftBoxSecond.getY())
				&& local.getY() <= Math.max(liftBoxFirst.getY(), liftBoxSecond.getY())
				&& local.getZ() >= Math.min(liftBoxFirst.getZ(), liftBoxSecond.getZ())
				&& local.getZ() <= Math.max(liftBoxFirst.getZ(), liftBoxSecond.getZ());
	}

	public int liftReleaseY() {
		return liftReleaseY == Integer.MIN_VALUE ? 32 : liftReleaseY;
	}

	@Nullable
	public BlockPos liftBoxFirst() {
		return liftBoxFirst;
	}

	@Nullable
	public BlockPos liftBoxSecond() {
		return liftBoxSecond;
	}

	// Rotor annotations supplement structural roles. Face masks allow two steam
	// outlets on the same block without replacing its material/validation role.
	final Set<Long> rotors = new HashSet<>();
	final Map<Long, EnumFacing> axes = new HashMap<>();
	final Map<Long, Integer> particleFaces = new HashMap<>();
	final Map<Long, IBlockState> hiddenRotorStates = new HashMap<>();
	double rotorAngle;
	int rotorVelocity;
	long rotorSyncTick;
	int visualRunState;
	int visualStateTicks;
	boolean rotorMapExplicit;

	public int getHeightOffset() {
		return heightOffset;
	}

	public boolean isProjectionEnabled() {
		return projectionEnabled;
	}

	public void toggleProjection() {
		projectionEnabled = !projectionEnabled;
		if (world != null && !world.isRemote) sync();
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 192D * 192D;
	}

	public BlockPos getOrigin() {
		return pos.add(-14, heightOffset, -14);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		BlockPos origin = getOrigin();
		return new AxisAlignedBB(
				origin,
				origin.add(
						StfrStructureTemplate.SIZE,
						StfrStructureTemplate.SIZE,
						StfrStructureTemplate.SIZE));
	}

	public void advanceHeight() {
		// Moving the analyzer changes every world-space rotor anchor. Restore the
		// old set first so a height adjustment cannot strand invisible blocks.
		heightOffset = heightOffset % 16 + 1;
		if (world != null && !world.isRemote) sync();
	}

	public boolean hasRotor(BlockPos local) {
		return rotors.contains(local.toLong());
	}

	public Set<Long> rotors() {
		return Collections.unmodifiableSet(rotors);
	}

	public Map<Long, EnumFacing> axes() {
		return Collections.unmodifiableMap(axes);
	}

	public Map<Long, Integer> particleFaces() {
		return Collections.unmodifiableMap(particleFaces);
	}

	public Map<Long, IBlockState> hiddenRotorStates() {
		return Collections.unmodifiableMap(hiddenRotorStates);
	}

	public IBlockState originalRotorState(BlockPos local) {
		return hiddenRotorStates.get(local.toLong());
	}

	public double rotorAngle(float partialTicks) {
		long elapsed = world == null ? 0 : Math.max(0, world.getTotalWorldTime() - rotorSyncTick);
		return rotorAngle + (elapsed + partialTicks) * rotorVelocity * 0.001D;
	}

	public int visualRunState() {
		return visualRunState;
	}

	public double visualStateTicks(float partialTicks) {
		long elapsed = world == null ? 0 : Math.max(0, world.getTotalWorldTime() - rotorSyncTick);
		return visualStateTicks + elapsed + partialTicks;
	}

	public void updateRotorSpin(double angle, int velocity, int runState, int stateTicks) {
		StfrAnalyzerRotors.updateRotorSpin(this, angle, velocity, runState, stateTicks);
	}

	public void markRotor(BlockPos local, EnumFacing face, RotorMark mark) {
		StfrAnalyzerRotors.markRotor(this, local, face, mark);
	}

	public enum RotorMark {
		ROTOR,
		AXIS,
		PARTICLE,
		UNSIGNED
	}

	/** Commit annotations without starting the rotor while the reactor is idle. */
	public int saveRotorMap() {
		return StfrAnalyzerRotors.saveRotorMap(this);
	}

	public int syncRotorBlocks(boolean spinning) {
		return StfrAnalyzerRotors.syncRotorBlocks(this, spinning);
	}

	@Override
	public void update() {
		// Runtime visuals are owned by the persistent reactor, not this editor block.
	}

	public void restoreRotorBlocks() {
		StfrAnalyzerRotors.restoreRotorBlocks(this);
	}

	public StfrBlockRole getRole(BlockPos local) {
		StfrBlockRole role = roles.get(local.toLong());
		if (role != null) return role;
		StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
		return cell == null
				? StfrStructureTemplate.airRoles().getOrDefault(local, StfrBlockRole.UNASSIGNED)
				: cell.defaultRole;
	}

	public void setRole(BlockPos local, StfrBlockRole role) {
		if (!containsLocalPos(local))
			throw new IllegalArgumentException("Role outside STFR template: " + local);
		if (role == StfrBlockRole.CASE_BREACH) {
			if (isTemplateCell(local)) editCaseBreach(local, !isCaseBreach(local));
			sync();
			return;
		}
		if (role == StfrBlockRole.UNASSIGNED) editCaseBreach(local, false);
		if (role == StfrBlockRole.UNASSIGNED
				&& !isTemplateCell(local)
				&& !StfrStructureTemplate.airRoles().containsKey(local))
			roles.remove(local.toLong());
		else roles.put(local.toLong(), role);
		sync();
	}

	/** Assign one turbine's role to matching blocks in all six rotated sectors. */
	public int setRadialRole(BlockPos local, @Nullable BlockPos anchor, StfrBlockRole role) {
		if (!containsLocalPos(local)) return 0;
		EnumFacing source = StfrRadialSymmetry.sourceDirection(local);
		if (source == null) {
			setRole(local, role);
			return 1;
		}
		StfrStructureTemplate.Cell sourceCell = StfrStructureTemplate.cellAt(local);
		boolean markBreach = !isCaseBreach(local);
		StfrStructureTemplate.Cell anchorCell =
				anchor == null ? null : StfrStructureTemplate.cellAt(anchor);
		boolean airRole =
				role.requiresAir()
						|| role == StfrBlockRole.UNASSIGNED && getRole(local).requiresAir();
		boolean mustAir =
				role == StfrBlockRole.MUST_AIR
						|| role == StfrBlockRole.UNASSIGNED
								&& getRole(local) == StfrBlockRole.MUST_AIR;
		Map<EnumFacing, BlockPos> targets = StfrRadialSymmetry.copies(local, source);
		Map<EnumFacing, BlockPos> anchors =
				anchorCell == null ? null : StfrRadialSymmetry.copies(anchor, source);
		int changed = 0;
		for (Map.Entry<EnumFacing, BlockPos> entry : targets.entrySet()) {
			BlockPos target = entry.getValue();
			if (!containsLocalPos(target)) continue;
			StfrStructureTemplate.Cell targetCell = StfrStructureTemplate.cellAt(target);
			if (airRole) {
				if (sourceCell != null || targetCell != null) continue;
			} else if (sourceCell == null
					|| targetCell == null
					|| !StfrRadialSymmetry.sameBlockType(
							sourceCell.expected.getBlock(), targetCell.expected.getBlock())) {
				continue;
			}
			if (anchors != null) {
				BlockPos targetAnchor = anchors.get(entry.getKey());
				StfrStructureTemplate.Cell targetAnchorCell =
						StfrStructureTemplate.cellAt(targetAnchor);
				if (targetAnchorCell == null
						|| !StfrRadialSymmetry.sameBlockType(
								targetAnchorCell.expected.getBlock(),
								anchorCell.expected.getBlock())) continue;
			} else if (mustAir && entry.getKey() != source) {
				// Without a matching boundary block there is no safe way to identify
				// a fuel-slot clearance in another turbine.
				continue;
			}
			if (role == StfrBlockRole.CASE_BREACH) {
				if (targetCell == null || isCaseBreach(target) == markBreach) continue;
				editCaseBreach(target, markBreach);
				changed++;
				continue;
			}
			if (role == StfrBlockRole.UNASSIGNED) editCaseBreach(target, false);
			if (getRole(target) == role) continue;
			if (role == StfrBlockRole.UNASSIGNED
					&& targetCell == null
					&& !StfrStructureTemplate.airRoles().containsKey(target))
				roles.remove(target.toLong());
			else roles.put(target.toLong(), role);
			changed++;
		}
		if (changed > 0) sync();
		return changed;
	}

	public boolean containsWorldPos(BlockPos worldPos) {
		return containsLocalPos(worldPos.subtract(getOrigin()));
	}

	public boolean containsLocalPos(BlockPos local) {
		return local.getX() >= 0
				&& local.getX() < 29
				&& local.getY() >= 0
				&& local.getY() < 29
				&& local.getZ() >= 0
				&& local.getZ() < 29;
	}

	public BlockPos toLocal(BlockPos worldPos) {
		return worldPos.subtract(getOrigin());
	}

	public boolean isTemplateCell(BlockPos local) {
		return StfrStructureTemplate.cellAt(local) != null;
	}

	public Map<BlockPos, StfrBlockRole> airRoles() {
		Map<BlockPos, StfrBlockRole> result = new HashMap<>(StfrStructureTemplate.airRoles());
		for (Map.Entry<Long, StfrBlockRole> entry : roles.entrySet()) {
			BlockPos local = BlockPos.fromLong(entry.getKey());
			if (!containsLocalPos(local) || isTemplateCell(local)) continue;
			if (entry.getValue().requiresAir()) result.put(local, entry.getValue());
			else result.remove(local);
		}
		return result;
	}

	public StfrMiscRole getMiscRole(BlockPos local) {
		StfrMiscRole override = miscRoles.get(local.toLong());
		if (override != null) return override;
		StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
		return cell == null ? StfrMiscRole.UNDEFINED : cell.miscRole;
	}

	public IBlockState injectorState(BlockPos local) {
		if (getMiscRole(local) != StfrMiscRole.INJECTOR_PLACEABLE) return null;
		IBlockState override = injectorStates.get(local.toLong());
		if (override != null) return override;
		StfrStructureTemplate.Cell cell = StfrStructureTemplate.cellAt(local);
		return cell == null ? null : cell.injectorState;
	}

	public boolean setMiscRole(BlockPos local, StfrMiscRole role) {
		if (!isTemplateCell(local)) return false;
		IBlockState actual = world.getBlockState(getOrigin().add(local));
		if (role == StfrMiscRole.INJECTOR_PLACEABLE
				&& actual.getBlock() != STFRBlocks.SOLID_INJECTOR) return false;
		miscRoles.put(local.toLong(), role);
		injectorStates.remove(local.toLong());
		if (role == StfrMiscRole.INJECTOR_PLACEABLE) injectorStates.put(local.toLong(), actual);
		sync();
		return true;
	}

	public void exportStructure(EntityPlayer player) {
		File output = saveRoles();
		player.sendMessage(
				new TextComponentTranslation("message.hbmstfr.analyzer.saved", output.getPath()));
	}

	public void audit(EntityPlayer player, boolean save) {
		StfrAnalyzerAudit.audit(this, player, save);
	}

	/** The same counts are used by the server audit and the wand's client HUD. */
	public AuditSummary inspect() {
		return StfrAnalyzerAudit.inspect(this);
	}

	public static final class AuditSummary {

		public final int wrong;
		public final int unassigned;

		AuditSummary(int wrong, int unassigned) {
			this.wrong = wrong;
			this.unassigned = unassigned;
		}
	}

	public static boolean matches(
			IBlockState expected, IBlockState actual, IBlockAccess blocks, BlockPos pos) {
		return StfrAnalyzerMatching.matches(expected, actual, blocks, pos);
	}

	static int innerCorner(IBlockState state) {
		return StfrAnalyzerMatching.innerCorner(state);
	}

	File saveRoles() {
		return StfrAnalyzerExport.saveRoles(this);
	}

	void sync() {
		StfrLogicNetwork.invalidate(world);
		markDirty();
		IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 3);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		return StfrAnalyzerPersistence.writeToNBT(this, compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		StfrAnalyzerPersistence.readFromNBT(this, compound);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (!rotorMapExplicit && rotors.isEmpty() && axes.isEmpty() && particleFaces.isEmpty())
			StfrRotorTemplate.populate(rotors, axes, particleFaces);
		rotorSyncTick = world.getTotalWorldTime();
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		readFromNBT(packet.getNbtCompound());
		if (world != null) {
			AxisAlignedBB bounds = getRenderBoundingBox();
			world.markBlockRangeForRenderUpdate(
					new BlockPos(bounds.minX, bounds.minY, bounds.minZ),
					new BlockPos(bounds.maxX, bounds.maxY, bounds.maxZ));
		}
	}
}
