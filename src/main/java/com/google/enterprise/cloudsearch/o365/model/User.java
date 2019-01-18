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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import java.io.IOException;

/** This class represents User object get from Microsoft graph API */
public class User extends DirectoryObject {
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  @Key private String mail;
  @Key private String userPrincipalName;
  @Key private boolean accountEnabled;

  public User() {
    super();
    setFactory(JSON_FACTORY);
  }

  public User(Builder builder) {
    this();
    this.mail = builder.mail;
    this.userPrincipalName = builder.userPrincipalName;
    this.accountEnabled = builder.accountEnabled;
    super.setId(builder.id);
  }

  @Override
  public boolean isValid() {
    return getId() != null && !getId().isEmpty();
  }

  public static User parse(String user) throws IOException {
    return JSON_FACTORY.fromString(user, User.class);
  }

  public static class Builder {

    private String mail;
    private String userPrincipalName;
    private boolean accountEnabled;
    private String id;

    public Builder setMail(String mail) {
      this.mail = mail;
      return this;
    }

    public Builder setUserPrincipalName(String userPrincipalName) {
      this.userPrincipalName = userPrincipalName;
      return this;
    }

    public Builder setAccountEnabled(boolean accountEnabled) {
      this.accountEnabled = accountEnabled;
      return this;
    }

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public User build() {
      checkNotNull(id, "user id can not be null");
      return new User(this);
    }
  }

  public String getMail() {
    return mail;
  }

  public String getUserPrincipalName() {
    return userPrincipalName;
  }

  public boolean isAccountEnabled() {
    return accountEnabled;
  }
}
