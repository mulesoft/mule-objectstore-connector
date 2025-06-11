/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package test.java.com.mulesoft.json.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.mulesoft.anypoint.tests.http.HttpResponse;
import com.mulesoft.anypoint.tita.environment.api.ApplicationSelector;
import com.mulesoft.anypoint.tita.environment.api.artifact.ApplicationBuilder;
import com.mulesoft.anypoint.tita.environment.api.artifact.Identifier;
import com.mulesoft.anypoint.tita.runner.ambar.Ambar;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.Application;
import com.mulesoft.anypoint.tita.runner.ambar.annotation.runtime.Standalone;
import com.mulesoft.anypoint.tita.environment.api.runtime.Runtime;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.mulesoft.anypoint.tita.environment.api.artifact.Identifier.identifier;

@RunWith(Ambar.class)
public class ObjectStoreDeploymentTestCase {

    private static final Identifier API = identifier("api1");
    private static final Identifier PORT = identifier("port");
    private static final String RUNNER_COMPATIBLE_RUNTIME_VERSION = "4.3.0-HF-SNAPSHOT";

    @Standalone(testing = RUNNER_COMPATIBLE_RUNTIME_VERSION)
    private Runtime runtime;

    @Application
    public static ApplicationBuilder app(ApplicationSelector runtimeBuilder) {
        return runtimeBuilder
                .custom("objectstore-store-app", "objectstore-store-app.xml")
                .withTemplatePomFile("objectstore-store-app-pom.xml")
                .withApi(API, PORT);
    }


    @Test
    public void testObjectStoreConnectionWithFailsDeploymentTrue() {
        HttpResponse response = runtime.api(API).request( "/storeFailsDeploymentTrue").get();
        assertThat(response.statusCode(), is(equalTo(500)));
    }
    @Test
    public void testObjectStoreConnectionWithFailsDeploymentFalse() {
        HttpResponse response = runtime.api(API).request( "/storeFailsDeploymentFalse").get();
        assertThat(response.statusCode(), is(equalTo(500)));
    }
}