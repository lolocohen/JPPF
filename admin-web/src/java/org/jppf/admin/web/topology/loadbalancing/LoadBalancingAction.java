/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.admin.web.topology.loadbalancing;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.topology.TopologyTreeData;
import org.jppf.admin.web.utils.AbstractManagerRoleAction;

/**
 * 
 * @author Laurent Cohen
 */
public class LoadBalancingAction extends AbstractManagerRoleAction {
  @Override
  public void setEnabled(final List<DefaultMutableTreeNode> selected) {
    // enabled only if a single node is selected, regardless how many drivers are selected.
    enabled = (TopologyTreeData.getSelectedNodes(selected).size() == 1);
  }
}