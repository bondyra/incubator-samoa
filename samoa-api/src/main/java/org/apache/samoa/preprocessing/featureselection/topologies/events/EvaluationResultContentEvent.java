package org.apache.samoa.preprocessing.featureselection.topologies.events;

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

import org.apache.samoa.core.ContentEvent;

/**
 * Evaluation result (leveraged by wrapper feature selection)
 */
public class EvaluationResultContentEvent implements ContentEvent {
  private static final long serialVersionUID = 0L;
  private final transient String key;
  private final int evaluationId;
  private final double evaluationResult;
  private final double evaluationUpperBound;

  /**
   * Create EvaluationResultContentEvent instance
   * 
   * @param evaluationId
   *          Evaluation identifier
   * @param evaluationResult
   *          Result of evaluation
   * @param evaluationUpperBound
   *          Upper bound of evaluation - used for result normalization purposes which could be handy
   */
  public EvaluationResultContentEvent(int evaluationId, double evaluationResult, double evaluationUpperBound) {
    this.evaluationId = evaluationId;
    this.evaluationResult = evaluationResult;
    this.evaluationUpperBound = evaluationUpperBound;
    this.key = "";
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public void setKey(String key) {

  }

  @Override
  public boolean isLastEvent() {
    return false;
  }

  public int getEvaluationId() {
    return evaluationId;
  }

  public double getEvaluationResult() {
    return evaluationResult;
  }

  public double getEvaluationUpperBound() {
    return evaluationUpperBound;
  }
}
