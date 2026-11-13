/**
 * =====================================================================
 *  EXTRA SORT  -  write ANY sorting algorithm you like here!
 * =====================================================================
 *
 *  1. Change getName() below to the name of your sort (e.g. "Bubble Sort").
 *     That's what shows up in the drop-down menu.
 *  2. Write your sort inside sort(...).
 *  3. Recompile and run SortVisualizer.
 *
 *  WANT MORE THAN ONE EXTRA SORT?
 *     Copy this file, e.g. to  BubbleSort.java, and change the class name on the
 *     line below to match the file name:   public class BubbleSort implements SortAlgorithm
 *     Then change getName(). Recompile, and it appears in the menu automatically.
 *
 *  IDEAS TO TRY
 *     Bubble Sort, Cocktail Shaker Sort, Gnome Sort, Comb Sort, Odd-Even Sort,
 *     Bottom-up Merge Sort, Quicksort with a median-of-three pivot, Binary Insertion Sort,
 *     ... or invent your own!
 *
 *  TOOLS (see SortArray.java for the full list)
 *      arr.size()           how many items
 *      arr.less(i, j)       true if a[i] < a[j]     (counts as a comparison)
 *      arr.compare(i, j)    negative / 0 / positive (counts as a comparison)
 *      arr.swap(i, j)       swap a[i] and a[j]      (counts as a swap)
 *      arr.get(i)           pick up the item at i   (free)
 *      arr.set(i, item)     put an item at i        (counts as a write)
 *      arr.mark("pass " + p)   optional: adds a row to the Trace Table
 *
 *  CHALLENGE: compare your sort with the ones from class. Is it faster or slower
 *  on Random data? On Already sorted data? Is it stable?
 */
public class MySort implements SortAlgorithm {

    @Override
    public String getName() { return "My Sort"; }

    @Override
    public void sort(SortArray arr) {
        // TODO: write your own sort here

    }
}
