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
package org.jppf.ui.options;

import java.awt.Dimension;
import java.util.*;
import javax.swing.JToolBar;

/**
 * This option class encapsulates a split pane, as the one present in the Swing api.
 * @author Laurent Cohen
 */
public class ToolbarOption extends AbstractOptionElement implements OptionsPage
{
	/**
	 * The list of children of this options page.
	 */
	protected List<OptionElement> children = new ArrayList<OptionElement>();

	/**
	 * Initialize the split pane with 2 fillers as left (or top) and right (or bottom) components.
	 */
	public ToolbarOption()
	{
	}

	/**
	 * Initialize the panel used to display this options page.
	 */
	public void createUI()
	{
		JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
		toolbar.setFloatable(false);
		if ((width > 0) && (height > 0)) toolbar.setPreferredSize(new Dimension(width, height));
		UIComponent = toolbar;
		toolbar.setOpaque(false);
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
		if (UIComponent != null) UIComponent.setEnabled(enabled);
		for (OptionElement elt: children) elt.setEnabled(enabled);
	}

	/**
	 * Enable or disable the events firing in this option and/or its children.
	 * @param enabled true to enable the events, false to disable them.
	 * @see org.jppf.ui.options.OptionElement#setEventsEnabled(boolean)
	 */
	public void setEventsEnabled(boolean enabled)
	{
		for (OptionElement elt: children) elt.setEventsEnabled(enabled);
	}

	/**
	 * Add an element to this options page. The element can be either an option, or another page.
	 * @param element the element to add.
	 * @see org.jppf.ui.options.OptionsPage#add(org.jppf.ui.options.OptionElement)
	 */
	public void add(OptionElement element)
	{
		JToolBar toolbar = (JToolBar) UIComponent;
		children.add(element);
		if (element instanceof AbstractOptionElement)
			((AbstractOptionElement) element).setParent(this);
		toolbar.add(element.getUIComponent());
	}

	/**
	 * Remove an element from this options page.
	 * @param element the element to remove.
	 * @see org.jppf.ui.options.OptionsPage#remove(org.jppf.ui.options.OptionElement)
	 */
	public void remove(OptionElement element)
	{
		JToolBar toolbar = (JToolBar) UIComponent;
		children.remove(element);
		if (element instanceof AbstractOptionElement)
			((AbstractOptionElement) element).setParent(null);
		toolbar.remove(element.getUIComponent());
	}

	/**
	 * Determines whether this page is part of another.
	 * @return true if this page is an outermost page, false if it is embedded within another page.
	 * @see org.jppf.ui.options.OptionsPage#isMainPage()
	 */
	public boolean isMainPage()
	{
		return false;
	}

	/**
	 * Get the options in this page.
	 * @return a list of <code>Option</code> instances.
	 * @see org.jppf.ui.options.OptionsPage#getChildren()
	 */
	public List<OptionElement> getChildren()
	{
		return Collections.unmodifiableList(children);
	}
}
