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
package org.jppf.ui.monitoring;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import org.apache.log4j.Logger;
import org.jppf.server.JPPFStats;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.StringUtils;
import org.jvnet.substance.SubstanceDefaultTableCellRenderer;

/**
 * This class provides a graphical interface for monitoring the status and health 
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) fot the whole UI.
 * @author Laurent Cohen
 */
public class MonitoringPanel extends JPanel implements StatsHandlerListener, StatsConstants
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(MonitoringPanel.class);
	/**
	 * Base name for localization bundle lookups.
	 */
	private static final String BASE = "org.jppf.ui.i18n.StatsPage";
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;
	/**
	 * Holds a list of table models to update wwhen new stats are received.
	 */
	private List<MonitorTableModel> tableModels = new ArrayList<MonitorTableModel>();

	/**
	 * Default contructor.
	 */
	public MonitoringPanel()
	{
		this.statsHandler = StatsHandler.getInstance();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(5));
		add(makeRefreshPanel());
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(EXECUTION_PROPS, StringUtils.getLocalized(BASE, "ExecutionTable.label")));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(NODE_EXECUTION_PROPS, StringUtils.getLocalized(BASE, "NodeExecutionTable.label")));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(TRANSPORT_PROPS, StringUtils.getLocalized(BASE, "NetworkOverheadTable.label")));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(QUEUE_PROPS, StringUtils.getLocalized(BASE, "QueueTable.label")));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(CONNECTION_PROPS, StringUtils.getLocalized(BASE, "ConnectionsTable.label")));
		add(Box.createVerticalGlue());
	}
	
	/**
	 * Called when new stats have been received from the server.
	 * @param event holds the new stats values.
	 */
	public void dataUpdated(StatsHandlerEvent event)
	{
		for (final MonitorTableModel model: tableModels)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					model.fireTableDataChanged();
				}
			});
		}
	}
	
	/**
	 * Create a chartPanel with a &quot;refresh now&quote; button.
	 * @return a <code>JComponent</code> instance.
	 */
	private JComponent makeRefreshPanel()
	{
		JButton btn = new JButton(StringUtils.getLocalized(BASE, "RefreshBtn.label"));
		String s = StringUtils.getLocalized(BASE, "RefreshBtn.tooltip");
		if (s != null) btn.setToolTipText(s);
		btn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				statsHandler.requestUpdate();
			}
		});
		btn.setPreferredSize(new Dimension(100, 20));
		return btn;
	}

	/**
	 * Create a chartPanel displaying a group of values.
	 * @param props the names of the values to display.
	 * @param title the title of the chartPanel.
	 * @return a <code>JComponent</code> instance.
	 */
	private JComponent makeTablePanel(Fields[] props, String title)
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		panel.setBorder(BorderFactory.createTitledBorder(title));
		JTable table = new JTable()
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		MonitorTableModel model = new MonitorTableModel(props);
		table.setModel(model);
		table.setOpaque(true);
		DefaultTableCellRenderer rend1 = new SubstanceDefaultTableCellRenderer();
		rend1.setHorizontalAlignment(JLabel.RIGHT);
		rend1.setOpaque(true);
		table.getColumnModel().getColumn(1).setCellRenderer(rend1);
		DefaultTableCellRenderer rend0 = new SubstanceDefaultTableCellRenderer();
		rend0.setHorizontalAlignment(JLabel.LEFT);
		rend0.setOpaque(true);
		table.getColumnModel().getColumn(0).setCellRenderer(rend0);
		tableModels.add(model);
		panel.add(table);
		table.setShowGrid(false);
		return panel;
	}

	/**
	 * Data model for the tables displaying the values.
	 */
	private class MonitorTableModel extends AbstractTableModel
	{
		/**
		 * The list of fields whose values are displayed in the table.
		 */
		private Fields[] fields = null;

		/**
		 * Initialize this table model witht he specified list of fields.
		 * @param fields the list of fields whose values are displayed in the table.
		 */
		MonitorTableModel(Fields[] fields)
		{
			this.fields = fields;
		}

		/**
		 * Get the number of columns in the table.
		 * @return 2.
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount()
		{
			return 2;
		}

		/**
		 * Get the number of rows in the table.
		 * @return the number of fields displayed in the table.
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount()
		{
			return fields.length;
		}

		/**
		 * Get a value at specified coordinates in the table.
		 * @param row the row coordinate.
		 * @param column the column coordinate.
		 * @return the value as an object.
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int row, int column)
		{
			Map<Fields, String> valuesMap = null;
			if (statsHandler.getStatsCount() > 0) valuesMap = statsHandler.getLatestStringValues();
			else valuesMap = StatsFormatter.getStringValuesMap(new JPPFStats());
			Fields name = fields[row];
			return column == 0 ? name : valuesMap.get(name);
		}
	}
}
