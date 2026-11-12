/**
 * One recorded moment of the sort: what happened, which positions were involved,
 * what the whole list looked like right after it, and the running totals.
 *
 * (Framework file - students do not need to edit this.)
 */
public final class Step {

    public enum Type { START, COMPARE, SWAP, WRITE, DONE }

    final Type type;
    final int i;            // first highlighted index (-1 = none)
    final int j;            // second highlighted index (-1 = none)
    final Item[] snapshot;  // the list right after this step
    final long comparisons;
    final long swaps;
    final long writes;
    final String message;
    final double[] soundKeys;  // values to play as tones for this step

    Step(Type type, int i, int j, Item[] snapshot,
         long comparisons, long swaps, long writes,
         String message, double[] soundKeys) {
        this.type = type;
        this.i = i;
        this.j = j;
        this.snapshot = snapshot;
        this.comparisons = comparisons;
        this.swaps = swaps;
        this.writes = writes;
        this.message = message;
        this.soundKeys = soundKeys;
    }

    static Step start(Item[] items) {
        return new Step(Type.START, -1, -1, items.clone(), 0, 0, 0,
                "Starting list. Press Play or Step to begin.", new double[0]);
    }
}
