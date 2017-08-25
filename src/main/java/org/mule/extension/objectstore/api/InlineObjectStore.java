/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.api;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * An ObjectStore which can only be defined embedded inside an owning component. No other component
 * will be able to reference it, but it will still be available through the {@link ObjectStoreManager}.
 *
 * Event though this ObjectStore cannot be referenced, it still needs to have a store name, so that it can be
 * identifiable in the management tools.
 *
 * @since 1.0
 */
@Alias("inlineObjectStore")
@TypeDsl(allowTopLevelDefinition = false, allowInlineDefinition = true)
public class InlineObjectStore extends ExtensionObjectStore {

  /**
   * The name of this store
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private String storeName;

  @Override
  protected String resolveStoreName() {
    return storeName;
  }
}