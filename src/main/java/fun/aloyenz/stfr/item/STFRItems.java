package fun.aloyenz.stfr.item;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import fun.aloyenz.stfr.Tags;
import fun.aloyenz.stfr.client.render.StfrReactorItemModel;

@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class STFRItems {

	public static final Item NETWORK_LOAD_ANALYZER = new ItemNetworkLoadAnalyzer();
	public static final Item KEY_CARD = new ItemStfrKeyCard();
	public static final Item DETONATOR = new ItemStfrDetonator();
	public static final Item FUEL_CAPSULE = new ItemFuelCapsule();
	public static final Item REACTOR_ANALYSIS_WAND = new ItemReactorAnalysisWand();
	public static final Item REACTOR_MISC_WAND = new ItemReactorMiscWand();
	public static final Item REACTOR_DEBUG_WAND = new ItemReactorDebugWand();
	public static final Item REACTOR_ROTOR_WAND = new ItemReactorRotorWand();
	public static final Item REACTOR_PORT_WAND = new ItemReactorPortWand();
	public static final Item REACTOR_CONTROL_WAND = new ItemReactorControlWand();
	public static final Item REACTOR_SIMULATION_WAND = new ItemReactorSimulationWand();
	public static final Item CORE_LIGHT_WAND = new ItemCoreLightWand();
	public static final Item STFR_SCREAM_WAND = new ItemStfrScreamWand();
	public static final Item REACTOR_DESTRUCTION_WAND = new ItemReactorDestructionWand();
	public static final Item SIGNAL_MODIFIER = new ItemSignalModifier();
	public static final Item REACTOR_MACHINE_DEBUG_WAND = new ItemReactorMachineWand(false);
	public static final Item REACTOR_RESET_WAND = new ItemReactorMachineWand(true);
	public static final Item MONITOR_PRESET_WAND = new ItemMonitorPresetWand();
	public static final Item YARONITE_DUST_TINY =
			new ItemDigammaProduct(Tags.MODID, "yaronite_dust_tiny");
	public static final Item STFR_REACTOR = new ItemStfrReactor();
	public static final Item OPERATOR_BOOK = new ItemOperatorBookMk2("operator_book");
	public static final Item OPERATOR_BOOK_MK2 = new ItemOperatorBookMk2("operator_book_mk2");

	private STFRItems() {}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		event.getRegistry()
				.registerAll(
						NETWORK_LOAD_ANALYZER,
						REACTOR_ANALYSIS_WAND,
						REACTOR_MISC_WAND,
						KEY_CARD,
						DETONATOR,
						FUEL_CAPSULE,
						REACTOR_DEBUG_WAND,
						REACTOR_ROTOR_WAND,
						REACTOR_PORT_WAND,
						REACTOR_CONTROL_WAND,
						REACTOR_DESTRUCTION_WAND,
						SIGNAL_MODIFIER,
						REACTOR_MACHINE_DEBUG_WAND,
						REACTOR_RESET_WAND,
						MONITOR_PRESET_WAND,
						REACTOR_SIMULATION_WAND,
						CORE_LIGHT_WAND,
						STFR_SCREAM_WAND,
						YARONITE_DUST_TINY,
						STFR_REACTOR,
						OPERATOR_BOOK,
						OPERATOR_BOOK_MK2);
	}

	@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
	public static final class ClientRegistration {

		private ClientRegistration() {}

		@SubscribeEvent
		public static void registerModels(ModelRegistryEvent event) {
			ModelLoader.setCustomModelResourceLocation(
					FUEL_CAPSULE,
					0,
					new ModelResourceLocation(FUEL_CAPSULE.getRegistryName(), "inventory"));
			ModelLoader.setCustomModelResourceLocation(
					FUEL_CAPSULE,
					1,
					new ModelResourceLocation(Tags.MODID + ":fuel_capsule_locked", "inventory"));
			ModelLoader.setCustomModelResourceLocation(
					REACTOR_DEBUG_WAND,
					0,
					new ModelResourceLocation(REACTOR_DEBUG_WAND.getRegistryName(), "inventory"));
			ModelLoader.setCustomModelResourceLocation(
					REACTOR_ROTOR_WAND,
					0,
					new ModelResourceLocation(REACTOR_ROTOR_WAND.getRegistryName(), "inventory"));
			ModelLoader.setCustomModelResourceLocation(
					REACTOR_PORT_WAND,
					0,
					new ModelResourceLocation(REACTOR_PORT_WAND.getRegistryName(), "inventory"));
			ModelLoader.setCustomModelResourceLocation(
					REACTOR_CONTROL_WAND,
					0,
					new ModelResourceLocation(REACTOR_CONTROL_WAND.getRegistryName(), "inventory"));
			ModelLoader.setCustomModelResourceLocation(
					REACTOR_DESTRUCTION_WAND,
					0,
					new ModelResourceLocation(REACTOR_DEBUG_WAND.getRegistryName(), "inventory"));
			for (Item item :
					new Item[] {
						NETWORK_LOAD_ANALYZER,
						REACTOR_ANALYSIS_WAND,
						REACTOR_MISC_WAND,
						KEY_CARD,
						DETONATOR,
						SIGNAL_MODIFIER,
						REACTOR_MACHINE_DEBUG_WAND,
						REACTOR_RESET_WAND,
						MONITOR_PRESET_WAND,
						REACTOR_SIMULATION_WAND,
						CORE_LIGHT_WAND,
						STFR_SCREAM_WAND,
						YARONITE_DUST_TINY,
						OPERATOR_BOOK,
						OPERATOR_BOOK_MK2
					})
				ModelLoader.setCustomModelResourceLocation(
						item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
			ModelLoader.setCustomModelResourceLocation(
					STFR_REACTOR, 0, new ModelResourceLocation(STFR_REACTOR.getRegistryName(), "inventory"));
		}

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void bakeReactorItem(ModelBakeEvent event) {
			StfrReactorItemModel.bake(event);
		}
	}
}
