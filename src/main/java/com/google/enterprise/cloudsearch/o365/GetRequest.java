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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.GenericJson;
import java.io.IOException;

/** {@link Request} implementation for making HTTP GET requests. */
public class GetRequest<T extends GenericJson> extends Request<T> {

  private GetRequest(Builder<T> builder) {
    super(builder);
  }

  @Override
  HttpRequest createHttpRequest(HttpClient client) throws IOException {
    return client.getRequestFactory().buildGetRequest(new GenericUrl(getRequestURL()));
  }

  public static class Builder<T extends GenericJson> extends Request.Builder<T> {

    @Override
    public GetRequest<T> build() {
      validate();
      return new GetRequest<T>(this);
    }
  }
}
