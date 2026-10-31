package net.samtrion.dotcraft.jei.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.samtrion.dotcraft.jei.energy.WheelYield.MobStats;

/** The entries of the wheel yield page: the numbers follow the formulas passed in, the order is by weight and stable. */
class WheelYieldTest {
    private static final List<Integer> PEAKS = List.of(10, 100, 1000, 10_000, 100_000);

    /** Stand-ins: weight is health / 10 (at least 1 from tier 3 on), share is {@code min(1, weight * activity / 2)}. */
    private static List<WheelYield.Entry> build(List<MobStats> mobs) {
        return WheelYield.entries(mobs, PEAKS, (h, s, t) -> t >= 3 ? Math.max(1, h / 10) : h / 10,
                (w, run) -> Math.min(1, w * (run ? 1 : 0.5) / 2));
    }

    /** Output is peak times share per tier, running and standing. */
    @Test
    void numbersFollowPeakAndShare() {
        var e = build(List.of(new MobStats("a:b", 10, 0.25))).get(0);
        assertEquals(List.of(5.0, 50.0, 500.0, 5000.0, 50_000.0), e.running());
        assertEquals(List.of(2.5, 25.0, 250.0, 2500.0, 25_000.0), e.standing());
    }

    /** A light mob counts as weight 1 from tier 3 on, and nothing exceeds the peak. */
    @Test
    void tierRuleAndCap() {
        var light = build(List.of(new MobStats("a:light", 2, 0.25))).get(0);
        assertEquals(0.2, light.weights().get(0), 1e-9);
        assertEquals(1.0, light.weights().get(2), 1e-9);
        var big = build(List.of(new MobStats("a:big", 400, 0.25))).get(0);
        assertEquals(100_000.0, big.running().get(4));
    }

    /** Heaviest first; equal weights by id. */
    @Test
    void sortedByWeightThenId() {
        var ids = build(List.of(new MobStats("z:x", 10, 1), new MobStats("a:y", 10, 1), new MobStats("m:h", 40, 1), new MobStats("m:l", 4, 1)))
                .stream().map(WheelYield.Entry::id).toList();
        assertEquals(List.of("m:h", "a:y", "z:x", "m:l"), ids);
    }

    /** Number formatting. */
    @Test
    void formatting() {
        assertEquals("1.3", WheelYield.fe(1.26));
        assertEquals("56,000", WheelYield.fe(56_000));
        assertEquals("1.0 / 100", WheelYield.perTier(List.of(1.0, 100.0)));
    }

    /** Every language carries the page keys with the right number of arguments. */
    @Test
    void languagesCarryTheKeys() throws java.io.IOException {
        for (String locale : List.of("en_us", "de_de")) {
            var path = "/assets/" + net.samtrion.dotcraft.jei.JeiMod.MODID + "/lang/" + locale + ".json";
            try (var in = WheelYieldTest.class.getResourceAsStream(path)) {
                org.junit.jupiter.api.Assertions.assertNotNull(in, path);
                var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                WheelYield.KEYS.forEach((key, args) -> {
                    org.junit.jupiter.api.Assertions.assertTrue(json.has(key), locale + " " + key);
                    var m = java.util.regex.Pattern.compile("%(\\d+)\\$s").matcher(json.get(key).getAsString());
                    int count = 0;
                    while (m.find()) count = Math.max(count, Integer.parseInt(m.group(1)));
                    assertEquals(args, count, locale + " " + key);
                });
            }
        }
    }
}
