import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * SORTING VISUALIZER - run this file (it has the main method).
 *
 * Pick an algorithm, type or generate some data, press "Run Sort", then step
 * through it (or press Play) to watch and hear every comparison, swap and write.
 *
 * (Framework file - students do not need to edit this.)
 */
public class SortVisualizer extends JFrame {

    /** The algorithms in the menu. Students write the code inside each of these classes. */
    private static SortAlgorithm[] algorithms() {
        return new SortAlgorithm[] {
            new SelectionSort(),
            new InsertionSort(),
            new ShellSort(),
            new MergeSort(),
            new HeapSort(),
            new QuickSort()
        };
    }

    private static final int MAX_ITEMS = 100;
    private static final int MAX_STEPS = 40000;
    private static final long TIMEOUT_MS = 3000;
    private static final String[] PATTERNS = { "Random", "Already sorted", "Reversed", "Nearly sorted", "Few unique values" };

    // ---- controls
    private final JComboBox<AlgoChoice> algoBox = new JComboBox<AlgoChoice>();
    private final JRadioButton numbersBtn = new JRadioButton("Numbers", true);
    private final JRadioButton charsBtn = new JRadioButton("Characters");
    private final JTextField dataField = new JTextField(40);
    private final JComboBox<String> patternBox = new JComboBox<String>(PATTERNS);
    private final JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(16, 2, MAX_ITEMS, 1));
    private final JButton generateBtn = new JButton("Generate");
    private final JButton runBtn = new JButton("Run Sort");

    private final BarPanel bars = new BarPanel();

    private final JButton startBtn = new JButton("|<  Start");
    private final JButton backBtn = new JButton("<  Back");
    private final JButton playBtn = new JButton("Play");
    private final JButton stepBtn = new JButton("Step  >");
    private final JButton endBtn = new JButton("End  >|");
    private final JSlider speedSlider = new JSlider(0, 100, 45);
    private final JCheckBox soundBox = new JCheckBox("Sound", true);

    private final JLabel statsLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");
    private final JLabel resultLabel = new JLabel(" ");

    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[] { "#", "Algorithm", "n", "Data", "Compares", "Swaps", "Writes", "Result" }, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // ---- state
    private final TonePlayer tones = new TonePlayer();
    private final Timer timer;
    private List<Step> steps = new ArrayList<Step>();
    private int index = 0;
    private double minKey = 0, maxKey = 1;
    private boolean finishedCorrectly = false;
    private String lastGeneratedText = "";
    private String lastGeneratedPattern = "";
    private int runCount = 0;
    private final Random rng = new Random();

    public SortVisualizer() {
        super("Sorting Algorithm Visualizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        for (SortAlgorithm a : algorithms()) algoBox.addItem(new AlgoChoice(a));
        ButtonGroup group = new ButtonGroup();
        group.add(numbersBtn);
        group.add(charsBtn);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(new EmptyBorder(8, 8, 8, 8));
        main.add(buildTop(), BorderLayout.NORTH);
        main.add(bars, BorderLayout.CENTER);
        main.add(buildBottom(), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, main, buildHistory());
        split.setResizeWeight(0.75);
        setContentPane(split);

        timer = new Timer(50, new ActionListener() {
            public void actionPerformed(ActionEvent e) { tick(); }
        });

        wireEvents();
        generateData();
        updateSpeed();

        setSize(1300, 760);
        setLocationRelativeTo(null);
        split.setDividerLocation(0.74);

        if (!tones.isAvailable()) {
            soundBox.setSelected(false);
            soundBox.setEnabled(false);
            soundBox.setText("Sound (no audio device)");
        }
    }

    // ================================================================ layout

    private JPanel buildTop() {
        JPanel top = new JPanel(new GridLayout(2, 1, 4, 4));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row1.add(new JLabel("Algorithm:"));
        row1.add(algoBox);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(new JLabel("Data type:"));
        row1.add(numbersBtn);
        row1.add(charsBtn);
        row1.add(Box.createHorizontalStrut(20));
        row1.add(new JLabel("Pattern:"));
        row1.add(patternBox);
        row1.add(new JLabel("Size:"));
        row1.add(sizeSpinner);
        row1.add(generateBtn);

        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.setBorder(new EmptyBorder(0, 8, 0, 8));
        row2.add(new JLabel("Data (edit me!):"), BorderLayout.WEST);
        dataField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        dataField.setToolTipText("Numbers: separate with commas or spaces, e.g. 5, 3, 9, 1.   "
                + "Characters: e.g. S O R T E X A M P L E  or just SORTEXAMPLE");
        row2.add(dataField, BorderLayout.CENTER);
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD, 14f));
        row2.add(runBtn, BorderLayout.EAST);

        top.add(row1);
        top.add(row2);
        return top;
    }

    private JPanel buildBottom() {
        JPanel bottom = new JPanel(new GridLayout(4, 1, 2, 2));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        playBtn.setPreferredSize(new Dimension(90, playBtn.getPreferredSize().height));
        controls.add(startBtn);
        controls.add(backBtn);
        controls.add(playBtn);
        controls.add(stepBtn);
        controls.add(endBtn);
        controls.add(Box.createHorizontalStrut(20));
        controls.add(new JLabel("Slow"));
        speedSlider.setPreferredSize(new Dimension(180, speedSlider.getPreferredSize().height));
        controls.add(speedSlider);
        controls.add(new JLabel("Fast"));
        controls.add(Box.createHorizontalStrut(20));
        controls.add(soundBox);

        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);

        bottom.add(controls);
        bottom.add(statsLabel);
        bottom.add(messageLabel);
        bottom.add(resultLabel);
        return bottom;
    }

    private JPanel buildHistory() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBorder(new TitledBorder("Run history (for your analysis)"));
        JTable table = new JTable(historyModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = { 30, 110, 35, 110, 70, 55, 55, 80 };
        for (int c = 0; c < widths.length; c++) table.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton copy = new JButton("Copy as CSV");
        JButton clear = new JButton("Clear");
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { copyHistoryCsv(); }
        });
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { historyModel.setRowCount(0); runCount = 0; }
        });
        buttons.add(copy);
        buttons.add(clear);
        p.add(buttons, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(320, 400));
        return p;
    }

    private void wireEvents() {
        generateBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { generateData(); }
        });
        ActionListener regen = new ActionListener() {
            public void actionPerformed(ActionEvent e) { generateData(); }
        };
        numbersBtn.addActionListener(regen);
        charsBtn.addActionListener(regen);
        patternBox.addActionListener(regen);
        dataField.addActionListener(new ActionListener() {  // pressing Enter in the box
            public void actionPerformed(ActionEvent e) { previewData(); }
        });
        algoBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { previewData(); }
        });
        runBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { runSort(); }
        });
        startBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pause(); goTo(0, false); }
        });
        backBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pause(); goTo(index - 1, true); }
        });
        stepBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pause(); goTo(index + 1, true); }
        });
        endBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pause(); goTo(steps.size() - 1, false); }
        });
        playBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { togglePlay(); }
        });
        speedSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) { updateSpeed(); }
        });
    }

    // ================================================================ data input

    private void generateData() {
        int n = (Integer) sizeSpinner.getValue();
        String pattern = (String) patternBox.getSelectedItem();
        boolean chars = charsBtn.isSelected();

        int[] v = new int[n];
        for (int k = 0; k < n; k++) {
            if (pattern.equals("Few unique values")) v[k] = 1 + rng.nextInt(4);
            else v[k] = chars ? rng.nextInt(26) : 1 + rng.nextInt(99);
        }
        if (pattern.equals("Already sorted") || pattern.equals("Reversed") || pattern.equals("Nearly sorted")) {
            java.util.Arrays.sort(v);
        }
        if (pattern.equals("Reversed")) {
            for (int a = 0, b = n - 1; a < b; a++, b--) { int t = v[a]; v[a] = v[b]; v[b] = t; }
        }
        if (pattern.equals("Nearly sorted")) {
            for (int s = 0; s < Math.max(1, n / 10); s++) {
                int a = rng.nextInt(n), b = Math.min(n - 1, a + 1 + rng.nextInt(2));
                int t = v[a]; v[a] = v[b]; v[b] = t;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < n; k++) {
            if (k > 0) sb.append(chars ? " " : ", ");
            if (chars) {
                int letter = pattern.equals("Few unique values") ? v[k] - 1 : v[k];
                sb.append((char) ('A' + letter));
            } else {
                sb.append(pattern.equals("Few unique values") ? v[k] * 10 : v[k]);
            }
        }
        dataField.setText(sb.toString());
        lastGeneratedText = sb.toString();
        lastGeneratedPattern = pattern;
        previewData();
    }

    /** Show the unsorted data so students can see it before running. */
    private void previewData() {
        pause();
        try {
            Item[] items = parseInput();
            setRange(items);
            steps = new ArrayList<Step>(Collections.singletonList(Step.start(items)));
            index = 0;
            finishedCorrectly = false;
            resultLabel.setForeground(Color.DARK_GRAY);
            resultLabel.setText("Press \"Run Sort\" to record "
                    + ((AlgoChoice) algoBox.getSelectedItem()).alg.getName() + " on this data.");
            refresh();
        } catch (IllegalArgumentException ex) {
            resultLabel.setForeground(new Color(190, 40, 40));
            resultLabel.setText(ex.getMessage());
        }
    }

    private Item[] parseInput() {
        String text = dataField.getText().trim();
        List<Item> list = new ArrayList<Item>();
        if (charsBtn.isSelected()) {
            for (char c : text.toCharArray()) {
                if (Character.isWhitespace(c) || c == ',') continue;
                list.add(new Item(c, String.valueOf(c), list.size()));
            }
        } else {
            for (String tok : text.split("[,\\s]+")) {
                if (tok.isEmpty()) continue;
                double d;
                try {
                    d = Double.parseDouble(tok);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("\"" + tok + "\" is not a number. "
                            + "Choose the Characters data type to sort letters.");
                }
                String label = (d == Math.rint(d) && Math.abs(d) < 1e15) ? String.valueOf((long) d) : tok;
                list.add(new Item(d, label, list.size()));
            }
        }
        if (list.size() < 2) throw new IllegalArgumentException("Enter at least 2 values to sort.");
        if (list.size() > MAX_ITEMS) throw new IllegalArgumentException(
                "Please use at most " + MAX_ITEMS + " values (you entered " + list.size() + ").");
        return list.toArray(new Item[0]);
    }

    private void setRange(Item[] items) {
        minKey = Double.MAX_VALUE;
        maxKey = -Double.MAX_VALUE;
        for (Item it : items) {
            minKey = Math.min(minKey, it.key());
            maxKey = Math.max(maxKey, it.key());
        }
    }

    // ================================================================ running the student's sort

    private void runSort() {
        pause();
        final Item[] items;
        try {
            items = parseInput();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check your data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final SortAlgorithm alg = ((AlgoChoice) algoBox.getSelectedItem()).alg;
        final String dataDesc = describeData(items.length);
        setRange(items);
        runBtn.setEnabled(false);
        resultLabel.setForeground(Color.DARK_GRAY);
        resultLabel.setText("Running " + alg.getName() + "...");

        new SwingWorker<SortRunner.Result, Void>() {
            @Override protected SortRunner.Result doInBackground() {
                return SortRunner.run(alg, items, MAX_STEPS, TIMEOUT_MS);
            }
            @Override protected void done() {
                runBtn.setEnabled(true);
                try {
                    showResult(alg, items.length, dataDesc, get());
                } catch (Exception ex) {
                    resultLabel.setForeground(new Color(190, 40, 40));
                    resultLabel.setText("Something went wrong: " + ex);
                }
            }
        }.execute();
    }

    private String describeData(int n) {
        String type = charsBtn.isSelected() ? "chars" : "nums";
        if (dataField.getText().equals(lastGeneratedText)) return lastGeneratedPattern + " " + type;
        return "Custom " + type;
    }

    private void showResult(SortAlgorithm alg, int n, String dataDesc, SortRunner.Result r) {
        steps = r.steps;
        index = 0;
        finishedCorrectly = r.error == null && r.sorted && r.sameItems;

        String verdict;
        Color color;
        if (r.error != null) {
            verdict = "Error";
            color = new Color(190, 40, 40);
            resultLabel.setText("<html><center>" + escape(r.error)
                    + "<br>You can still step through what happened before the problem.</center></html>");
        } else if (!r.sameItems) {
            verdict = "Lost values";
            color = new Color(190, 40, 40);
            resultLabel.setText("Not correct: some values were lost or copied twice. Check your set() calls.");
        } else if (!r.sorted) {
            verdict = "Not sorted";
            color = new Color(200, 110, 0);
            if (r.comparisons == 0 && r.swaps == 0 && r.writes == 0) {
                resultLabel.setText("Nothing happened - has " + alg.getName() + " been written yet? Open "
                        + alg.getClass().getSimpleName() + ".java and get coding!");
            } else {
                resultLabel.setText("Not sorted yet: the list isn't in order at the end. Step through to find where it goes wrong.");
            }
        } else {
            verdict = "Correct";
            color = new Color(30, 140, 60);
            String stable = r.stable == null ? "(no duplicate values, so stability not tested)"
                    : (r.stable ? "Stable on this data: YES" : "Stable on this data: NO");
            resultLabel.setText("Sorted correctly!  " + n + " items:  " + r.comparisons + " comparisons, "
                    + r.swaps + " swaps, " + r.writes + " writes.   " + stable);
        }
        resultLabel.setForeground(color);

        runCount++;
        historyModel.addRow(new Object[] { runCount, alg.getName(), n, dataDesc,
                r.comparisons, r.swaps, r.writes, verdict });
        refresh();
    }

    // ================================================================ stepping & playback

    private void goTo(int newIndex, boolean withSound) {
        if (steps.isEmpty()) return;
        newIndex = Math.max(0, Math.min(steps.size() - 1, newIndex));
        boolean moved = newIndex != index;
        index = newIndex;
        refresh();
        if (withSound && moved) playSoundFor(steps.get(index));
    }

    private void togglePlay() {
        if (timer.isRunning()) {
            pause();
        } else {
            if (index >= steps.size() - 1) index = 0;  // replay from the beginning
            timer.start();
            playBtn.setText("Pause");
        }
    }

    private void pause() {
        if (timer != null && timer.isRunning()) timer.stop();
        playBtn.setText("Play");
    }

    private int stepsPerTick = 1;

    private void updateSpeed() {
        // slider 0 -> 1 second per step ... slider 100 -> about 3 ms per step
        double delay = 1000 * Math.pow(0.003, speedSlider.getValue() / 100.0);
        if (delay < 15) {
            stepsPerTick = (int) Math.ceil(15 / delay);
            timer.setDelay(15);
        } else {
            stepsPerTick = 1;
            timer.setDelay((int) delay);
        }
    }

    private void tick() {
        if (index >= steps.size() - 1) {
            pause();
            return;
        }
        goTo(index + stepsPerTick, true);
        if (index >= steps.size() - 1) pause();
    }

    private void playSoundFor(Step s) {
        if (!soundBox.isSelected()) return;
        if (s.type == Step.Type.DONE && finishedCorrectly) {
            double[] f = new double[s.snapshot.length];
            for (int k = 0; k < f.length; k++) f[k] = TonePlayer.frequencyFor(s.snapshot[k].key(), minKey, maxKey);
            tones.sweep(f, Math.max(15, 600 / f.length));
            return;
        }
        if (s.soundKeys.length == 0) return;
        double[] f = new double[s.soundKeys.length];
        for (int k = 0; k < f.length; k++) f[k] = TonePlayer.frequencyFor(s.soundKeys[k], minKey, maxKey);
        int ms = (int) Math.max(25, Math.min(150, timer.getDelay() * stepsPerTick * 0.8));
        tones.play(f, ms);
    }

    private void refresh() {
        if (steps.isEmpty()) return;
        Step s = steps.get(index);
        boolean atEnd = index == steps.size() - 1;
        bars.show(s, minKey, maxKey, atEnd && finishedCorrectly && s.type == Step.Type.DONE);

        statsLabel.setText("Step " + index + " / " + (steps.size() - 1)
                + "        Comparisons: " + s.comparisons
                + "        Swaps: " + s.swaps
                + "        Writes: " + s.writes);
        messageLabel.setText(s.message);

        startBtn.setEnabled(index > 0);
        backBtn.setEnabled(index > 0);
        stepBtn.setEnabled(!atEnd);
        endBtn.setEnabled(!atEnd);
        playBtn.setEnabled(steps.size() > 1);
    }

    // ================================================================ helpers

    private void copyHistoryCsv() {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < historyModel.getColumnCount(); c++) {
            if (c > 0) sb.append(',');
            sb.append(historyModel.getColumnName(c));
        }
        sb.append('\n');
        for (int r = 0; r < historyModel.getRowCount(); r++) {
            for (int c = 0; c < historyModel.getColumnCount(); c++) {
                if (c > 0) sb.append(',');
                sb.append(historyModel.getValueAt(r, c));
            }
            sb.append('\n');
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(this, "Copied! Paste it into a spreadsheet to graph your results.");
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class AlgoChoice {
        final SortAlgorithm alg;
        AlgoChoice(SortAlgorithm alg) { this.alg = alg; }
        @Override public String toString() { return alg.getDay() + "  -  " + alg.getName(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) { }
                new SortVisualizer().setVisible(true);
            }
        });
    }
}
