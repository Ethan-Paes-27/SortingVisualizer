/**
 * One value in the list being sorted. It can hold a number or a character.
 *
 * STUDENTS: you never need to look inside an Item. Treat it like a sealed box:
 * move it around with arr.get(...) / arr.set(...) / arr.swap(...), and compare
 * it ONLY with arr.compare(...) or arr.less(...), so the visualizer can count
 * your comparisons.
 *
 * (Framework file - students do not need to edit this.)
 */
public final class Item {

    private final double key;    // what gets compared (the number, or the character's code)
    private final String label;  // what gets shown on screen
    private final int id;        // original position in the input (used to check stability)

    Item(double key, String label, int id) {
        this.key = key;
        this.label = label;
        this.id = id;
    }

    double key()  { return key; }
    int id()      { return id; }

    /** The text shown under the bar, e.g. "42" or "Q". */
    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
