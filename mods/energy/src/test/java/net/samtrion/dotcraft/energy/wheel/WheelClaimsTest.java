package net.samtrion.dotcraft.energy.wheel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for the claims that let a mob drive one wheel only. */
class WheelClaimsTest {
    /** A claimed mob is refused by another wheel, the owner keeps it. */
    @Test
    void oneWheelPerMob() {
        var claims = new WheelClaims<String, String>();
        assertTrue(claims.claim("pig", "a", 100));
        assertFalse(claims.claim("pig", "b", 100));
        assertFalse(claims.claim("pig", "b", 110));
        assertTrue(claims.claim("pig", "a", 120));
        assertTrue(claims.claim("cow", "b", 120), "another mob is free");
    }

    /** A claim that is not renewed lapses, so a removed wheel frees its mobs. */
    @Test
    void claimLapses() {
        var claims = new WheelClaims<String, String>();
        assertTrue(claims.claim("pig", "a", 100));
        assertFalse(claims.claim("pig", "b", 100 + WheelClaims.LAPSE_TICKS));
        assertTrue(claims.claim("pig", "b", 101 + WheelClaims.LAPSE_TICKS));
        assertFalse(claims.claim("pig", "a", 102 + WheelClaims.LAPSE_TICKS), "the new owner holds it now");
    }

    /** Asking whether a mob is free does not claim it. */
    @Test
    void freeDoesNotClaim() {
        var claims = new WheelClaims<String, String>();
        assertTrue(claims.free("pig", "a", 100));
        assertTrue(claims.free("pig", "b", 100));
        assertTrue(claims.claim("pig", "a", 100));
        assertFalse(claims.free("pig", "b", 101));
        assertTrue(claims.free("pig", "a", 101));
        assertTrue(claims.free("pig", "b", 101 + WheelClaims.LAPSE_TICKS));
    }

    /** A released wheel frees its mobs at once. */
    @Test
    void releaseFreesTheMobs() {
        var claims = new WheelClaims<String, String>();
        assertTrue(claims.claim("pig", "a", 100));
        claims.release("a");
        assertTrue(claims.claim("pig", "b", 101));
    }
}
