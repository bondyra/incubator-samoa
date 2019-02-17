package org.apache.samoa.preprocessing.featureselection.ranking.models;

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

import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Product of feature selection
 */
public class Selection implements Serializable {
  private Collection<Integer> selectedIndexes;
  /**
   * Dimensionality of selection
   */
  private int dimensionality;

  /**
   * Create Selection instance
   * 
   * @param selectedIndexes
   *          Collection of selected indexes
   */
  public Selection(Collection<Integer> selectedIndexes) {
    this.selectedIndexes = new HashSet<>(selectedIndexes); // to keep it unique
    this.dimensionality = this.selectedIndexes.size();
  }

  /**
   * @return Collection of selected indexes
   */
  public Collection<Integer> getSelectedIndexes() {
    return selectedIndexes;
  }

  /**
   * @param second
   *          Second selection
   * @return True if selections contain exact same indexes
   */
  public boolean equalTo(Selection second) {
    if (second == null)
      return false;
    return CollectionUtils.isEqualCollection(selectedIndexes, second.getSelectedIndexes());
  }

  /**
   * @return Dimensionality of selected feature set - number of unique indexes
   */
  public int getDimensionality() {
    return dimensionality;
  }
}
