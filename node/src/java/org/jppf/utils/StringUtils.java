/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.utils;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;


/**
 * This class provides a set of utility methods for manipulating strings.
 * @author Laurent Cohen
 */
public final class StringUtils
{
	/**
	 * Logger for this class.
	 */
	//private static Logger log = LoggerFactory.getLogger(StringUtils.class);
	/**
	 * Keywords to look for and replace in the legend items of the charts.
	 */
	private static final String[] KEYWORDS = { "Execution", "Maximum", "Minimum", "Average", "Cumulated" };
	/**
	 * The the replacements words for the keywords in the legend items. Used to shorten the legend labels.
	 */
	private static final String[] REPLACEMENTS = { "Exec", "Max", "Min", "Avg", "Cumul" };
	/**
	 * Charset instance for UTF-8 encoding.
	 */
	public static final Charset UTF_8 = makeUTF8();
	/**
	 * Constant for an empty array of URLs.
	 */
	public static final String[] ZERO_STRING = new String[0];
	/**
	 * Constant for an empty array of Objects.
	 */
	public static final Object[] ZERO_OBJECT = new Object[0];
	/**
	 * Constant for an empty array of URLs.
	 */
	public static final URL[] ZERO_URL = new URL[0];

	/**
	 * Instantiation of this class is not permitted.
	 */
	private StringUtils()
	{
	}

	/**
	 * Format a string so that it fits into a string of specified length.<br>
	 * If the string is longer than the specified length, then characters on the left are truncated, ortherwise
	 * the specified character is appended to the result on the left  to obtain the appropriate length.
	 * @param source the string to format; if null, it is considered an empty string.
	 * @param padChar the character used to fill the result up to the specified length.
	 * @param maxLen the length of the formatted string.
	 * @return a string formatted to the specified length.
	 */
	public static String padLeft(final String source, final char padChar, final int maxLen)
	{
		StringBuilder sb = new StringBuilder();
		String src = (source == null) ? "" : source;
		int length = src.length();
		//if (length > maxLen) sb.append(source, length-maxLen, maxLen);
		if (length > maxLen) return source;
		else
		{
			for (int i=0; i<maxLen-length; i++) sb.append(padChar);
			sb.append(src);
		}
		return sb.toString();
	}

	/**
	 * Padds a string on the right side with a given character
	 * If the string is longer than the specified length, then characters on the right are truncated, ortherwise
	 * the specified character is appended to the result on the right  to obtain the appropriate length.
	 * @param source the string to pad to the right
	 * @param padChar the character used for padding
	 * @param maxLen the length to pad the string up to
	 * if its length is greater than the padding length
	 * @return the padded (or truncated) string
	 */
	public static String padRight(final String source, final char padChar, final int maxLen)
	{
		String s = source;
		if (s == null) s = "";
		if (s.length() > maxLen) s = s.substring(0, maxLen);
		StringBuilder sb = new StringBuilder(s);
		while (sb.length() < maxLen) sb.append(padChar);
		return sb.toString();
	}

	/**
	 * An array of char containing the hex digits in ascending order.
	 */
	private static char[] hexDigits =
	{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	/**
	 * Convert a part of an array of bytes, into a string of space-separated hexadecimal numbers.<br>
	 * This method is proposed as a convenience for debugging purposes.
	 * @param bytes the array that contains the sequence of byte values to convert.
	 * @param start the index to start at in the byte array.
	 * @param length the number of bytes to convert in the array.
	 * @return the converted bytes as a string of space-separated hexadecimal numbers.
	 */
	public static String dumpBytes(final byte[] bytes, final int start, final int length)
	{
		StringBuilder sb = new StringBuilder();
		if (length >= 0)
		{
			for (int i=start; i<Math.min(bytes.length, start+length); i++)
			{
				if (i > start) sb.append(' ');
				sb.append(toHexString(bytes[i]));
			}
		}
		return sb.toString();
	}

	/**
	 * Convert a byte value into a 2-digits hexadecimal value. The first digit is 0 if the value is less than 16.<br>
	 * If a value is negative, its 2-complement value is converted, otherwise the value itself is converted.
	 * @param b the byte value to convert.
	 * @return a string containing the 2-digit hexadecimal representation of the byte value.
	 */
	public static String toHexString(final byte b)
	{
		int n = (b < 0) ? b + 256 : b;
		StringBuilder sb = new StringBuilder();
		sb.append(hexDigits[n / 16]);
		sb.append(hexDigits[n % 16]);
		return sb.toString();
	}

	/**
	 * Convert a string of space-separated hexadecimal numbers into an array of bytes.
	 * @param hexString the string to convert.
	 * @return the resulting array of bytes.
	 */
	public static byte[] toBytes(final String hexString)
	{
		String[] bytes = hexString.split("\\s");
		List<Byte> list = new ArrayList<Byte>(bytes.length);
		byte[] result = new byte[list.size()];
		for (int i=0; i<bytes.length; i++)
		{
			int n = Byte.parseByte(bytes[i].substring(0, 1), 16);
			n = 16 * n + Byte.parseByte(bytes[i].substring(1), 16);
			result[i] = Byte.valueOf((byte) n);
		}
		return result;
	}

	/**
	 * Tranform a duration in milliseconds into a string with hours, minutes, seconds and milliseconds..
	 * @param duration the duration to transform, expressed in milliseconds.
	 * @return a string specifiying the duration in terms of hours, minutes, seconds and milliseconds.
	 */
	public static String toStringDuration(final long duration)
	{
		long elapsed = duration;
		StringBuilder sb = new StringBuilder();
		sb.append(padLeft(""+(elapsed / 3600000L), '0', 2)).append(':');
		elapsed = elapsed % 3600000L;
		sb.append(padLeft(""+(elapsed / 60000L), '0', 2)).append(':');
		elapsed = elapsed % 60000L;
		sb.append(padLeft(""+(elapsed / 1000L), '0', 2)).append('.');
		sb.append(padLeft(""+(elapsed % 1000L), '0', 3));
		return sb.toString();
	}

	/**
	 * Replace pre-determined keywords in a string, with shorter ones.
	 * @param key the string to shorten.
	 * @return the string with its keywords replaced.
	 */
	public static String shortenLabel(final String key)
	{
		String result = key;
		for (int i=0; i<KEYWORDS.length; i++)
		{
			if (result.contains(KEYWORDS[i])) result = result.replace(KEYWORDS[i], REPLACEMENTS[i]);
		}
		return result;
	}

	/**
	 * Returns the IP address of the remote host for a socket channel.
	 * @param channel the channel to get the host from.
	 * @return an IP address as a string.
	 */
	public static String getRemoteHost(final Channel channel)
	{
		StringBuilder sb = new StringBuilder();
		if (channel instanceof SocketChannel)
		{
			if (channel.isOpen())
			{
				Socket s = ((SocketChannel)channel).socket();
				sb.append(getRemoteHost(s.getRemoteSocketAddress()));
			}
			else
			{
				sb.append("[channel closed]");
			}
		}
		else
		{
			sb.append("[JVM-local]");
		}
		return sb.toString();
	}

	/**
	 * Returns the IP address of the remote host for a socket channel.
	 * @param address the address to get the host from.
	 * @return an IP address as a string.
	 */
	public static String getRemoteHost(final SocketAddress address)
	{
		StringBuilder sb = new StringBuilder();
		//sb.append("[");
		if (address instanceof InetSocketAddress)
		{
			InetSocketAddress add = (InetSocketAddress) address;
			sb.append(add.getHostName()).append(':').append(add.getPort());
		}
		else sb.append("socket address type not handled: ").append(address);
		//sb.append("]");
		return sb.toString();
	}

	/**
	 * Get a String representation of an array of any type.
	 * @param <T> the type of the array.
	 * @param array the array from which to build a string representation.
	 * @return the array's content as a string.
	 */
	public static <T> String arrayToString(final T[] array)
	{
		return arrayToString(array, ",", "[", "]");
	}

	/**
	 * Get a String representation of an array of any type.
	 * @param <T> the type of the array.
	 * @param array the array from which to build a string representation.
	 * @param sep the separator to use for values. If null, no separator is used.
	 * @param prefix the prefix to use at the start of the resulting string. If null, no prefix is used.
	 * @param suffix the suffix to use at the end of the resulting string. If null, no suffix is used.
	 * @return the array's content as a string.
	 */
	public static <T> String arrayToString(final T[] array, final String sep, final String prefix, final String suffix)
	{
		StringBuilder sb = new StringBuilder();
		if (array == null) sb.append("null");
		else
		{
			if (prefix != null) sb.append(prefix);
			for (int i=0; i<array.length; i++)
			{
				if ((i > 0) && (sep != null)) sb.append(sep);
				sb.append(array[i]);
			}
			if (suffix != null) sb.append(suffix);
		}
		return sb.toString();
	}

	/**
	 * Parse an array of port numbers from a string containing a list of space-separated port numbers.
	 * @param s list of space-separated port numbers
	 * @return an array of int port numbers.
	 */
	public static int[] parseIntValues(final String s)
	{
		String[] strPorts = s.split("\\s");
		int[] ports = new int[strPorts.length];
		for (int i=0; i<strPorts.length; i++)
		{
			try
			{
				int n = Integer.valueOf(strPorts[i].trim());
				ports[i] = n;
			}
			catch(NumberFormatException e)
			{
				return null;
			}
		}
		return ports;
	}

	/**
	 * Convert an array of int values into a space-separated string.
	 * @param ports list of port numbers
	 * @return a space-separated list of ports.
	 */
	public static String buildString(final int[] ports)
	{
		if ((ports == null) || (ports.length == 0)) return "";
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<ports.length; i++)
		{
			if (i > 0) sb.append(' ');
			sb.append(ports[i]);
		}
		return sb.toString();
	}

	/**
	 * Parse a host:port string into a pair made of a host string and an integer port.
	 * @param s a host:port string.
	 * @return a <code>Pair&lt;String, Integer&gt;</code> instance.
	 */
	public static HostPort parseHostPort(final String s)
	{
		String[] comps = s.split(":");
		int port = -1;
		try
		{
			port = Integer.valueOf(comps[1].trim());
		}
		catch(NumberFormatException e)
		{
			//log.error("invalid port number format: " + comps[1]);
			return null;
		}
		return new HostPort(comps[0], port);
	}

	/**
	 * Get a throwable's stack trace.
	 * @param t the throwable to get the stack trace from.
	 * @return the stack trace as astring.
	 */
	public static String getStackTrace(final Throwable t)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.close();
		return sw.toString();
	}

	/**
	 * Build a string made of the specified tokens.
	 * @param args the tokens composing the string.
	 * @return the concatenation of the string values of the tokens.
	 */
	public static String buildString(final Object...args)
	{
		if (args == null) return null;
		StringBuilder sb = new StringBuilder();
		for (Object o: args) sb.append(o);
		return sb.toString();
	}

	/**
	 * Determine whether the specified source string starts with one of the specified values.
	 * @param source the string to match with the values.
	 * @param ignoreCase specifies whether case should be ignore in the string matching.
	 * @param values the values to match the source with.
	 * @return true if the source matches one of the values, false otherwise.
	 */
	public static boolean startsWithOneOf(final String source, final boolean ignoreCase, final String...values)
	{
		if ((source == null) || (values == null)) return false;
		String s = ignoreCase ? source.toLowerCase(): source;
		for (String val: values)
		{
			if (val == null) continue;
			String s2 = ignoreCase ? val.toLowerCase() : val;
			if (s.startsWith(s2)) return true;
		}
		return false;
	}

	/**
	 * Determine whether the specified source string is equal to one of the specified values.
	 * @param source the string to match with the values.
	 * @param ignoreCase specifies whether case should be ignore in the string matching.
	 * @param values the values to match the source with.
	 * @return true if the source matches one of the values, false otherwise.
	 */
	public static boolean isOneOf(final String source, final boolean ignoreCase, final String...values)
	{
		if ((source == null) || (values == null)) return false;
		String s = ignoreCase ? source.toLowerCase(): source;
		for (String val: values)
		{
			if (val == null) continue;
			String s2 = ignoreCase ? val.toLowerCase() : val;
			if (s.equals(s2)) return true;
		}
		return false;
	}

	/**
	 * Convert an IP address int array.
	 * @param addr the source address to convert.
	 * @return an array of int values, or null if the source could not be parsed.
	 */
	public static int[] toIntArray(final InetAddress addr)
	{
		try
		{
			byte[] bytes = addr.getAddress();
			String ip = addr.getHostAddress();
			int[] result = null;
			if (addr instanceof Inet6Address)
			{
				result = new int[8];
				String[] comp = ip.split(":");
				for (int i=0; i<comp.length; i++) result[i] = Integer.decode("0x" + comp[i].toLowerCase());
			}
			else
			{
				result = new int[4];
				String[] comp = ip.split("\\.");
				for (int i=0; i<comp.length; i++) result[i] = Integer.valueOf(comp[i]);
			}
			return result;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Convert a string with <code>separator</code>-separated values into an int array.
	 * @param source the source string to convert.
	 * @param separatorPattern the values separator, expressed as a regular expression, must comply with the specifications for {@link java.util.regex.Pattern}.
	 * @return an array of int value, or null if the source cvould not be parsed.
	 */
	public static int[] toIntArray(final String source, final Pattern separatorPattern)
	{
		try
		{
			String[] vals = separatorPattern.split(source);
			int[] result = new int[vals.length];
			for (int i=0; i<vals.length; i++) result[i] = Integer.valueOf(vals[i]);
			return result;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Create an instance of the UTF-8 charset.
	 * @return a {@link Charset} instance for UTF-8, or null if the charset could not be instantiated.
	 */
	private static Charset makeUTF8()
	{
		Charset utf8 = null;
		try
		{
			utf8 = Charset.forName("UTF-8");
		}
		catch(Exception e)
		{
			//log.error("Charset UTF-8 could not be instantiated", e);
			return null;
		}
		return utf8;
	}
}
