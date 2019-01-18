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

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.GenericJson;
import com.google.api.client.testing.http.HttpTesting;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.enterprise.cloudsearch.o365.Request.RequestHelper;
import com.google.enterprise.cloudsearch.o365.model.User;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.junit.Test;
import org.mockito.Mock;

public class RequestTest {

  @Mock
  private HttpClient mockHttpClient;

  private static final String CONTENT_TYPE = "text/json";
  private static final String DATA = "Hello world";
  private static final String HTTP_HEADER_CONTENT_LENGTH_KEY = "Content-Length";

  @Test
  public void executeContentRequest_succeeds() throws Exception {
    Request<GenericJson> getContentRequest = new FakeRequest.Builder<GenericJson>()
        .setContent(DATA)
        .setContentType(CONTENT_TYPE)
        .setResponseClass(GenericJson.class)
        .setRequestURL(new URL("http://www.google.com"))
        .build();

    ContentResponse response = new RequestHelper<GenericJson>().executeContentRequest(
        getContentRequest, mockHttpClient);
    String result = CharStreams.toString(new InputStreamReader(
        response.getContent().getInputStream(), Charsets.UTF_8));

    assertEquals(DATA, result);
    assertEquals(DATA.length(), response.getContent().getLength());
    assertEquals(CONTENT_TYPE, response.getContentType());
    assertEquals(true, response.getContent().getCloseInputStream());
    assertEquals(GenericJson.class, getContentRequest.getResponseClass());
  }

  @Test
  public void executeContentRequest_contentLengthNull_succeeds() throws Exception {
    Request<GenericJson> getContentRequest = new FakeRequest.Builder<GenericJson>()
        .setContent("")
        .setContentType(CONTENT_TYPE)
        .setResponseClass(GenericJson.class)
        .setRequestURL(new URL("http://www.google.com")).build();

    ContentResponse response = new RequestHelper<GenericJson>().executeContentRequest(
        getContentRequest, mockHttpClient);

    assertEquals(-1, response.getContent().getLength());
    assertEquals(CONTENT_TYPE, response.getContentType());
    assertEquals(true, response.getContent().getCloseInputStream());
  }

  @Test
  public void executeRequest_succeeds() throws Exception {
    User user = new User();
    user.setId("user1");
    Request<User> getContentRequest =
        new FakeRequest.Builder<User>()
            .setContent(user.toPrettyString())
            .setResponseClass(User.class)
            .setRequestURL(new URL("http://www.google.com"))
            .build();

    assertEquals(user, new RequestHelper<User>().executeRequest(getContentRequest, mockHttpClient));
  }

  @Test
  public void testEquals() throws Exception {
    Request<GenericJson> getContentRequest1 =
        new FakeRequest.Builder<GenericJson>()
            .setContent(DATA)
            .setContentType(CONTENT_TYPE)
            .setResponseClass(GenericJson.class)
            .setRequestURL(new URL("http://www.google.com"))
            .build();

    Request<GenericJson> getContentRequest2 =
        new FakeRequest.Builder<GenericJson>()
            .setContent(DATA)
            .setContentType(CONTENT_TYPE)
            .setResponseClass(GenericJson.class)
            .setRequestURL(new URL("http://www.google.com"))
            .build();

    Request<User> getContentRequestAnother =
        new FakeRequest.Builder<User>()
            .setContent(DATA)
            .setContentType(CONTENT_TYPE)
            .setResponseClass(User.class)
            .setRequestURL(new URL("http://www.google.com"))
            .build();
    assertEquals(getContentRequest1, getContentRequest2);
    assertEquals(getContentRequest1.hashCode(), getContentRequest1.hashCode());
    assertFalse(
        "getContentRequest1 should not be equal to getContentRequestAnother",
        getContentRequest1.equals(getContentRequestAnother));
  }

  private static class FakeRequest<T extends GenericJson> extends Request<T> {

    private String content;
    private String contentType;

    public static class Builder<T extends GenericJson> extends Request.Builder<T> {

      private String content;
      private String contentType;

      @Override
      public Request<T> build() {
        return new FakeRequest<>(this);
      }

      public Builder<T> setContent(String content) {
        this.content = content;
        return this;
      }

      public Builder<T> setContentType(String contentType) {
        this.contentType = contentType;
        return this;
      }
    }

    FakeRequest(Builder<T> builder) {
      super(builder);
      this.content = builder.content;
      this.contentType = builder.contentType;
    }

    @Override
    HttpRequest createHttpRequest(HttpClient client) throws IOException {
      HttpTransport transport = new MockHttpTransport() {
        @Override
        public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
          return new MockLowLevelHttpRequest() {
            @Override
            public LowLevelHttpResponse execute() throws IOException {
              MockLowLevelHttpResponse response = new MockLowLevelHttpResponse();
              if (!content.isEmpty()) {
                response
                    .addHeader(HTTP_HEADER_CONTENT_LENGTH_KEY, String.valueOf(content.length()));
              }
              response.setContentType(contentType);
              response.setContent(content);
              return response;
            }
          };
        }
      };

      return transport.createRequestFactory()
          .buildGetRequest(HttpTesting.SIMPLE_GENERIC_URL);
    }
  }
}
