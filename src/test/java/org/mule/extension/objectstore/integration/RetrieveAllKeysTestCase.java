/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.RETRIEVE_ALL_KEYS;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(OS_CONNECTOR)
@Story(RETRIEVE_ALL_KEYS)
public class RetrieveAllKeysTestCase extends ParameterizedObjectStoreTestCase {

  public RetrieveAllKeysTestCase(String name) {
    super(name);
  }

  @Override
  protected String doGetConfigFile() {
    return "retrieve-all-keys-config.xml";
  }

  @Test
  @Description("Retrieves all the keys in the store")
  public void retrieveAllKeys() throws Exception {
    Map<String, Serializable> values = new LinkedHashMap<>();
    values.put(KEY, TEST_VALUE);
    values.put(KEY + "a", TEST_VALUE + "a");

    for (Map.Entry<String, Serializable> value : values.entrySet()) {
      getObjectStore().store(value.getKey(), value.getValue());
    }

    List<String> retrieved = (List<String>) flowRunner("retrieveAllKeys").run().getMessage().getPayload().getValue();
    assertThat(retrieved, hasSize(values.size()));
    retrieved.forEach(key -> assertThat(values.containsKey(key), is(true)));
  }
}
