/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.OS_CONNECTOR;
import static org.mule.extension.objectstore.AllureConstants.ObjectStoreFeature.ObjectStoreStory.IMPLICIT_STORE;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.InternalEvent;

import java.io.Serializable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(OS_CONNECTOR)
@Story(IMPLICIT_STORE)
public class DefaultPartitionTestCase extends AbstractObjectStoreTestCase {

  private static final String VALUE = "value";

  private ObjectStore<Serializable> defaultPartition;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    defaultPartition = objectStoreManager.getDefaultPartition();
  }

  @Override
  protected String getConfigFile() {
    return "default-partition-config.xml";
  }

  @Test
  @Description("Store in implicit default partition")
  public void store() throws Exception {
    test("store");
    TypedValue<Serializable> typedValue = (TypedValue<Serializable>) defaultPartition.retrieve(KEY);
    assertThat(typedValue.getValue(), equalTo(VALUE));
  }

  @Test
  @Description("Retrieve from implicit default partition")
  public void retrieve() throws Exception {
    storeTestValue();
    InternalEvent event = test("retrieve");
    assertThat(event.getMessage().getPayload().getValue(), equalTo(VALUE));
  }

  @Test
  @Description("Execute contains operation on implicit default partition")
  public void contains() throws Exception {
    storeTestValue();
    InternalEvent event = test("contains");
    assertThat(event.getMessage().getPayload().getValue(), equalTo(true));
  }

  @Test
  @Description("Remove from implicit default partition")
  public void remove() throws Exception {
    storeTestValue();
    test("remove");
    assertThat(defaultPartition.contains(KEY), is(false));
  }

  @Test
  @Description("Clear implicit default partition")
  public void clear() throws Exception {
    storeTestValue();
    test("clear");
    assertThat(defaultPartition.allKeys(), hasSize(0));
  }

  private void storeTestValue() throws org.mule.runtime.api.store.ObjectStoreException {
    defaultPartition.store(KEY, VALUE);
  }

  private InternalEvent test(String flowName) throws Exception {
    return flowRunner(flowName).withVariable("key", KEY).withPayload(VALUE).run();
  }
}
