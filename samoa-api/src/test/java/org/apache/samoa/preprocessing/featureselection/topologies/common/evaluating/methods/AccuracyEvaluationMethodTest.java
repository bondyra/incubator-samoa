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

import mockit.*;
import org.apache.samoa.instances.Instance;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AccuracyEvaluationMethodTest {
    @Tested
    private AccuracyEvaluationMethod method;

    @Before
    public void setUp(){
        method = new AccuracyEvaluationMethod();
    }

    @Test
    public void testUpdateSingle(@Mocked final Instance instance){
        new StrictExpectations(){
            {
                instance.classValue();
                result = 2;
            }
        };

        method.update(new double[] {0d, 0d, 1d}, instance);

        new Verifications(){
            {
                assertEquals(method.getScore(), 1d, Double.MIN_VALUE);
            }
        };
    }

    @Test
    public void testUpdateMultiple(@Mocked final Instance instance){
        new StrictExpectations(){
            {
                instance.classValue();
                returns(2d, 1d);
            }
        };

        method.update(new double[] {0d, 0d, 1d}, instance);
        method.update(new double[] {0d, 0d, 1d}, instance);

        new Verifications(){
            {
                assertEquals(method.getScore(), 0.5d, Double.MIN_VALUE);
            }
        };
    }

    @Test
    public void testReset(@Mocked final Instance instance){

        new Expectations(){
            {
                instance.classValue();
                result = 1d;
            }
        };

        method.update(new double[] {0d, 1d, 0d}, instance);
        method.update(new double[] {0d, 0d, 1d}, instance);
        method.reset();
        method.update(new double[] {0d, 1d, 0d}, instance);

        new Verifications(){
            {
                instance.classValue();
                times = 3;

                assertEquals(method.getScore(), 1d, Double.MIN_VALUE);
            }
        };
    }
}
