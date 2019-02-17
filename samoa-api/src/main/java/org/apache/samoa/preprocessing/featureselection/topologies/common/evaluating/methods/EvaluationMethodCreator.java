package org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods;

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
 * Simple creator of evaluation methods
 */
public class EvaluationMethodCreator {
  private final String methodName;

  public EvaluationMethodCreator() {
    this.methodName = "AccuracyEvaluationMethod"; // default constructor for test purposes
  }

  public EvaluationMethodCreator(String methodName) {
    this.methodName = methodName;
  }

  public EvaluationMethod getMethod() throws InstantiationException {
    if (methodName.equals("AccuracyEvaluationMethod")) {
      return new AccuracyEvaluationMethod();
    }
    throw new InstantiationException("Method do not exist.");
  }
}
