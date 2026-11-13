package fun.aloyenz.stfr.block;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fun.aloyenz.stfr.STFR;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.item.STFRItems;
import fun.aloyenz.stfr.logic.StfrAlarmOpenPacket;
import fun.aloyenz.stfr.logic.StfrAutopilotOpenPacket;
import fun.aloyenz.stfr.logic.StfrDestabilizerOpenPacket;
import fun.aloyenz.stfr.logic.StfrLogicNetwork;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrMainframeOpenPacket;
import fun.aloyenz.stfr.logic.StfrSuppressorPacket;
import fun.aloyenz.stfr.reactor.StfrAutopilot;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrDetonatorProgramming;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

public class BlockLogicDevice extends Block implements ITileEntityProvider {

	public enum Kind {
		MAINFRAME,
		TRANSMITTER,
		RECEIVER,
		MONITOR,
		BREAKER,
		ALARM,
		CRASH_SUPPRESSOR,
		PRIVILEGES,
		DETONATOR_CONNECTOR,
		AUTOPILOT_1,
		AUTOPILOT_2,
		AUTOPILOT_3,
		DESTABILIZER
	}

	public static final PropertyBool CLOSED = PropertyBool.create("closed");
	private static final AxisAlignedBB MONITOR_BOX = new AxisAlignedBB(0, 0, 0, 1, 0.125, 1);
	private final Kind kind;

	public BlockLogicDevice(String name, Kind kind) {
		super(Material.IRON);
		this.kind = kind;
		setRegistryName(Tags.MODID, name);
		setTranslationKey(Tags.MODID + "." + name);
		setHardness(3);
		if (kind == Kind.CRASH_SUPPRESSOR) {
			setBlockUnbreakable();
			setResistance(6_000_000);
		}
		setCreativeTab(STFRBlocks.TAB);
		setDefaultState(blockState.getBaseState().withProperty(CLOSED, false));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);
		String key = switch (kind) {
			case AUTOPILOT_1 -> "tooltip.hbmstfr.autopilot_1";
			case AUTOPILOT_2 -> "tooltip.hbmstfr.autopilot_2";
			case AUTOPILOT_3 -> "tooltip.hbmstfr.autopilot_3";
			default -> null;
		};
		if (key != null)
			tooltip.add(TextFormatting.GRAY + new TextComponentTranslation(key).getFormattedText());
	}

	public Kind kind() {
		return kind;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, CLOSED);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(CLOSED, (meta & 1) != 0);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(CLOSED) ? 1 : 0;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityLogicDevice();
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return kind != Kind.MONITOR;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return kind != Kind.MONITOR;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return kind == Kind.MONITOR ? MONITOR_BOX : FULL_BLOCK_AABB;
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		return super.canPlaceBlockAt(world, pos);
	}

	@Override
	public void neighborChanged(
			IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos neighborPos) {
		if (kind == Kind.MONITOR && !canPlaceBlockAt(world, pos)) world.destroyBlock(pos, true);
	}

	@Override
	public boolean onBlockActivated(
			World world,
			BlockPos pos,
			IBlockState state,
			EntityPlayer player,
			EnumHand hand,
			EnumFacing side,
			float x,
			float y,
			float z) {
		if (kind == Kind.MAINFRAME
				&& (player.getHeldItem(hand).getItem() == STFRItems.NETWORK_LOAD_ANALYZER
						|| (player.getHeldItem(hand).getItem()
												== Item.getItemFromBlock(STFRBlocks.LOGIC_MAINFRAME)
										|| player.getHeldItem(hand).getItem()
												== Item.getItemFromBlock(STFRBlocks.MAINFRAME))
								&& side.getAxis().isVertical())) return false;
		if (player.getHeldItem(hand).getItem() == STFRItems.KEY_CARD
				|| player.getHeldItem(hand).getItem() == STFRItems.DETONATOR) return false;
		if (!world.isRemote
				&& !StfrSecurity.allows(
						player,
						world,
						pos,
						kind == Kind.PRIVILEGES
								? StfrAccessPolicy.ADMIN
								: StfrAccessPolicy.CONTROL)) {
			StfrSecurity.message(player, "denied");
			return true;
		}
		if (kind == Kind.PRIVILEGES) {
			if (!world.isRemote)
				player.openGui(STFR.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
			return true;
		}
		if (player.getHeldItem(hand).getItem() == STFRItems.SIGNAL_MODIFIER) return false;
		if (kind == Kind.RECEIVER
				&& side != EnumFacing.DOWN
				&& player.getHeldItem(hand).getItem()
						== Item.getItemFromBlock(STFRBlocks.LOGIC_MONITOR)) {
			BlockPos target = pos.offset(side);
			if (!world.mayPlace(STFRBlocks.LOGIC_MONITOR, target, false, side, player)
					|| !player.canPlayerEdit(target, side, player.getHeldItem(hand))) return true;
			if (!world.isRemote) {
				world.setBlockState(
						target,
						STFRBlocks.LOGIC_MONITOR
								.getDefaultState()
								.withProperty(
										BlockLogicMonitor.FACING,
										side == EnumFacing.UP
												? player.getHorizontalFacing().getOpposite()
												: side)
								.withProperty(BlockLogicMonitor.WALL, side != EnumFacing.UP),
						3);
				if (!player.capabilities.isCreativeMode) player.getHeldItem(hand).shrink(1);
			}
			return true;
		}
		if (kind == Kind.CRASH_SUPPRESSOR) {
			if (!world.isRemote && player instanceof EntityPlayerMP)
				StfrLogicPacket.CHANNEL.sendTo(
						new StfrSuppressorPacket(
								pos,
								((TileEntityLogicDevice) world.getTileEntity(pos))
										.suppressionMask()),
						(EntityPlayerMP) player);
			return true;
		}
		if (kind == Kind.ALARM) {
			if (!world.isRemote && player instanceof EntityPlayerMP) {
				StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, pos);
				if (!network.ioAvailable || !network.deviceAvailable(pos)
						|| network.reactor == null) {
					StfrSecurity.message(player, "no_network");
					return true;
				}
				StfrReactor reactor = network.reactor;
				StfrLogicPacket.CHANNEL.sendTo(new StfrAlarmOpenPacket(pos,
						reactor.announcementMessages(), reactor.announcementVoice(),
						reactor.announcementMusic()), (EntityPlayerMP) player);
			}
			return true;
		}
		if (kind == Kind.MAINFRAME) {
			if (!(this instanceof BlockLogicMainframe) || !((BlockLogicMainframe) this).creative())
				return false;
			if (!world.isRemote && player instanceof EntityPlayerMP)
				StfrLogicPacket.CHANNEL.sendTo(
						new StfrMainframeOpenPacket(world, pos), (EntityPlayerMP) player);
			return true;
		}
		if (kind == Kind.AUTOPILOT_1 || kind == Kind.AUTOPILOT_2 || kind == Kind.AUTOPILOT_3) {
			if (!world.isRemote && player instanceof EntityPlayerMP)
				StfrLogicPacket.CHANNEL.sendTo(
						new StfrAutopilotOpenPacket(
								pos,
								((TileEntityLogicDevice) world.getTileEntity(pos))
										.autopilotFuelThreshold(),
								StfrAutopilot.level(kind)),
						(EntityPlayerMP) player);
			return true;
		}
		if (kind == Kind.DESTABILIZER) {
			if (!world.isRemote && player instanceof EntityPlayerMP) {
				StfrLogicNetwork.Snapshot net = StfrLogicNetwork.inspect(world, pos);
				StfrLogicPacket.CHANNEL.sendTo(
						new StfrDestabilizerOpenPacket(pos, net.reactor, world.isBlockPowered(pos)),
						(EntityPlayerMP) player);
			}
			return true;
		}
		if (kind == Kind.BREAKER) {
			if (!world.isRemote) {
				StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, pos);
				StfrReactor reactor = network.reactor;
				boolean closed = !state.getValue(CLOSED);
				if (closed
						&& reactor != null
						&& reactor.runState() != StfrReactor.RunState.STARTUP
						&& reactor.runState() != StfrReactor.RunState.MANUAL_RESTABILIZATION) {
					player.sendMessage(
							new TextComponentTranslation("message.hbmstfr.breaker.not_expected"));
					return true;
				}
				if (!closed
						&& reactor != null
						&& reactor.runState() != StfrReactor.RunState.OFFLINE) return true;
				if (closed && reactor != null && reactor.resources().energy() < 1_000_000L) {
					player.sendMessage(
							new TextComponentTranslation("message.hbmstfr.breaker.no_power"));
					return true;
				}
				world.setBlockState(pos, state.withProperty(CLOSED, closed), 3);
				if (closed) {
					world.playSound(
							null, pos, STFRSounds.BREAKER_ARM, SoundCategory.BLOCKS, 1.0F, 1.5F);
					emitBreakerEngagedEffect(world, pos);
				} else {
					world.playSound(
							null, pos, STFRSounds.BREAKER_SWITCH, SoundCategory.BLOCKS, 8.0F, 0.8F);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote) StfrDetonatorProgramming.eject(world, pos, world.getTileEntity(pos));
		super.breakBlock(world, pos, state);
	}

	/** 1.12 replacement for TERF's electric_spark burst when a breaker engages. */
	public static void emitBreakerEngagedEffect(World world, BlockPos pos) {
		if (!(world instanceof WorldServer)) return;
		((WorldServer) world)
				.spawnParticle(
						EnumParticleTypes.CRIT_MAGIC,
						true,
						pos.getX() + 0.5,
						pos.getY() + 0.5,
						pos.getZ() + 0.5,
						5,
						0.2,
						0.2,
						0.2,
						0.0);
	}
}
