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
package org.jppf.ui.monitoring;

import java.util.*;
import org.jvnet.substance.plugin.SubstanceThemePlugin;
import org.jvnet.substance.theme.ThemeInfo;
import org.jvnet.substance.theme.SubstanceTheme.ThemeKind;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFThemePlugin implements SubstanceThemePlugin
{
	/**
	 * 
	 * @return .
	 * @see org.jvnet.substance.plugin.SubstanceThemePlugin#getDefaultThemeClassName()
	 */
	public String getDefaultThemeClassName()
	{
		return JPPFTheme.class.getName();
	}

	/**
	 * 
	 * @return .
	 * @see org.jvnet.substance.plugin.SubstanceThemePlugin#getThemes()
	 */
	public Set<ThemeInfo> getThemes()
	{
		Set<ThemeInfo> set = new HashSet<ThemeInfo>();
		set.add(new ThemeInfo("JPPF", JPPFTheme.class.getName(), ThemeKind.COLD));
		return set;
	}
}
