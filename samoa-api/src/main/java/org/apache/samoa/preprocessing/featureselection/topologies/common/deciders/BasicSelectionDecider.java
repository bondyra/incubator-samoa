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

import com.github.javacliparser.IntOption;
import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;

import java.util.Collection;

/**
 * Decider that takes into account ranking of specific selections (leveraged in non-wrapper feature selection)
 */
public class BasicSelectionDecider implements SelectionDecider {
  public IntOption rankingSumDecisionBoundaryOption = new IntOption("rankingSumDecisionBoundary",
      'd',
      "For how many percent difference of sums for two selections should be bigger to share new selection?",
      5, 0, 100);

  /**
   * Selection that was recently shared
   */
  private Selection bestSelection = null;

  private Stream decisionStream = null;

  public void processSelections(Collection<Selection> selections, InstanceRanking ranking)
      throws IllegalAccessException {
    boolean shouldPutToStream = false;
    if (bestSelection == null) {
      Selection newBestSelection = findBestSelection(selections, ranking);
      if (newBestSelection == null || newBestSelection.getSelectedIndexes().isEmpty())
        return;
      // if there is any selection, share it
      bestSelection = newBestSelection;
      shouldPutToStream = true;
    } else {
      Selection newBestSelection = findBestSelection(selections, ranking);
      // compute sums of ranking attributes relevancy values for old and new selection
      double sumForNewBest = getAbsRankingSumForSelection(newBestSelection, ranking);

      double sumForCurrentBest = getAbsRankingSumForSelection(bestSelection, ranking);
      if (!newBestSelection.getSelectedIndexes().isEmpty() &&
          sumForNewBest > (1d + rankingSumDecisionBoundaryOption.getValue() / 100d) * sumForCurrentBest) {
        // if sum for new selection is substantially better, share new selection
        bestSelection = newBestSelection;
        shouldPutToStream = true;
      }
    }
    if (shouldPutToStream) {
      decisionStream.put(new SelectionContentEvent(bestSelection));
    }
  }

  public Selection getBestSelection() {
    return bestSelection;
  }

  /**
   * @param selections
   *          List of selections
   * @param ranking
   *          Instance ranking to be analyzed
   * @return Best selection according to ranking or null if there are no selections
   * @throws IllegalAccessException
   */
  private Selection findBestSelection(Collection<Selection> selections, InstanceRanking ranking)
      throws IllegalAccessException {
    Selection bestSelection = null;
    double bestSum = Double.MIN_VALUE;
    for (Selection selection : selections) {
      double currentSum = getAbsRankingSumForSelection(selection, ranking);
      if (bestSelection == null || currentSum > bestSum)
        bestSelection = selection;
        bestSum = currentSum;
    }
    return bestSelection;
  }

  /**
   * @param selection
   *          Selection to be analyzed
   * @param ranking
   *          Ranking to be analyzed
   * @return Sum of absolute values of relevancies from ranking for predefined selection
   * @throws IllegalAccessException
   */
  private double getAbsRankingSumForSelection(Selection selection, InstanceRanking ranking)
      throws IllegalAccessException {
    double absSum = 0d;
    for (int index : selection.getSelectedIndexes()) {
      if (ranking.getNominalRankingValues().containsKey(index))
        absSum += Math.abs(ranking.getNominalRankingValues().get(index));
      else if (ranking.getNumericRankingValues().containsKey(index))
        absSum += Math.abs(ranking.getNumericRankingValues().get(index));
      else
        throw new IllegalAccessException("Selected index " + index + "not found in provided ranking");
    }
    return absSum;
  }

  @Override
  public void setDecisionStream(Stream stream) {
    decisionStream = stream;
  }
}
