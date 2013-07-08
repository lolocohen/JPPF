/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package sample.matrix;

import org.jppf.server.protocol.JPPFTask;

/**
 * JPPF task used to test how exceptions are handled within the nodes.
 * @author Laurent Cohen
 */
public class ExceptionTestTask extends JPPFTask
{
	/**
	 * The index of the method to invoke.
	 */
	int methodNumber = 0;

	/**
	 * Default constructor - the method that throws an NPE will be invoked.
	 */
	public ExceptionTestTask()
	{
	}

	/**
	 * Initialize this test task with the index of a method to invoke.
	 * @param n th emethod index as int value.
	 */
	public ExceptionTestTask(int n)
	{
		this.methodNumber = n;
	}

	/**
	 * Run some code that throws an exception.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		if (methodNumber == 0) testThrowNPE();
		else if (methodNumber == 1) testThrowArrayIndexOutOfBoundsException();
		else testThrowSecurityException();
	}
	
	/**
	 * This method throws a <code>NullPointerException</code>.
	 */
	protected void testThrowNPE()
	{
		String s = null;
		s.length();
	}
	
	/**
	 * This method throws an <code>ArrayIndexOutOfBoundsException</code>.
	 */
	protected void testThrowArrayIndexOutOfBoundsException()
	{
		int[] intArray = new int[2];
		int n = intArray[3];
		n += 1;
	}
	
	/**
	 * This method throws a <code>SecurityException</code>.
	 */
	protected void testThrowSecurityException()
	{
		System.getProperty("throw.security.exception");
	}
}