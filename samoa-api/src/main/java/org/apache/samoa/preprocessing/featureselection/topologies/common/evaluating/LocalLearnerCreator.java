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

import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.learners.classifiers.NaiveBayes;
import org.apache.samoa.learners.classifiers.SimpleClassifierAdapter;

/**
 * LocalLearner creator
 */
public class LocalLearnerCreator {
  private final String learnerName;

  /**
   * Create default LocalLearnerCreator instance
   */
  public LocalLearnerCreator() {
    this.learnerName = "SimpleClassifierAdapter"; // default constructor for mocking
  }

  /**
   * Create LocalLearnerCreator instance
   * 
   * @param learnerName
   *          Name of the learner to be created
   */
  public LocalLearnerCreator(String learnerName) {
    this.learnerName = learnerName;
  }

  /**
   * Create LocalLearner instance
   * 
   * @return Instantiated class
   * @throws InstantiationException
   *           If learner with configured name does not exist
   */
  public LocalLearner getLearner() throws InstantiationException {
    if (learnerName.equals("SimpleClassifierAdapter")) {
      return new SimpleClassifierAdapter();
    } else if (learnerName.equals("NaiveBayes")) {
      return new NaiveBayes();
    }
    throw new InstantiationException("Local learner do not exist.");
  }
}
