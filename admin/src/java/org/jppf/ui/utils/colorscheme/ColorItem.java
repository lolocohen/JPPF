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

package org.jppf.ui.utils.colorscheme;

import java.awt.Color;

/**
 * 
 * @author Laurent Cohen
 */
public class ColorItem
{
	/**
	 * Display name of this item.
	 */
	public String name = null;
	/**
	 * Color to use for this item.
	 */
	public Color color = null;

	/**
	 * Initialize this item with the specified name and color.
	 * @param name the display name of this item.
	 * @param color the color to use for this item.
	 */
	public ColorItem(String name, Color color)
	{
		this.name = name;
		this.color = color;
	}

	/**
	 * Get a string representation of this item.
	 * @return this item's display name.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name == null ? "[no name]" : name;
	}
}
