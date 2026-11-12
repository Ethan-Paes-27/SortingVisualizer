import java.util.ArrayList;
import java.util.List;

/**
 * The list your sorting algorithm works on.
 *
 * Every comparison, swap and write you make through these methods is counted
 * and recorded so the visualizer can replay it step by step (with sound!).
 *
 * ============================  QUICK REFERENCE  ============================
 *
 *   arr.size()              how many items are in the list
 *
 *   arr.less(i, j)          true if the item at i is smaller than the item at j
 *   arr.compare(i, j)       negative if a[i] < a[j], 0 if equal, positive if a[i] > a[j]
 *                           (both count as ONE comparison)
 *
 *   arr.swap(i, j)          swap the items at positions i and j   (counts as a SWAP)
 *
 *   arr.get(i)              look at / pick up the item at i        (free - not counted)
 *   arr.set(i, item)        put an item into position i            (counts as a WRITE)
 *
 *   You can also compare items you are "holding" in a variable or temp array:
 *   arr.less(item, j)   arr.less(i, item)   arr.less(itemA, itemB)
 *   arr.compare(item, j)   arr.compare(i, item)   arr.compare(itemA, itemB)
 *
 * Example:
 *     if (arr.less(3, 7)) {      // is a[3] < a[7] ?
 *         arr.swap(3, 7);
 *     }
 * ==========================================================================
 *
 * (Framework file - students do not need to edit this.)
 */
public class SortArray {

    private final Item[] a;
    private final List<Step> steps = new ArrayList<Step>();
    private final int maxSteps;
    private long comparisons, swaps, writes;

    SortArray(Item[] data, int maxSteps) {
        this.a = data.clone();
        this.maxSteps = maxSteps;
        steps.add(Step.start(a));
    }

    // ------------------------------------------------------------------ size

    /** Number of items in the list. */
    public int size() {
        return a.length;
    }

    // ------------------------------------------------------------------ reading / writing

    /** Look at the item at position i. Not counted. */
    public Item get(int i) {
        check(i);
        return a[i];
    }

    /** Put an item into position i. Counts as one WRITE. */
    public void set(int i, Item item) {
        check(i);
        if (item == null) {
            throw new IllegalArgumentException("set(" + i + ", null): you tried to put an empty (null) item into the list.");
        }
        writes++;
        a[i] = item;
        record(Step.Type.WRITE, i, -1,
                "Write " + item + " into a[" + i + "]",
                new double[] { item.key() });
    }

    /** Swap the items at positions i and j. Counts as one SWAP. */
    public void swap(int i, int j) {
        check(i);
        check(j);
        swaps++;
        Item x = a[i], y = a[j];
        a[i] = y;
        a[j] = x;
        record(Step.Type.SWAP, i, j,
                "Swap a[" + i + "]=" + x + " with a[" + j + "]=" + y,
                new double[] { x.key(), y.key() });
    }

    // ------------------------------------------------------------------ comparing by index

    /** Negative if a[i] < a[j], zero if equal, positive if a[i] > a[j]. Counts as one COMPARISON. */
    public int compare(int i, int j) {
        check(i);
        check(j);
        return doCompare(a[i], a[j], i, j, "a[" + i + "]", "a[" + j + "]");
    }

    /** True if a[i] < a[j]. Counts as one COMPARISON. */
    public boolean less(int i, int j) {
        return compare(i, j) < 0;
    }

    // ------------------------------------------------------------------ comparing held items

    /** Compare the item at index i with an item you are holding. Counts as one COMPARISON. */
    public int compare(int i, Item item) {
        check(i);
        checkItem(item);
        return doCompare(a[i], item, i, indexOf(item), "a[" + i + "]", "held");
    }

    /** Compare an item you are holding with the item at index j. Counts as one COMPARISON. */
    public int compare(Item item, int j) {
        checkItem(item);
        check(j);
        return doCompare(item, a[j], indexOf(item), j, "held", "a[" + j + "]");
    }

    /** Compare two items you are holding (e.g. from a temp array). Counts as one COMPARISON. */
    public int compare(Item x, Item y) {
        checkItem(x);
        checkItem(y);
        return doCompare(x, y, indexOf(x), indexOf(y), "held", "held");
    }

    public boolean less(int i, Item item)  { return compare(i, item) < 0; }
    public boolean less(Item item, int j)  { return compare(item, j) < 0; }
    public boolean less(Item x, Item y)    { return compare(x, y) < 0; }

    // ------------------------------------------------------------------ internals

    private int doCompare(Item x, Item y, int hi, int hj, String nameX, String nameY) {
        comparisons++;
        int result = Double.compare(x.key(), y.key());
        String sign = result < 0 ? "<" : (result > 0 ? ">" : "=");
        record(Step.Type.COMPARE, hi, hj,
                "Compare " + nameX + "=" + x + " with " + nameY + "=" + y + "   (" + x + " " + sign + " " + y + ")",
                new double[] { x.key(), y.key() });
        return result;
    }

    /** Best-effort: find where a held item currently sits so it can be highlighted. */
    private int indexOf(Item item) {
        for (int k = 0; k < a.length; k++) {
            if (a[k] == item) return k;
        }
        return -1;
    }

    private void check(int i) {
        if (i < 0 || i >= a.length) {
            throw new IndexOutOfBoundsException(
                    "Index " + i + " is out of bounds (valid indexes are 0 to " + (a.length - 1) + ")");
        }
    }

    private void checkItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("You compared an empty (null) item - is a temp array slot not filled yet?");
        }
    }

    private synchronized void record(Step.Type type, int i, int j, String msg, double[] sound) {
        if (steps.size() >= maxSteps) {
            throw new TooManyStepsException(maxSteps);
        }
        steps.add(new Step(type, i, j, a.clone(), comparisons, swaps, writes, msg, sound));
    }

    synchronized void finish() {
        steps.add(new Step(Step.Type.DONE, -1, -1, a.clone(), comparisons, swaps, writes,
                "Done!", new double[0]));
    }

    synchronized List<Step> stepsCopy()    { return new ArrayList<Step>(steps); }
    synchronized Item[] currentState()     { return a.clone(); }
    long comparisons() { return comparisons; }
    long swaps()       { return swaps; }
    long writes()      { return writes; }

    /** Thrown when a sort runs far too long (usually an infinite loop). */
    static class TooManyStepsException extends RuntimeException {
        TooManyStepsException(int max) {
            super("Stopped after " + max + " steps - is there an infinite loop?");
        }
    }
}
