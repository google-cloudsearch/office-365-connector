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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;

/**
 * This class represents Group Object get from Microsoft graph API
 */
public class Group extends DirectoryObject {

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  public Group() {
    super();
    setFactory(JSON_FACTORY);
  }

  Group(Builder builder) {
    this();
    super.setId(builder.id);
  }

  public static Group parse(String group) throws IOException {
    return JSON_FACTORY.fromString(group, Group.class);
  }

  @Override
  public boolean isValid() {
    return getId() != null && !getId().isEmpty();
  }

  public static class Builder {
    private String id;

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Group build() {
      checkNotNull(id, "id can not be null");
      checkArgument(!id.isEmpty(), "id can not be empty");
      return new Group(this);
    }
  }
}
