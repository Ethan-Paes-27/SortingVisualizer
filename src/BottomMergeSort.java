/**
 * =====================================================================
 *  DAY 3  -  MERGE SORT
 * =====================================================================
 *
 *  THE IDEA  (divide and conquer)
 *   1. Split the list in half.
 *   2. Sort each half (using merge sort! - this is recursion).
 *   3. MERGE the two sorted halves into one sorted list.
 *  A list with 0 or 1 items is already sorted - that's your base case.
 *
 *  MERGING two sorted halves  a[lo..mid]  and  a[mid+1..hi]:
 *   - Copy a[lo..hi] into a temporary array first:   temp[k] = arr.get(k);
 *   - Keep one "finger" at the start of each half (in temp).
 *   - Compare the two items under your fingers, write the smaller one back
 *     into the list with arr.set(k, ...), and move that finger forward.
 *   - When one half runs out, copy the rest of the other half.
 *
 *  SUGGESTED STRUCTURE (you write the bodies):
 *      public void sort(SortArray arr)                        -> make temp, call mergeSort on the whole list
 *      private void mergeSort(SortArray arr, Item[] temp, int lo, int hi)
 *      private void merge(SortArray arr, Item[] temp, int lo, int mid, int hi)
 *
 *  TOOLS
 *      Item[] temp = new Item[arr.size()];   a temporary array
 *      arr.get(k)                            read an item (free)
 *      arr.less(temp[i], temp[j])            compare two items you are holding
 *      arr.set(k, item)                      write an item into the list
 *
 *  ANALYSIS QUESTIONS
 *   1. Merge sort makes 0 swaps. Why? What does it do instead?
 *   2. Try n = 16, 32, 64 Random. Does the comparison count roughly fit n * log2(n)?
 *   3. Does merge sort care whether the data starts sorted, reversed or random?
 *   4. Is your merge sort stable? (Hint: when two items are EQUAL, which one goes first?)
 */
public class BottomMergeSort implements SortAlgorithm {

    @Override
    public String getName() { return "Merge Sort"; }


    @Override
    public void sort(SortArray arr) {
        mergeSort(arr, new Item[arr.size()]);
    }

    private void mergeSort(SortArray arr, Item[] temp) {
        int n = arr.size();

        for (int size = 1; size < n; size *= 2) {
            for (int lo = 0; lo < n - size; lo+= 2*size) {
                int mid = lo+size-1;
                int hi = Math.min(lo + 2 * size - 1, n - 1);
                    merge(arr, temp, lo, mid, hi);
            }
        }
    }

    private void merge(SortArray arr, Item[] temp, int lo, int mid, int hi) {
        for (int i = lo; i <= hi; i++) {
            temp[i] = arr.get(i);
        }

        int arrWrite = lo;
        int point1 = lo;
        int point2 = mid+1;

        while (point1 <= mid && point2 <= hi) {
            if (arr.less(temp[point2], temp[point1])) {
                arr.set(arrWrite++, temp[point2++]);
            }
            else {
                arr.set(arrWrite++, temp[point1++]);
            }
        }

        while (point1 <= mid) {
            arr.set(arrWrite++, temp[point1++]);
        }

        while (point2 <= hi) {
            arr.set(arrWrite++, temp[point2++]);
        }

    }
}
