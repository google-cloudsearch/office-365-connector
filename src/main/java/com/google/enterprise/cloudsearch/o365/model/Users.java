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

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * This class models response from Microsoft Graph API when get all users.
 *
 * <pre><ul>
 *   <li> {@link #getOdataContext()} returns returns the context url for the payload.
 *   <li>{@link #getOdataNextlink()} returns the URL to retrieve next available page of changes, if
 *   there are additional changes in the current set.
 *   <li>{@link #getOdataDeltalink()} returns the URL to retrievenext set of changes in the future.
 *   <li> {@link #getValue()} returns a list of {@link User}.
 * </ul></pre>
 */
public class Users extends DirectoryObjects {

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  @Key("@odata.context")
  private String odataContext;

  @Key("@odata.nextLink")
  private String odataNextlink;

  @Key("@odata.deltaLink")
  private String odataDeltalink;

  @Key private List<User> value;

  public Users() {
    super();
    setFactory(JSON_FACTORY);
  }

  public Users(Builder builder) {
    new Users();
    this.value = builder.value;
    this.odataContext = builder.odataContext;
    this.odataNextlink = builder.odataNextlink;
    this.odataDeltalink = builder.odataDeltalink;
  }

  @Override
  public boolean isValid() {
    return value != null;
  }

  public static Users parse(String response) throws IOException {
    return JSON_FACTORY.fromString(response, Users.class);
  }

  public static class Builder {

    private List<User> value;
    private String odataContext;
    private String odataNextlink;
    private String odataDeltalink;

    public Builder setValue(List<User> value) {
      this.value = value;
      return this;
    }

    public Builder setOdataContext(String odataContext) {
      this.odataContext = odataContext;
      return this;
    }

    public Builder setOdataNextlink(String odataNextlink) {
      this.odataNextlink = odataNextlink;
      return this;
    }

    public Builder setOdataDeltalink(String odataDeltalink) {
      this.odataDeltalink = odataDeltalink;
      return this;
    }

    public Users build() {
      if (value == null) {
        value = Collections.emptyList();
      }
      return new Users(this);
    }
  }

  public String getOdataContext() {
    return odataContext;
  }

  @Override
  public List<User> getValue() {
    return value;
  }

  @Override
  public String getOdataNextlink() {
    return odataNextlink;
  }

  @Override
  public String getOdataDeltalink() {
    return odataDeltalink;
  }
}
