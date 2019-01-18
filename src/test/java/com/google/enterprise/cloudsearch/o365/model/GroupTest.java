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

public class GroupTest {

  private static final String GROUP_RESPONSE = "get_group.json";

  @Test
  public void test() throws IOException {
    Group group = Group.parse(LoadTestJson.loadTestJson(GROUP_RESPONSE, GroupTest.class));
    assertTrue(group.isValid());
    assertEquals("5433b4fc-bd1c-4c31-8ebd-77d1e294a217", group.getId());
  }
}
