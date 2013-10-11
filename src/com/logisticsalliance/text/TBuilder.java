package com.logisticsalliance.text;

import java.io.Serializable;
import java.text.Format;
import java.text.NumberFormat;

/**
 * The class allows to build text in the form of table. Normally each row of
 * the table has same count of characters and starts with a new line.
 * @author Val Pashnev
 * @version %I%,%G%
 * @since 1.0
 */
public class TBuilder implements Serializable {
	private static final long serialVersionUID = 10L;
	
	public static final String TWO_DOTS = "..";

	StringBuilder buf;

	public TBuilder() {
		buf = new StringBuilder();
	}
	/**
	 * Constructs the builder with the initial {@code capacity}.
	 * @param capacity
	 */
	public TBuilder(int capacity) {
		buf = new StringBuilder(capacity);
	}
	/**
	 * Returns the character count of the text this builder accumulated.
	 * @return Count of characters
	 */
	public int length() {
		return buf.length();
	}
	/**
	 * Replaces the characters in the substring of this builder with the
	 * characters in the specified string.
	 * @param fromIndex the first index of the substring
	 * @param count the character count of the substring
	 * @param s the string that will replace the substring
	 */
	public void replace(int fromIndex, int count, String s) {
		buf.replace(fromIndex, fromIndex+count, s);
	}
	/**
	 * Adds the specified string to this builder.
	 * @param s the string to add
	 */
	public void add(String s) {
		buf.append(s);
	}
	/**
	 * Adds the specified character to this builder.
	 * @param c the character to add
	 */
	public void add(char c) {
		buf.append(c);
	}
	/**
	 * Adds the specified character to this builder the {@code count} of times.
	 * @param c the character to add
	 * @param count the character count
	 */
	public void add(char c, int count) {
		add(buf, c, count);
	}
	/**
	 * Invokes the method<br>
	 * {@link #add(char,int) add(' ', count)}.
	 */
	public void addSpace(int count) {
		add(' ', count);
	}
	/**
	 * Formats the {@code value} unless it is null and invokes the method<br>
	 * {@link #addCell(Object,int,boolean,boolean)
	 * addCell(value, length, false, last)}.
	 * @param value produces the string representation of the cell
	 * @param length the count of characters the cell must fit
	 * @param f the format
	 * @param last if {@code false}, adds two trailing spaces to the cell
	 * @return Formatted value
	 */
	public String addCell(Object value, int length, Format f, boolean last) {
		String v = null;
		if (value != null) {
			v = f == null ? value.toString() : f.format(value);
		}
		addCell(v, length, false, last);
		return v;
	}

	/**
	 * Formats the {@code value} unless it is null and invokes the method<br>
	 * {@link #addCell(Object,int,boolean,boolean)
	 * addCell(value, length, true, last)}.
	 * @param value produces the string representation of the cell
	 * @param length the count of characters the cell must fit
	 * @param f the format
	 * @param last if {@code false}, adds two trailing spaces to the cell
	 * @return Formatted value
	 */
	public String addCell(Number value, int length,
		NumberFormat f, boolean last) {
		String v = null;
		if (value != null && value.doubleValue() != 0) {
			v = f == null ? value.toString() : f.format(value);
		}
		addCell(v, length, true, last);
		return v;
	}
	/**
	 * Converts the {@code value} to the string representation of the cell and
	 * adds the string to this builder. <br>If the value is null, the result
	 * is empty string; otherwise it is {@code value.toString()}.
	 * If the character count of the string does not fit the argument
	 * {@code length}, it is either truncated or extended with leading spaces
	 * (toRight is {@code true}) or trailing spaces (toRight is {@code false}).
	 * In addition two trailing spaces are added, if the argument
	 * {@code last} is {@code false}
	 * @param value produces the string representation of the cell
	 * @param length the count of characters the cell must fit
	 * @param toRight determines whether to add leading or trailing spaces
	 * @param last if {@code false}, adds two trailing spaces to the cell
	 */
	public void addCell(Object value, int length,
		boolean toRight, boolean last) {
		if (value == null) {
			if (last) { return;}
			addSpace(length);
		}
		else {
			String v = value.toString();
			boolean cut = false;
			if (v.length() > length) {
				v = v.substring(0, length < 2 ? 0 : length-2);
				cut = true;
			}
			if (toRight) {
				if (cut) {
					buf.append(v); buf.append(TWO_DOTS);
				}
				else {
					addSpace(length-v.length());
					buf.append(v);
				}
			}
			else {
				buf.append(v);
				if (cut) {
					buf.append(TWO_DOTS);
				}
				else if (!last) { addSpace(length-v.length());}
			}
		}
		if (!last) {
			buf.append(' '); buf.append(' ');
		}
	}
	/**
	 * Starts a new line
	 */
	public void newLine() {
		buf.append('\r'); buf.append('\n');
	}
	/**
	 * Returns the text accumulated by this builder
	 * @return Text of this builder
	 */
	public String getText() {
		return buf.toString();
	}
	/**
	 * Clears the text accumulated by this builder
	 * @return Text which this builder had before clearing
	 */
	public String clear() {
		String v = buf.toString();
		buf.delete(0, buf.length());
		return v;
	}
	/**
	 * Invokes the method {@link #getText()}
	 */
	public String toString() {
		return getText();
	}
	public static StringBuilder add(StringBuilder b, char c, int count) {
		for (int i = 0; i < count; i++) {
			b.append(c);
		}
		return b;
	}
	public void addProperty20(String name, Object value, int valueLenght) {
		addCell(name, 20, false, false);
		addCell(value, valueLenght, false, true);
		newLine();
	}
}
