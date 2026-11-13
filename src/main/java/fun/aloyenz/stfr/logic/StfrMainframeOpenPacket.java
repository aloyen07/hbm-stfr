package fun.aloyenz.stfr.logic;

import io.netty.buffer.ByteBuf;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fun.aloyenz.stfr.StfrConfig;
import fun.aloyenz.stfr.client.gui.GuiMainframe;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Compact state needed for the ordinary mainframe control panel. */
public final class StfrMainframeOpenPacket implements IMessage {

	private BlockPos pos;
	public String issue = "NO CABLE NETWORK";
	public String state = "OFFLINE";
	public String lock = "UNLOCKED";
	public boolean networkAvailable;
	public int fuel, turbines, breakers, seals, maintenance;
	public long water, steam, power, fuelLeft, lubricant, products;
	public double temperature, pressure;
	public double coreTemperature, shield, reactionPower, coreFuel, coreHelium4;
	public int suppression, offset, lasers, vent;
	public String fuelType = "NONE";
	public boolean key, sstb;
	public int crashTicks, failureTicks, recoveryTicks;
	public long recoveryCharge;
	public boolean recoveryReady;
	public String failureCause = "";
	public String hint = "";
	public String feedback = "";
	private boolean responseOnly;

	public StfrMainframeOpenPacket() {}

	public StfrMainframeOpenPacket(World world, BlockPos pos) {
		this(world, pos, "", false);
	}

	public StfrMainframeOpenPacket(
			World world, BlockPos pos, String feedback, boolean responseOnly) {
		this.pos = pos;
		this.feedback = feedback;
		this.responseOnly = responseOnly;
		StfrLogicNetwork.Snapshot network = StfrLogicNetwork.inspect(world, pos);
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityLogicDevice)
			crashTicks = ((TileEntityLogicDevice) tile).crashTicks();
		issue = network.issue;
		networkAvailable = network.ioAvailable;
		breakers = network.breakers;
		StfrReactor reactor = network.reactor;
		if (reactor == null) {
			hint = "gui.hbmstfr.mainframe.hint.no_reactor";
			return;
		}
		state = reactor.runState().name();
		failureTicks = reactor.stateTicks();
		recoveryTicks =
				reactor.emergencyState()
						? reactor.emergency().recoveryTicks()
						: reactor.failure().recoveryTicks();
		recoveryCharge = StfrConfig.recoveryCharge;
		recoveryReady =
				reactor.emergencyState()
						? reactor.emergency().canConfirm(world)
						: reactor.failure().canRecover();
		failureCause = reactor.failure().cause();
		lock = reactor.lockState().name();
		fuel = reactor.fuelInputLevel();
		turbines = reactor.workingTurbines();
		seals = reactor.sealFailures();
		maintenance = reactor.maintenance().count();
		water = reactor.waterAmount();
		steam = reactor.steamAmount();
		power = reactor.energyAmount();
		fuelLeft = reactor.fuelRemaining();
		lubricant = reactor.lubricantAmount();
		products = reactor.productsAmount();
		temperature = reactor.telemetry(reactor.temperature(), 1);
		pressure = reactor.telemetry(reactor.pressure(), 2);
		coreTemperature = reactor.telemetry(reactor.core().coreTemperature(), 3);
		shield = reactor.telemetry(reactor.core().shield(), 4);
		reactionPower = reactor.telemetry(reactor.core().reactionPower(), 5);
		coreFuel = reactor.telemetry(reactor.core().coreFuel(), 6);
		coreHelium4 = reactor.core().coreHelium4();
		fuelType = reactor.core().fuelLabel();
		suppression = reactor.suppression();
		offset = reactor.offset();
		lasers = reactor.lasers();
		vent = reactor.vent();
		key = reactor.keyOn();
		sstb = reactor.sstbOn();
		switch (reactor.runState()) {
			case OFFLINE:
				hint =
						reactor.lockState() == StfrReactor.LockState.UNLOCKED
								? "gui.hbmstfr.mainframe.hint.offline"
								: reactor.lockState() == StfrReactor.LockState.LOCKED
										? "gui.hbmstfr.mainframe.hint.offline_locked"
										: "gui.hbmstfr.mainframe.hint.unlocking";
				break;
			case STARTUP:
				String problem = reactor.startupProblem(world);
				hint = problem.isEmpty() ? "gui.hbmstfr.mainframe.hint.ready" : problem;
				break;
			case STARTUP_CONFIRMED:
				hint = "gui.hbmstfr.mainframe.hint.confirmed";
				break;
			case ONLINE:
				hint =
						reactor.nominalShutdownAllowed()
								? "gui.hbmstfr.mainframe.hint.online"
								: "gui.hbmstfr.mainframe.feedback.shutdown_unsafe";
				break;
			case SHUTDOWN:
				hint = "gui.hbmstfr.mainframe.hint.shutdown";
				break;
			case STARTUP_FAILURE:
				hint =
						recoveryTicks > 0
								? "gui.hbmstfr.mainframe.feedback.recovering"
								: "gui.hbmstfr.mainframe.hint.failure";
				break;
			case MELTDOWN:
				hint = "gui.hbmstfr.mainframe.hint.meltdown";
				break;
			case SHUTDOWN_FAILURE:
				hint =
						recoveryReady
								? "gui.hbmstfr.mainframe.hint.shutdown_failure"
								: "message.hbmstfr.recovery."
										+ reactor.emergency().confirmationProblem(world);
				break;
			case DETONATION:
				hint = "gui.hbmstfr.mainframe.hint.detonation";
				break;
			default:
				hint = "gui.hbmstfr.mainframe.hint." + state.toLowerCase(Locale.ROOT);
				break;
		}
	}

	public BlockPos pos() {
		return pos;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		issue = ByteBufUtils.readUTF8String(buf);
		state = ByteBufUtils.readUTF8String(buf);
		lock = ByteBufUtils.readUTF8String(buf);
		fuel = buf.readUnsignedByte();
		turbines = buf.readUnsignedByte();
		breakers = buf.readUnsignedByte();
		seals = buf.readInt();
		maintenance = buf.readInt();
		water = buf.readLong();
		steam = buf.readLong();
		power = buf.readLong();
		fuelLeft = buf.readLong();
		lubricant = buf.readLong();
		products = buf.readLong();
		temperature = buf.readDouble();
		pressure = buf.readDouble();
		coreTemperature = buf.readDouble();
		shield = buf.readDouble();
		reactionPower = buf.readDouble();
		coreFuel = buf.readDouble();
		coreHelium4 = buf.readDouble();
		suppression = buf.readInt();
		offset = buf.readInt();
		lasers = buf.readInt();
		vent = buf.readInt();
		fuelType = ByteBufUtils.readUTF8String(buf);
		key = buf.readBoolean();
		sstb = buf.readBoolean();
		crashTicks = buf.readInt();
		failureTicks = buf.readInt();
		recoveryTicks = buf.readInt();
		recoveryCharge = buf.readLong();
		recoveryReady = buf.readBoolean();
		failureCause = ByteBufUtils.readUTF8String(buf);
		hint = ByteBufUtils.readUTF8String(buf);
		feedback = ByteBufUtils.readUTF8String(buf);
		responseOnly = buf.readBoolean();
		networkAvailable = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		ByteBufUtils.writeUTF8String(buf, issue);
		ByteBufUtils.writeUTF8String(buf, state);
		ByteBufUtils.writeUTF8String(buf, lock);
		buf.writeByte(fuel);
		buf.writeByte(turbines);
		buf.writeByte(breakers);
		buf.writeInt(seals);
		buf.writeInt(maintenance);
		buf.writeLong(water);
		buf.writeLong(steam);
		buf.writeLong(power);
		buf.writeLong(fuelLeft);
		buf.writeLong(lubricant);
		buf.writeLong(products);
		buf.writeDouble(temperature);
		buf.writeDouble(pressure);
		buf.writeDouble(coreTemperature);
		buf.writeDouble(shield);
		buf.writeDouble(reactionPower);
		buf.writeDouble(coreFuel);
		buf.writeDouble(coreHelium4);
		buf.writeInt(suppression);
		buf.writeInt(offset);
		buf.writeInt(lasers);
		buf.writeInt(vent);
		ByteBufUtils.writeUTF8String(buf, fuelType);
		buf.writeBoolean(key);
		buf.writeBoolean(sstb);
		buf.writeInt(crashTicks);
		buf.writeInt(failureTicks);
		buf.writeInt(recoveryTicks);
		buf.writeLong(recoveryCharge);
		buf.writeBoolean(recoveryReady);
		ByteBufUtils.writeUTF8String(buf, failureCause);
		ByteBufUtils.writeUTF8String(buf, hint);
		ByteBufUtils.writeUTF8String(buf, feedback);
		buf.writeBoolean(responseOnly);
		buf.writeBoolean(networkAvailable);
	}

	public static final class Handler
			implements IMessageHandler<StfrMainframeOpenPacket, IMessage> {
		@Override
		public IMessage onMessage(StfrMainframeOpenPacket message, MessageContext context) {
			Minecraft.getMinecraft()
					.addScheduledTask(
							() -> {
								GuiScreen screen = Minecraft.getMinecraft().currentScreen;
								if (screen instanceof GuiMainframe
										&& ((GuiMainframe) screen).pos().equals(message.pos)) {
									((GuiMainframe) screen).update(message);
								} else if (!message.responseOnly) {
									Minecraft.getMinecraft()
											.displayGuiScreen(new GuiMainframe(message));
								}
							});
			return null;
		}
	}
}
