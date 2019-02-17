package org.apache.samoa.preprocessing.featureselection.topologies.common.splitters;

/*
 * #%L
 * SAMOA
 * %%
 * Copyright (C) 2014 - 2018 Apache Software Foundation
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.javacliparser.Configurable;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.preprocessing.featureselection.topologies.events.FeatureSetContentEvent;

import java.util.Collection;
import java.util.List;

/**
 * Instance splitting algorithm interface. Instances in feature selection systems are split to be able to distribute
 * ranking computations
 */
public interface Splitter extends Configurable {
  /**
   * Split instance into parts
   * 
   * @param instance
   *          Input instance
   * @return List of partial instance information
   */
  List<FeatureSetContentEvent> split(Instance instance);

  /**
   * @param identifiers
   *          Identifiers of rankings declared to be used by feature selection system
   */
  void setRankingIdentifiers(Collection<Integer> identifiers);

  /**
   * @return Incrementing number of processed instance
   */
  long getInstanceNumber();
}
