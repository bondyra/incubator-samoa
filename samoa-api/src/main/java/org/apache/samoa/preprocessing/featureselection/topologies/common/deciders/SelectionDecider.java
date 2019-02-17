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

import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;

import java.util.Collection;

/**
 *
 */
public interface SelectionDecider extends Decider {
    /**
     * Process selections and possibly share new decision (selection)
     * @param selections List of selections that needs to be analyzed
     * @param ranking Ranking of attribute relevancy that needs to be analyzed
     * @throws IllegalAccessException
     */
    void processSelections(Collection<Selection> selections, InstanceRanking ranking)
            throws IllegalAccessException;
}
