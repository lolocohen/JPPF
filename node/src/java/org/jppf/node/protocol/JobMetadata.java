/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.node.protocol;

import java.util.Map;

/**
 * Instances of this interface hold metadata about a job, that can be used from a driver or node extension or plugin.
 * @author Laurent Cohen
 */
public interface JobMetadata
{

	/**
	 * Retrieve a parameter in the metadata.
	 * @param key the parameter's key.
	 * @return the parameter's value or null if no parameter with the specified key exists.
	 */
	Object getParameter(Object key);

	/**
	 * Retrieve a parameter in the metadata.
	 * @param key the parameter's key.
	 * @param def a default value to return if no parameter with the specified key can be found.
	 * @return the parameter's value or null if no parameter with the specified key exists.
	 */
	Object getParameter(Object key, Object def);

	/**
	 * Get a copy of the metadata map.
	 * @return a map of the metadata contained in this object.
	 */
	Map<Object, Object> getAll();

}