/**
 * Every sorting algorithm in this project implements this interface.
 *
 * (Framework file - students do not need to edit this.)
 */
public interface SortAlgorithm {

    /** The name shown in the drop-down menu, e.g. "Selection Sort". */
    String getName();

    /**
     * Sort the list into ascending order (smallest first).
     * Use ONLY arr's methods to compare and move items, so everything is counted.
     */
    void sort(SortArray arr);
}
