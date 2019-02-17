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
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.Evaluation;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;

/**
 * Evaluation request (leveraged by wrapper feature selection)
 */
public class EvaluationContentEvent implements ContentEvent {
  private static final long serialVersionUID = 0L;
  private final transient String key;
  /**
   * Evaluation identifier
   */
  private final int evaluationId;
  /**
   * Selection to be evaluated
   */
  private final Selection selection;

  /**
   * Create EvaluationContentEvent instance
   * 
   * @param evaluation
   *          Evaluation
   */
  public EvaluationContentEvent(Evaluation evaluation) {
    this.selection = evaluation.getSelection();
    this.evaluationId = evaluation.getEvaluationId();
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

  public Selection getSelection() {
    return selection;
  }

  public int getEvaluationId() {
    return evaluationId;
  }
}
