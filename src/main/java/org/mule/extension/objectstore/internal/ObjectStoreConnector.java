/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.internal;

import org.mule.extension.objectstore.internal.error.ObjectStoreErrors;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

/**
 * Connector that provides functionality to access Mule's default object store.
 *
 * In future versions, we will also support creating and managing additional ones.
 *
 * @since 1.0
 */
@Extension(name = "ObjectStore",
    description = "A Mule connector that provides functionality to access, create and manage Object stores")
@Operations(ObjectStoreOperations.class)
@ErrorTypes(ObjectStoreErrors.class)
@Xml(prefix = "os")
public class ObjectStoreConnector {

}
