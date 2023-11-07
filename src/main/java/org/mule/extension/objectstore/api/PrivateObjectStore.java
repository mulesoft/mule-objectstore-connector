/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.api;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.stereotype.ObjectStoreStereotype;

import java.util.UUID;

/**
 * An ObjectStore which can only be defined embedded inside an owning component. No other component
 * will be able to reference it, but it will still be available through the {@link ObjectStoreManager}.
 *
 * Event though this ObjectStore cannot be referenced, it still needs to have a store name, so that it can be
 * identifiable in the management tools.
 *
 * @since 1.0
 */
@Alias("privateObjectStore")
@TypeDsl(substitutionGroup = "mule:abstract-private-object-store", baseType = "mule:abstractObjectStoreType")
@Stereotype(ObjectStoreStereotype.class)
public class PrivateObjectStore extends ExtensionObjectStore {

  /**
   * A friendly name to refer to this store from the management UI. Provide this alias if you would like it
   * to be easier to identify this store. If not provided, Mule will auto generate an ID.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB)
  private String alias;

  private LazyValue<String> storeName = new LazyValue<>(this::generateStoreName);

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  @Override
  protected String resolveStoreName() {
    return storeName.get();
  }

  private String generateStoreName() {
    if (alias != null && alias.trim().length() > 0) {
      return alias;
    }

    String uuid = UUID.randomUUID().toString();
    String configRef = getConfigName();
    if (configRef != null) {
      uuid = configRef + "/" + uuid;
    }

    return uuid;
  }
}
