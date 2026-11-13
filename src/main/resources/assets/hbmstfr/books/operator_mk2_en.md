# STFR
## Operator's Manual
### Second Edition

To understand the reactor, look for the cause behind every reading. Keep this book at the console. Open it before you turn a control.

---

## 1. Your Machine

STFR means Stabilizers Thermonuclear Fusion Reactor.

A plasma core sits at the center. Around it stand six sections with fuel slots, rotors, and electrodes. All six should be in working order.

---

## 2. Three Spaces

Fuel from the capsules enters the core. The reaction there produces helium-4.

Matter can pass through the magnetic shield into the casing. Venting removes it from there.

Water, steam, and products have shared tanks.

---

## 3. Before Starting

Check the structure and open chamber space with the analyzer. Inspect all six sections, ports, and hatches.

Fit deuterium capsules and lock their slots. Supply water, lubricant, and power. Give steam a clear outlet.

---

## 4. Console and Network

Data cable connects the mainframe and instruments. Transmitters receive commands; receivers serve the monitors.

If a screen says “NO SIGNAL,” check cable, power, and network load with the analyzer. The core may be fine.

---

## 4a. Screen Readings

CAP, MSSR, CPRESS, and the other short names in this book are monitor readings.

Open a monitor editor and enter a name with a $ sign, such as $CAP, $MSSR, or $CPRESS. The screen will show its current value.

---

## 5. KEY and SSTB

KEY permits a start. SSTB means Start/Stop Transition Button.

Starting needs one SSTB pulse to lock the hatches and another, separate pulse to confirm. Holding the button down achieves nothing.

---

## 6. Starting Sequence

Turn on KEY and pulse SSTB. Wait for the hatches to lock and all 15 breakers to close.

Check again, then give SSTB a second pulse. Watch the reactor state until the monitor shows ONLINE.

---

## 7. Startup Power

During confirmed startup, the reactor needs 1,000,000 HE each operating cycle. HE is the console's energy unit.

A lasting power loss will abort the start. Check stored energy and the supply line before the second SSTB pulse.

---

## 8. First Minutes ONLINE

Watch core temperature, core pressure, shield, total stress, water, and steam together.

Stable operation means heat leaves, the shield holds, pressure stays level, and the reaction continues.

---

## 8a. Controls

Use the signal modifier to assign increase and decrease commands for fuel feed, RF, OFFSET, LASER, PERM, and VENT to transmitter faces.

A short pulse moves a setting. Wait for the instruments after each pulse.

---

## 9. Two Thermometers

CTEMP is core temperature in thousands of kelvins. The working range begins around 31,500,000 kK.

TEMP is casing temperature in °C. At 950°C it is too hot; at 1010°C the condition is critical.

---

## 10. Reaction

RR shows how much fuel reacts. RXHE is its released power. GEN is electricity accepted by the output.

If GEN falls while RR and RXHE remain steady, check the accumulator and electrodes before adding fuel.

---

## 11. Fuel Feed

FUEL sets injection from working sections. FLEFT is fuel left in ready capsules; FTYPE identifies it.

More feed adds matter to the core and can raise pressure. An empty or unlocked capsule cannot feed its section.

---

## 12. RF Suppress

RF Suppress restrains the reaction. A higher RF setting means less fuel burns and less heat is released.

If the core heats too quickly, raise RF a little at a time. If the reaction fades, check RF, fuel, and temperature.

---

## 13. Lasers

LASER spends charge from the internal capacitor, CAP, to heat the core.

Lasers can support cooling plasma, but CAP is finite. Raising LASER when CAP is empty will not help. Lasers cannot replace fuel.

---

## 14. Why It Is Hot

Fuel particles must move fast for a reaction to begin. Plasma is therefore far hotter than the casing.

Lasers provide the first heat. The reaction then sustains it as long as energy gained covers the losses.

---

## 15. Shield

SHIELD shows containment integrity. The electrodes form a field around the plasma; all 30 chains contribute.

Damaged chains weaken the field and power collection. At stress 100, the shield degrades. If SHIELD falls, find the cause.

---

## 16. Core Pressure

CPRESS rises as fuel and helium-4 collect in the core.

Too much pressure strains the shield. Too little leaves the plasma without firm containment. Aim for balance, not the lowest possible reading.

---

## 17. Three Stresses

PSTRESS comes from pressure. RSTRESS comes from poor rigidity at low pressure. SPSTRESS comes from excessive spin.

SSTRESS combines the three. Find out which part is rising before you choose a control.

---

## 18. Spin

OFFSET changes the speed of the stabilizers' field. Spin helps contain dense plasma and transfer heat.

Slow spin leaves the shield without reserve. Too much spin creates SPSTRESS. Adjust OFFSET in small steps.

---

## 19. MSSR

MSSR means Magnetic Stabilizer Spin Reserve.

The moving field produces it. Higher MSSR takes some pressure load off the shield. Do not spin the stabilizers without limit: spin stress will rise.

---

## 20. Two Speed Gauges

RPM shows the calculated spin of the core field. TRPM belongs to the visible section rotors.

They are separate readings. To understand MSSR or SPSTRESS, look first at RPM and OFFSET.

---

## 21. Permeability

Shield Perm, or PERM, lets some matter pass from the core into the casing space.

This eases core pressure, but the matter must then leave the casing. Flow changes gradually. Watch CPRESS and PRESS.

---

## 22. Venting

Pressure Vent, or VENT, removes matter from the casing space.

Helium-4 enters the product tank while there is room. Vented fuel is lost. When opening VENT, watch what PERM is sending into the casing.

---

## 23. Helium's Route

The reaction leaves helium-4 behind. It begins in the core, passes through the shield into the casing, then reaches the product tank.

If this route is closed, helium collects and raises pressure. Watch CHE4, CASEHE4, and PROD.

---

## 24. Heat's Route

The core passes heat to the casing. The casing heats water and turns it into steam.

A cool casing does not mean a cool core. After a strong reaction, the casing may keep heating even after you increase suppression.

---

## 25. Water and Steam

WATER shows coolant supply; STEAM shows how full the steam tank is.

Without water or room for steam, boiling stops and casing cooling fails. Keep water supplied and the steam outlet open.

---

## 26. Capacitor

The hot casing charges the CAP capacitor. Lasers spend that charge to heat the plasma.

Water cools the casing: it carries away the remaining heat and turns to steam. CAP stores energy for lasers, not water.

---

## 27. Electric Output

Electrodes collect energy from the reaction. A strong reaction and sound electrode coverage produce GEN.

A full accumulator can bring GEN to zero while the core still runs. Compare POWER, RXHE, and electrode condition.

---

## 28. Lubricant and Repairs

Working mechanisms need LUBE. Running dry can cause defects shown by MAINT.

Find the marked block and repair it with a wrench. A neglected defect will eventually break the block and may disable its whole section.

---

## 29. Cooling Core

Check FLEFT, locked capsules, RF, RR, and CAP.

If fuel is available, ease RF slowly. Lasers can restore heat. Watch CPRESS: excessive venting of core matter can also disrupt operation.

---

## 30. Overheating Core

Raise RF Suppress by a small step. Make sure heat reaches the casing and water carries it away.

Check core pressure and shield. High temperature alone does not tell you which system has failed.

---

## 31. Rising Pressure

Watch CPRESS, PSTRESS, and MSSR. Check for accumulated fuel and helium.

Carefully balance feed, PERM, and VENT. Matter moved to the casing has not vanished; watch PRESS afterward.

---

## 32. Falling Shield

First find out whether PSTRESS, RSTRESS, or SPSTRESS is rising. Then inspect all six sections and electrode chains.

The system warns at SHIELD 75%, 50%, and 25%. Do not wait until it reaches zero.

---

## 33. Hot Casing

At TEMP 950°C, look for lost cooling. Check WATER, STEAM, flow through the ports, and section operation.

If the steam tank is full, clear its outlet. At 1010°C the casing is in critical condition.

---

## 34. Normal Shutdown

For a safe stop, SHIELD must be at least 60% and SSTRESS no more than 20%.

Bring the reactor into that range, turn off KEY, and pulse SSTB. Wait for the stop to finish and hatches to unlock before entering the chamber.

---

## 35. Emergencies

The reactor has several emergency states. Each has its own cause and recovery procedure.

Read the mainframe message. Remove the cause, then confirm recovery with SSTB if asked. Do not press it blindly.

---

## 36. Holding the Regime

Wait for the instruments after each adjustment. PERM and VENT change flow gradually; the casing also takes time to cool.

Change one setting and watch CTEMP, CPRESS, TEMP, SHIELD, and SSTRESS. Then decide what to do next.

---

## 37. Calculation Notes

The next pages explain how readings relate to one another once the reactor is stable.

Letters stand for instrument readings. Rounding down means discarding the fractional part.

---

## 38. Reaction Temperature

Particle energy in keV equals CTEMP × 0.00008617718.

Deuterium reaction peaks near 2715 keV. The working band is 31.5 to 47.3 million kK. Above the peak, more heat does not automatically strengthen the reaction.

---

## 39. Suppression

Reaction strength depends on RF Suppress and the number of working stabilizer groups.

Its multiplier is (400 − RF × S) / 4, rounded down; S is no higher than 4. When sections fail, the same RF setting suppresses less.

---

## 40. Spin Reserve

MSSR counts complete groups of 2500 RPM.

Pressure stress is CPRESS / 60,000, rounded down, minus MSSR. A negative result counts as zero. This is how spin helps the shield hold a dense core.

---

## 41. Low Pressure

Rigidity stress is (1,000,000 − CPRESS) / 1000, rounded down. A negative result counts as zero.

Thus excessive permeability and weak fuel feed can strain the shield just as an overfilled core can.

---

## 42. Excess Speed

Spin stress first rises about one point per 4000 RPM. Above 160,000 RPM, an additional load grows much faster.

MSSR is not an endless benefit: beyond useful speed, the shield pays for the spin.

---

## 43. Permeability and Vent

Each operating cycle, a fraction of the core's contents equal to PERM / 5000 passes through the shield.

A fraction equal to VENT / 20000 leaves the casing. Old contents leave before new matter arrives from the core.

---

## 44. Final Check

A stable reactor has its own rhythm: fuel enters, the reaction holds, steam leaves, and the shield recovers.

If one reading turns alarming, seek its cause in the other instruments. They will show trouble before the final alarm sounds.
