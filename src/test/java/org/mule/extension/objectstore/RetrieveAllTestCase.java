/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.RETRIEVE_ALL;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(OS_CONNECTOR)
@Story(RETRIEVE_ALL)
public class RetrieveAllTestCase extends ParameterizedObjectStoreTestCase {

  public RetrieveAllTestCase(String name) {
    super(name);
  }

  @Override
  protected String doGetConfigFile() {
    return "retrieve-all-config.xml";
  }

  @Test
  @Description("Retrieves the entire store when all values are not stored as typed values")
  public void retrieveAllNonTypedValues() throws Exception {
    Map<String, Serializable> values = new HashMap<>();
    values.put(KEY, TEST_VALUE);
    values.put(KEY + "a", TEST_VALUE + "a");

    for (Map.Entry<String, Serializable> value : values.entrySet()) {
      getObjectStore().store(value.getKey(), value.getValue());
    }

    Map<String, Serializable> retrieved =
        (Map<String, Serializable>) flowRunner("retrieveAll").run().getMessage().getPayload().getValue();
    assertThat(retrieved, equalTo(values));
  }

  @Test
  @Description("Retrieves the entire store when all values are not stored as typed values")
  public void retrieveAllTypedValues() throws Exception {
    Map<String, Serializable> values = new HashMap<>();
    values.put(KEY, TEST_VALUE);
    values.put(KEY + "a", TEST_VALUE + "a");

    for (Map.Entry<String, Serializable> value : values.entrySet()) {
      getObjectStore().store(value.getKey(), new TypedValue<>(value.getValue(), DataType.fromObject(value.getValue())));
    }

    Map<String, Serializable> retrieved =
        (Map<String, Serializable>) flowRunner("retrieveAll").run().getMessage().getPayload().getValue();
    assertThat(retrieved, equalTo(values));
  }

  @Test
  @Description("Verifies correct error type when trying to retrieve all from a store which doesn't exists")
  public void retrieveAllFromUnexisting() throws Exception {
    String response = flowRunner("unexistingStore").run().getMessage().getPayload().getValue().toString();
    assertThat(response, equalTo("STORE_NOT_FOUND"));
  }
}
