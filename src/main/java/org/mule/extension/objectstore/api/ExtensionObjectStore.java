/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.objectstore.api;

import static java.lang.String.format;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.extension.objectstore.internal.ObjectStoreConnector;
import org.mule.extension.objectstore.internal.ObjectStoreRegistry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.reference.ConfigReference;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

/**
 * Base class for {@link ObjectStore stores} defined through this connector
 *
 * @since 1.0
 */
public abstract class ExtensionObjectStore implements ObjectStore<Serializable>, Startable, Stoppable, NamedObject {

  private static final Logger LOGGER = getLogger(ExtensionObjectStore.class);
  private boolean started = false;

  @Inject
  private ExtensionManager extensionManager;

  @Inject
  private ObjectStoreRegistry registry;

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager runtimeObjectStoreManager;

  /**
   * Whether the store is persistent or transient.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  private boolean persistent;

  /**
   * The max number of entries allowed. Exceeding entries will be removed when expiration thread runs. If absent, then the
   * described {@link ObjectStore} will have no size boundaries.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private Integer maxEntries;

  /**
   * The entry timeout. If absent, then the described {@link ObjectStore} will have no time boundaries.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private Long entryTtl;

  /**
   * A {@link TimeUnit} which qualifies the {@link #entryTtl}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  @Expression(NOT_SUPPORTED)
  private TimeUnit entryTtlUnit;

  private ComponentLocation location;

  /**
   * How frequently should the expiration thread run.
   * <p>
   * If {@link #maxEntries} nor {@link #entryTtl} are set, then the expiration thread will not run despite of the value set here.
   * <p>
   * If set to a value lower or equal to zero, then there will be no expiration.
   */
  @Parameter
  @Optional(defaultValue = "1")
  @Expression(NOT_SUPPORTED)
  private Long expirationInterval;

  /**
   * A {@link TimeUnit} which qualifies the {@link #expirationInterval}
   */
  @Parameter
  @Optional(defaultValue = "MINUTES")
  @Expression(NOT_SUPPORTED)
  private TimeUnit expirationIntervalUnit;

  /**
   * A reference to an {@code os:config} element which will be used to declare this ObjectStore.
   * <p>
   * If not provided, the runtime's default {@link ObjectStoreManager} will be used. Setting this parameter is only necessary when
   * you want to use a non default {@link ObjectStore} implementation. For example, if you want to have a store which is backed by
   * redis or JDBC
   */
  @Parameter
  @Optional
  @DisplayName("Configuration Reference")
  @Alias("config-ref")
  @ConfigReference(name = "CONFIG", namespace = "OS")
  @Expression(NOT_SUPPORTED)
  @ParameterDsl(allowInlineDefinition = false)
  protected ObjectStoreConnector config;

  private transient ConnectionProvider<ObjectStoreManager> storeManagerProvider;
  private transient ObjectStoreManager objectStoreManager;
  private transient ObjectStore<Serializable> delegateStore;

  protected abstract String resolveStoreName();

  @Override
  public void start() throws MuleException {
    if (started) {
      return;
    }
    storeManagerProvider = getObjectStoreManagerProvider();
    objectStoreManager = getObjectStoreManager();

    final ObjectStoreSettings.Builder settings = ObjectStoreSettings.builder()
        .persistent(persistent)
        .maxEntries(maxEntries)
        .expirationInterval(expirationIntervalUnit.toMillis(expirationInterval));

    if (entryTtl != null) {
      settings.entryTtl(entryTtlUnit.toMillis(entryTtl));
    }

    delegateStore = objectStoreManager.createObjectStore(resolveStoreName(), settings.build());

    registry.register(resolveStoreName(), this);
    started = true;
  }

  @Override
  public void stop() throws MuleException {
    registry.unregister(resolveStoreName());

    if (delegateStore != null) {
      try {
        delegateStore.close();
      } catch (Exception e) {
        LOGGER.warn(format("Found exception trying to close Object Store '%s'", resolveStoreName()), e);
      }
    }

    if (storeManagerProvider != null && objectStoreManager != null) {
      try {
        storeManagerProvider.disconnect(objectStoreManager);
      } catch (Exception e) {
        LOGGER.warn(format("Found exception trying to disconnect from ObjectStoreManager obtained through config '%s'",
                           getConfigName()),
                    e);
      }
    }

    storeManagerProvider = null;
    objectStoreManager = null;
    delegateStore = null;
    started = false;
  }

  @Override
  public boolean contains(String key) throws ObjectStoreException {
    return delegateStore.contains(key);
  }

  @Override
  public void store(String key, Serializable value) throws ObjectStoreException {
    delegateStore.store(key, value);
  }

  @Override
  public Serializable retrieve(String key) throws ObjectStoreException {
    return delegateStore.retrieve(key);
  }

  @Override
  public Serializable remove(String key) throws ObjectStoreException {
    return delegateStore.remove(key);
  }

  @Override
  public boolean isPersistent() {
    return delegateStore.isPersistent();
  }

  @Override
  public void clear() throws ObjectStoreException {
    delegateStore.clear();
  }

  @Override
  public void open() throws ObjectStoreException {
    delegateStore.open();
  }

  @Override
  public void close() throws ObjectStoreException {
    delegateStore.close();
  }

  @Override
  public List<String> allKeys() throws ObjectStoreException {
    return delegateStore.allKeys();
  }

  @Override
  public Map<String, Serializable> retrieveAll() throws ObjectStoreException {
    return delegateStore.retrieveAll();
  }

  @Override
  public String getName() {
    return resolveStoreName();
  }

  private ObjectStoreManager getObjectStoreManager() throws MuleException {
    ObjectStoreManager storeManager;
    try {
      storeManager = storeManagerProvider.connect();
    } catch (ConnectionException e) {
      throw new DefaultMuleException(format("Could not obtain ObjectStore Manager from config '%s'", getConfigName()), e);
    }

    ConnectionValidationResult validationResult = storeManagerProvider.validate(storeManager);
    if (!validationResult.isValid()) {
      String errorType = validationResult.getErrorType()
          .map(type -> type.getNamespace() + ":" + type.getIdentifier())
          .orElse("UNKNOWN");

      throw new DefaultMuleException(format("Obtained invalid connection from ObjectStore config '%s'.\n"
          + "Error Type: %s.\nMessage: %s",
                                            getConfigName(), errorType, validationResult.getMessage()));
    }

    return storeManager;
  }

  private ConnectionProvider<ObjectStoreManager> getObjectStoreManagerProvider() throws MuleException {
    if (config == null) {
      return new FallbackObjectStoreManagerProvider();
    }

    CoreEvent event = CoreEvent.builder(create(resolveStoreName(), "dummy", fromSingleComponent(resolveStoreName()),
                                               NullExceptionHandler.getInstance()))
        .message(Message.of("none"))
        .build();

    ConfigurationInstance configurationProvider;
    try {
      configurationProvider = extensionManager.getConfiguration(getConfigName(), event);
    } catch (IllegalArgumentException e) {
      throw new DefaultMuleException(format("ObjectStore '%s' points to configuration '%s' which doesn't exits",
                                            resolveStoreName(), getConfigName()),
                                     e);
    }

    return configurationProvider.getConnectionProvider().orElseGet(FallbackObjectStoreManagerProvider::new);
  }

  protected String getConfigName() {
    return config != null ? config.getConfigName() : "default";
  }

  private class FallbackObjectStoreManagerProvider implements ConnectionProvider<ObjectStoreManager> {

    @Override
    public ObjectStoreManager connect() throws ConnectionException {
      return runtimeObjectStoreManager;
    }

    @Override
    public void disconnect(ObjectStoreManager connection) {

    }

    @Override
    public ConnectionValidationResult validate(ObjectStoreManager connection) {
      return success();
    }
  }

  public Integer getMaxEntries() {
    return maxEntries;
  }

  public Long getEntryTtl() {
    return entryTtl;
  }

  public TimeUnit getEntryTtlUnit() {
    return entryTtlUnit;
  }

  public ComponentLocation getLocation() {
    return location;
  }

  public Long getExpirationInterval() {
    return expirationInterval;
  }

  public TimeUnit getExpirationIntervalUnit() {
    return expirationIntervalUnit;
  }
}
