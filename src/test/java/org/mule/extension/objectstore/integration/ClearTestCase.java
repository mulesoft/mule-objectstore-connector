/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.CLEAR;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(OS_CONNECTOR)
@Story(CLEAR)
public class ClearTestCase extends ParameterizedObjectStoreTestCase {

  public ClearTestCase(String name) {
    super(name);
  }

  @Override
  protected String doGetConfigFile() {
    return "clear-config.xml";
  }

  @Test
  @Description("Clears the entire store")
  public void clear() throws Exception {
    getObjectStore().store(AbstractObjectStoreTestCase.KEY, AbstractObjectStoreTestCase.TEST_VALUE);
    flowRunner("clear").run();
    assertThat(getObjectStore().contains(AbstractObjectStoreTestCase.KEY), is(false));
  }
}
