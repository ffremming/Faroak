package resources.app;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Simple looped background-music player backed by Java Sound Clip.
 */
public final class BackgroundMusicPlayer implements AutoCloseable {

    private static final String TRACK_PATH = "resources/sound/music/background.wav";

    private final AudioSettings settings;
    private Clip clip;
    private FloatControl gainControl;
    private boolean loadAttempted;
    private int lastVolumePercent = Integer.MIN_VALUE;

    public BackgroundMusicPlayer(AudioSettings settings) {
        this.settings = settings;
    }

    /** Start playback in a continuous loop (no-op if file is unavailable). */
    public void startLoop() {
        Clip loaded = ensureClip();
        if (loaded == null) return;
        applyMusicVolume();
        if (!loaded.isRunning()) {
            loaded.setFramePosition(0);
            loaded.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /** Re-apply current audio settings (currently music volume only). */
    public void syncSettings() {
        applyMusicVolume();
    }

    private Clip ensureClip() {
        if (clip != null) return clip;
        if (loadAttempted) return null;
        loadAttempted = true;

        File track = new File(TRACK_PATH);
        if (!track.exists()) {
            System.err.println("Background music not found: " + TRACK_PATH);
            return null;
        }

        try (AudioInputStream in = AudioSystem.getAudioInputStream(track)) {
            Clip loaded = AudioSystem.getClip();
            loaded.open(in);
            clip = loaded;
            if (loaded.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                gainControl = (FloatControl) loaded.getControl(FloatControl.Type.MASTER_GAIN);
            }
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            System.err.println("Failed to load background music '" + TRACK_PATH + "': " + ex.getMessage());
            return null;
        }
    }

    private void applyMusicVolume() {
        int volume = clampPercent(settings.musicVolume());
        if (volume == lastVolumePercent) return;
        lastVolumePercent = volume;
        if (gainControl == null) return;

        float min = gainControl.getMinimum();
        float max = gainControl.getMaximum();
        float gainDb;
        if (volume <= 0) {
            gainDb = min;
        } else {
            gainDb = (float) (20.0 * Math.log10(volume / 100.0));
            if (gainDb < min) gainDb = min;
            if (gainDb > max) gainDb = max;
        }
        gainControl.setValue(gainDb);
    }

    private static int clampPercent(int percent) {
        if (percent < 0) return 0;
        if (percent > 100) return 100;
        return percent;
    }

    @Override
    public void close() {
        if (clip == null) return;
        clip.stop();
        clip.close();
        clip = null;
        gainControl = null;
    }
}
