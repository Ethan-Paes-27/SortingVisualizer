import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * TRACE TABLE window: shows the sort as a table, one row per step, one column
 * per array index - like the trace tables in a textbook.
 *
 * - "Grow with playback" ON : rows appear as the visualizer plays / steps.
 * - "Grow with playback" OFF: the whole trace is shown at once (a full report).
 * - Click a row to jump the visualizer to that step.
 * - Export an HTML report (opens in any browser, prints nicely) or copy as CSV.
 *
 * (Framework file - students do not need to edit this.)
 */
public class TraceWindow extends JFrame {

    /** Lets the main window jump to a step when a row is clicked. */
    interface JumpListener { void jumpTo(int stepIndex); }

    private static final String[] MODES = {
        "Each change (swaps & writes)",
        "Every step (incl. comparisons)",
        "Only my arr.mark(...) rows"
    };

    // pale versions of the bar colours so the text stays readable
    private static final Color CMP_BG   = new Color(255, 236, 150);
    private static final Color SWAP_BG  = new Color(255, 180, 180);
    private static final Color WRITE_BG = new Color(225, 195, 245);
    private static final Color MARK_BG  = new Color(215, 235, 255);
    private static final Color CURRENT  = new Color(200, 225, 255);
    private static final Color SAME_FG  = new Color(170, 170, 170);

    // (these must be declared before the table below, which reads them when it is built)
    private List<Step> steps = new ArrayList<Step>();
    private final List<Integer> rows = new ArrayList<Integer>();  // step indexes shown as rows

    private final JumpListener jumper;
    private final JComboBox<String> modeBox = new JComboBox<String>(MODES);
    private final JCheckBox followBox = new JCheckBox("Grow with playback", true);
    private final JLabel infoLabel = new JLabel(" ");
    private final TraceModel model = new TraceModel();
    private final JTable table = new JTable(model);

    private int visible = 0;
    private int current = 0;
    private int currentRow = -1;   // table row that matches the visualizer's current step
    private String algName = "";
    private String summary = "";

    public TraceWindow(JumpListener jumper) {
        super("Trace Table");
        this.jumper = jumper;
        setDefaultCloseOperation(HIDE_ON_CLOSE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Rows:"));
        top.add(modeBox);
        top.add(followBox);
        JButton exportBtn = new JButton("Export HTML report...");
        JButton csvBtn = new JButton("Copy as CSV");
        top.add(exportBtn);
        top.add(csvBtn);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(22);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new TraceRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        legend.add(swatch(CMP_BG, "compared"));
        legend.add(swatch(SWAP_BG, "swapped"));
        legend.add(swatch(WRITE_BG, "written"));
        JLabel grey = new JLabel("grey = unchanged from row above");
        grey.setForeground(SAME_FG.darker());
        legend.add(grey);
        legend.add(Box.createHorizontalStrut(10));
        legend.add(infoLabel);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(new EmptyBorder(6, 6, 6, 6));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        content.add(legend, BorderLayout.SOUTH);
        setContentPane(content);

        modeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { rebuild(); }
        });
        followBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { updateVisible(); }
        });
        exportBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { exportHtml(); }
        });
        csvBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { copyCsv(); }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0 && r < visible) TraceWindow.this.jumper.jumpTo(rows.get(r));
            }
        });

        setSize(950, 520);
    }

    // ================================================================ called by the main window

    /** A new recording (or new data) is ready. */
    public void setSteps(List<Step> steps, String algName, String summary) {
        this.steps = steps;
        this.algName = algName;
        this.summary = summary;
        this.current = 0;
        setTitle("Trace Table - " + algName);
        rebuild();
    }

    /** The visualizer moved to a new step. */
    public void setCurrent(int stepIndex) {
        current = stepIndex;
        updateVisible();
    }

    // ================================================================ rows

    private void rebuild() {
        rows.clear();
        int mode = modeBox.getSelectedIndex();
        boolean hasMarks = false;
        for (int k = 0; k < steps.size(); k++) {
            Step.Type t = steps.get(k).type;
            if (t == Step.Type.MARK) hasMarks = true;
            boolean include;
            if (t == Step.Type.START || t == Step.Type.DONE) include = true;
            else if (mode == 0) include = t == Step.Type.SWAP || t == Step.Type.WRITE || t == Step.Type.MARK;
            else if (mode == 1) include = true;
            else include = t == Step.Type.MARK;
            if (include) rows.add(k);
        }
        if (mode == 2 && !hasMarks) {
            infoLabel.setText("No marks yet - add  arr.mark(\"i=\" + i);  at the end of each pass in your sort.");
        } else {
            infoLabel.setText(" ");
        }
        visible = 0;
        model.fireTableStructureChanged();
        sizeColumns();
        updateVisible();
    }

    private void updateVisible() {
        int newVisible;
        if (followBox.isSelected()) {
            // number of rows whose step index is <= current (rows is sorted, so binary search)
            int lo = 0, hi = rows.size();
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (rows.get(mid) <= current) lo = mid + 1; else hi = mid;
            }
            newVisible = lo;
        } else {
            newVisible = rows.size();
        }

        if (newVisible > visible) {
            int old = visible;
            visible = newVisible;
            model.fireTableRowsInserted(old, newVisible - 1);
        } else if (newVisible < visible) {
            int old = visible;
            visible = newVisible;
            model.fireTableRowsDeleted(newVisible, old - 1);
        }
        table.repaint();  // current-row highlight moves even when no rows are added

        currentRow = rowForCurrent();
        int show = currentRow;
        if (show >= 0) table.scrollRectToVisible(table.getCellRect(show, 0, true));
    }

    /** The last visible row at or before the current step. */
    private int rowForCurrent() {
        int best = -1;
        for (int r = 0; r < visible; r++) {
            if (rows.get(r) <= current) best = r; else break;
        }
        return best;
    }

    private void sizeColumns() {
        int maxLen = 1;
        if (!steps.isEmpty()) {
            for (Item it : steps.get(0).snapshot) maxLen = Math.max(maxLen, it.getLabel().length());
        }
        int valueWidth = Math.max(30, 12 + maxLen * 9);
        int[] fixed = { 50, 130, 50, 50, 50 };
        for (int c = 0; c < table.getColumnCount(); c++) {
            TableColumn col = table.getColumnModel().getColumn(c);
            col.setPreferredWidth(c < fixed.length ? fixed[c] : valueWidth);
        }
    }

    private int arraySize() {
        return steps.isEmpty() ? 0 : steps.get(0).snapshot.length;
    }

    // ================================================================ text for each cell

    static String action(Step s) {
        switch (s.type) {
            case START:   return "initial";
            case DONE:    return "final";
            case COMPARE: return "compare " + idx(s.i) + "," + idx(s.j);
            case SWAP:    return "swap " + s.i + "," + s.j;
            case WRITE:   return "a[" + s.i + "] = " + s.snapshot[s.i];
            case MARK:    return s.message.startsWith("Mark: ") ? s.message.substring(6) : s.message;
            default:      return "";
        }
    }

    private static String idx(int i) {
        return i < 0 ? "held" : String.valueOf(i);
    }

    private static final int FIXED_COLS = 5;

    private String cell(Step s, int stepIndex, int col) {
        switch (col) {
            case 0: return String.valueOf(stepIndex);
            case 1: return action(s);
            case 2: return String.valueOf(s.comparisons);
            case 3: return String.valueOf(s.swaps);
            case 4: return String.valueOf(s.writes);
            default: return s.snapshot[col - FIXED_COLS].getLabel();
        }
    }

    private String header(int col) {
        switch (col) {
            case 0: return "Step";
            case 1: return "Action";
            case 2: return "Comps";
            case 3: return "Swaps";
            case 4: return "Writes";
            default: return "[" + (col - FIXED_COLS) + "]";
        }
    }

    /** Highlight colour for array cell k in step s, or null. */
    private static Color highlight(Step s, int k) {
        if (k != s.i && k != s.j) return null;
        switch (s.type) {
            case COMPARE: return CMP_BG;
            case SWAP:    return SWAP_BG;
            case WRITE:   return WRITE_BG;
            default:      return null;
        }
    }

    /** True if array cell k holds the same item as in the previous table row. */
    private boolean unchanged(int row, int k) {
        if (row == 0) return false;
        return steps.get(rows.get(row - 1)).snapshot[k] == steps.get(rows.get(row)).snapshot[k];
    }

    // ================================================================ table model & renderer

    private final class TraceModel extends AbstractTableModel {
        public int getRowCount()    { return visible; }
        public int getColumnCount() { return steps.isEmpty() ? 0 : FIXED_COLS + arraySize(); }
        @Override public String getColumnName(int c) { return header(c); }
        public Object getValueAt(int r, int c) {
            int k = rows.get(r);
            return cell(steps.get(k), k, c);
        }
    }

    private final class TraceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, false, false, row, col);
            int stepIndex = rows.get(row);
            Step s = steps.get(stepIndex);
            boolean isCurrent = row == currentRow;

            setHorizontalAlignment(col == 1 ? LEFT : CENTER);
            setFont(t.getFont().deriveFont(Font.PLAIN));
            setForeground(Color.BLACK);
            Color bg = isCurrent ? CURRENT : (s.type == Step.Type.MARK ? MARK_BG : Color.WHITE);

            if (col >= FIXED_COLS) {
                int k = col - FIXED_COLS;
                Color h = highlight(s, k);
                if (h != null) {
                    bg = h;
                    setFont(t.getFont().deriveFont(Font.BOLD));
                } else if (unchanged(row, k)) {
                    setForeground(SAME_FG);
                } else {
                    setFont(t.getFont().deriveFont(Font.BOLD));
                }
            } else if (col == 1) {
                setFont(t.getFont().deriveFont(Font.ITALIC));
            }
            setBackground(bg);
            return this;
        }
    }

    private static JLabel swatch(Color c, String text) {
        JLabel l = new JLabel("  " + text + "  ");
        l.setOpaque(true);
        l.setBackground(c);
        return l;
    }

    // ================================================================ export

    private static final int MAX_EXPORT_ROWS = 3000;

    private void exportHtml() {
        if (steps.isEmpty()) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(algName.replace(' ', '_') + "_trace.html"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".html")) f = new File(f.getPath() + ".html");

        try {
            Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            try {
                w.write(buildHtml());
            } finally {
                w.close();
            }
            int open = JOptionPane.showConfirmDialog(this, "Saved " + f.getName() + ". Open it now?",
                    "Report saved", JOptionPane.YES_NO_OPTION);
            if (open == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(f.toURI());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save the report: " + ex.getMessage());
        }
    }

    /** The full trace (all rows for the chosen mode), regardless of playback position. */
    private String buildHtml() {
        int cols = FIXED_COLS + arraySize();
        int total = rows.size();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>")
          .append(esc(algName)).append(" trace</title><style>")
          .append("body{font-family:Arial,sans-serif;margin:24px;color:#222}")
          .append("table{border-collapse:collapse;font-size:13px}")
          .append("th,td{border:1px solid #ccc;padding:3px 6px;text-align:center}")
          .append("th{background:#eee}td.act{text-align:left;font-style:italic}")
          .append("td.same{color:#aaa}td.chg{font-weight:bold}")
          .append("td.cmp{background:#ffec96;font-weight:bold}td.swp{background:#ffb4b4;font-weight:bold}")
          .append("td.wrt{background:#e1c3f5;font-weight:bold}tr.mark td{background:#d7ebff}")
          .append(".legend span{padding:2px 8px;margin-right:6px}")
          .append("</style></head><body>");
        sb.append("<h2>").append(esc(algName)).append(" &mdash; trace table</h2>");
        sb.append("<p>").append(esc(summary)).append("</p>");
        sb.append("<p>Rows: ").append(esc((String) modeBox.getSelectedItem())).append(" &nbsp;|&nbsp; ")
          .append(total).append(" rows</p>");
        sb.append("<p class='legend'><span style='background:#ffec96'>compared</span>")
          .append("<span style='background:#ffb4b4'>swapped</span>")
          .append("<span style='background:#e1c3f5'>written</span>")
          .append("<span style='color:#aaa'>grey = unchanged from row above</span></p>");
        sb.append("<table><tr>");
        for (int c = 0; c < cols; c++) sb.append("<th>").append(esc(header(c))).append("</th>");
        sb.append("</tr>");

        int limit = Math.min(total, MAX_EXPORT_ROWS);
        for (int r = 0; r < limit; r++) {
            int k = rows.get(r);
            Step s = steps.get(k);
            sb.append(s.type == Step.Type.MARK ? "<tr class='mark'>" : "<tr>");
            for (int c = 0; c < cols; c++) {
                String cls = "";
                if (c == 1) cls = "act";
                if (c >= FIXED_COLS) {
                    int idx = c - FIXED_COLS;
                    Color h = highlight(s, idx);
                    if (h == CMP_BG) cls = "cmp";
                    else if (h == SWAP_BG) cls = "swp";
                    else if (h == WRITE_BG) cls = "wrt";
                    else cls = unchanged(r, idx) ? "same" : "chg";
                }
                sb.append("<td class='").append(cls).append("'>").append(esc(cell(s, k, c))).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        if (total > limit) {
            sb.append("<p><i>Only the first ").append(limit).append(" of ").append(total)
              .append(" rows are included. Choose \"Each change\" or \"Only my arr.mark rows\", or use less data.</i></p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private void copyCsv() {
        if (steps.isEmpty()) return;
        int cols = FIXED_COLS + arraySize();
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < cols; c++) {
            if (c > 0) sb.append(',');
            sb.append(header(c));
        }
        sb.append('\n');
        for (int r = 0; r < rows.size(); r++) {
            int k = rows.get(r);
            for (int c = 0; c < cols; c++) {
                if (c > 0) sb.append(',');
                String v = cell(steps.get(k), k, c);
                sb.append(v.contains(",") ? "\"" + v + "\"" : v);
            }
            sb.append('\n');
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        JOptionPane.showMessageDialog(this, "Copied " + rows.size() + " rows. Paste into a spreadsheet.");
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
