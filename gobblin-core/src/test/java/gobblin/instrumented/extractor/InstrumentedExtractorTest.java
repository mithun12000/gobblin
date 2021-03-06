/*
 *
 * Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */

package gobblin.instrumented.extractor;

import java.io.IOException;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import gobblin.MetricsHelper;
import gobblin.configuration.ConfigurationKeys;
import gobblin.configuration.WorkUnitState;
import gobblin.Constructs;
import gobblin.metrics.MetricNames;
import gobblin.source.extractor.DataRecordException;
import gobblin.source.extractor.Extractor;


public class InstrumentedExtractorTest {

  public class TestInstrumentedExtractor extends InstrumentedExtractor<String, String> {

    public TestInstrumentedExtractor(WorkUnitState workUnitState) {
      super(workUnitState);
    }

    @Override
    public String readRecordImpl(String reuse)
        throws DataRecordException, IOException {
      return "test";
    }

    @Override
    public String getSchema() {
      return null;
    }

    @Override
    public long getExpectedRecordCount() {
      return 0;
    }

    @Override
    public long getHighWatermark() {
      return 0;
    }
  }

  public class TestExtractor implements Extractor<String, String> {

    @Override
    public String readRecord(String reuse)
        throws DataRecordException, IOException {
      return "test";
    }

    @Override
    public String getSchema() {
      return null;
    }

    @Override
    public long getExpectedRecordCount() {
      return 0;
    }

    @Override
    public long getHighWatermark() {
      return 0;
    }

    @Override
    public void close()
        throws IOException {

    }
  }

  @Test
  public void test() throws DataRecordException, IOException {
    WorkUnitState state = new WorkUnitState();
    state.setProp(ConfigurationKeys.METRICS_ENABLED_KEY, Boolean.toString(true));
    TestInstrumentedExtractor extractor = new TestInstrumentedExtractor(state);
    testBase(extractor);
  }

  @Test
  public void testDecorated() throws DataRecordException, IOException {
    WorkUnitState state = new WorkUnitState();
    state.setProp(ConfigurationKeys.METRICS_ENABLED_KEY, Boolean.toString(true));
    InstrumentedExtractorBase instrumentedExtractor = new InstrumentedExtractorDecorator(state,
        new TestInstrumentedExtractor(state)
    );
    testBase(instrumentedExtractor);

    InstrumentedExtractorBase nonInstrumentedExtractor = new InstrumentedExtractorDecorator(state,
        new TestExtractor());
    testBase(nonInstrumentedExtractor);
  }

  public void testBase(InstrumentedExtractorBase<String, String> extractor)
      throws DataRecordException, IOException {
    extractor.readRecord("");

    Map<String, Long> metrics = MetricsHelper.dumpMetrics(extractor.getMetricContext());
    Assert.assertEquals(metrics.get(MetricNames.ExtractorMetrics.RECORDS_READ_METER), Long.valueOf(1));
    Assert.assertEquals(metrics.get(MetricNames.ExtractorMetrics.RECORDS_FAILED_METER), Long.valueOf(0));
    Assert.assertEquals(metrics.get(MetricNames.ExtractorMetrics.EXTRACT_TIMER), Long.valueOf(1));

    Assert.assertEquals(MetricsHelper.dumpTags(extractor.getMetricContext()).get("construct"),
        Constructs.EXTRACTOR.toString());
  }

}
