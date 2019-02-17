package org.apache.samoa.learners.classifiers.ensemble.featureadaptation;

/*
 * #%L
 * SAMOA
 * %%
 * Copyright (C) 2014 - 2015 Apache Software Foundation
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
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;

import java.util.Collection;

/**
 * Information about feature selection and ensemble members identifiers to which it should be applied
 */
final public class SelectFeaturesContentEvent implements ContentEvent {
  private static final long serialVersionUID = 0L;
  private final transient String key;
  private Collection<Integer> ensembleStreamIds;
  private Selection selection;

  /**
   * Create SelectFeaturesContentEvent instance
   * 
   * @param ensembleStreamIds
   *          Ensemble members identifiers
   * @param selection
   *          Selection that should be applied
   */
  public SelectFeaturesContentEvent(Collection<Integer> ensembleStreamIds, Selection selection) {
    this.ensembleStreamIds = ensembleStreamIds;
    this.selection = selection;
    this.key = "";
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public void setKey(String str) {

  }

  @Override
  public boolean isLastEvent() {
    return false;
  }

  public Collection<Integer> getEnsembleStreamIds() {
    return ensembleStreamIds;
  }

  public Selection getSelection() {
    return selection;
  }
}
