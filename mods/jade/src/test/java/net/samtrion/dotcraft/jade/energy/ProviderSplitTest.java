package net.samtrion.dotcraft.jade.energy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;

/** Jade refuses a data provider that is also a component provider, so the two halves must be separate classes. */
class ProviderSplitTest {
    /** No family provider is both, and each has a distinct client half that shares its UID. */
    @Test
    void halvesAreSeparateClasses() {
        for (var provider : EnergyProviders.all()) {
            var name = provider.getClass().getSimpleName();
            assertTrue(provider instanceof IServerDataProvider, name);
            assertFalse(provider instanceof IComponentProvider, name);
            var client = provider.client();
            assertTrue(client instanceof IComponentProvider, name);
            assertFalse(client instanceof IServerDataProvider, name);
            assertTrue(provider.getUid().equals(client.getUid()), name);
        }
    }
}
