/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.Serializable;
import java.util.Collection;

import io.qameta.allure.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public abstract class ParameterizedObjectStoreTestCase extends AbstractObjectStoreTestCase {

  public static final String TRANSIENT_STORE_NAME = "transientStore";
  public static final String PERSISTENT_STORE_NAME = "persistentStore";
  public static final String IMPLICIT_STORE_NAME = "implicitStore";
  private final String definitionConfigFileName;
  protected final String objectStoreName;

  @Rule
  public SystemProperty objectStoreNameSystemProperty;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {PERSISTENT_STORE_NAME},
        {TRANSIENT_STORE_NAME},
        {IMPLICIT_STORE_NAME}});
  }

  public ParameterizedObjectStoreTestCase(String name) {
    objectStoreName = name;
    if (PERSISTENT_STORE_NAME.equals(name)) {
      definitionConfigFileName = PERSISTENT_STORE_DEFINITION_CONFIG_FILE_NAME;
    } else if (TRANSIENT_STORE_NAME.equals(name)) {
      definitionConfigFileName = TRANSIENT_STORE_DEFINITION_CONFIG_FILE_NAME;
    } else if (IMPLICIT_STORE_NAME.equals(name)) {
      definitionConfigFileName = IMPLICIT_STORE_DEFINITION_CONFIG_FILE_NAME;
    } else {
      throw new IllegalArgumentException("Unknown type " + name);
    }

    objectStoreNameSystemProperty = new SystemProperty("objectStore", objectStoreName);
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {definitionConfigFileName, doGetConfigFile()};
  }

  @Override
  protected final String getConfigFile() {
    return null;
  }

  protected abstract String doGetConfigFile();

  protected void retrieveAndCompare(String key, Serializable value) throws Exception {
    TypedValue<Serializable> typedValue = (TypedValue<Serializable>) getObjectStore().retrieve(key);
    assertThat(typedValue.getValue(), equalTo(value));
  }

  protected ObjectStore<Serializable> getObjectStore() {
    return objectStoreManager.getObjectStore(objectStoreName);
  }

  @Test
  @Description("Verify that pointing to an undefined ObjectStore results in a STORE_NOT_FOUND error")
  public void undefinedStore() throws Exception {
    BaseEvent event = flowRunner("unexistingStore")
        .withPayload("")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), equalTo("STORE_NOT_FOUND"));
  }

}
