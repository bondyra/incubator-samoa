/*
 * Copyright 2018 The Apache Software Foundation.
 *
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
 */
package org.apache.samoa.preprocessing.featureselection.ranking;

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

import org.apache.samoa.instances.*;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;

import java.io.Serializable;
import java.util.*;

/**
 * Creates filtered instances basing on known selection (single-label instances only)
 */
public class FeatureFilter implements Serializable {
  private static final long serialVersionUID = 1L;
  private Selection selection = null;
  protected InstancesHeader dataset = null;
  private boolean initialized = false;

  /**
   * @param newSelection
   *          New selection that will be a base of filter
   */
  public void setSelection(Selection newSelection) {
    if (selection == null || !selection.equalTo(newSelection)) {
      selection = newSelection;
      reset();
    }
  }

  /**
   * Reset the filter state
   */
  private void reset() {
    dataset = null;
    initialized = false;
  }

  /**
   * Initialize filter by reading instance data
   * 
   * @param instance
   *          Instance that contains data required to configure filter
   */
  private void initialize(Instance instance) {
    Instances ds = new Instances();
    Collection<Integer> selectedIndices = selection.getSelectedIndexes();
    int totAttributes = selection.getDimensionality() + 1; // + one for class
    // read attributes and their vaues
    List<Attribute> attributes = new ArrayList<>(totAttributes);
    List<Integer> indexValues = new ArrayList<>(totAttributes);
    int ct = 0;
    for (int index : selectedIndices) {
      attributes.add(instance.attribute(index));
      indexValues.add(ct);
      ct++;
    }
    // add class information
    attributes.add(instance.classAttribute());
    indexValues.add(ct);

    ds.setAttributes(attributes, indexValues);
    ds.setClassIndex(ct);
    dataset = (new InstancesHeader(ds));
    initialized = true;
  }

  /**
   * @param instance
   *          Input instance
   * @return Instance filtered to selection
   * @throws IllegalStateException
   *           If filter does not have selection set up
   */
  public Instance processInstance(Instance instance) throws IllegalStateException {
    if (selection == null) {
      throw new IllegalStateException("No selection set up.");
    }
    if (!initialized) {
      initialize(instance);
    }

    double[] attValues = new double[dataset.numAttributes()];
    Instance newInstance = new InstanceImpl(instance.weight(), attValues);
    int count = 0;
    for (int index : selection.getSelectedIndexes()) {
      newInstance.setValue(count, instance.value(index));
      count++;
    }

    newInstance.setDataset(dataset);
    newInstance.setClassValue(instance.classValue());
    return newInstance;
  }

  /**
   * @return Instance dataset for current filter state
   */
  public Instances getDataset() {
    return this.dataset;
  }
}