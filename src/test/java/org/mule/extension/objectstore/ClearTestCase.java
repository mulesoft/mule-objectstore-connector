/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.CLEAR;

import org.junit.Test;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

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
    getObjectStore().store(KEY, TEST_VALUE);
    flowRunner("clear").run();
    assertThat(getObjectStore().contains(KEY), is(false));
  }

  @Test
  @Description("Verifies correct error type when trying to retrieve all from a store which doesn't exists")
  public void clearUnexisting() throws Exception {
    String response = flowRunner("unexistingStore").run().getMessage().getPayload().getValue().toString();
    assertThat(response, equalTo("STORE_NOT_FOUND"));
  }

}
