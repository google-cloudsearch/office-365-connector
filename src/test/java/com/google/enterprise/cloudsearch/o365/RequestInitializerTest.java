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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpRequest;
import com.google.enterprise.cloudsearch.o365.model.User;
import java.net.URL;
import org.junit.Test;

public class RequestInitializerTest {
  @Test
  public void testInitializerSetsRequiredHeaders() throws Exception {
    Request<User> request =
        new GetRequest.Builder<User>()
            .setRequestURL(new URL("http://tenant.o365.com"))
            .setResponseClass(User.class)
            .build();
    ApiConnection connection = mock(ApiConnection.class);
    when(connection.getAccessToken()).thenReturn("token1");
    HttpClient httpClient = HttpClient.newBuilder().build();

    RequestInitalizer initializer = new RequestInitalizer(connection);
    HttpRequest httpRequest = request.createHttpRequest(httpClient);
    initializer.initialize(httpRequest);

    assertEquals("application/json", httpRequest.getHeaders().getAccept());
    assertEquals("Bearer token1", httpRequest.getHeaders().getAuthorization());
  }
}
