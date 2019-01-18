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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.json.GenericJson;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * {@link Request} implementation for making HTTP POST requests. {@link
 * PostRequest#content} is sent as POST request body in URL encoded format as {@link
 * UrlEncodedContent}
 */
public class PostRequest<T extends GenericJson> extends Request<T> {

  private final Map<String, String> content;

  private PostRequest(Builder<T> builder) {
    super(builder);
    this.content = builder.content;
  }

  @Override
  HttpRequest createHttpRequest(HttpClient client) throws IOException {
    return client.getRequestFactory().buildPostRequest(new GenericUrl(getRequestURL()),
        new UrlEncodedContent(content));
  }

  static class Builder<T extends GenericJson> extends Request.Builder<T> {

    private Map<String, String> content = Collections.emptyMap();

    public Builder<T> setConetnt(Map<String, String> content) {
      this.content = content;
      return this;
    }

    @Override
    public PostRequest<T> build() {
      validate();
      checkNotNull(content, "content map can not be null");
      return new PostRequest<T>(this);
    }
  }

}
