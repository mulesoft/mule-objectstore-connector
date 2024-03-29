/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import org.mule.extension.objectstore.api.PrivateObjectStore;
import org.mule.extension.objectstore.api.TopLevelObjectStore;
import org.mule.extension.objectstore.internal.error.ObjectStoreErrors;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.meta.JavaVersion;

/**
 * Connector that provides functionality to access and create {@link ObjectStore} instances.
 *
 * @since 1.0
 */
@Extension(name = "ObjectStore")
@Operations(ObjectStoreOperations.class)
@ConnectionProviders(MuleObjectStoreManagerProvider.class)
@ErrorTypes(ObjectStoreErrors.class)
@SubTypeMapping(baseType = ObjectStore.class, subTypes = {TopLevelObjectStore.class, PrivateObjectStore.class})
@Xml(prefix = "os")
@JavaVersionSupport({JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17})
public class ObjectStoreConnector {

  @RefName
  private String name;

  public String getConfigName() {
    return name;
  }
}
