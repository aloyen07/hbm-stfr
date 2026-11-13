package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.block.BlockLogicMainframe;
import fun.aloyenz.stfr.reactor.StfrManager;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.security.StfrAccessPolicy;
import fun.aloyenz.stfr.security.StfrSecurity;
import fun.aloyenz.stfr.sound.STFRSounds;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Player controls on the mainframe; action zero only refreshes the display. */
public final class StfrMainframePacket implements IMessage {

	private BlockPos pos;
	private int action;

	public StfrMainframePacket() {}

	public StfrMainframePacket(BlockPos pos, int action) {
		this.pos = pos;
		this.action = action;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		action = buf.readUnsignedByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeByte(action);
	}

	public static final class Handler implements IMessageHandler<StfrMainframePacket, IMessage> {

		@Override
		public IMessage onMessage(StfrMainframePacket message, MessageContext context) {
			EntityPlayerMP player = context.getServerHandler().player;
			player.getServerWorld()
					.addScheduledTask(
							() -> {
								if (!StfrSecurity.allows(
										player,
										player.world,
										message.pos,
										StfrAccessPolicy.CONTROL)) return;

								if (player.getDistanceSq(message.pos) > 64
										|| message.action > 17
										|| !player.world.isBlockLoaded(message.pos)) return;
								if (!(player.world.getBlockState(message.pos).getBlock()
												instanceof BlockLogicMainframe)
										|| !((BlockLogicMainframe)
														player.world
																.getBlockState(message.pos)
																.getBlock())
												.creative()) return;
								TileEntity tile = player.world.getTileEntity(message.pos);
								if (!(tile instanceof TileEntityLogicDevice)
										|| ((TileEntityLogicDevice) tile).kind()
												!= BlockLogicDevice.Kind.MAINFRAME) return;
								StfrLogicNetwork.Snapshot network =
										StfrLogicNetwork.inspect(player.world, message.pos);
								StfrReactor reactor = network.reactor;
								if (reactor != null) {
									if (message.action != 0
											&& (!network.ioAvailable
													|| ((TileEntityLogicDevice) tile).crashTicks()
															> 0
													|| reactor.catastrophic()
															&& message.action != 7
															&& message.action != 8
															&& message.action != 17)) {
										StfrLogicPacket.CHANNEL.sendTo(
												new StfrMainframeOpenPacket(
														player.world,
														message.pos,
														"gui.hbmstfr.mainframe.feedback.failure_blocked",
														true),
												player);
										return;
									}
									boolean changed = false;
									String feedback = "";
									switch (message.action) {
										case 17:
											changed = reactor.restabilize(player.world);
											feedback =
													changed
															? "gui.hbmstfr.mainframe.feedback.recovering"
															: "gui.hbmstfr.mainframe.feedback.recovery_required";
											break;
										case 5:
											reactor.adjustFuelInputLevel(1);
											changed = true;
											feedback = "gui.hbmstfr.mainframe.feedback.fuel";
											break;
										case 6:
											reactor.adjustFuelInputLevel(-1);
											changed = true;
											feedback = "gui.hbmstfr.mainframe.feedback.fuel";
											break;
										case 7:
											reactor.setKeyOn(!reactor.keyOn());
											changed = true;
											feedback =
													reactor.keyOn()
															? "gui.hbmstfr.mainframe.feedback.key_on"
															: "gui.hbmstfr.mainframe.feedback.key_off";
											break;
										case 8:
											StfrReactor.SstbResult result =
													reactor.pulseSstb(player.world, message.pos);
											changed = result.accepted;
									feedback = result.translationKey();
											break;
										case 9:
											reactor.adjustSuppression(1);
											changed = true;
											break;
										case 10:
											reactor.adjustSuppression(-1);
											changed = true;
											break;
										case 11:
											reactor.adjustOffset(1);
											changed = true;
											break;
										case 12:
											reactor.adjustOffset(-1);
											changed = true;
											break;
										case 13:
											reactor.adjustLasers(1);
											changed = true;
											break;
										case 14:
											reactor.adjustLasers(-1);
											changed = true;
											break;
										case 15:
											reactor.adjustVent(1);
											changed = true;
											break;
										case 16:
											reactor.adjustVent(-1);
											changed = true;
											break;
										default:
											break;
									}
									if (changed) {
										StfrManager.get(player.world).markDirty();
										// SSTB has its own TERF alarm acknowledgement in
										// pulseSstb().
										// Do not cover it with the generic breaker-switch click.
										if (message.action != 8)
											player.world.playSound(
													null,
													message.pos,
													STFRSounds.BREAKER_SWITCH,
													SoundCategory.BLOCKS,
													2.0F,
													1.0F);
									} else if (message.action != 0 && message.action != 8)
										player.world.playSound(
												null,
												message.pos,
												STFRSounds.ALARM_BEEP,
												SoundCategory.BLOCKS,
												2.0F,
												0.7F);
									StfrLogicPacket.CHANNEL.sendTo(
											new StfrMainframeOpenPacket(
													player.world, message.pos, feedback, true),
											player);
								} else {
									StfrLogicPacket.CHANNEL.sendTo(
											new StfrMainframeOpenPacket(
													player.world, message.pos, "", true),
											player);
								}
							});
			return null;
		}
	}
}
