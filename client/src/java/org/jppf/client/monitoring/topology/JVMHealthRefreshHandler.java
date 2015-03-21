/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.management.*;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JVMHealthRefreshHandler extends AbstractRefreshHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JVMHealthRefreshHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this node handler.
   * @param manager the topology manager.
   */
  public JVMHealthRefreshHandler(final TopologyManager manager) {
    super(manager, "JVM Health Update Timer", JPPFConfiguration.getProperties().getLong("jppf.admin.refresh.interval.health", 1000L));
  }

  @Override
  protected synchronized void performRefresh() {
    for (TopologyDriver driver: manager.getDrivers()) {
      if (driver.getDiagnostics() == null) continue;
      JMXDriverConnectionWrapper jmx = driver.getJmx();
      if ((jmx == null) || !jmx.isConnected()) continue;
      try {
        HealthSnapshot health = driver.getDiagnostics().healthSnapshot();
        if (log.isTraceEnabled()) log.trace("got driver health snapshot: " + health);
        if ((health != null) && !health.equals(driver.getHealthSnapshot())) {
          driver.refreshHealthSnapshot(health);
          manager.driverUpdated(driver, TopologyEvent.UpdateType.JVM_HEALTH);
        }
      } catch (Exception e) {
        if (debugEnabled) log.debug("error getting health snapshot for driver {} : {}" + driver, ExceptionUtils.getMessage(e));
      }
      if ((driver.getChildCount() <= 0) || (driver.getForwarder() == null)) continue;
      Map<String, TopologyNode> uuidMap = new HashMap<>();
      for (AbstractTopologyComponent comp: driver.getChildren()) {
        if (comp.isPeer()) continue;
        TopologyNode node = (TopologyNode) comp;
        uuidMap.put(node.getUuid(), node);
      }
      Map<String, Object> result = null;
      try {
        result = driver.getForwarder().healthSnapshot(new UuidSelector(new HashSet<>(uuidMap.keySet())));
      } catch(Exception e) {
        if (debugEnabled) log.debug("error getting nodes health for driver {} : {}" + driver, ExceptionUtils.getMessage(e));
      }
      if (result == null) continue;
      for (Map.Entry<String, Object> entry: result.entrySet()) {
        TopologyNode node = uuidMap.get(entry.getKey());
        if (node == null) continue;
        if (entry.getValue() instanceof Exception) {
          node.setStatus(TopologyNodeStatus.DOWN);
          if (debugEnabled) log.debug("exception raised for node " + entry.getKey() + " : " + ExceptionUtils.getMessage((Exception) entry.getValue()));
        } else if (entry.getValue() instanceof HealthSnapshot) {
          HealthSnapshot health = (HealthSnapshot) entry.getValue();
          if (!health.equals(node.getHealthSnapshot())) {
            node.refreshHealthSnapshot((HealthSnapshot) entry.getValue());
            manager.nodeUpdated(driver, node, TopologyEvent.UpdateType.JVM_HEALTH);
            if (log.isTraceEnabled()) log.trace("got new node health snapshot: " + entry.getValue());
          }
        }
      }
    }
  }
}
