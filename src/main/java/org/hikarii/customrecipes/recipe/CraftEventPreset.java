package org.hikarii.customrecipes.recipe;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CraftEventPreset {
    private final String name;
    private final List<SoundEvent> sounds;
    private final List<ParticleEvent> particles;
    private final List<CommandEvent> commands;
    private RecipeScope scope; 

    public CraftEventPreset(String name) {
        this.name = name;
        this.sounds = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.scope = RecipeScope.ALL;
    }

    public CraftEventPreset(String name, List<SoundEvent> sounds, List<ParticleEvent> particles,
                           List<CommandEvent> commands, RecipeScope scope) {
        this.name = name;
        this.sounds = sounds != null ? new ArrayList<>(sounds) : new ArrayList<>();
        this.particles = particles != null ? new ArrayList<>(particles) : new ArrayList<>();
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
        this.scope = scope != null ? scope : RecipeScope.ALL;
    }

    public String getName() {
        return name;
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

    public RecipeScope getScope() {
        return scope;
    }

    public void setScope(RecipeScope scope) {
        this.scope = scope;
    }

    public void addSound(SoundEvent sound) {
        sounds.add(sound);
    }

    public void removeSound(Sound sound) {
        sounds.removeIf(s -> s.getSound() == sound);
    }

    public void addParticle(ParticleEvent particle) {
        particles.add(particle);
    }

    public void removeParticle(Particle particle) {
        particles.removeIf(p -> p.getParticle() == particle);
    }

    public void addCommand(CommandEvent command) {
        commands.add(command);
    }

    public void removeCommand(CommandEvent command) {
        commands.remove(command);
    }

    public boolean hasAnyEvent() {
        return !sounds.isEmpty() || !particles.isEmpty() || !commands.isEmpty();
    }

    public CraftEvents toCraftEvents() {
        List<CraftEvents.SoundEvent> soundEvents = new ArrayList<>();
        for (SoundEvent s : sounds) {
            soundEvents.add(new CraftEvents.SoundEvent(s.getSound(), s.getVolume(), s.getPitch()));
        }

        List<CraftEvents.ParticleEvent> particleEvents = new ArrayList<>();
        for (ParticleEvent p : particles) {
            particleEvents.add(new CraftEvents.ParticleEvent(
                    p.getParticle(), p.getCount(),
                    p.getOffsetX(), p.getOffsetY(), p.getOffsetZ(),
                    p.getSpeed()
            ));
        }

        List<CraftEvents.CommandEvent> commandEvents = new ArrayList<>();
        for (CommandEvent c : commands) {
            CraftEvents.CommandEvent.CommandType type = CraftEvents.CommandEvent.CommandType.valueOf(c.getType().name());
            commandEvents.add(new CraftEvents.CommandEvent(c.getCommand(), type));
        }

        return new CraftEvents(soundEvents, particleEvents, commandEvents);
    }

    public boolean canApplyTo(boolean isCustomRecipe) {
        return switch (scope) {
            case ALL -> true;
            case CUSTOM_ONLY -> isCustomRecipe;
            case VANILLA_ONLY -> !isCustomRecipe;
        };
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

    public enum RecipeScope {
        ALL,          
        CUSTOM_ONLY,  
        VANILLA_ONLY  
    }

    public static class SoundEvent {
        private final Sound sound;
        private float volume;
        private float pitch;

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

        public void setVolume(float volume) {
            this.volume = volume;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
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

        public double getOffsetX() {
            return offsetX;
        }

        public double getOffsetY() {
            return offsetY;
        }

        public double getOffsetZ() {
            return offsetZ;
        }

        public double getSpeed() {
            return speed;
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
