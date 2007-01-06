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
package org.jppf.ui.options.xml;

import java.awt.Insets;
import java.io.*;
import java.net.*;
import org.apache.log4j.Logger;
import org.jppf.ui.options.*;
import org.jppf.ui.options.event.*;
import org.jppf.ui.options.xml.OptionDescriptor.*;
import org.jppf.utils.*;

/**
 * Instances of this class build options pages from XML descriptors.
 * @author Laurent Cohen
 */
public class OptionsPageBuilder
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(OptionsPageBuilder.class);
	/**
	 * Base name used to localize labels and tooltips.
	 */
	public final static String BASE_NAME = "org.jppf.ui.i18n.";
	/**
	 * Base name used to localize labels and tooltips.
	 */
	private String baseName = null;
	/**
	 * Base name used to localize labels and tooltips.
	 */
	private boolean eventEnabled = true;
	/**
	 * Element factory used by this builder.
	 */
	private OptionElementFactory factory = null;

	/**
	 * Default constructor.
	 */
	public OptionsPageBuilder()
	{
	}

	/**
	 * Initialize this page builder.
	 * @param enableEvents determines if events triggering should be performed
	 * once the page is built.
	 */
	public OptionsPageBuilder(boolean enableEvents)
	{
		this.eventEnabled = enableEvents;
	}

	/**
	 * Build an option page from the specified XML descriptor.
	 * @param content the text of the XML document to parse.
	 * @param baseName the base path where the localization resources are located.
	 * @return an <code>OptionElement</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while parsing the xml document or building the page.
	 */
	public OptionElement buildPageFromContent(String content, String baseName) throws Exception
	{
		this.baseName = baseName;
		OptionDescriptor desc = new OptionDescriptorParser().parse(new StringReader(content));
		if (desc == null) return null;
		OptionElement page = build(desc);
		if (eventEnabled) triggerInitialEvents(page);
		return page;
	}

	/**
	 * Build an option page from an XML descriptor specified as a URL.
	 * @param urlString the URL of the XML descriptor file.
	 * @param baseName the base path where the localization resources are located.
	 * @return an <code>OptionsPage</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while parsing the xml document or building the page.
	 */
	public OptionElement buildPageFromURL(String urlString, String baseName) throws Exception
	{
		if (urlString == null) return null;
		URL url = null;
		try
		{
			url = new URL(urlString);
		}
		catch(MalformedURLException e)
		{
			log.error(e.getMessage(), e);
			return null;
		}
		Reader reader = new InputStreamReader(url.openStream());
		return buildPageFromContent(FileUtils.readTextFile(reader), baseName);
	}

	/**
	 * Build an option page from the specified XML descriptor.
	 * @param xmlPath the path to the XML descriptor file.
	 * @param baseName the base path where the localization resources are located.
	 * @return an <code>OptionElement</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while parsing the xml document or building the page.
	 */
	public OptionElement buildPage(String xmlPath, String baseName) throws Exception
	{
		if (baseName == null)
		{
			int idx = xmlPath.lastIndexOf("/");
			this.baseName = BASE_NAME + ((idx < 0) ? xmlPath : xmlPath.substring(idx + 1));
			idx = this.baseName.lastIndexOf(".xml");
			if (idx >= 0) this.baseName = this.baseName.substring(0, idx);
		}
		else this.baseName = baseName;
		OptionDescriptor desc = new OptionDescriptorParser().parse(xmlPath);
		if (desc == null) return null;
		OptionElement page = build(desc);
		if (eventEnabled) triggerInitialEvents(page);
		return page;
	}

	/**
	 * Trigger all events listeners for all options, immeidately after the page has been built.
	 * This ensures the consistence of the UI's initial state.
	 * @param elt the root element of the options on which to trigger the events.
	 */
	private void triggerInitialEvents(OptionElement elt)
	{
		if (elt == null) return;
		if (elt.getInitializer() != null)
		{
			elt.getInitializer().valueChanged(new ValueChangeEvent(elt));
		}
		if (elt instanceof OptionsPage)
		{
			for (OptionElement child: ((OptionsPage) elt).getChildren())
			{
				triggerInitialEvents(child);
			}
		}
	}

	/**
	 * Initialize the attributes common to all option elements from an option descriptor. 
	 * @param elt the element whose attributes are to be initialized.
	 * @param desc the descriptor to get the attribute values from.
	 */
	public void initCommonAttributes(AbstractOptionElement elt, OptionDescriptor desc)
	{
		elt.setName(desc.name);
		elt.setLabel(StringUtils.getLocalized(baseName, desc.name+".label", desc.getProperty("label")));
		String s = desc.getProperty("orientation", "horizontal");
		elt.setOrientation("horizontal".equalsIgnoreCase(s) ? OptionsPage.HORIZONTAL : OptionsPage.VERTICAL);
		elt.setToolTipText(StringUtils.getLocalized(baseName, desc.name+".tooltip", desc.getProperty("tooltip")));
		elt.setScrollable(desc.getBoolean("scrollable", false));
		elt.setBordered(desc.getBoolean("bordered", false));
		elt.setWidth(desc.getInt("width", -1));
		elt.setHeight(desc.getInt("height", -1));
		s = desc.getProperty("insets");
		int defMargin = 2;
		if ((s == null) || ("".equals(s.trim())))
			elt.setInsets(new Insets(defMargin, defMargin, defMargin, defMargin));
		else
		{
			String[] sVals = s.split(",");
			if (sVals.length != 4) elt.setInsets(new Insets(defMargin, defMargin, defMargin, defMargin));
			else
			{
				int[] vals = new int[4];
				for (int i=0; i<4; i++)
				{
					try
					{
						vals[i] = Integer.parseInt(sVals[i].trim());
					}
					catch(NumberFormatException e)
					{
						vals[i] = defMargin;
					}
				}
				elt.setInsets(new Insets(vals[0], vals[1], vals[2], vals[3]));
			}
		}
		for (ScriptDescriptor script: desc.scripts) elt.getScripts().add(script);
		if (desc.initializer != null) elt.setInitializer(createListener(desc.initializer));
	}

	/**
	 * Initialize the attributes common to all options from an option descriptor. 
	 * @param option the option whose attributes are to be initialized.
	 * @param desc the descriptor to get the attribute values from.
	 */
	public void initCommonOptionAttributes(AbstractOption option, OptionDescriptor desc)
	{
		initCommonAttributes(option, desc);
		option.setPersistent(desc.getBoolean("persistent", false));
		for (ListenerDescriptor listenerDesc: desc.listeners)
		{
			ValueChangeListener listener = createListener(listenerDesc);
			if (listener != null) option.addValueChangeListener(listener);
		}
	}

	/**
	 * Create a value change listener from a listener descriptor.
	 * @param listenerDesc the listener descriptor to get the listener properties from.
	 * @return a ValueChangeListener instance.
	 */
	public ValueChangeListener createListener(ListenerDescriptor listenerDesc)
	{
		ValueChangeListener listener = null;
		try
		{
			if (listenerDesc != null)
			{
				if ("java".equals(listenerDesc.type))
				{
					Class clazz = Class.forName(listenerDesc.className);
					listener = (ValueChangeListener) clazz.newInstance();
				}
				else
				{
					ScriptDescriptor script = listenerDesc.script;
					listener = new ScriptedValueChangeListener(script.language, script.source);
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return listener;
	}

	/**
	 * Add all the children elements in a page.
	 * @param desc the descriptor for the page.
	 * @return an OptionElement instance.
	 * @throws Exception if an error was raised while building the page.
	 */
	public OptionElement build(OptionDescriptor desc) throws Exception
	{
		OptionElementFactory f = getFactory();
		OptionElement elt = null; 
		String type = desc.type;
		if ("page".equals(type)) elt = f.buildPage(desc);
		else if ("SplitPane".equals(desc.type)) elt = f.buildSplitPane(desc);
		else if ("TabbedPane".equals(desc.type)) elt = f.buildTabbedPane(desc);
		else if ("Toolbar".equals(desc.type)) elt = f.buildToolbar(desc);
		else if ("ToolbarSeparator".equals(desc.type)) elt = f.buildToolbarSeparator(desc);
		else if ("Button".equals(desc.type)) elt = f.buildButton(desc);
		else if ("TextArea".equals(desc.type)) elt = f.buildTextArea(desc);
		else if ("XMLEditor".equals(desc.type)) elt = f.buildXMLEditor(desc);
		else if ("Password".equals(desc.type)) elt = f.buildPassword(desc);
		else if ("PlainText".equals(desc.type)) elt = f.buildPlainText(desc);
		else if ("FormattedNumber".equals(desc.type)) elt = f.buildFormattedNumber(desc);
		else if ("SpinnerNumber".equals(desc.type)) elt = f.buildSpinnerNumber(desc);
		else if ("Boolean".equals(desc.type)) elt = f.buildBoolean(desc);
		else if ("ComboBox".equals(desc.type)) elt = f.buildComboBox(desc);
		else if ("Filler".equals(desc.type)) elt = f.buildFiller(desc);
		else if ("List".equals(desc.type)) elt = f.buildList(desc);
		else if ("FileChooser".equals(desc.type)) elt = f.buildFileChooser(desc);
		else if ("Label".equals(desc.type)) elt = f.buildLabel(desc);
		else if ("import".equals(desc.type)) elt = f.loadImport(desc);
		return elt;
	}

	/**
	 * Get the element factory used by this builder.
	 * @return an <code>OptionElementFactory</code> instance.
	 */
	public OptionElementFactory getFactory()
	{
		if (factory == null) factory = new OptionElementFactory(this);
		return factory;
	}

	/**
	 * Get the base name used to localize labels and tooltips.
	 * @return the base name as a string value.
	 */
	public String getBaseName()
	{
		return baseName;
	}
}
