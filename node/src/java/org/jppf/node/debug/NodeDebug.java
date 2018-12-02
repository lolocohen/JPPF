/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.node.debug;

import java.util.*;

import org.jppf.node.Node;
import org.jppf.scripting.*;
import org.jppf.server.node.JPPFNode;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class NodeDebug implements NodeDebugMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeDebug.class);
  /**
   * The debugged node.
   */
  private final Node node;

  /**
   * @param node the debugged node.
   */
  public NodeDebug(final Node node) {
    this.node = node;
  }

  @Override
  public void log(final String... messages) {
    if (messages != null) {
      for (String message: messages) log.info(message);
    }
  }

  @Override
  public void cancel() {
    ((JPPFNode) node).getExecutionManager().cancelAllTasks(true, false);
  }

  @Override
  public Object executeScript(final String language, final String script) throws JPPFScriptingException {
    final Map<String, Object> bindings = new HashMap<String, Object>() {{ put("node", node); }};
    if (log.isTraceEnabled()) log.trace(String.format("request to execute %s script:%n%s", language, script));
    return new ScriptDefinition(language, script, bindings).evaluate();
  }
}
