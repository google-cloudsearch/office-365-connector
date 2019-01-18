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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.enterprise.cloudsearch.o365.util.LoadTestJson;
import java.io.IOException;
import org.junit.Test;

public class UsersTest {

  private static final String USERS_RESPONSE = "get_users.json";

  @Test
  public void testParsing() throws IOException {
    Users users = Users.parse(LoadTestJson.loadTestJson(USERS_RESPONSE, Users.class));
    assertTrue(users.isValid());
    assertNull(users.getOdataNextlink());
    assertNull(users.getOdataDeltalink());
    assertEquals("https://graph.microsoft.com/v1.0/$metadata#users", users.getOdataContext());
    assertEquals(9, users.getValue().size());
    users.getValue().forEach(u -> assertTrue(u.isValid()));
  }
}
