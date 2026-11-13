package fun.aloyenz.stfr.reactor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;
import fun.aloyenz.stfr.reactor.physics.StfrSelfDestructMechanics;
import fun.aloyenz.stfr.structure.StfrBlockRole;

class StfrSelfDestructMechanicsTest {
    @Test void closesOnceAtFirstLaserHitOrAtExecutionForAnExistingCore() {
        for(boolean offline:new boolean[]{false,true}) {
            int calls=0;
            for(int tick=0;tick<3740;tick++)
                if(StfrSelfDestructMechanics.closeHatches(tick,offline)) {
                    calls++;assertEquals(offline?2455:2405,tick);
                }
            assertEquals(1,calls);
        }
        assertFalse(StfrSelfDestructMechanics.hatchesHandedOff(2454,true));
        assertTrue(StfrSelfDestructMechanics.hatchesHandedOff(2455,true));
        assertTrue(StfrSelfDestructMechanics.hatchesHandedOff(2485,false));
    }
    @Test void allHatchGroupsReachTheSamePositionAsNormalStartup() {
        assertTrue(StfrSelfDestructMechanics.accessHatch(StfrBlockRole.HATCH));
        assertTrue(StfrSelfDestructMechanics.accessHatch(StfrBlockRole.HATCH_FREE));
        assertTrue(StfrSelfDestructMechanics.accessHatch(StfrBlockRole.HATCH_FIXED));
        assertFalse(StfrSelfDestructMechanics.lockedHatchOpen(StfrBlockRole.HATCH,true));
        assertTrue(StfrSelfDestructMechanics.lockedHatchOpen(StfrBlockRole.HATCH_FREE,false));
        assertFalse(StfrSelfDestructMechanics.lockedHatchOpen(StfrBlockRole.HATCH_FREE,true));
        assertTrue(StfrSelfDestructMechanics.lockedHatchOpen(StfrBlockRole.HATCH_FIXED,true));
        assertFalse(StfrSelfDestructMechanics.lockedHatchOpen(StfrBlockRole.HATCH_FIXED,false));
        assertFalse(StfrSelfDestructMechanics.accessHatch(StfrBlockRole.SEAL));
    }
    @Test void fragmentsLaunchOutwardsOnAllSixAxesWithVariedBoundedVelocities() {
        Random random=new Random(82);
        for(EnumFacing face:EnumFacing.VALUES) {
            Vec3d direction=new Vec3d(face.getDirectionVec());
            Set<Vec3d> velocities=new HashSet<>();
            for(int i=0;i<100;i++) {
                Vec3d v=StfrSelfDestructMechanics.shellVelocity(new BlockPos(face.getDirectionVec()).offset(face,4),random);
                assertTrue(v.dotProduct(direction)>.5);
                assertTrue(v.length()<1.5);velocities.add(v);
            }
            assertEquals(100,velocities.size());
        }
    }
}
