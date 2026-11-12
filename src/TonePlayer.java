import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Makes the beeps. Sound plays only for COMPARISONS: the two values being compared
 * are played as two quick notes. Small values = HIGH pitch, large values = LOW pitch.
 * Tones are generated as sine waves, so no sound files are needed.
 *
 * (Framework file - students do not need to edit this.)
 */
public final class TonePlayer {

    private static final float RATE = 44100f;
    private static final double HIGH_HZ = 1400.0;  // pitch for the smallest value
    private static final double LOW_HZ = 120.0;    // pitch for the largest value

    /** Never let more than this much sound pile up waiting to be played. */
    private static final double MAX_LAG_SECONDS = 0.06;

    private SourceDataLine line;
    private final AtomicInteger generation = new AtomicInteger();  // bumped by stopAll() to cancel queued notes
    private final AtomicInteger pending = new AtomicInteger();     // note groups waiting in the queue
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
            SourceDataLine l = AudioSystem.getSourceDataLine(fmt);
            l.open(fmt, (int) (RATE * 2 * 0.08)); // ask for ~80 ms (some computers give a bigger buffer anyway)
            l.start();
            line = l;
        } catch (Exception e) {
            line = null;  // no sound card available - the program still works silently
        }
    }

    /** For testing: use a given audio line. */
    TonePlayer(SourceDataLine line) {
        this.line = line;
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

    /**
     * Play notes one right after another (used for a comparison: first the
     * value on the left, then the value on the right).
     *
     * Keeping in sync: if the sound card still has more than about 60 ms of
     * older sound waiting, this comparison is SKIPPED instead of being added
     * to the pile. At fast speeds you hear a sample of the comparisons, but
     * the sound never falls behind the bars.
     */
    public void playInOrder(final double[] freqs, final int msEach) {
        if (line == null || freqs.length == 0) return;
        if (pending.get() > 0) return;                 // previous comparison not even started yet
        final int gen = generation.get();
        pending.incrementAndGet();
        audioThread.submit(new Runnable() {
            public void run() {
                try {
                    if (gen != generation.get()) return;       // cancelled by stopAll()
                    if (queuedSeconds() > MAX_LAG_SECONDS) return;  // device is behind: skip
                    byte[] buf = synth(freqs, msEach);
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
        final int gen = generation.get();
        audioThread.submit(new Runnable() {
            public void run() {
                for (double f : freqs) {
                    if (gen != generation.get()) return;  // stopped part-way through
                    byte[] buf = synth(new double[] { f }, msEach);
                    line.write(buf, 0, buf.length);
                }
            }
        });
    }

    /** Silence everything right now: cancel queued notes and throw away buffered sound. */
    public void stopAll() {
        generation.incrementAndGet();
        if (line != null) {
            line.stop();
            line.flush();
            line.start();
        }
    }

    /** How many seconds of sound are sitting in the sound card's buffer, not yet heard. */
    private double queuedSeconds() {
        int queuedBytes = line.getBufferSize() - line.available();
        return queuedBytes / (RATE * 2.0);
    }

    /** Build one buffer holding each frequency as its own note, one after another. */
    private static byte[] synth(double[] freqs, int msEach) {
        byte[][] parts = new byte[freqs.length][];
        int total = 0;
        for (int k = 0; k < freqs.length; k++) {
            parts[k] = tone(freqs[k], msEach);
            total += parts[k].length;
        }
        byte[] all = new byte[total];
        int pos = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, all, pos, part.length);
            pos += part.length;
        }
        return all;
    }

    private static byte[] tone(double freq, int ms) {
        double[] freqs = { freq };
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