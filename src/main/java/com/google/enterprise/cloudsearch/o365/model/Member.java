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
import com.google.common.base.Strings;
import java.io.IOException;

/**
 * This class represents an individual member under a group. Group membership can be fetched using
 * Microsoft Graph API.
 * Request URL: https://graph.microsoft.com/v1.0/groups/{group_id}/members.
 */
public class Member extends DirectoryObject {
  public static final String MEMBER_TYPE_USER = "#microsoft.graph.user";
  public static final String MEMBER_TYPE_GROUP = "#microsoft.graph.group";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

  @Key("@odata.type")
  private String memberType;
  @Key private String displayName;
  @Key private String mail;
  @Key private String userPrincipalName;

  public Member() {
    super();
    setFactory(JSON_FACTORY);
  }

  public Member(Builder builder) {
    this();
    this.displayName = builder.displayName;
    this.mail = builder.mail;
    this.userPrincipalName = builder.userPrincipalName;
    this.memberType = builder.memberType;
    super.setId(builder.id);
  }

  @Override
  public boolean isValid() {
    return !Strings.isNullOrEmpty(getId());
  }

  public static Member parse(String user) throws IOException {
    return JSON_FACTORY.fromString(user, Member.class);
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getMail() {
    return mail;
  }

  public String getUserPrincipalName() {
    return userPrincipalName;
  }

  public String getMemberType() {
    return memberType;
  }

  public boolean isUser() {
    return MEMBER_TYPE_USER.equals(memberType);
  }

  public boolean isGroup() {
    return MEMBER_TYPE_GROUP.equals(memberType);
  }

  public static class Builder {
    private String memberType;
    private String displayName;
    private String mail;
    private String userPrincipalName;
    private String id;

    public Builder setDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder setMail(String mail) {
      this.mail = mail;
      return this;
    }

    public Builder setUserPrincipalName(String userPrincipalName) {
      this.userPrincipalName = userPrincipalName;
      return this;
    }

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setMemberType(String memberType) {
      this.memberType = memberType;
      return this;
    }

    public Member build() {
      checkNotNull(id, "member id can not be null");
      return new Member(this);
    }
  }
}
