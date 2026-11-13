package fun.aloyenz.stfr.client.gui;

import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrMainframeOpenPacket;
import fun.aloyenz.stfr.logic.StfrMainframePacket;
import fun.aloyenz.stfr.reactor.StfrReactor;
import fun.aloyenz.stfr.reactor.timelines.StfrEmergencyTimeline;

/** Player-facing reactor controls with a visible next step and action result. */
public final class GuiMainframe extends GuiScreen {

	private StfrMainframeOpenPacket status;
	private String feedback = "";
	private int refreshTicks;

	public GuiMainframe(StfrMainframeOpenPacket status) {
		this.status = status;
	}

	public BlockPos pos() {
		return status.pos();
	}

	public void update(StfrMainframeOpenPacket value) {
		status = value;
		if (!value.feedback.isEmpty()) feedback = value.feedback;
		updateButtons();
	}

	private int top() {
		return Math.max(4, height / 2 - 145);
	}

	@Override
	public void initGui() {
		buttonList.clear();
		int x = width / 2 - 150, y = top() + 136;
		buttonList.add(new GuiButton(7, x, y, 145, 20, ""));
		buttonList.add(new GuiButton(8, x + 155, y, 145, 20, ""));
		buttonList.add(
				new GuiButton(5, x, y + 24, 145, 20, I18n.format("gui.hbmstfr.mainframe.fuel_up")));
		buttonList.add(
				new GuiButton(
						6,
						x + 155,
						y + 24,
						145,
						20,
						I18n.format("gui.hbmstfr.mainframe.fuel_down")));
		int[] actions = {9, 10, 11, 12, 13, 14, 15, 16};
		for (int i = 0; i < actions.length; i++)
			buttonList.add(
					new GuiButton(
							actions[i], x + (i % 2) * 155, y + 48 + (i / 2) * 24, 145, 20, ""));
		updateButtons();
	}

	private void updateButtons() {
		if (buttonList.size() < 4) return;
		boolean reactor = status.networkAvailable;
		for (GuiButton button : buttonList) button.enabled = reactor;
		boolean failure = "STARTUP_FAILURE".equals(status.state);
		boolean emergencyMode;
		try {
			emergencyMode =
					StfrReactor.RunState.valueOf(status.state).ordinal()
							>= StfrReactor.RunState.OVERLOAD.ordinal();
		} catch (IllegalArgumentException ex) {
			emergencyMode = false;
		}
		boolean terminal =
				"MELTDOWN".equals(status.state)
						|| "DETONATION".equals(status.state)
						|| "UNDERLOAD".equals(status.state)
						|| "SELF_DESTRUCT".equals(status.state);
		for (GuiButton button : buttonList) {
			if (button.id == 17) {
				button.visible = false;
				button.enabled = reactor && status.crashTicks == 0 && status.recoveryReady;
			} else {
				button.visible = (!failure && !emergencyMode) || button.id == 8 || emergencyMode && button.id == 7;
				button.enabled =
						reactor
								&& !terminal
								&& status.crashTicks == 0
								&& (!failure || button.id == 8 && status.recoveryReady)
								&& (!emergencyMode
										|| button.id == 7
										|| button.id == 8 && status.recoveryReady);
			}
		}
		buttonList.get(0).displayString =
				I18n.format(
						status.key
								? "gui.hbmstfr.mainframe.key_remove"
								: "gui.hbmstfr.mainframe.key_insert");
		buttonList.get(1).displayString =
				I18n.format(
						failure || emergencyMode
								? "gui.hbmstfr.mainframe.sstb_restabilize"
								: "gui.hbmstfr.mainframe.sstb_press");
		int[] values = {status.suppression, status.offset, status.lasers, status.vent};
		String[] keys = {"suppression", "offset", "lasers", "vent"};
		for (int i = 0; i < 4; i++) {
			buttonList.get(4 + i * 2).displayString =
					I18n.format("gui.hbmstfr.mainframe." + keys[i], values[i]) + " +1";
			buttonList.get(5 + i * 2).displayString =
					I18n.format("gui.hbmstfr.mainframe." + keys[i], values[i]) + " -1";
		}
	}

	@Override
	public void updateScreen() {
		if (++refreshTicks >= 20) {
			refreshTicks = 0;
			StfrLogicPacket.CHANNEL.sendToServer(new StfrMainframePacket(pos(), 0));
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		int left = width / 2 - 157, top = top();
		drawRect(left, top, left + 314, top + 292, 0xDC111821);
		drawRect(left + 4, top + 4, left + 310, top + 26, 0xFF24394A);
		if (status.crashTicks > 0) {
			if (status.crashTicks > 1160)
				for (int i = 0; i < 14; i++)
					drawRect(
							left + 8,
							top + 32 + i * 16,
							left + 306,
							top + 43 + i * 16,
							0xFF000000
									| (int) (Math.sin(i * 37D + refreshTicks * 13D) * 60 + 100)
											* 0x010101);
			drawCenteredString(
					fontRenderer,
					I18n.format("gui.hbmstfr.mainframe.crash", (status.crashTicks + 19) / 20),
					width / 2,
					top + 276,
					0xFF7777);
			return;
		}
		drawCenteredString(
				fontRenderer,
				I18n.format("gui.hbmstfr.mainframe.title"),
				width / 2,
				top + 11,
				0xA3E9FF);
		int stateColor =
				"ONLINE".equals(status.state)
						? 0x66EE99
						: "OFFLINE".equals(status.state) ? 0xB0B0B0 : 0xFFD46A;
		drawCenteredString(fontRenderer, stage(), width / 2, top + 31, stateColor);
		drawCenteredString(
				fontRenderer,
				I18n.format(
						"gui.hbmstfr.mainframe.network",
						I18n.format(
								"gui.hbmstfr.network."
										+ status.issue.toLowerCase(Locale.ROOT).replace(' ', '_')),
						status.breakers),
				width / 2,
				top + 45,
				"READY".equals(status.issue) ? 0x80E5A1 : 0xFFAA77);
		drawCenteredString(
				fontRenderer,
				fit(
						I18n.format(
										"gui.hbmstfr.mainframe.health",
										status.turbines,
										status.seals,
										status.fuel)
								+ I18n.format(
										"gui.hbmstfr.mainframe.maintenance", status.maintenance)),
				width / 2,
				top + 60,
				0xE6F0F5);
		drawCenteredString(
				fontRenderer,
				I18n.format(
						"gui.hbmstfr.mainframe.inputs",
						status.water,
						status.lubricant,
						status.fuelLeft),
				width / 2,
				top + 73,
				0xA9D9E8);
		drawCenteredString(
				fontRenderer,
				I18n.format(
						"gui.hbmstfr.mainframe.outputs",
						status.steam,
						status.products,
						status.power),
				width / 2,
				top + 86,
				0xA9D9E8);
		if ("STARTUP_FAILURE".equals(status.state))
			drawCenteredString(
					fontRenderer,
					I18n.format("gui.hbmstfr.mainframe.recovery_energy", status.recoveryCharge),
					width / 2,
					top + 162,
					0xFFE69A);
		if ("STARTUP_FAILURE".equals(status.state))
			drawCenteredString(
					fontRenderer,
					fit(
							I18n.format(
									"message.hbmstfr.failure.cause."
											+ status.failureCause.toLowerCase(Locale.ROOT))),
					width / 2,
					top + 210,
					0xFF7777);
		if (status.crashTicks > 0)
			drawCenteredString(
					fontRenderer,
					I18n.format("gui.hbmstfr.mainframe.crash", (status.crashTicks + 19) / 20),
					width / 2,
					top + 186,
					0xFF7777);
		drawCenteredString(
				fontRenderer,
				I18n.format(
						"gui.hbmstfr.mainframe.thermal",
						String.format(Locale.ROOT, "%.1f", status.temperature),
						String.format(Locale.ROOT, "%.2f", status.pressure)),
				width / 2,
				top + 99,
				0xA9D9E8);
		drawCenteredString(
				fontRenderer,
				fit(
						I18n.format(
								"gui.hbmstfr.mainframe.core",
								status.fuelType,
								String.format(Locale.ROOT, "%.1f", status.coreFuel),
								String.format(Locale.ROOT, "%.1f", status.coreHelium4))),
				width / 2,
				top + 110,
				0xA9D9E8);
		drawCenteredString(
				fontRenderer,
				fit(
						I18n.format(
								"gui.hbmstfr.mainframe.physics",
								String.format(Locale.ROOT, "%.1f", status.coreTemperature),
								String.format(Locale.ROOT, "%.1f", status.shield),
								String.format(Locale.ROOT, "%.0f", status.reactionPower))),
				width / 2,
				top + 121,
				0xA9D9E8);
		drawCenteredString(
				fontRenderer, fit(I18n.format(status.hint)), width / 2, top + 132, 0xFFE69A);
		if (!feedback.isEmpty())
			drawCenteredString(
					fontRenderer,
					fit(I18n.format(feedback, status.fuel)),
					width / 2,
					top + 282,
					feedback.contains("blocked")
									|| feedback.contains("not_ready")
									|| feedback.contains("required")
							? 0xFF7777
							: 0x88EEAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private String fit(String text) {
		return fontRenderer.trimStringToWidth(text, 300);
	}

	private String stage() {
		switch (status.state) {
			case "OVERLOAD":
			case "REACTION_LOSS":
			case "REIGNITION":
			case "UNDERLOAD":
			case "MANUAL_RESTABILIZATION":
			case "STABILIZER_LOSS":
			case "SHUTDOWN_FAILURE":
			case "SELF_DESTRUCT":
				int limit =
						StfrEmergencyTimeline.deadline(StfrReactor.RunState.valueOf(status.state));
				return I18n.format("state.hbmstfr." + status.state)
						+ (status.recoveryTicks > 0
								? " · " + I18n.format("gui.hbmstfr.mainframe.feedback.recovering")
								: limit > 0
										? " · T−"
												+ Math.max(
														0, (limit - status.failureTicks + 19) / 20)
												+ "s"
										: "");
			case "STARTUP_FAILURE":
				return I18n.format("gui.hbmstfr.mainframe.stage.failure")
						+ " · "
						+ (status.recoveryTicks > 0
								? I18n.format(
										"gui.hbmstfr.mainframe.recovery_timer",
										(601 - status.recoveryTicks + 19) / 20)
								: "T−" + (8040 - status.failureTicks + 19) / 20 + "s");
			case "MELTDOWN":
				return I18n.format("gui.hbmstfr.mainframe.stage.meltdown")
						+ " · T−"
						+ (1398 - status.failureTicks + 19) / 20
						+ "s";
			case "DETONATION":
				return I18n.format("gui.hbmstfr.mainframe.stage.detonation");
			case "STARTUP":
				return I18n.format(
						"LOCKING".equals(status.lock)
								? "gui.hbmstfr.mainframe.stage.locking"
								: "gui.hbmstfr.mainframe.stage.startup");
			case "STARTUP_CONFIRMED":
				return I18n.format("gui.hbmstfr.mainframe.stage.confirmed");
			case "ONLINE":
				return I18n.format("gui.hbmstfr.mainframe.stage.online");
			case "SHUTDOWN":
				return I18n.format("gui.hbmstfr.mainframe.stage.shutdown");
			default:
				return I18n.format("gui.hbmstfr.mainframe.stage.offline");
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.enabled)
			StfrLogicPacket.CHANNEL.sendToServer(new StfrMainframePacket(pos(), button.id));
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
