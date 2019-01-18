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
package com.google.enterprise.cloudsearch.o365.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.enterprise.cloudsearch.o365.util.LoadTestJson;
import java.io.IOException;
import org.junit.Test;

public class UserTest {

  private static final String USER_RESPONSE = "get_user.json";

  @Test
  public void testUser() throws IOException {
    User user = User.parse(LoadTestJson.loadTestJson(USER_RESPONSE, UserTest.class));
    assertEquals("12355", user.getId());
    assertTrue(user.isValid());
    assertEquals("user1@test.onmicrosoft.com", user.getMail());
    assertEquals("user1@test.onmicrosoft.com", user.getUserPrincipalName());
  }
}
