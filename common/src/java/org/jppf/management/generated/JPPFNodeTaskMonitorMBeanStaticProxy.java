/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.management.generated;

import javax.management.MBeanNotificationInfo;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.JPPFNodeTaskMonitorMBean;

/**
 * Generated static proxy for the {@link org.jppf.management.JPPFNodeTaskMonitorMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
public class JPPFNodeTaskMonitorMBeanStaticProxy extends AbstractMBeanStaticProxy implements JPPFNodeTaskMonitorMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public JPPFNodeTaskMonitorMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=task.monitor,type=node");
  }

  @Override
  public void reset() {
    invoke("reset", (Object[]) null, (String[]) null);
  }

  @Override
  public Integer getTotalTasksExecuted() {
    return (Integer) getAttribute("TotalTasksExecuted");
  }

  @Override
  public Integer getTotalTasksInError() {
    return (Integer) getAttribute("TotalTasksInError");
  }

  @Override
  public Integer getTotalTasksSucessfull() {
    return (Integer) getAttribute("TotalTasksSucessfull");
  }

  @Override
  public Long getTotalTaskCpuTime() {
    return (Long) getAttribute("TotalTaskCpuTime");
  }

  @Override
  public Long getTotalTaskElapsedTime() {
    return (Long) getAttribute("TotalTaskElapsedTime");
  }

  @Override
  public void removeNotificationListener(final NotificationListener param0, final NotificationFilter param1, final Object param2) {
    invoke("removeNotificationListener", new Object[] { param0, param1, param2 }, new String[] { "javax.management.NotificationListener", "javax.management.NotificationFilter", "java.lang.Object" });
  }

  @Override
  public void removeNotificationListener(final NotificationListener param0) {
    invoke("removeNotificationListener", new Object[] { param0 }, new String[] { "javax.management.NotificationListener" });
  }

  @Override
  public void addNotificationListener(final NotificationListener param0, final NotificationFilter param1, final Object param2) {
    invoke("addNotificationListener", new Object[] { param0, param1, param2 }, new String[] { "javax.management.NotificationListener", "javax.management.NotificationFilter", "java.lang.Object" });
  }

  @Override
  public MBeanNotificationInfo[] getNotificationInfo() {
    return (MBeanNotificationInfo[]) getAttribute("NotificationInfo");
  }
}
