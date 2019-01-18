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

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import com.google.common.base.Strings;
import java.io.IOException;

/**
 * This class represents a generic object with unique Id.
 */
public class DirectoryObject extends GenericJson {
  @Key private String id;

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  public boolean isValid() {
    return !Strings.isNullOrEmpty(id);
  }

  public DirectoryObject() {
    super();
    setFactory(JSON_FACTORY);
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public static DirectoryObject parse(String directoryObject) throws IOException {
    return JSON_FACTORY.fromString(directoryObject, DirectoryObject.class);
  }
}
