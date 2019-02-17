package org.apache.samoa;

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

import org.apache.samoa.LocalDoTask;
import org.junit.Test;
import org.apache.samoa.TestParams;

public class AlgosTest {

  @Test
  public void testVHTLocal() throws Exception {
    TestParams vhtConfig = new TestParams.Builder()
        .inputInstances(200_000)
        .samplingSize(20_000)
        .evaluationInstances(200_000)
        .classifiedInstances(200_000)
        .labelSamplingSize(10l)
        .classificationsCorrect(75f)
        .kappaStat(0f)
        .kappaTempStat(0f)
        .cliStringTemplate(TestParams.Templates.PREQEVAL_VHT_RANDOMTREE)
        .resultFilePollTimeout(10)
        .prePollWait(10)
        .taskClassName(LocalDoTask.class.getName())
        .build();
    TestUtils.test(vhtConfig);
  }

  @Test
  public void testBaggingLocal() throws Exception {
    TestParams baggingConfig = new TestParams.Builder()
        .inputInstances(200_000)
        .samplingSize(20_000)
        .evaluationInstances(200_000)
        .classifiedInstances(200_000)
        .labelSamplingSize(1l)
        .classificationsCorrect(60f)
        .kappaStat(0f)
        .kappaTempStat(0f)
        .cliStringTemplate(TestParams.Templates.PREQEVAL_BAGGING_RANDOMTREE)
        .prePollWait(10)
        .resultFilePollTimeout(10)
        .taskClassName(LocalDoTask.class.getName())
        .build();
    TestUtils.test(baggingConfig);
  }

  @Test
  public void testNaiveBayesLocal() throws Exception {
    TestParams vhtConfig = new TestParams.Builder()
        .inputInstances(200_000)
        .samplingSize(20_000)
        .evaluationInstances(200_000)
        .classifiedInstances(200_000)
        .labelSamplingSize(10l)
        .classificationsCorrect(65f)
        .kappaStat(0f)
        .kappaTempStat(0f)
        .cliStringTemplate(TestParams.Templates.PREQEVAL_NAIVEBAYES_HYPERPLANE)
        .resultFilePollTimeout(10)
        .prePollWait(10)
        .taskClassName(LocalDoTask.class.getName())
        .build();
    TestUtils.test(vhtConfig);
  }

  @Test
  public void testCVPreqVHTLocal() throws Exception {
    TestParams vhtConfig = new TestParams.Builder()
            .inputInstances(200_000)
            .samplingSize(20_000)
            .evaluationInstances(200_000)
            .classifiedInstances(200_000)
            .classificationsCorrect(75f)
            .kappaStat(0f)
            .kappaTempStat(0f)
            .cliStringTemplate(TestParams.Templates.PREQCVEVAL_VHT_RANDOMTREE)
            .resultFilePollTimeout(10)
            .prePollWait(10)
            .taskClassName(LocalDoTask.class.getName())
            .labelFileCreated(false)
            .build();
    TestUtils.test(vhtConfig);
  }

  @Test
  public void testPreqBFSLocal() throws Exception {
    TestParams bfsConfig = new TestParams.Builder()
            .inputInstances(200_000)
            .samplingSize(20_000)
            .evaluationInstances(200_000)
            .classifiedInstances(200_000)
            .labelSamplingSize(10l)
            .classificationsCorrect(54f)
            .kappaStat(-1f)
            .kappaTempStat(-1f)
            .cliStringTemplate(TestParams.Templates.PREQEVAL_BFS_HYPERPLANE)
            .resultFilePollTimeout(10)
            .prePollWait(10)
            .taskClassName(LocalDoTask.class.getName())
            .build();
    TestUtils.test(bfsConfig);
  }

    @Test
    public void testPreqBWFSLocal() throws Exception {
        TestParams bwfsConfig = new TestParams.Builder()
                .inputInstances(200_000)
                .samplingSize(20_000)
                .evaluationInstances(200_000)
                .classifiedInstances(200_000)
                .labelSamplingSize(10l)
                .classificationsCorrect(54f)
                .kappaStat(-1f)
                .kappaTempStat(-1f)
                .cliStringTemplate(TestParams.Templates.PREQEVAL_BWFS_HYPERPLANE)
                .resultFilePollTimeout(10)
                .prePollWait(10)
                .taskClassName(LocalDoTask.class.getName())
                .build();
        TestUtils.test(bwfsConfig);
    }

    @Test
    public void testPreqIFSLocal() throws Exception {
        TestParams ifsConfig = new TestParams.Builder()
                .inputInstances(200_000)
                .samplingSize(20_000)
                .evaluationInstances(200_000)
                .classifiedInstances(200_000)
                .labelSamplingSize(10l)
                .classificationsCorrect(50f)
                .kappaStat(-1f)
                .kappaTempStat(-1f)
                .cliStringTemplate(TestParams.Templates.PREQEVAL_IFS_HYPERPLANE)
                .resultFilePollTimeout(10)
                .prePollWait(10)
                .taskClassName(LocalDoTask.class.getName())
                .build();
        TestUtils.test(ifsConfig);
    }

    @Test
    public void testPreqIWFSLocal() throws Exception {
        TestParams iwfsConfig = new TestParams.Builder()
                .inputInstances(200_000)
                .samplingSize(20_000)
                .evaluationInstances(200_000)
                .classifiedInstances(200_000)
                .labelSamplingSize(10l)
                .classificationsCorrect(50f)
                .kappaStat(-1f)
                .kappaTempStat(-1f)
                .cliStringTemplate(TestParams.Templates.PREQEVAL_IWFS_HYPERPLANE)
                .resultFilePollTimeout(10)
                .prePollWait(10)
                .taskClassName(LocalDoTask.class.getName())
                .build();
        TestUtils.test(iwfsConfig);
    }
}
