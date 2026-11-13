package fun.aloyenz.stfr.client.gui;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.block.BlockLogicDevice;
import fun.aloyenz.stfr.logic.StfrLogicPacket;
import fun.aloyenz.stfr.logic.StfrSignal;
import fun.aloyenz.stfr.tile.TileEntityLogicDevice;

/** Player-facing per-side signal editor, populated from the server's saved state. */
public final class GuiSignalModifier extends GuiScreen {

	private final BlockPos pos;
	private final EnumFacing clickedFace;
	private final StfrSignal[] selected;
	private final BlockLogicDevice.Kind kind;
	private boolean inline;
	private boolean repeatEnabled;
	private String repeatText;
	private GuiTextField repeatField;
	private int slot;
	private int page;
	private static final int PAGE_SIZE = 8;
	private StfrSignal[] values = new StfrSignal[0];

	public GuiSignalModifier(
			BlockPos pos,
			EnumFacing face,
			StfrSignal[] selected,
			boolean inline,
			BlockLogicDevice.Kind kind,
			int[] repeatIntervals) {
		this.pos = pos;
		this.clickedFace = face;
		this.selected = selected;
		this.inline = inline;
		this.kind = kind;
		int interval = Math.max(0, repeatIntervals[face.getIndex()]);
		repeatEnabled = interval > 0;
		repeatText = Integer.toString(interval > 0 ? interval : 20);
	}

	private EnumFacing editedFace() {
		return kind != BlockLogicDevice.Kind.MONITOR
				? clickedFace
				: inline ? TileEntityLogicDevice.monitorSlotFace(slot) : EnumFacing.UP;
	}

	@Override
	public void initGui() {
		if (repeatField != null) repeatText = repeatField.getText();
		buttonList.clear();
		if (kind == BlockLogicDevice.Kind.TRANSMITTER) {
			buttonList.add(
					new GuiButton(
							302,
							width / 2 - 150,
							height / 2 - 105,
							300,
							20,
							(repeatEnabled ? "[X] " : "[ ] ")
									+ I18n.format("gui.hbmstfr.signal.repeat")));
			repeatField =
					new GuiTextField(303, fontRenderer, width / 2 + 10, height / 2 - 79, 75, 20);
			repeatField.setMaxStringLength(10);
			repeatField.setText(repeatText);
			repeatField.setValidator(text -> text.matches("[0-9]*"));
			buttonList.add(
					new GuiButton(
							304,
							width / 2 + 90,
							height / 2 - 79,
							60,
							20,
							I18n.format("gui.hbmstfr.signal.apply")));
		}
		if (kind == BlockLogicDevice.Kind.MONITOR) {
			buttonList.add(
					new GuiButton(
							100,
							width / 2 - 150,
							height / 2 - 105,
							145,
							20,
							(!inline ? "[X] " : "[ ] ")
									+ I18n.format("gui.hbmstfr.signal.single")));
			buttonList.add(
					new GuiButton(
							101,
							width / 2 + 5,
							height / 2 - 105,
							145,
							20,
							(inline ? "[X] " : "[ ] ")
									+ I18n.format("gui.hbmstfr.signal.multiple")));
			if (inline) {
				for (int i = 0; i < 4; i++)
					buttonList.add(
							new GuiButton(
									110 + i,
									width / 2 - 150 + i * 76,
									height / 2 - 79,
									70,
									20,
									(slot == i ? "[X] " : "")
											+ I18n.format("gui.hbmstfr.signal.line", i + 1)));
			}
		}
		values = StfrSignal.choicesFor(kind);
		int first = page * PAGE_SIZE;
		for (int i = first; i < Math.min(first + PAGE_SIZE, values.length); i++) {
			int local = i - first;
			int x = width / 2 - 150 + (local % 2) * 155;
			int y = height / 2 - 53 + (local / 2) * 22;
			String label =
					(values[i] == selected[editedFace().getIndex()] ? "[X] " : "")
							+ I18n.format(values[i].translationKey());
			buttonList.add(
					new GuiButton(
							200 + i, x, y, 145, 20, fontRenderer.trimStringToWidth(label, 137)));
		}
		if (values.length > PAGE_SIZE) {
			buttonList.add(new GuiButton(300, width / 2 - 150, height / 2 + 47, 65, 20, "<"));
			buttonList.add(new GuiButton(301, width / 2 + 85, height / 2 + 47, 65, 20, ">"));
			buttonList.get(buttonList.size() - 2).enabled = page > 0;
			buttonList.get(buttonList.size() - 1).enabled = (page + 1) * PAGE_SIZE < values.length;
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		String title =
				kind == BlockLogicDevice.Kind.MONITOR
						? I18n.format(
								inline
										? "gui.hbmstfr.signal.title_line"
										: "gui.hbmstfr.signal.title_full",
								slot + 1)
						: I18n.format("gui.hbmstfr.signal.title_face", clickedFace.name());
		drawCenteredString(fontRenderer, title, width / 2, height / 2 - 122, 0xFFFFFF);
		if (values.length > PAGE_SIZE)
			drawCenteredString(
					fontRenderer,
					(page + 1) + "/" + ((values.length + PAGE_SIZE - 1) / PAGE_SIZE),
					width / 2,
					height / 2 + 53,
					0xA0D4DF);
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (repeatField != null) {
			drawString(
					fontRenderer,
					I18n.format("gui.hbmstfr.signal.repeat_ticks"),
					width / 2 - 150,
					height / 2 - 73,
					0xFFFFFF);
			repeatField.drawTextBox();
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 302 || button.id == 304) {
			if (!saveRepeat(button.id == 302 ? !repeatEnabled : repeatEnabled)) return;
		} else if (button.id == 100 || button.id == 101) {
			inline = button.id == 101;
			StfrLogicPacket.CHANNEL.sendToServer(StfrLogicPacket.mode(pos, inline));
		} else if (button.id >= 110 && button.id < 114) {
			slot = button.id - 110;
		} else if (button.id == 300 || button.id == 301) {
			page += button.id == 301 ? 1 : -1;
		} else if (button.id >= 200 && button.id < 200 + values.length) {
			EnumFacing face = editedFace();
			selected[face.getIndex()] = values[button.id - 200];
			StfrLogicPacket.CHANNEL.sendToServer(
					new StfrLogicPacket(pos, face, selected[face.getIndex()]));
		}
		initGui();
	}

	private boolean saveRepeat(boolean enabled) {
		try {
			int ticks = Integer.parseInt(repeatField.getText());
			if (ticks < 1) return false;
			repeatEnabled = enabled;
			StfrLogicPacket.CHANNEL.sendToServer(
					StfrLogicPacket.repeat(pos, clickedFace, enabled ? ticks : 0));
			return true;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	@Override
	public void updateScreen() {
		if (repeatField != null) repeatField.updateCursorCounter();
	}

	@Override
	protected void keyTyped(char character, int key) throws IOException {
		if (repeatField != null && (key == 28 || key == 156)) {
			saveRepeat(repeatEnabled);
			return;
		}
		if (repeatField != null && repeatField.textboxKeyTyped(character, key)) return;
		super.keyTyped(character, key);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		if (repeatField != null) repeatField.mouseClicked(x, y, button);
		super.mouseClicked(x, y, button);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
