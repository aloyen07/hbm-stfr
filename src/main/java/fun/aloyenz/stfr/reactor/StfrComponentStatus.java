package fun.aloyenz.stfr.reactor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import fun.aloyenz.stfr.reactor.states.StfrEmergencyRules;
import fun.aloyenz.stfr.structure.StfrBlockRole;
import fun.aloyenz.stfr.structure.StfrRadialSymmetry;

/** Immutable diagnostic snapshot for one of the reactor's six stabilizers. */
public final class StfrComponentStatus {

	public final EnumFacing direction;
	public final boolean turbineStructureValid;
	public final boolean mainElectrodeEnabled;
	public final int secondaryElectrodeMask;
	public final int optionalMissing;
	public final int optionalTotal;
	public final int sealFailures;
	public final int shieldFailures;
	public final int lightningRodFailures;
	public final int supercriticalFailures;
	public final int criticalFailures;
	public final List<Defect> defects;

	StfrComponentStatus(Builder builder) {
		direction = builder.direction;
		mainElectrodeEnabled =
				builder.mainElectrodeEnabled
						&& builder.criticalFailures == 0
						&& builder.mustAirBlocked == 0
						&& builder.supercriticalFailures == 0
						&& (builder.optionalTotal == 0
								|| builder.optionalMissing < builder.optionalTotal);
		secondaryElectrodeMask =
				builder.supercriticalFailures == 0 ? builder.secondaryElectrodeMask : 0;
		turbineStructureValid =
				StfrEmergencyRules.turbineAvailable(
						builder.supercriticalFailures,
						mainElectrodeEnabled,
						secondaryElectrodeMask);
		optionalMissing = builder.optionalMissing;
		optionalTotal = builder.optionalTotal;
		sealFailures = builder.sealFailures;
		shieldFailures = builder.shieldFailures;
		lightningRodFailures = builder.lightningRodFailures;
		supercriticalFailures = builder.supercriticalFailures;
		criticalFailures = builder.criticalFailures;
		defects = Collections.unmodifiableList(new ArrayList<>(builder.defects));
	}

	/** 0: damaged; 1: fully functional with seal damage; 2: fully restored and sealed. */
	public int restorationLevel() {
		if (!turbineStructureValid
				|| electrodeShieldDegraded()
				|| defects.stream().anyMatch(defect -> defect.role != StfrBlockRole.SEAL)) return 0;
		return sealed() ? 2 : 1;
	}

	public boolean secondaryElectrodeEnabled(int index) {
		if (index < 0 || index >= 4)
			throw new IllegalArgumentException("Secondary electrode index: " + index);
		return (secondaryElectrodeMask & (1 << index)) != 0;
	}

	public int secondaryElectrodesEnabled() {
		return Integer.bitCount(secondaryElectrodeMask);
	}

	public double efficiency() {
		return optionalTotal == 0 ? 1D : (double) (optionalTotal - optionalMissing) / optionalTotal;
	}

	public boolean sealed() {
		return sealFailures == 0;
	}

	public boolean shieldIntact() {
		return shieldFailures == 0;
	}

	/** Each failed electrode chain independently weakens the plasma shield. */
	public boolean electrodeShieldDegraded() {
		return !mainElectrodeEnabled || secondaryElectrodeMask != 15;
	}

	public boolean grounded() {
		return lightningRodFailures == 0;
	}

	public static final class Defect {

		public final BlockPos localPos;
		public final int radialDepth;
		public final StfrBlockRole role;
		public final String reason;
		public final String expected;
		public final String actual;

		Defect(
				BlockPos localPos,
				int radialDepth,
				StfrBlockRole role,
				String reason,
				String expected,
				String actual) {
			this.localPos = localPos;
			this.radialDepth = radialDepth;
			this.role = role;
			this.reason = reason;
			this.expected = expected;
			this.actual = actual;
		}
	}

	static final class Builder {

		final EnumFacing direction;
		final List<Defect> defects = new ArrayList<>();
		int secondaryElectrodeMask = 15;
		boolean mainElectrodeEnabled = true;
		int criticalFailures;
		int supercriticalFailures;
		int optionalMissing;
		int optionalTotal;
		int sealFailures;
		int shieldFailures;
		int lightningRodFailures;
		int mustAirBlocked;

		Builder(EnumFacing direction) {
			this.direction = direction;
		}

		void defect(
				BlockPos localPos,
				StfrBlockRole role,
				String reason,
				String expected,
				String actual) {
			defects.add(
					new Defect(
							localPos,
							StfrRadialSymmetry.radialDepth(localPos, direction),
							role,
							reason,
							expected,
							actual));
			switch (role) {
				case SUPERCRITICAL:
					supercriticalFailures++;
					break;
				case SEAL:
					sealFailures++;
					break;
				case RADIATION_SHIELD:
					shieldFailures++;
					break;
				case LIGHTNING_ROD:
					lightningRodFailures++;
					break;
				case OPTIONAL_EFFICIENCY:
					optionalMissing++;
					break;
				case ELECTRODE:
					int index = StfrRadialSymmetry.secondaryElectrodeIndex(localPos, direction);
					if (index >= 0) secondaryElectrodeMask &= ~(1 << index);
					else criticalFailures++;
					break;
				case MUST_AIR:
					mustAirBlocked++;
					break;
				case HATCH_FREE:
					break;
				case HATCH_FIXED:
					if (!"REFERENCE_OPEN_STATE".equals(reason)) criticalFailures++;
					break;
				default:
					criticalFailures++;
					break;
			}
		}
	}
}
