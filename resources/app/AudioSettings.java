package resources.app;

/**
 * Global in-game audio preferences.
 *
 * The current codebase does not yet stream background tracks or sfx clips, so
 * this class stores the user-facing settings that audio playback code can
 * consume once those systems are wired in.
 */
public final class AudioSettings {

    private int musicVolume = 100;
    private int soundVolume = 100;

    public int musicVolume() { return musicVolume; }
    public int soundVolume() { return soundVolume; }

    /** Cycle through 100% -> 75% -> 50% -> 25% -> 0% -> 100%. */
    public int cycleMusicVolume() {
        musicVolume = nextStep(musicVolume);
        return musicVolume;
    }

    /** Cycle through 100% -> 75% -> 50% -> 25% -> 0% -> 100%. */
    public int cycleSoundVolume() {
        soundVolume = nextStep(soundVolume);
        return soundVolume;
    }

    private static int nextStep(int current) {
        return switch (current) {
            case 100 -> 75;
            case 75  -> 50;
            case 50  -> 25;
            case 25  -> 0;
            default  -> 100;
        };
    }
}
