package org.hikarii.customrecipes.util;

import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;

public class ParticleVersionAdapter {
    private static final int minorVersion = MaterialVersionAdapter.getMinorVersion();

    private static final String[] WORKING_PARTICLES = {
            "EXPLOSION_NORMAL",
            "EXPLOSION_LARGE",
            "EXPLOSION_HUGE",
            "DAMAGE_INDICATOR",
            "SWEEP_ATTACK",
            "SPIT",
            "SQUID_INK",
            "CAMPFIRE_COSY_SMOKE",
            "CAMPFIRE_SIGNAL_SMOKE",
            
            "GLOW",
            "GLOW_SQUID_INK",
            "WAX_ON",
            "WAX_OFF",
            "ELECTRIC_SPARK",
            "SCRAPE",
            
            "SCULK_SOUL",
            "SCULK_CHARGE_POP",
            "SONIC_BOOM",
            
            "CHERRY_LEAVES"
    };

    public static List<Particle> getSortedParticles() {
        List<Particle> result = new ArrayList<>();
        for (String name : WORKING_PARTICLES) {
            try {
                result.add(Particle.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                
            }
        }
        return result;
    }

    public static int getMinorVersion() {
        return minorVersion;
    }
}
