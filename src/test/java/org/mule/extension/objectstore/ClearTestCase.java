/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.CLEAR;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(OS_CONNECTOR)
@Stories(CLEAR)
public class ClearTestCase extends AbstractObjectStoreTestCase {

  @Override
  protected String getConfigFile() {
    return "clear-config.xml";
  }

  @Test
  @Description("Clears the entire store")
  public void clear() throws Exception {
    objectStore.store(KEY, TEST_VALUE);
    flowRunner("clear").run();
    assertThat(objectStore.contains(KEY), is(false));
  }

}
