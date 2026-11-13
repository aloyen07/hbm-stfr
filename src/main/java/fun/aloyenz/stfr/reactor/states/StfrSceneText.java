package fun.aloyenz.stfr.reactor.states;

/** Stable translation keys for untimed protocol announcements. */
final class StfrSceneText {

	private StfrSceneText() {}

	static String key(String voice, String text) {
		if (!voice.isEmpty()) return "message.hbmstfr.scene." + voice;
		return switch (text) {
			case "Complete Stabilization Loss Detected! Preparing Contingency Protocols." ->
					"message.hbmstfr.scene.stabilization_lost";
			case "Reactor Shield Has Been Successfully Preserved." ->
					"message.hbmstfr.scene.shield_preserved";
			case "The Fusion Reactor Self-Destruct Protocol Has Been Aborted By Facility"
							+ " Administration." ->
					"message.hbmstfr.scene.self_aborted";
			default -> "";
		};
	}
}
