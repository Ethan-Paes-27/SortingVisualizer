import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Makes the beeps. Small values get HIGH pitches, large values get LOW pitches.
 * Tones are generated as sine waves, so no sound files are needed.
 *
 * (Framework file - students do not need to edit this.)
 */
public final class TonePlayer {

    private static final float RATE = 44100f;
    private static final double HIGH_HZ = 1400.0;  // pitch for the smallest value
    private static final double LOW_HZ = 120.0;    // pitch for the largest value

    private SourceDataLine line;
    private final AtomicInteger pending = new AtomicInteger();
    private final ExecutorService audioThread = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "tone-player");
            t.setDaemon(true);
            return t;
        }
    });

    public TonePlayer() {
        try {
            AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, (int) (RATE * 2 * 0.08)); // ~80 ms buffer keeps sound in sync with the bars
            line.start();
        } catch (Exception e) {
            line = null;  // no sound card available - the program still works silently
        }
    }

    public boolean isAvailable() {
        return line != null;
    }

    /**
     * Turn a value into a pitch. t = 0 for the smallest value, 1 for the largest.
     * Uses an exponential scale so equal steps in value sound like equal steps in pitch.
     */
    public static double frequencyFor(double key, double min, double max) {
        double t = (max > min) ? (key - min) / (max - min) : 0.5;
        return HIGH_HZ * Math.pow(LOW_HZ / HIGH_HZ, t);
    }

    /** Play several frequencies together (e.g. the two values being compared). */
    public void play(final double[] freqs, final int ms) {
        if (line == null || freqs.length == 0) return;
        if (pending.get() > 1) return;  // too far behind: skip this beep so sound stays in sync
        pending.incrementAndGet();
        audioThread.submit(new Runnable() {
            public void run() {
                try {
                    byte[] buf = synth(freqs, ms);
                    line.write(buf, 0, buf.length);
                } finally {
                    pending.decrementAndGet();
                }
            }
        });
    }

    /** Play a quick run of notes one after another (the "victory sweep" at the end). */
    public void sweep(final double[] freqs, final int msEach) {
        if (line == null || freqs.length == 0) return;
        audioThread.submit(new Runnable() {
            public void run() {
                for (double f : freqs) {
                    byte[] buf = synth(new double[] { f }, msEach);
                    line.write(buf, 0, buf.length);
                }
            }
        });
    }

    public void stopAll() {
        if (line != null) line.flush();
    }

    private static byte[] synth(double[] freqs, int ms) {
        int n = Math.max(1, (int) (RATE * ms / 1000));
        byte[] buf = new byte[n * 2];
        int fade = Math.max(1, Math.min(n / 4, (int) (RATE * 0.006)));  // short fade in/out avoids clicks
        for (int k = 0; k < n; k++) {
            double t = k / RATE;
            double s = 0;
            for (double f : freqs) s += Math.sin(2 * Math.PI * f * t);
            s /= freqs.length;

            double env = 1.0;
            if (k < fade) env = (double) k / fade;
            else if (k > n - fade) env = (double) (n - k) / fade;

            short v = (short) (s * env * 0.30 * Short.MAX_VALUE);
            buf[2 * k] = (byte) (v & 0xff);
            buf[2 * k + 1] = (byte) ((v >> 8) & 0xff);
        }
        return buf;
    }
}
