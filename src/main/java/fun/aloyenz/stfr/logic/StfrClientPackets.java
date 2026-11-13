package fun.aloyenz.stfr.logic;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only packet handlers must never be loaded by a dedicated server. */
@SideOnly(Side.CLIENT)
final class StfrClientPackets {

	private StfrClientPackets() {}

	static void register() {
		StfrLogicPacket.CHANNEL.registerMessage(StfrCoreFlashPacket.Client.class,StfrCoreFlashPacket.class,20,Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrLogicSyncPacket.Handler.class, StfrLogicSyncPacket.class, 1, Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrMainframeOpenPacket.Handler.class,
				StfrMainframeOpenPacket.class,
				4,
				Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrRotorSyncPacket.Handler.class, StfrRotorSyncPacket.class, 5, Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrShakePacket.Handler.class, StfrShakePacket.class, 6, Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrSuppressorPacket.Client.class, StfrSuppressorPacket.class, 8, Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrCompatParticlePacket.Handler.class,
				StfrCompatParticlePacket.class,
				9,
				Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrMusicPacket.Handler.class, StfrMusicPacket.class, 12, Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrVoicePacket.Handler.class, StfrVoicePacket.class, 17, Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(StfrAlarmOpenPacket.Handler.class,
				StfrAlarmOpenPacket.class, 19, Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrAutopilotOpenPacket.Handler.class,
				StfrAutopilotOpenPacket.class,
				14,
				Side.CLIENT);
		StfrLogicPacket.CHANNEL.registerMessage(
				StfrDestabilizerOpenPacket.Handler.class,
				StfrDestabilizerOpenPacket.class,
				15,
				Side.CLIENT);
	}
}
