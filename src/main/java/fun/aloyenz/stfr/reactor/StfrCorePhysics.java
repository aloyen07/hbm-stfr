package fun.aloyenz.stfr.reactor;

import static fun.aloyenz.stfr.reactor.StfrCoreSimulation.*;

import com.hbm.inventory.fluid.trait.FT_Heatable;

/** One physical core/case integration step. */
final class StfrCorePhysics {
	private StfrCorePhysics() {}

	static void tick(StfrCoreSimulation c,
			StfrThermalResources resources,
			int workingStabilizers,
			int injection,
			int suppression,
			int offset,
			int permeability,
			int lasers,
			int vent,
			boolean produceSteam,
			double electrodeCoverage,
			boolean leakingCase){
		c.reactionPower = c.reactionRate = c.heatTransferRate = c.coolingRate = c.radiation = 0;
		if (!c.coreExists) return;
		int stabilizers = Math.min(4, Math.max(0, workingStabilizers));
		double caseDensity = c.caseDensity();
		double coreShc =
				Math.max(
						1D,
						(c.coreFuel * TERF_UNITS_PER_MB * SHC_PER_MOLE
										+ c.coreHelium4
												* StfrCoreSimulation.heliumDensityFactor()
												* TERF_UNITS_PER_MB
												* 20.785512186D)
								* .00001D);

		double reactivity = StfrCoreSimulation.reactivity(c.coreTemperature) * c.reactionFactor;
		c.effectiveReactivity = reactivity;
		int reactionMultiplier = Math.max(0, (400 - suppression * stabilizers) / 4);
		for (Fuel substance : Fuel.values()) {
			if (substance == Fuel.NONE) continue;
			int index = substance.ordinal();
			double reacted =
					Math.min(
							c.coreAmounts[index],
							Math.floor(
											reactivity
													* 1_000_000D
													* .0000000017D
													* c.coreAmounts[index]
													* TERF_UNITS_PER_MB
													* reactionMultiplier)
									/ TERF_UNITS_PER_MB);
			c.coreAmounts[index] -= reacted;
			c.coreHelium4 += reacted * Math.max(0D, Math.min(1D, StfrCoreSimulation.heliumYield(substance)));
			c.reactionRate += reacted;
			double kevTotal =
					substance == Fuel.HELIUM3 ? 10950D : substance == Fuel.TRITIUM ? 9125D : 3650D;
			c.reactionPower += Math.floor(kevTotal * .862D) * reacted * TERF_UNITS_PER_MB / 10000D;
		}
		c.totals();
		c.radiation = c.reactionRate * TERF_UNITS_PER_MB / 12D;
		c.coreTemperature += c.reactionPower * 10_000D / coreShc;

		c.pressure = c.coreDensity() + c.pressureBonus;
		c.casePressure =
				caseDensity * .000001D * (c.caseTemperature * 100_000D + 100_000_000D) / 10_000D;
		double absoluteRpm = Math.abs(c.rotation) / 1000D;
		c.mssr = Math.floor(absoluteRpm / 2500D);
		c.pressureStress = Math.max(0D, Math.floor(c.pressure / 60_000D) - c.mssr);
		c.rigidityStress = Math.max(0D, Math.floor((1_000_000D - c.pressure) / 1000D));
		c.spinStress = Math.floor(absoluteRpm / 4000D);
		double exponential = Math.floor(absoluteRpm / 200D) - 800D;
		if (exponential >= 0) c.spinStress += exponential;
		c.shieldStress = c.pressureStress + c.spinStress + c.rigidityStress;

		double slowRate =
				(absoluteRpm + (absoluteRpm >= 3000D ? 1D + c.casePressure : 0D)) / 4D + 50D;
		double spinChange = offset * 120D * stabilizers;
		if (c.rotation >= 3500) spinChange -= slowRate;
		else if (c.rotation <= -3500) spinChange += slowRate;
		else if (c.rotation >= 20) spinChange -= 10;
		else if (c.rotation <= -20) spinChange += 10;
		else if (c.rotation >= 1) spinChange -= 1;
		else if (c.rotation <= 0) spinChange += 1;
		c.rotation += spinChange;

		double laserHeat = Math.min(c.capacitor, lasers * 50D * stabilizers / 4D);
		c.capacitor -= laserHeat;
		c.coreTemperature += laserHeat * 1000D / coreShc;

		if (!c.flowInitialized) {
			c.effectivePermeability = permeability;
			c.effectiveVent = vent;
			c.flowInitialized = true;
		} else {
			// One second time constant; a knob change does not step the flow.
			c.effectivePermeability += (permeability - c.effectivePermeability) * .05D;
			c.effectiveVent += (vent - c.effectiveVent) * .05D;
		}
		// Original order: leak existing case contents, vent, then permeate the core.
		if (leakingCase) c.leakCase();
		for (int i = 1; i < c.coreAmounts.length; i++) {
			double released = StfrCoreSimulation.flow(c.caseAmounts[i], c.effectiveVent, 20000D);
			c.caseAmounts[i] -= released;
			double permeated = StfrCoreSimulation.flow(c.coreAmounts[i], c.effectivePermeability, 5000D);
			c.coreAmounts[i] -= permeated;
			c.caseAmounts[i] += permeated;
		}
		c.totals();
		double releasedHelium = StfrCoreSimulation.flow(c.caseHelium4, c.effectiveVent, 20000D);
		c.caseHelium4 -= releasedHelium;
		double permeatedHelium = StfrCoreSimulation.flow(c.coreHelium4, c.effectivePermeability, 5000D);
		c.coreHelium4 -= permeatedHelium;
		c.caseHelium4 += permeatedHelium;
		c.productRemainder += releasedHelium / PLASMA_UNITS_PER_MB;
		int output = Math.min((int) c.productRemainder, resources.productRoom());
		int delivered = resources.produceHelium(output);
		c.productRemainder -= Math.min(output, Math.max(0, delivered));

		double rawCooling =
				Math.max(0D, Math.floor((c.caseTemperature * 100_000D - 10_000_000D) / 420D));
		double capCharge = Math.min(1000D, Math.min(MAX_CAPACITOR - c.capacitor, rawCooling));
		c.capacitor += capCharge;
		double boilExact = Math.max(0D, (rawCooling - capCharge) / 67D) + c.steamRemainder;
		FT_Heatable.HeatingStep steamStep = StfrCoreSimulation.waterToSteamStep();
		int waterPerOperation = steamStep == null ? 0 : steamStep.amountReq;
		int steamPerOperation = steamStep == null ? 0 : steamStep.amountProduced;
		int operationsWanted =
				!produceSteam || waterPerOperation == 0
						? 0
						: (int) Math.floor(boilExact / waterPerOperation);
		int operationsByWater =
				waterPerOperation == 0 ? 0 : resources.waterAmount() / waterPerOperation;
		int steamRoom = resources.steamRoom();
		int operationsBySteam = steamPerOperation == 0 ? 0 : steamRoom / steamPerOperation;
		int boilingOperations =
				Math.min(operationsWanted, Math.min(operationsByWater, operationsBySteam));
		c.steamBlocked =
				operationsWanted > 0 && (steamStep == null || operationsBySteam < operationsWanted);
		c.steamProduced = 0;
		if (produceSteam && boilingOperations == operationsWanted && steamStep != null) {
			resources.consumeWater(boilingOperations * waterPerOperation);
			c.steamProduced = resources.produceSteam(boilingOperations * steamPerOperation);
			c.coolingRate = rawCooling / 10D;
			c.caseTemperature -= rawCooling / 100_000D;
			c.steamRemainder = boilExact - operationsWanted * waterPerOperation;
		} else {
			c.steamRemainder = 0;
		}

		double heatRaw = (absoluteRpm + 69_000D) * 1.4285714285714286E-16D;
		heatRaw *= c.casePressure * 10_000D + 4_200_000D;
		heatRaw *= Math.min(c.coreTemperature, 2_000_000D);
		heatRaw = Math.max(0D, heatRaw * 2857.142857142857D);
		// TERF's integer arithmetic can wrap when SHC collapses. Preserve the
		// formula, but never transfer more thermal energy than the core has
		// above the case temperature.
		double availableHeat = Math.max(0D, c.coreTemperature - c.caseTemperature * 1000D) * coreShc;
		heatRaw = Math.min(heatRaw, availableHeat);
		c.heatTransferRate = heatRaw / 10_000D;
		if (c.coreTemperature / 1000D > c.caseTemperature) {
			c.caseTemperature += heatRaw / 1000D / 100_000D;
			c.coreTemperature -= heatRaw / coreShc;
		}
		c.coreTemperature = Math.max(0D, c.coreTemperature);
		c.caseTemperature = Math.max(-273.15D, c.caseTemperature);

		double shieldBefore = c.shield;
		c.shield =
				StfrShieldControl.advance(
						c.shield, c.shieldStress, workingStabilizers, electrodeCoverage);
		// Resonance search deliberately overloads the field to compress the
		// core. The displayed stress remains real, but the driven field cannot
		// consume its own integrity while the destabilizer has power.
		if (c.shieldOverdrive) c.shield = Math.max(shieldBefore, c.shield);

		c.pressure = c.coreDensity() + c.pressureBonus;
		caseDensity = c.caseDensity();
		c.casePressure =
				Math.max(
						0D,
						caseDensity
								* .000001D
								* (c.caseTemperature * 100_000D + 100_000_000D)
								/ 10_000D);
	}
}
