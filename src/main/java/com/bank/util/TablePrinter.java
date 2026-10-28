package com.bank.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Beautiful ASCII Table Formatter for terminal presentation.
 * Automatically computes column widths, strips ANSI codes when calculating widths,
 * and handles proper text and numerical alignments.
 */
public class TablePrinter {

    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();
    private final List<Boolean> rightAlign = new ArrayList<>();
    private String title = null;

    public TablePrinter(String... headers) {
        this.headers = Arrays.asList(headers);
        for (int i = 0; i < headers.length; i++) {
            rightAlign.add(false);
        }
    }

    public TablePrinter setTitle(String title) {
        this.title = title;
        return this;
    }

    public TablePrinter setRightAlign(int columnIndex, boolean alignRight) {
        if (columnIndex >= 0 && columnIndex < rightAlign.size()) {
            rightAlign.set(columnIndex, alignRight);
        }
        return this;
    }

    public TablePrinter addRow(String... rowValues) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            if (i < rowValues.length && rowValues[i] != null) {
                row.add(rowValues[i]);
            } else {
                row.add("");
            }
        }
        rows.add(row);
        return this;
    }

    public void print() {
        System.out.println(render());
    }

    public String render() {
        if (headers.isEmpty()) return "";

        int colCount = headers.size();
        int[] colWidths = new int[colCount];

        // Header widths
        for (int i = 0; i < colCount; i++) {
            colWidths[i] = Math.max(colWidths[i], getVisibleLength(headers.get(i)));
        }

        // Data row widths
        for (List<String> row : rows) {
            for (int i = 0; i < colCount; i++) {
                colWidths[i] = Math.max(colWidths[i], getVisibleLength(row.get(i)));
            }
        }

        // Minimum padding
        for (int i = 0; i < colCount; i++) {
            colWidths[i] += 2; // 1 space on each side
        }

        StringBuilder sb = new StringBuilder();

        // Optional Title Banner
        int totalWidth = Arrays.stream(colWidths).sum() + colCount + 1;
        if (title != null && !title.isEmpty()) {
            sb.append(buildSeparator(colWidths, '+', '-')).append("\n");
            String centeredTitle = centerText(title, totalWidth - 2);
            sb.append("|").append(AnsiColor.bold(centeredTitle)).append("|\n");
        }

        // Top Border
        String topBorder = buildSeparator(colWidths, '+', '-');
        sb.append(topBorder).append("\n");

        // Header Row
        sb.append("|");
        for (int i = 0; i < colCount; i++) {
            sb.append(padCell(headers.get(i), colWidths[i], false, true));
            sb.append("|");
        }
        sb.append("\n");

        // Header Separator
        sb.append(buildSeparator(colWidths, '+', '=')).append("\n");

        // Data Rows
        if (rows.isEmpty()) {
            sb.append("|");
            String emptyMsg = centerText("(No records found)", totalWidth - 2);
            sb.append(AnsiColor.dim(emptyMsg)).append("|\n");
        } else {
            for (List<String> row : rows) {
                sb.append("|");
                for (int i = 0; i < colCount; i++) {
                    boolean alignRight = rightAlign.get(i);
                    sb.append(padCell(row.get(i), colWidths[i], alignRight, false));
                    sb.append("|");
                }
                sb.append("\n");
            }
        }

        // Bottom Border
        sb.append(topBorder).append("\n");

        return sb.toString();
    }

    private String buildSeparator(int[] colWidths, char corner, char line) {
        StringBuilder sb = new StringBuilder();
        sb.append(corner);
        for (int w : colWidths) {
            for (int i = 0; i < w; i++) {
                sb.append(line);
            }
            sb.append(corner);
        }
        return sb.toString();
    }

    private String padCell(String text, int width, boolean rightAlign, boolean isHeader) {
        int visibleLen = getVisibleLength(text);
        int pad = Math.max(0, width - visibleLen - 2); // 2 spaces total padding

        StringBuilder sb = new StringBuilder();
        sb.append(" "); // Leading space
        if (rightAlign) {
            for (int i = 0; i < pad; i++) sb.append(" ");
            sb.append(isHeader ? AnsiColor.bold(text) : text);
        } else {
            sb.append(isHeader ? AnsiColor.bold(text) : text);
            for (int i = 0; i < pad; i++) sb.append(" ");
        }
        sb.append(" "); // Trailing space
        return sb.toString();
    }

    private int getVisibleLength(String text) {
        if (text == null) return 0;
        // Strip ANSI escape codes to calculate visual characters length
        String stripped = text.replaceAll("\u001B\\[[;\\d]*m", "");
        return stripped.length();
    }

    private String centerText(String text, int width) {
        int visible = getVisibleLength(text);
        if (visible >= width) return text;
        int left = (width - visible) / 2;
        int right = width - visible - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }
}
