/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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
package org.jppf.utils;

import java.io.Serializable;

/**
 * Utility class holding a pair of references to two objects.
 * @param <U> the type of the first element in the pair.
 * @param <V> the type of the second element in the pair.
 * @author Laurent Cohen
 */
public class Pair<U, V> implements Serializable
{
	/**
	 * The first object of this pair.
	 */
	private U first = null;
	/**
	 * The second object of this pair.
	 */
	private V second = null;
	
	/**
	 * Initialize this pair with two values.
	 * @param first the first value of the new pair.
	 * @param second the second value of the new pair.
	 */
	public Pair(U first, V second)
	{
		this.first = first;
		this.second = second;
	}

	/**
	 * Get the first value of this pair.
	 * @return an object of type U.
	 */
	public U first()
	{
		return first;
	}

	/**
	 * Get the second value of this pair.
	 * @return an object of type V.
	 */
	public V second()
	{
		return second;
	}
}
