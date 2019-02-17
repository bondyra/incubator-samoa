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

import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.Evaluation;

import java.util.Collection;

/**
 * Decider that takes into account evaluation results of specific selections (leveraged in wrapper feature selection)
 */
public interface EvaluationDecider extends Decider {
    /**
     * Process evaluations and possibly share new decision (selection)
     * @param evaluations List of evaluations that needs to be analyzed
     * @throws IllegalStateException
     */
    void processEvaluationResults(Collection<Evaluation> evaluations) throws IllegalStateException;
}
