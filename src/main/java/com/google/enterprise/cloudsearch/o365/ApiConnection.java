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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.json.webtoken.JsonWebSignature.Header;
import com.google.api.client.json.webtoken.JsonWebToken;
import com.google.api.client.json.webtoken.JsonWebToken.Payload;
import com.google.api.client.util.Key;
import com.google.api.client.util.SecurityUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.enterprise.cloudsearch.o365.Request.RequestHelper;
import com.google.enterprise.cloudsearch.sdk.config.Configuration;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class creates application connection with Azure active directory v2.0 endpoint.
 *
 * <p>
 *
 * <ul>
 *   <li>{@value #CLIENT_ID} - Application Id that the Microsoft Application Registration Portal
 *       assigned to the app.
 *   <li>{@value #TENANT} - The directory tenant that user wants to request permission from. This
 *       can be in GUID or friendly name format.
 *   <li>{@value #CLIENT_SECRET} - The application secret that generated for the app in the app
 *       registration portal.
 *   <li>{@value #KEYSTORE_FILE} - Specifies the file path to the keystore which contains RSA
 *       private key and certificate.
 *   <li>{@value #KEYSTORE_PASSWORD} - Specifies the password of the keystore
 *   <li>{@value #KEYSTORE_ALIAS} - Specifies the alias of the key in the keystore.
 * </ul>
 */
public class ApiConnection {

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String TOKEN_ENDPOINT =
      "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
  public static final String CLIENT_ID = "o365.clientId";
  public static final String TENANT = "o365.tenant";
  public static final String CLIENT_SECRET = "o365.clientSecret";
  public static final String KEYSTORE_FILE = "o365.keyStore.file";
  public static final String KEYSTORE_PASSWORD = "o365.keyStore.password";
  public static final String KEYSTORE_ALIAS = "o365.keyStore.alias";

  private static final long DEFAULT_JWT_TOKEN_EXPIRATION_SECONDS = 300;
  private static final String SCOPE = "https://graph.microsoft.com/.default";
  private static final String GRANT_TYPE = "client_credentials";
  private static final String JWT_CLIENT_ASSERTION_TYPE =
      "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

  private final RequestHelper<AccessToken> requestHelper;
  private final HttpClient httpClient;
  private URL accessTokenURL;
  private final JWTHelper jwtHelper;
  private ImmutableMap<String, String> content;
  private boolean isKeyStore;
  private String clientId;
  private String tenant;
  private static final ImmutableMap<String, String> commonContent =
      ImmutableMap.<String, String>builder()
          .put("scope", SCOPE)
          .put("grant_type", GRANT_TYPE)
          .build();

  private final LoadingCache<String, AccessToken> connectionCache;

  static ApiConnection fromConfiguration(HttpClient httpClient)
      throws IOException, GeneralSecurityException {
    return fromConfiguration(
        httpClient,
        (file, password, alias) ->
            new JWTHelper.Builder()
                .setKeyStoreFilePath(file)
                .setKeyStorePassword(password)
                .setKeyStoreAlias(alias)
                .build());
  }

  @VisibleForTesting
  static ApiConnection fromConfiguration(HttpClient httpClient, JwtHelperBuilder jwtHelperBuilder)
      throws IOException, GeneralSecurityException {
    checkState(Configuration.isInitialized(), "configuration not initialized");
    // required fields
    String clientId = Configuration.getString(CLIENT_ID, null).get();
    String tenant = Configuration.getString(TENANT, null).get();
    String clientSecret = Configuration.getString(CLIENT_SECRET, "").get();

    if (clientSecret.isEmpty()) {
      // key store can not be null
      String keyStoreFilePath = Configuration.getString(KEYSTORE_FILE, null).get();
      String keyStorePassword = Configuration.getString(KEYSTORE_PASSWORD, null).get();
      String keyStoreAlias = Configuration.getString(KEYSTORE_ALIAS, null).get();
      JWTHelper jwtHelper =
          jwtHelperBuilder.build(keyStoreFilePath, keyStorePassword, keyStoreAlias);
      return new Builder()
          .setClientId(clientId)
          .setTenant(tenant)
          .setHttpClient(httpClient)
          .setJWTHelper(jwtHelper)
          .build();
    }

    return new Builder()
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .setTenant(tenant)
        .setHttpClient(httpClient)
        .build();
  }

  @VisibleForTesting
  @FunctionalInterface
  interface JwtHelperBuilder {
    JWTHelper build(String keyStoreFilePath, String keyStorePassword, String keyStoreAlias)
        throws IOException, GeneralSecurityException;
  }

  String getAccessToken() throws ExecutionException {
    return connectionCache.get("access-token").access_token;
  }

  private AccessToken acquireAccessToken() throws IOException, GeneralSecurityException {
    ImmutableMap<String, String> completeContent = content;
    if (isKeyStore) {
      completeContent = ImmutableMap.<String, String>builder()
          .putAll(content).put("client_assertion", jwtHelper.getJWT(clientId, tenant)).build();
    }

    Request<AccessToken> accessTokenRequest = new PostRequest.Builder<AccessToken>()
        .setConetnt(completeContent)
        .setRequestURL(accessTokenURL).setResponseClass(AccessToken.class).build();
    return requestHelper.executeRequest(accessTokenRequest, httpClient);
  }

  ApiConnection(Builder builder) throws MalformedURLException {
    accessTokenURL = new URL(String.format(TOKEN_ENDPOINT, builder.tenant));
    jwtHelper = builder.jwtHelper;
    isKeyStore = builder.isKeyStore;
    clientId = builder.clientId;
    tenant = builder.tenant;

    if (!isKeyStore) {
      content =
          ImmutableMap.<String, String>builder().put("client_id", builder.clientId)
              .put("client_secret", builder.clientSecret)
              .putAll(commonContent).build();
    } else {
      content = ImmutableMap.<String, String>builder().put("client_id", builder.clientId)
          .put("client_assertion_type", JWT_CLIENT_ASSERTION_TYPE)
          .putAll(commonContent).build();
    }

    this.httpClient = builder.httpClient;
    this.requestHelper = builder.requestHelper;

    //TODO(ruoxiwang):If necessary, we can make this configurable by user later.
    this.connectionCache =
        CacheBuilder.newBuilder()
            .refreshAfterWrite(30, TimeUnit.MINUTES)
            .expireAfterWrite(45, TimeUnit.MINUTES)
            .build(
                new CacheLoader<String, AccessToken>() {
                  @Override
                  public AccessToken load(String clientId)
                      throws IOException, GeneralSecurityException {
                    return acquireAccessToken();
                  }
                });
  }

  public static class Builder {
    private String clientId;
    private String tenant;
    private String clientSecret;
    private HttpClient httpClient;
    private RequestHelper<AccessToken> requestHelper = new RequestHelper<>();
    private JWTHelper jwtHelper;
    boolean isKeyStore = false;

    Builder setClientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    Builder setTenant(String tenant) {
      this.tenant = tenant;
      return this;
    }

    Builder setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
      return this;
    }

    Builder setHttpClient(HttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    Builder setRequestHelper(RequestHelper<AccessToken> requestHelper) {
      this.requestHelper = requestHelper;
      return this;
    }

    Builder setJWTHelper(JWTHelper jwtHelper) {
      this.jwtHelper = jwtHelper;
      return this;
    }

    ApiConnection build() throws MalformedURLException {
      checkArgument(!Strings.isNullOrEmpty(clientId), "clientId can not be null or empty");
      checkArgument(!Strings.isNullOrEmpty(tenant), "tenant can not be null or empty");
      if (Strings.isNullOrEmpty(clientSecret)) {
        checkNotNull(jwtHelper, "jwt helper can not be null when clientSecret is null or empty");
        isKeyStore = true;
      }
      checkNotNull(httpClient, "HttpClient can not be null");
      checkNotNull(requestHelper, "RequestHelper can not be null");
      return new ApiConnection(this);
    }
  }

  public static class AccessToken extends GenericJson {
    @Key String token_type;
    @Key Integer expires_in;
    @Key Integer ext_expires_in;
    @Key String access_token;

    public AccessToken() {
      super();
      setFactory(JSON_FACTORY);
    }
  }

  static class JWTHelper {

    private PrivateKey privateKey;
    private JsonWebSignature.Header header;
    private JsonWebToken.Payload payload;

    JWTHelper(Builder builder) {

      this.privateKey = builder.privateKey;
      header = new Header();
      header.setAlgorithm("RS256");
      header.setType("JWT");
      header.setX509Thumbprint(builder.certCustomKeyId);
    }

    private synchronized String getJWT(String clientId, String tenant)
        throws GeneralSecurityException, IOException {

      checkNotNull(header, "header can not be null");

      if (payload == null) {
        payload = new Payload();
        payload.setIssuer(clientId);
        payload.setAudience(String.format(TOKEN_ENDPOINT, tenant));
        payload.setSubject(clientId);
      }
      //update expiration time
      ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC);
      long currentTimeseconds = zdt.getLong(ChronoField.INSTANT_SECONDS);
      payload.setIssuedAtTimeSeconds(currentTimeseconds);
      payload.setExpirationTimeSeconds(currentTimeseconds + DEFAULT_JWT_TOKEN_EXPIRATION_SECONDS);

      return JsonWebSignature.signUsingRsaSha256(privateKey, JSON_FACTORY, header, payload);

    }

    static class Builder {

      private Path keyStoreFilePath;
      private String keyStorePassword;
      private String keyStoreAlias;
      private String keyStoreFile;
      private PrivateKey privateKey;
      private String certCustomKeyId;

      Builder setKeyStoreFilePath(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
        this.keyStoreFilePath = Paths.get(keyStoreFile);
        return this;
      }

      Builder setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
      }

      Builder setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
        return this;
      }

      JWTHelper build() throws IOException, GeneralSecurityException {
        checkArgument(!Strings.isNullOrEmpty(keyStoreFile),
            "Key store file path can not be null or empty");

        checkArgument(Files.exists(keyStoreFilePath),
            keyStoreFilePath + " does not exist");
        checkArgument(!Files.isDirectory(keyStoreFilePath),
            keyStoreFilePath + "is a directory. A file is expected");
        checkArgument(!Strings.isNullOrEmpty(keyStorePassword),
            "Key store password can not be null or empty");
        checkArgument(!Strings.isNullOrEmpty(keyStoreAlias),
            "Key store alias can not be null or empty");

        KeyStore keystore = SecurityUtils.getPkcs12KeyStore();
        SecurityUtils.loadKeyStore(
            keystore, new FileInputStream(keyStoreFilePath.toFile()), keyStorePassword);
        privateKey = SecurityUtils.getPrivateKey(keystore, keyStoreAlias, keyStorePassword);
        Certificate cert = keystore.getCertificate(keyStoreAlias);

        byte[] certData = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(certData);
        certCustomKeyId = Base64.getEncoder().encodeToString(md.digest());
        return new JWTHelper(this);
      }
    }
  }
}
