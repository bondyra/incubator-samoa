package org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating;

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

import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;

/**
 * Evaluation model
 */
public class Evaluation {
  /**
   * Identifier of evaluation
   */
  private int evaluationId;
  private Selection selection;
  /**
   * Upper bound of evaluation result - for normalization purpose
   */
  private double evaluationUpperBound;
  /**
   * Result of evaluation - dynamically changed
   */
  private Double evaluationResult = null;

  /**
   * Create Evaluation instance
   * @param selection Selection that evaluation refers to
   */
  public Evaluation(Selection selection) {
    this.selection = selection;
  }

  public double getEvaluationResult() throws IllegalStateException {
    if (evaluationResult == null)
      throw new IllegalStateException();
    return evaluationResult;
  }

  public void setEvaluationResult(double evaluationResult) {
    this.evaluationResult = evaluationResult;
  }

  public Selection getSelection() {
    return this.selection;
  }

  public int getEvaluationId() {
    return evaluationId;
  }

  public void setEvaluationId(int evaluationId) {
    this.evaluationId = evaluationId;
  }

  /**
   * @return True if evaluation result has been set
   */
  public boolean isPrepared() {
    return evaluationResult != null;
  }

  public double getNormalizedEvaluationResult() throws IllegalStateException {
    if (evaluationResult == null)
      throw new IllegalStateException();
    return evaluationResult / this.evaluationUpperBound;
  }

  public void setEvaluationUpperBound(double evaluationUpperBound) {
    this.evaluationUpperBound = evaluationUpperBound;
  }
}
