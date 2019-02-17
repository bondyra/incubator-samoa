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

import com.github.javacliparser.FloatOption;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.Evaluation;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;

import java.util.Collection;

/**
 * Decider that takes into account evaluation results of specific selections (leveraged in wrapper feature selection)
 */
public class BasicEvaluationDecider implements EvaluationDecider {
  public FloatOption evaluationDecisionBoundaryOption = new FloatOption("evaluationDecisionBoundary",
      'e',
      "What is the minimal evaluation result difference for two evaluations to share new selection?",
      0.05d, 0d, 1d);

  /**
   * Evaluation of selection that was recently shared
   */
  private Evaluation currentEvaluation = null;

  private Stream decisionStream;

  @Override
  public void processEvaluationResults(Collection<Evaluation> evaluations) throws IllegalStateException {
    boolean shouldPutToStream = false;
    if (currentEvaluation == null) {
      Evaluation newBestEvaluation = findBestEvaluation(evaluations);
      if (newBestEvaluation == null || newBestEvaluation.getSelection().getSelectedIndexes().isEmpty())
        return;
        // share best selection if no selection was addressed before
      currentEvaluation = newBestEvaluation;
      shouldPutToStream = true;
    } else {
      // update evaluation for current selection based on fresh results
      currentEvaluation = findEvaluationForCurrentSelection(evaluations);

      Evaluation bestEvaluation = findBestEvaluation(evaluations);
      if (!bestEvaluation.getSelection().equalTo(currentEvaluation.getSelection()) &&
          !bestEvaluation.getSelection().getSelectedIndexes().isEmpty() &&
          bestEvaluation.getNormalizedEvaluationResult() > currentEvaluation.getNormalizedEvaluationResult()
              + evaluationDecisionBoundaryOption.getValue()) {
        // new evaluation has different selection than previous one and is substantially better - so share it
        currentEvaluation = bestEvaluation;
        shouldPutToStream = true;
      }
    }
    if (shouldPutToStream) {
      decisionStream.put(new SelectionContentEvent(currentEvaluation.getSelection()));
    }
  }

  @Override
  public void setDecisionStream(Stream stream) {
    this.decisionStream = stream;
  }

  @Override
  public Selection getBestSelection() {
    if (currentEvaluation == null)
      return null;
    return currentEvaluation.getSelection();
  }

    /**
     * @param evaluations Collection of evaluations
     * @return Best evaluation according to normalized result
     */
  private Evaluation findBestEvaluation(Collection<Evaluation> evaluations) {
    Evaluation bestEvaluation = null;
    for (Evaluation evaluation : evaluations) {
      if (bestEvaluation == null ||
          evaluation.getNormalizedEvaluationResult() > bestEvaluation.getNormalizedEvaluationResult()) {
        bestEvaluation = evaluation;
      }
    }
    return bestEvaluation;
  }

    /**
     * @param evaluations Collection of evaluations
     * @return Evaluation that corresponds to currently shared selection
     * @throws IllegalStateException
     */
  private Evaluation findEvaluationForCurrentSelection(Collection<Evaluation> evaluations)
      throws IllegalStateException {
    if (currentEvaluation == null)
      throw new IllegalStateException("No current best evaluation in deciding process");
    for (Evaluation evaluation : evaluations) {
      if (evaluation.getSelection().equalTo(currentEvaluation.getSelection())) {
        return evaluation;
      }
    }
    return null;
  }
}
