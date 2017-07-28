/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore;

import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import org.mule.extension.objectstore.api.ExtensionObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;

import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class ObjectStoreDefinitionTestCase extends AbstractObjectStoreTestCase {

  @Mock
  private ObjectStoreManager objectStoreManager;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        TRANSIENT_STORE_DEFINITION_CONFIG_FILE_NAME,
        PERSISTENT_STORE_DEFINITION_CONFIG_FILE_NAME,
        IMPLICIT_STORE_DEFINITION_CONFIG_FILE_NAME
    };
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    objectStoreManager = mock(ObjectStoreManager.class, RETURNS_DEEP_STUBS);
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getRegistry().registerObject(OBJECT_STORE_MANAGER, objectStoreManager);
      }
    });
  }

  @Test
  public void defineTransientStore() throws Exception {
    assertStoreDefinition("transientStore", false);
  }

  @Test
  public void definePersistentStore() throws Exception {
    assertStoreDefinition("persistentStore", true);
  }

  @Test
  public void defineImplicitStore() throws Exception {
    assertStoreDefinition("implicitStore", false);
  }

  private void assertStoreDefinition(String storeName, boolean persistent) {
    ExtensionObjectStore objectStore = muleContext.getRegistry().lookupObject(storeName);
    assertThat(objectStore, is(notNullValue()));
    ArgumentCaptor<ObjectStoreSettings> settingsCaptor = ArgumentCaptor.forClass(ObjectStoreSettings.class);
    verify(objectStoreManager).createObjectStore(eq(storeName), settingsCaptor.capture());

    ObjectStoreSettings settings = settingsCaptor.getValue();
    assertThat(settings, is(notNullValue()));
    assertThat(settings.isPersistent(), is(persistent));
    assertThat(settings.getEntryTTL().get(), is(HOURS.toMillis(1)));
    assertThat(settings.getExpirationInterval(), is(HOURS.toMillis(2)));
    assertThat(settings.getMaxEntries().get(), is(10));
  }
}
