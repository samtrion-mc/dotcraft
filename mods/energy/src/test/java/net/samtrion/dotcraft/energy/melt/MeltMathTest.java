package net.samtrion.dotcraft.energy.melt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import net.samtrion.dotcraft.energy.EnergyCommonConfig;

/** Unit tests for the rules of the melt generators: yields per ice, the rate limiter, the reservoir, the tiers and the economy. */
class MeltMathTest {
    private static final int[] BATTERIES = {
            EnergyCommonConfig.BATTERY_TIER_1.capacity(), EnergyCommonConfig.BATTERY_TIER_2.capacity(), EnergyCommonConfig.BATTERY_TIER_3.capacity(),
            EnergyCommonConfig.BATTERY_TIER_4.capacity(), EnergyCommonConfig.BATTERY_TIER_5.capacity()};

    private static int peak(int tier) {
        return EnergyCommonConfig.MELT_GENERATORS.get(tier - 1).peak();
    }

    /** The better the ice, the more FE; by default one FE costs the same ice, as packed ice is nine ice and blue ice nine packed ice. */
    @Test
    void yieldPerIce() {
        assertEquals(1_000, MeltIce.ICE.yield());
        assertEquals(9 * MeltIce.ICE.yield(), MeltIce.PACKED_ICE.yield());
        assertEquals(9 * MeltIce.PACKED_ICE.yield(), MeltIce.BLUE_ICE.yield());
        assertTrue(MeltIce.BLUE_ICE.yield() > MeltIce.PACKED_ICE.yield() && MeltIce.PACKED_ICE.yield() > MeltIce.ICE.yield());
    }

    /** The rate limiter never releases more than the peak, the reservoir or the room of the buffer. */
    @Test
    void releaseIsLimited() {
        assertEquals(15, MeltMath.release(9_000, 15, 9_000));
        assertEquals(7, MeltMath.release(7, 15, 9_000));
        assertEquals(4, MeltMath.release(9_000, 15, 4));
        assertEquals(0, MeltMath.release(9_000, 15, 0));
        assertEquals(0, MeltMath.release(0, 15, 9_000));
        assertEquals(0, MeltMath.release(-5, 15, 9_000));
    }

    /** Releasing a block of every kind takes at least yield / peak ticks, however often it is asked. */
    @Test
    void blockLastsLongEnough() {
        for (int tier = 1; tier <= 5; tier++) {
            for (var ice : MeltIce.values()) {
                long pending = ice.yield();
                long ticks = 0;
                while (pending > 0) {
                    pending -= MeltMath.release(pending, peak(tier), Integer.MAX_VALUE);
                    ticks++;
                }
                assertEquals(MeltMath.fillTicks(ice.yield(), peak(tier)), ticks, "tier " + tier + " " + ice);
            }
        }
    }

    /** A block melts if it fits into the reservoir, or if the reservoir is empty; never when it is full. */
    @Test
    void reservoir() {
        int peak = peak(1);
        long reserve = MeltMath.reserve(peak);
        assertEquals(20 * 600L, reserve);
        assertTrue(MeltMath.mayMelt(0, MeltIce.ICE.yield(), peak));
        assertTrue(MeltMath.mayMelt(0, MeltIce.BLUE_ICE.yield(), peak), "a big block must burn when the reservoir is empty");
        assertFalse(MeltMath.mayMelt(1, MeltIce.BLUE_ICE.yield(), peak), "a big block must not melt on top of a filled reservoir");
        assertTrue(MeltMath.mayMelt(reserve - 1_000, 1_000, peak));
        assertFalse(MeltMath.mayMelt(reserve - 999, 1_000, peak));
        assertFalse(MeltMath.mayMelt(0, 0, peak), "a block that gives nothing is not melted");
    }

    /** Every tier has more slots or a faster pulse or a new ability than the one below. */
    @Test
    void everyTierHasAPerk() {
        for (int tier = 2; tier <= 5; tier++) {
            var lower = MeltMath.tier(tier - 1);
            var higher = MeltMath.tier(tier);
            assertTrue(higher.slots() > lower.slots() || higher.interval() < lower.interval() || (higher.water() && !lower.water()), "tier " + tier);
            assertTrue(higher.slots() >= lower.slots() && higher.interval() <= lower.interval() && (higher.water() || !lower.water()), "tier " + tier + " is worse");
        }
        assertFalse(MeltMath.tier(1).water());
        assertTrue(MeltMath.tier(4).water() && MeltMath.tier(5).water());
        assertTrue(MeltMath.tier(5).slots() <= 5, "at most five sides can hold ice, the sixth must hold the heat");
    }

    /** The peaks follow the battery ladder, tier 1 makes at least 20 FE/t. */
    @Test
    void peakLadder() {
        assertEquals(20, peak(1));
        assertEquals(75, peak(2));
        assertEquals(560, peak(3));
        assertEquals(5_600, peak(4));
        assertEquals(56_000, peak(5));
    }

    /** A fed generator fills the battery of its tier in well under one MC day (a tenth of it), and its daily yield is many batteries. */
    @Test
    void dailyYieldFillsTheBattery() {
        for (int tier = 1; tier <= 5; tier++) {
            long fill = MeltMath.fillTicks(BATTERIES[tier - 1], peak(tier));
            assertTrue(fill * 10 < MeltMath.DAY_TICKS, "tier " + tier + " needs " + fill + " ticks");
            assertTrue(MeltMath.dailyYield(peak(tier)) >= 10L * BATTERIES[tier - 1], "tier " + tier);
        }
        assertEquals(480_000, MeltMath.dailyYield(20));
    }

    /**
     * The price: the daily yield of tier 1 costs 480 ice, which is 53 packed ice or 5.9 blue ice, whichever is burnt; and the
     * peak of every tier can be reached with blue ice, the higher tiers cannot with plain ice (they need the compact fuels).
     */
    @Test
    void costAndSupply() {
        assertEquals(480, MeltMath.dailyYield(peak(1)) / MeltIce.ICE.yield());
        assertEquals(53, MeltMath.dailyYield(peak(1)) / MeltIce.PACKED_ICE.yield());
        for (int tier = 1; tier <= 5; tier++) {
            assertTrue(MeltMath.supply(tier, MeltIce.BLUE_ICE.yield()) >= peak(tier), "tier " + tier + " cannot reach its peak with blue ice");
        }
        assertTrue(MeltMath.supply(1, MeltIce.ICE.yield()) >= peak(1), "tier 1 reaches its peak with plain ice");
        assertTrue(MeltMath.supply(5, MeltIce.ICE.yield()) < peak(5), "tier 5 cannot run on plain ice");
    }

    /** The buffer of a tier holds 600 ticks of the peak and an operation gives five times the peak, as for the panels. */
    @Test
    void bufferDefaults() {
        for (int tier = 1; tier <= 5; tier++) {
            var config = EnergyCommonConfig.MELT_GENERATORS.get(tier - 1);
            assertEquals(MeltMath.reserve(peak(tier)), config.capacity());
            assertEquals(5 * peak(tier), config.maxOutput());
        }
    }

    /** Tiers 4 and 5 are burst tiers: a day at peak costs far more blue ice than a player can farm, tier 1 does not. */
    @Test
    void sustainedSupplyPerDay() {
        long[] blueIcePerDay = new long[5];
        for (int tier = 1; tier <= 5; tier++) blueIcePerDay[tier - 1] = MeltMath.dailyYield(peak(tier)) / MeltIce.BLUE_ICE.yield();
        assertEquals(5, blueIcePerDay[0]);
        assertTrue(blueIcePerDay[3] > 1_000 && blueIcePerDay[4] > 10_000, "tiers 4 and 5 are burst tiers");
        for (int tier = 1; tier <= 5; tier++) {
            long reservoirTicks = MeltMath.reserve(peak(tier)) / peak(tier);
            assertEquals(MeltMath.RESERVE_TICKS, reservoirTicks);
        }
    }

    /** Tier 4 gives one bucket per block, tier 5 the water of the ice a block is made of, and nothing where water evaporates. */
    @Test
    void completeRecoveryOnlyAtTierFive() {
        int ice = 1_000;
        for (int yield : new int[] {ice, 9 * ice, 81 * ice}) {
            assertEquals(0, MeltMath.waterBlocks(MeltMath.tier(1), false, yield, ice));
            assertEquals(0, MeltMath.waterBlocks(MeltMath.tier(3), false, yield, ice));
            assertEquals(1, MeltMath.waterBlocks(MeltMath.tier(4), false, yield, ice));
            assertEquals(0, MeltMath.waterBlocks(MeltMath.tier(4), true, yield, ice));
            assertEquals(0, MeltMath.waterBlocks(MeltMath.tier(5), true, yield, ice));
        }
        assertEquals(1, MeltMath.waterBlocks(MeltMath.tier(5), false, ice, ice));
        assertEquals(9, MeltMath.waterBlocks(MeltMath.tier(5), false, 9 * ice, ice));
        assertEquals(81, MeltMath.waterBlocks(MeltMath.tier(5), false, 81 * ice, ice));
        assertEquals(1, MeltMath.waterBlocks(MeltMath.tier(5), false, 10, ice), "never less than one bucket");
        assertTrue(MeltMath.tier(5).complete() && !MeltMath.tier(4).complete());
    }

    /** The tank holds 32 buckets; water that does not fit is lost, and a full tank cuts the FE of the ice to a quarter. */
    @Test
    void tankAndEfficiency() {
        assertEquals(32_000, MeltMath.TANK_CAPACITY_MB);
        assertEquals(1_000, MeltMath.tankFit(0, 1_000));
        assertEquals(500, MeltMath.tankFit(31_500, 9_000), "only the room is taken");
        assertEquals(0, MeltMath.tankFit(32_000, 1_000), "a full tank takes nothing");
        assertEquals(0, MeltMath.tankFit(0, 0));
        // blue ice at tier 5 is 81 buckets: more than the whole tank
        assertEquals(32_000, MeltMath.tankFit(0, 81 * MeltMath.BUCKET_MB));
        for (var ice : MeltIce.values()) {
            assertEquals(ice.yield(), MeltMath.effectiveYield(ice.yield(), false));
            assertEquals(ice.yield() / 4, MeltMath.effectiveYield(ice.yield(), true), "75 percent less while the tank is full");
        }
    }

    /** Water is left by the tiers with the perk, never where water evaporates. */
    @Test
    void waterOnlyWhereItStays() {
        assertTrue(MeltMath.leavesWater(MeltMath.tier(4), false));
        assertFalse(MeltMath.leavesWater(MeltMath.tier(4), true));
        assertFalse(MeltMath.leavesWater(MeltMath.tier(5), true));
        assertFalse(MeltMath.leavesWater(MeltMath.tier(1), false));
    }

    /** The comparator signal is 0 only when empty and 15 only when full. */
    @Test
    void comparatorSignal() {
        assertEquals(0, MeltMath.signal(0, 1_000));
        assertEquals(1, MeltMath.signal(1, 1_000));
        assertEquals(8, MeltMath.signal(500, 1_000));
        assertEquals(15, MeltMath.signal(1_000, 1_000));
        assertEquals(15, MeltMath.signal(5_000, 1_000));
        assertEquals(0, MeltMath.signal(5, 0));
    }

    /** A removed generator gives its FE to the neighbours one after the other; what does not fit is lost. */
    @Test
    void handOffFillsNeighbours() {
        var a = new SimpleEnergyHandler(300, 1_000, 1_000);
        var b = new SimpleEnergyHandler(500, 1_000, 1_000);
        assertEquals(0, MeltEnergy.handOff(600, List.of(a, b)));
        assertEquals(300, a.getAmountAsInt());
        assertEquals(300, b.getAmountAsInt());
        assertEquals(800, MeltEnergy.handOff(1_000, List.of(a, b)), "only the 200 FE of room in b fit");
        assertEquals(500, b.getAmountAsInt());
    }
}
