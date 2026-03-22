package org.example.util;

import java.util.List;

public class FormatUtils {
    private static final String TABLE_TOP_LEFT = "┌";
    private static final String TABLE_TOP_MID = "┬";
    private static final String TABLE_TOP_RIGHT = "┐";
    private static final String TABLE_MID_LEFT = "├";
    private static final String TABLE_MID_MID = "┼";
    private static final String TABLE_MID_RIGHT = "┤";
    private static final String TABLE_BOTTOM_LEFT = "└";
    private static final String TABLE_BOTTOM_MID = "┴";
    private static final String TABLE_BOTTOM_RIGHT = "┘";
    private static final String TABLE_ROW_LEFT = "│";
    private static final String TABLE_ROW_MID = "│";
    private static final String TABLE_ROW_RIGHT = "│";
    private static final String TABLE_HORIZONTAL_LINE = "─";

    public static String padRight(String text, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative: " + length);
        }
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative: " + length);
        }
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return " ".repeat(length - text.length()) + text;
    }

    public static String truncate(String text, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must be non-negative: " + maxLength);
        }
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        if (maxLength < 4) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String formatBox(String text) {
        if (text == null) {
            text = "";
        }
        String[] lines = text.split("\n");
        int maxLen = 0;
        for (String line : lines) {
            maxLen = Math.max(maxLen, line.length());
        }
        StringBuilder sb = new StringBuilder();
        String border = "+" + "-".repeat(maxLen + 2) + "+";
        sb.append(border).append("\n");
        for (String line : lines) {
            sb.append("| ").append(padRight(line, maxLen)).append(" |\n");
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatHeader(String text) {
        if (text == null) {
            text = "";
        }
        int width = text.length() + 4;
        String line = "=".repeat(width);
        return line + "\n  " + text + "  \n" + line;
    }

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null) {
            headers = new String[0];
        }
        if (rows == null) {
            rows = List.of();
        }

        int colCount = headers.length;
        if (colCount == 0 && !rows.isEmpty()) {
            colCount = rows.get(0).length;
        }
        if (colCount == 0) {
            return "";
        }

        int[] colWidths = new int[colCount];
        for (int i = 0; i < colCount; i++) {
            if (i < headers.length && headers[i] != null) {
                colWidths[i] = headers[i].length();
            }
        }
        for (String[] row : rows) {
            for (int i = 0; i < colCount; i++) {
                String cell = (i < row.length && row[i] != null) ? row[i] : "";
                colWidths[i] = Math.max(colWidths[i], cell.length());
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append(buildSeparator(colWidths, TABLE_TOP_LEFT, TABLE_TOP_MID, TABLE_TOP_RIGHT));

        boolean hasHeaders = false;
        for (String h : headers) {
            if (h != null) {
                hasHeaders = true;
                break;
            }
        }
        if (hasHeaders) {
            sb.append(buildRow(headers, colWidths, TABLE_ROW_LEFT, TABLE_ROW_MID));
            sb.append(buildSeparator(colWidths, TABLE_MID_LEFT, TABLE_MID_MID, TABLE_MID_RIGHT));
        }

        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            sb.append(buildRow(row, colWidths, TABLE_ROW_LEFT, TABLE_ROW_MID));
            if (r < rows.size() - 1) {
                sb.append(buildSeparator(colWidths, TABLE_MID_LEFT, TABLE_MID_MID, TABLE_MID_RIGHT));
            }
        }

        sb.append(buildSeparator(colWidths, TABLE_BOTTOM_LEFT, TABLE_BOTTOM_MID, TABLE_BOTTOM_RIGHT));

        return sb.toString();
    }

    private static String buildRow(String[] cells, int[] widths, String left, String mid) {
        StringBuilder row = new StringBuilder(left);
        for (int i = 0; i < widths.length; i++) {
            String cell = (i < cells.length && cells[i] != null) ? cells[i] : "";
            row.append(" ").append(padRight(cell, widths[i])).append(" ");
            if (i < widths.length - 1) {
                row.append(mid);
            } else {
                row.append(TABLE_ROW_RIGHT);
            }
        }
        return row.append("\n").toString();
    }

    private static String buildSeparator(int[] widths, String left, String mid, String right) {
        StringBuilder sep = new StringBuilder(left);
        for (int i = 0; i < widths.length; i++) {
            sep.append(TABLE_HORIZONTAL_LINE.repeat(widths[i] + 2));
            if (i < widths.length - 1) {
                sep.append(mid);
            } else {
                sep.append(right);
            }
        }
        return sep.append("\n").toString();
    }
}