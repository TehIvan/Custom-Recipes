package org.hikarii.customrecipes.recipe;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CraftEvents {
    private final List<SoundEvent> sounds;
    private final List<ParticleEvent> particles;
    private final List<CommandEvent> commands;

    public CraftEvents(List<SoundEvent> sounds, List<ParticleEvent> particles, List<CommandEvent> commands) {
        this.sounds = sounds != null ? sounds : new ArrayList<>();
        this.particles = particles != null ? particles : new ArrayList<>();
        this.commands = commands != null ? commands : new ArrayList<>();
    }

    public CraftEvents() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public boolean hasAnyEvent() {
        return !sounds.isEmpty() || !particles.isEmpty() || !commands.isEmpty();
    }

    public List<SoundEvent> getSounds() {
        return sounds;
    }

    public List<ParticleEvent> getParticles() {
        return particles;
    }

    public List<CommandEvent> getCommands() {
        return commands;
    }

    public void execute(Player player) {
        Location loc = player.getLocation();

        for (SoundEvent sound : sounds) {
            sound.play(player);
        }

        for (ParticleEvent particle : particles) {
            particle.spawn(player, loc);
        }

        for (CommandEvent command : commands) {
            command.execute(player);
        }
    }

    public static class SoundEvent {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundEvent(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public SoundEvent(Sound sound) {
            this(sound, 1.0f, 1.0f);
        }

        public void play(Player player) {
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }

        public Sound getSound() {
            return sound;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }
    }

    public static class ParticleEvent {
        private final Particle particle;
        private final int count;
        private final double offsetX;
        private final double offsetY;
        private final double offsetZ;
        private final double speed;

        public ParticleEvent(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double speed) {
            this.particle = particle;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
        }

        public ParticleEvent(Particle particle, int count) {
            this(particle, count, 0.5, 0.5, 0.5, 0.1);
        }

        public ParticleEvent(Particle particle) {
            this(particle, 10);
        }

        public void spawn(Player player, Location location) {
            if (particle != null) {
                player.getWorld().spawnParticle(
                        particle,
                        location.clone().add(0, 1, 0),
                        count,
                        offsetX, offsetY, offsetZ,
                        speed
                );
            }
        }

        public Particle getParticle() {
            return particle;
        }

        public int getCount() {
            return count;
        }
    }

    public static class CommandEvent {
        private final String command;
        private final CommandType type;

        public CommandEvent(String command, CommandType type) {
            this.command = command;
            this.type = type;
        }

        public CommandEvent(String command) {
            this(command, CommandType.CONSOLE);
        }

        public void execute(Player player) {
            if (command == null || command.isEmpty()) return;

            String cmd = command
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString())
                    .replace("{world}", player.getWorld().getName())
                    .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
                    .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
                    .replace("{z}", String.valueOf(player.getLocation().getBlockZ()));

            switch (type) {
                case CONSOLE:
                    player.getServer().dispatchCommand(
                            player.getServer().getConsoleSender(),
                            cmd
                    );
                    break;
                case PLAYER:
                    player.performCommand(cmd);
                    break;
                case OP:
                    boolean wasOp = player.isOp();
                    try {
                        player.setOp(true);
                        player.performCommand(cmd);
                    } finally {
                        if (!wasOp) {
                            player.setOp(false);
                        }
                    }
                    break;
            }
        }

        public String getCommand() {
            return command;
        }

        public CommandType getType() {
            return type;
        }

        public enum CommandType {
            CONSOLE,  
            PLAYER,   
            OP        
        }
    }
}
