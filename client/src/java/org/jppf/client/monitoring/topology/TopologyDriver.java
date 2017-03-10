/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.client.monitoring.topology;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.server.job.management.DriverJobManagementMBean;

/**
 * Implementation of {@link TopologyDriver} for JPPF drivers.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyDriver extends AbstractTopologyComponent {
  /**
   * Determines whether the corresponding driver is collapsed in the visualization panel.
   */
  private boolean collapsed = false;
  /**
   * A driver connection.
   */
  private final JPPFClientConnection connection;

  /**
   * Initialize this topology data as a driver related object.
   * @param connection a reference to the driver connection.
   */
  TopologyDriver(final JPPFClientConnection connection) {
    super(connection.getDriverUuid());
    this.connection = connection;
    JPPFConnectionPool pool = connection.getConnectionPool();
    this.managementInfo = new JPPFManagementInfo(pool.getDriverHost(), pool.getDriverIPAddress(), pool.getJmxPort(), pool.getDriverUuid(), JPPFManagementInfo.DRIVER, pool.isSslEnabled());
  }

  /**
   * This method always returns {@code true}.
   * @return {@code true}.
   */
  @Override
  public boolean isDriver() {
    return true;
  }

  /**
   * Get the wrapper holding the connection to the JMX server on a driver or node.
   * @return a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public JMXDriverConnectionWrapper getJmx() {
    return (connection == null) ? null : connection.getConnectionPool().getJmxConnection();
  }

  /**
   * Get the driver connection.
   * @return a {@link JPPFClientConnection} instance.
   */
  public JPPFClientConnection getConnection() {
    return connection;
  }

  /**
   * Determine whether the corresponding driver is collapsed in the visualization panel.
   * @return <code>true</code> if the driver is collapsed, <code>false</code> otherwise.
   * @exclude
   */
  public boolean isCollapsed() {
    return collapsed;
  }

  /**
   * Specify whether the corresponding driver is collapsed in the visualization panel.
   * @param collapsed <code>true</code> if the driver is collapsed, <code>false</code> otherwise.
   * @exclude
   */
  public void setCollapsed(final boolean collapsed) {
    this.collapsed = collapsed;
  }

  /**
   * Get the proxy to the driver MBean that forwards node management requests.
   * @return an instance of {@link JPPFNodeForwardingMBean}.
   */
  public JPPFNodeForwardingMBean getForwarder() {
    JMXDriverConnectionWrapper jmx = getJmx();
    if ((jmx != null) && jmx.isConnected()) {
      try {
        return jmx.getNodeForwarder();
      } catch (Exception ignore) {
      }
    }
    return null;
  }

  /**
   * Get the proxy to the driver MBean that manages and monitors jobs.
   * @return an instance of {@link DriverJobManagementMBean}.
   */
  public DriverJobManagementMBean getJobManager() {
    JMXDriverConnectionWrapper jmx = getJmx();
    if ((jmx != null) && jmx.isConnected()) {
      try {
        return jmx.getJobManager();
      } catch (Exception ignore) {
      }
    }
    return null;
  }

  /**
   * Gert the diagnostics mbean for this driver.
   * @return a {@link DiagnosticsMBean} instance.
   */
  public DiagnosticsMBean getDiagnostics() {
    JMXDriverConnectionWrapper jmx = getJmx();
    if ((jmx != null) && jmx.isConnected()) {
      try {
        return jmx.getDiagnosticsProxy();
      } catch (Exception ignore) {
      }
    }
    return null;
  }

  @Override
  public String toString() {
    JMXDriverConnectionWrapper jmx = getJmx();
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("connection=").append(connection);
    sb.append(", managementInfo=").append(managementInfo);
    sb.append(", uuid=").append(uuid);
    sb.append(']');
    //return (jmx == null) ? (managementInfo == null ? "?" : managementInfo.toDisplayString()) : jmx.getDisplayName();
    return sb.toString();
  }

  @Override
  public String getDisplayName() {
    return managementInfo == null ? toString() : managementInfo.toDisplayString();
  }

  /**
   * Convenience method to get the nodes attached to this driver as {@link TopologyNode} objects.
   * @return a list of {@link TopologyNode} objects, possibly empty if this driver has no attache node.
   * @since 5.1
   */
  public List<TopologyNode> getNodes() {
    List<TopologyNode> nodes = new ArrayList<>(getChildCount());
    synchronized(this) {
      for (AbstractTopologyComponent comp: children.values()) {
        if (comp.isNode()) nodes.add((TopologyNode) comp);
      }
    }
    return nodes;
  }

  /**
   * Convenience method to get the peers connected to this driver as {@link TopologyPeer} objects.
   * @return a list of {@link TopologyPeer} objects, possibly empty if this driver is not connected to any peer.
   * @since 5.1
   */
  public List<TopologyPeer> getPeers() {
    List<TopologyPeer> peers = new ArrayList<>(getChildCount());
    synchronized(this) {
      for (AbstractTopologyComponent comp: children.values()) {
        if (comp.isPeer()) peers.add((TopologyPeer) comp);
      }
    }
    return peers;
  }

  /**
   * Convenience method to get the nodes and peers connected to this driver as {@link TopologyNode} objects.
   * @return a list of {@link TopologyNode} objects, possibly empty if this driver has no attached node and isn't connected to any peer.
   * @since 5.1
   */
  public List<TopologyNode> getNodesAndPeers() {
    List<TopologyNode> nodes = new ArrayList<>(getChildCount());
    synchronized(this) {
      for (AbstractTopologyComponent comp: children.values()) nodes.add((TopologyNode) comp);
    }
    return nodes;
  }
}
