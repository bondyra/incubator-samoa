package org.apache.samoa.learners.classifiers.ensemble.featureadaptation;

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

/**
 * Simple class that represent ensemble member priority used in reset decision process
 */
public class LearnerPriority implements Comparable<LearnerPriority> {
  private final int learnerId;
  private final Double learnerPriority;

  /**
   * Create LearnerPriority instance
   * 
   * @param learnerId
   *          Identifier of learner
   * @param learnerPriority
   *          Priority of learner
   */
  public LearnerPriority(int learnerId, double learnerPriority) {
    this.learnerId = learnerId;
    this.learnerPriority = learnerPriority;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof LearnerPriority) {
      LearnerPriority otherPriority = (LearnerPriority) other;
      return (otherPriority.getLearnerId() == this.getLearnerId());
    }
    return false;
  }

  @Override
  public int compareTo(LearnerPriority other) {
    return this.getLearnerPriority().compareTo(other.getLearnerPriority());
  }

  public int getLearnerId() {
    return learnerId;
  }

  public Double getLearnerPriority() {
    return learnerPriority;
  }
}
