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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;

/** Utility class for building {@link HttpRequest}. */
public class HttpClient {
  private final HttpTransport transport;
  private final HttpRequestFactory requestFactory;
  private final HttpRequestInitializer requestInitializer;

  private HttpClient(Builder builder) {
    transport = builder.transport;
    requestInitializer = builder.requestInitializer;
    requestFactory = transport.createRequestFactory(requestInitializer);
  }

  public static Builder newBuilder() throws GeneralSecurityException, IOException {
    return new Builder();
  }

  public HttpRequestFactory getRequestFactory() {
    return requestFactory;
  }

  public static HttpClient fromConfiguration() throws IOException {
    try {
      HttpClient authenticationHttpClient =
          HttpClient.newBuilder()
              .setHttpRequestInitializer(
                  request -> request.getHeaders().setAccept("application/json"))
              .build();
      ApiConnection apiConnection =
          ApiConnection.fromConfiguration(authenticationHttpClient);
      return HttpClient.newBuilder()
          .setHttpRequestInitializer(new RequestInitalizer(apiConnection))
          .build();
    } catch (GeneralSecurityException e) {
      throw new IOException("Error initializing HttpClient", e);
    }
  }

  public static final class Builder {

    private HttpTransport transport;
    private HttpRequestInitializer requestInitializer;

    private Builder() throws GeneralSecurityException, IOException {
      this.transport = GoogleNetHttpTransport.newTrustedTransport();
      JacksonFactory.getDefaultInstance();
    }

    public Builder setTransport(HttpTransport transport) {
      this.transport = transport;
      return this;
    }

    public Builder setHttpRequestInitializer(HttpRequestInitializer httpRequestInitializer) {
      this.requestInitializer = httpRequestInitializer;
      return this;
    }

    public HttpClient build() {
      checkNotNull(transport, "HttpTransport can not be null");
      return new HttpClient(this);
    }
  }
}
