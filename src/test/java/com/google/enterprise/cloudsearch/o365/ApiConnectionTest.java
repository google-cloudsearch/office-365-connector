/*
 * Copyright © 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.enterprise.cloudsearch.o365;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.enterprise.cloudsearch.o365.ApiConnection.AccessToken;
import com.google.enterprise.cloudsearch.o365.ApiConnection.Builder;
import com.google.enterprise.cloudsearch.o365.ApiConnection.JWTHelper;
import com.google.enterprise.cloudsearch.o365.ApiConnection.JwtHelperBuilder;
import com.google.enterprise.cloudsearch.o365.Request.RequestHelper;
import com.google.enterprise.cloudsearch.sdk.InvalidConfigurationException;
import com.google.enterprise.cloudsearch.sdk.config.Configuration.ResetConfigRule;
import com.google.enterprise.cloudsearch.sdk.config.Configuration.SetupConfigRule;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Properties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ApiConnectionTest {
  @Rule public ExpectedException thrown = ExpectedException.none();
  @Rule public ResetConfigRule resetConfig = new ResetConfigRule();
  @Rule public SetupConfigRule setupConfig = SetupConfigRule.uninitialized();
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private void createFile(File file, String content) throws IOException {
    PrintWriter pw = new PrintWriter(new FileWriter(file));
    pw.write(content);
    pw.close();
  }

  @Test
  public void testNullClientId() throws MalformedURLException {
    thrown.expect(IllegalArgumentException.class);
    new Builder().setClientId(null).setClientSecret("clientSecret").setTenant("Tenant").build();
  }

  @Test
  public void testNullClientSecret() throws MalformedURLException {
    thrown.expect(NullPointerException.class);
    new Builder().setClientId("testId").setClientSecret(null).setTenant("testTenant").build();
  }

  @Test
  public void testNullTenant() throws MalformedURLException {
    thrown.expect(IllegalArgumentException.class);
    new Builder().setClientId("testId").setClientSecret("testClientSecret").setTenant(null).build();
  }

  @Test
  public void testValidBuilder() throws GeneralSecurityException, IOException {
    new Builder()
        .setClientId("testId")
        .setClientSecret("testClientSecret")
        .setTenant("testTenant")
        .setHttpClient(HttpClient.newBuilder().build())
        .build();
  }

  @Test
  public void testFromConfigurationNotInitialized() throws GeneralSecurityException, IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("configuration not initialized");
    ApiConnection.fromConfiguration(HttpClient.newBuilder().build());
  }

  @Test
  public void testFromConfigurationClientSecret() throws Exception {
    Properties config = new Properties();
    config.put(ApiConnection.CLIENT_ID, "client id");
    config.put(ApiConnection.CLIENT_SECRET, "secret");
    config.put(ApiConnection.TENANT, "tenant");
    setupConfig.initConfig(config);
    ApiConnection.fromConfiguration(HttpClient.newBuilder().build());
  }

  @Test
  public void testGetAccessToken() throws Exception {
    HttpClient httpClient = HttpClient.newBuilder().build();
    RequestHelper<AccessToken> requestHelper = spy(new RequestHelper<>());
    ApiConnection connection =
        new ApiConnection.Builder()
            .setClientId("c1")
            .setClientSecret("sec")
            .setTenant("tenant")
            .setHttpClient(httpClient)
            .setRequestHelper(requestHelper)
            .build();

    doAnswer(
            invocation -> {
              AccessToken accessToken = new AccessToken();
              accessToken.access_token = "token1";
              return accessToken;
            })
        .when(requestHelper)
        .executeRequest(any(), eq(httpClient));
    String actual = connection.getAccessToken();
    assertEquals("token1", actual);
  }

  @Test
  public void testFromConfigurationJwt() throws Exception {

    Properties config = new Properties();
    config.put(ApiConnection.CLIENT_ID, "client id");
    config.put(ApiConnection.TENANT, "tenant");
    config.put(ApiConnection.KEYSTORE_FILE, "some file");
    config.put(ApiConnection.KEYSTORE_PASSWORD, "password");
    config.put(ApiConnection.KEYSTORE_ALIAS, "alias");
    setupConfig.initConfig(config);
    JwtHelperBuilder jwtHelperBuilder = mock(JwtHelperBuilder.class);
    when(jwtHelperBuilder.build("some file", "password", "alias"))
        .thenReturn(mock(JWTHelper.class));
    ApiConnection.fromConfiguration(HttpClient.newBuilder().build(), jwtHelperBuilder);
  }

  @Test
  public void testJWTHelperBuilderInvalidFilePath() throws Exception {
    String testName = Paths.get("test/test.pfx").toString();
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(testName + " does not exist");
    new JWTHelper.Builder().setKeyStoreFilePath(testName)
        .setKeyStoreAlias("testalias").setKeyStorePassword("testpassword").build();
  }

  @Test
  public void testJWTHelperBuilderNullFilePath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Key store file path can not be null or empty");
    new JWTHelper.Builder().setKeyStoreAlias("testalias").setKeyStorePassword("testpassword")
        .build();
  }

  @Test
  public void testJWTHelperBuilderNullAlias() throws Exception {
    File tmpfile = temporaryFolder.newFile("testJKS.pfx");
    createFile(tmpfile, "TEST");
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Key store alias can not be null or empty");
    new JWTHelper.Builder().setKeyStoreFilePath(tmpfile.getAbsolutePath())
        .setKeyStorePassword("testpassword")
        .build();
  }

  @Test
  public void testJWTHelperBuilderNullPassword() throws Exception {
    File tmpfile = temporaryFolder.newFile("testJKS.pfx");
    createFile(tmpfile, "TEST");
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Key store password can not be null or empty");
    new JWTHelper.Builder().setKeyStoreFilePath(tmpfile.getAbsolutePath())
        .setKeyStoreAlias("alias")
        .build();
  }

  @Test
  public void testFromConfigurationNullClientSecretNoJKS() throws Exception {
    Properties config = new Properties();
    config.put(ApiConnection.CLIENT_ID, "client id");
    config.put(ApiConnection.TENANT, "tenant");
    setupConfig.initConfig(config);
    thrown.expect(InvalidConfigurationException.class);
    thrown.expectMessage("Required Config Key o365.keyStore.file not initialized");
    ApiConnection.fromConfiguration(HttpClient.newBuilder().build());
  }
}
