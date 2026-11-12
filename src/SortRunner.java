import java.util.List;

/**
 * Runs a student's sort on its own thread, records every step, and then checks
 * whether the answer is actually correct.
 *
 * (Framework file - students do not need to edit this.)
 */
public final class SortRunner {

    /** Everything we learned from one run. */
    static final class Result {
        List<Step> steps;
        String error;          // null if the code finished normally
        boolean sorted;        // ascending order?
        boolean sameItems;     // nothing lost or duplicated?
        Boolean stable;        // null = no duplicate values, so stability can't be tested
        long comparisons, swaps, writes;
        boolean timedOut;
    }

    private SortRunner() { }

    static Result run(final SortAlgorithm algorithm, Item[] input, int maxSteps, long timeoutMs) {
        final SortArray arr = new SortArray(input, maxSteps);
        final Throwable[] failure = new Throwable[1];

        Thread worker = new Thread(new Runnable() {
            public void run() {
                try {
                    algorithm.sort(arr);
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        }, "student-sort");
        worker.setDaemon(true);
        worker.start();
        try {
            worker.join(timeoutMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        Result r = new Result();
        if (worker.isAlive()) {
            r.timedOut = true;
            r.error = "Your sort did not finish within " + (timeoutMs / 1000)
                    + " seconds - it is probably stuck in an infinite loop. (Restart the program after fixing it.)";
        } else if (failure[0] != null) {
            r.error = describe(failure[0]);
        } else {
            arr.finish();
        }

        r.steps = arr.stepsCopy();
        r.comparisons = arr.comparisons();
        r.swaps = arr.swaps();
        r.writes = arr.writes();

        Item[] out = arr.currentState();
        r.sameItems = sameItems(input, out);
        r.sorted = isSorted(out);
        r.stable = (r.sameItems && r.sorted) ? checkStable(out) : null;
        return r;
    }

    private static String describe(Throwable t) {
        if (t instanceof StackOverflowError) {
            return "StackOverflowError - your recursion never stops. Check your base case!";
        }
        String name = t.getClass().getSimpleName();
        String where = "";
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            if (!cls.equals("SortArray") && !cls.startsWith("java.") && !cls.equals("SortRunner")
                    && !cls.startsWith("SortRunner$")) {
                where = " (at " + e.getFileName() + " line " + e.getLineNumber() + ")";
                break;
            }
        }
        return name + ": " + t.getMessage() + where;
    }

    private static boolean isSorted(Item[] a) {
        for (int k = 1; k < a.length; k++) {
            if (a[k - 1].key() > a[k].key()) return false;
        }
        return true;
    }

    /** Each original item appears exactly once (nothing lost, nothing copied twice). */
    private static boolean sameItems(Item[] in, Item[] out) {
        if (in.length != out.length) return false;
        boolean[] seen = new boolean[in.length];
        for (Item it : out) {
            if (it == null) return false;
            int id = it.id();
            if (id < 0 || id >= seen.length || seen[id]) return false;
            seen[id] = true;
        }
        return true;
    }

    /** Equal values must keep their original left-to-right order. */
    private static Boolean checkStable(Item[] a) {
        boolean hasDuplicates = false;
        for (int k = 1; k < a.length; k++) {
            if (a[k - 1].key() == a[k].key()) {
                hasDuplicates = true;
                if (a[k - 1].id() > a[k].id()) return Boolean.FALSE;
            }
        }
        return hasDuplicates ? Boolean.TRUE : null;
    }
}
