package org.apache.samoa.preprocessing.featureselection.topologies.common.deciders;

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
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.topology.Stream;

/**
 * Common interface of selection sharing deciders
 */
public interface Decider extends Configurable {
    /**
     * @param stream Stream into which decider should put selections
     */
    void setDecisionStream(Stream stream);
    /**
     * @return Selection that was recently shared by decider
     */
    Selection getBestSelection();
}
