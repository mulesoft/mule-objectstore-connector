/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.api;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.RefName;

/**
 * A global object store which any component can reference by name and use.
 *
 * @since 1.0
 */
@Alias("objectStore")
@TypeDsl(allowTopLevelDefinition = true, allowInlineDefinition = false)
public class TopLevelObjectStore extends ExtensionObjectStore {

  @RefName
  private String name;

  @Override
  protected String resolveStoreName() {
    return name;
  }
}
