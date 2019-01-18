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

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * Base class for O365 requests.
 *
 * <p>Use {@link RequestHelper#executeRequest(Request, HttpClient)} to execute request and
 * parse response.
 */
public abstract class Request<T extends GenericJson> {

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  protected final URL requestURL;
  protected final Class<T> responseClass;

  Request(Builder<T> builder) {
    requestURL = builder.requestURL;
    responseClass = builder.responseClass;
  }

  public URL getRequestURL() {
    return requestURL;
  }

  public Class<T> getResponseClass() {
    return responseClass;
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestURL, responseClass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Request)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    Request<T> other = (Request<T>) obj;
    return Objects.equals(requestURL, other.requestURL)
        && Objects.equals(responseClass, other.responseClass);
  }

  abstract HttpRequest createHttpRequest(HttpClient client) throws IOException;

  /**
   * Helper class to execute {@link Request}.
   */
  public static class RequestHelper<T extends GenericJson> {

    /**
     * Executes a {@link Request} request built using {@link HttpClient} client.
     *
     * @param request to execute
     * @param client to create {@link HttpRequest} for input request
     * @return response as {@link Request#responseClass}
     * @throws IOException if request execution fails.
     */
    public T executeRequest(Request<T> request, HttpClient client) throws IOException {
      HttpRequest req = request.createHttpRequest(client);
      req.setParser(new JsonObjectParser(JSON_FACTORY));
      return req.execute().parseAs(request.getResponseClass());
    }

    /**
     * Executes a {@link Request} request built using {@link HttpClient} client and returns the
     * response as {@link ContentResponse}.
     *
     * @param request to execute.
     * @param client to create {@link HttpRequest} for input request.
     * @return response as {@link ContentResponse}.
     * @throws IOException if request execution fails.
     */
    public ContentResponse executeContentRequest(
        Request<T> request, HttpClient client) throws IOException {
      HttpRequest req = request.createHttpRequest(client);
      HttpResponse httpResponse = req.execute();

      InputStreamContent content =
          new InputStreamContent(httpResponse.getContentType(), httpResponse.getContent());
      content.setCloseInputStream(true);
      Optional.ofNullable(httpResponse.getHeaders().getContentLength())
          .ifPresent(content::setLength);

      return new ContentResponse.Builder()
          .setContent(content)
          .setContentType(httpResponse.getContentType())
          .build();
    }
  }

  public abstract static class Builder<T extends GenericJson> {

    private URL requestURL;
    private Class<T> responseClass;

    public Builder<T> setRequestURL(URL requestURL) {
      this.requestURL = requestURL;
      return this;
    }

    public Builder<T> setResponseClass(Class<T> responseClass) {
      this.responseClass = responseClass;
      return this;
    }

    public void validate() {
      checkNotNull(requestURL, "Request URL can not be null");
      checkNotNull(responseClass, "Response class can not be null");
    }

    public abstract Request<T> build();
  }
}
