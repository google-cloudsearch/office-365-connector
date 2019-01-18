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
package com.google.enterprise.cloudsearch.o365.identity;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.api.client.json.GenericJson;
import com.google.api.services.cloudidentity.v1beta1.model.EntityKey;
import com.google.api.services.cloudidentity.v1beta1.model.Membership;
import com.google.api.services.cloudidentity.v1beta1.model.MembershipRole;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.enterprise.cloudsearch.o365.GetRequest;
import com.google.enterprise.cloudsearch.o365.HttpClient;
import com.google.enterprise.cloudsearch.o365.Request;
import com.google.enterprise.cloudsearch.o365.Request.RequestHelper;
import com.google.enterprise.cloudsearch.o365.model.Group;
import com.google.enterprise.cloudsearch.o365.model.Groups;
import com.google.enterprise.cloudsearch.o365.model.Member;
import com.google.enterprise.cloudsearch.o365.model.Members;
import com.google.enterprise.cloudsearch.o365.model.User;
import com.google.enterprise.cloudsearch.o365.model.Users;
import com.google.enterprise.cloudsearch.sdk.CheckpointCloseableIterable;
import com.google.enterprise.cloudsearch.sdk.CheckpointCloseableIterableImpl;
import com.google.enterprise.cloudsearch.sdk.PaginationIterable;
import com.google.enterprise.cloudsearch.sdk.config.Configuration;
import com.google.enterprise.cloudsearch.sdk.identity.IdentityGroup;
import com.google.enterprise.cloudsearch.sdk.identity.IdentityUser;
import com.google.enterprise.cloudsearch.sdk.identity.Repository;
import com.google.enterprise.cloudsearch.sdk.identity.RepositoryContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

class O365IdentityRepository implements Repository {
  private static final Logger logger = Logger.getLogger(O365IdentityRepository.class.getName());
  private static final String USER_PAGINATION_SIZE_CONFIG = "o365.userPaginationSize";
  private static final String GROUP_PAGINATION_SIZE_CONFIG = "o365.groupPaginationSize";
  private static final String MEMBER_PAGINATION_SIZE_CONFIG = "o365.memberPaginationSize";
  static final int DEFAULT_USER_PAGINATION_SIZE = 50;
  static final int DEFAULT_GROUP_PAGINATION_SIZE = 50;
  static final int DEFAULT_MEMBER_PAGINATION_SIZE = 50;

  static final String USERS_ENDPOINT_FORMAT = "https://graph.microsoft.com/v1.0/users?$top=%s";
  static final String GROUPS_ENDPOINT_FORMAT = "https://graph.microsoft.com/v1.0/groups?$top=%s";
  static final String MEMBERS_ENDPOINT_FORMAT =
      "https://graph.microsoft.com/v1.0/groups/%s/members?$top=%s";
  static final ImmutableList<MembershipRole> MEMBER_ROLES =
      ImmutableList.of(new MembershipRole().setName("MEMBER"));

  private final O365RequestHelperFactory requestHelperFactory;

  private HttpClient requestHttpClient;
  private RepositoryContext repositoryContext;
  private URL usersEndpoint;
  private URL groupsEndpoint;
  private int membersPageSize;

  O365IdentityRepository() {
    this(new O365RequestHelperFactory());
  }

  O365IdentityRepository(O365RequestHelperFactory requestHelperFactory) {
    this.requestHelperFactory =
        checkNotNull(requestHelperFactory, "requestHelperFactory can not be null");
  }

  @Override
  public void init(RepositoryContext context) throws IOException {
    this.repositoryContext = checkNotNull(context, "repository context can not be null");
    requestHttpClient = HttpClient.fromConfiguration();
    int userPageSize =
        getPaginationSize(USER_PAGINATION_SIZE_CONFIG, DEFAULT_USER_PAGINATION_SIZE, "user");
    usersEndpoint = new URL(String.format(USERS_ENDPOINT_FORMAT, userPageSize));
    int groupsPageSize =
        getPaginationSize(GROUP_PAGINATION_SIZE_CONFIG, DEFAULT_GROUP_PAGINATION_SIZE, "group");
    groupsEndpoint = new URL(String.format(GROUPS_ENDPOINT_FORMAT, groupsPageSize));
    membersPageSize =
        getPaginationSize(MEMBER_PAGINATION_SIZE_CONFIG, DEFAULT_MEMBER_PAGINATION_SIZE, "member");
  }

  @Override
  public CheckpointCloseableIterable<IdentityUser> listUsers(byte[] checkpoint) throws IOException {
    return getUsersForCheckpoint(parseCheckpoint(checkpoint, usersEndpoint));
  }

  @Override
  public CheckpointCloseableIterable<IdentityGroup> listGroups(byte[] checkpoint)
      throws IOException {
    return getGroupsForCheckpoint(parseCheckpoint(checkpoint, groupsEndpoint));
  }

  @Override
  public void close() {
  }

  private static int getPaginationSize(String configKey, int defaultValue, String kind) {
    int pageSize = Configuration.getInteger(configKey, defaultValue).get();
    Configuration.checkConfiguration(
        pageSize > 0,
        "Invalid %s pagination size [%s] for configuration key [%s]",
        kind,
        pageSize,
        configKey);
    return pageSize;
  }

  private CheckpointCloseableIterable<IdentityUser> getUsersForCheckpoint(URL url)
      throws IOException {
    Request<Users> allUsers =
        new GetRequest.Builder<Users>()
            .setRequestURL(url)
            .setResponseClass(Users.class)
            .build();
    RequestHelper<Users> requestHelper =
        requestHelperFactory.getO365RequestHelper(Users.class);
    Users users = requestHelper.executeRequest(allUsers, requestHttpClient);
    List<User> fetchedUsers =
        (users == null) || (users.getValue() == null) ? Collections.emptyList() : users.getValue();
    List<IdentityUser> identityUsers =
        fetchedUsers
            .stream()
            .filter(Objects::nonNull)
            .map(u -> convertToIdentityUser(u))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    byte[] checkpoint =
        Optional.ofNullable(users)
            .map(g -> g.getOdataNextlink())
            .map(s -> s.getBytes(UTF_8))
            .orElse(null);
    return new CheckpointCloseableIterableImpl.Builder<>(identityUsers)
        .setCheckpoint(checkpoint)
        .setHasMore(checkpoint != null)
        .build();
  }

  private CheckpointCloseableIterable<IdentityGroup> getGroupsForCheckpoint(URL url)
      throws IOException {
    Request<Groups> allGroups =
        new GetRequest.Builder<Groups>()
            .setRequestURL(url)
            .setResponseClass(Groups.class)
            .build();
    RequestHelper<Groups> requestHelper =
        requestHelperFactory.getO365RequestHelper(Groups.class);
    RequestHelper<Members> membersRequestHelper =
        requestHelperFactory.getO365RequestHelper(Members.class);
    Groups groups = requestHelper.executeRequest(allGroups, requestHttpClient);
    List<Group> fetchedGroups =
        (groups == null) || (groups.getValue() == null)
            ? Collections.emptyList()
            : groups.getValue();
    List<IdentityGroup> identityGroups =
        fetchedGroups
            .stream()
            .filter(Objects::nonNull)
            .map(g -> convertToIdentityGroup(g, membersRequestHelper))
            .collect(Collectors.toList());
    byte[] checkpoint =
        Optional.ofNullable(groups)
            .map(g -> g.getOdataNextlink())
            .map(s -> s.getBytes(UTF_8))
            .orElse(null);

    return new CheckpointCloseableIterableImpl.Builder<>(identityGroups)
        .setCheckpoint(checkpoint)
        .setHasMore(checkpoint != null)
        .build();
  }

  private IdentityUser convertToIdentityUser(User u) {
    if (Strings.isNullOrEmpty(u.getMail()) || Strings.isNullOrEmpty(u.getUserPrincipalName())) {
      logger.log(Level.WARNING, "Skipping invalid User [{0}].", u);
      return null;
    }

    return repositoryContext.buildIdentityUser(u.getMail(), u.getUserPrincipalName());
  }

  private IdentityGroup convertToIdentityGroup(
      Group g, RequestHelper<Members> requestHelper) {
    String groupId = g.getId();
    String membersEndpoint = String.format(MEMBERS_ENDPOINT_FORMAT, groupId, membersPageSize);
    Iterable<Member> members =
        new MembersIterable(Optional.of(membersEndpoint), requestHttpClient, requestHelper);
    Iterable<Member> filteredMembers =
        Iterables.filter(members, m -> isValidMember(m));
    Iterable<Membership> memberships =
        Iterables.transform(
            filteredMembers,
            new Function<Member, Membership>() {
              @Override
              @Nullable
              public Membership apply(@Nullable Member input) {
                checkNotNull(input);
                EntityKey memberKey =
                    input.isUser()
                        ? new EntityKey().setId(input.getMail())
                        : repositoryContext.buildEntityKeyForGroup(input.getId());
                return new Membership().setMemberKey(memberKey).setRoles(MEMBER_ROLES);
              }
            });
    return repositoryContext.buildIdentityGroup(
        groupId, () -> ImmutableSet.<Membership>builder().addAll(memberships).build());
  }

  private static boolean isValidMember(Member member) {
    if (member == null) {
      return false;
    }
    if (member.isUser()) {
      if (Strings.isNullOrEmpty(member.getMail())) {
        logger.log(Level.WARNING, "Skipping invalid member User [{0}].", member);
        return false;
      }
      return true;
    }
    return member.isGroup();
  }

  private static URL parseCheckpoint(byte[] checkpoint, URL defaultUrl) {
    if (checkpoint == null) {
      return defaultUrl;
    }
    String checkpointString = new String(checkpoint, UTF_8);
    try {
      return new URL(checkpointString);
    } catch (MalformedURLException e) {
      logger.log(
          Level.WARNING,
          String.format(
              "Failed to parse checkpoint [%s]. Resetting checkpoint to default [%s].",
              checkpointString, defaultUrl),
          e);
      return defaultUrl;
    }
  }

  private static class MembersIterable extends PaginationIterable<Member, String> {
    private final HttpClient requestHttpClient;
    private final RequestHelper<Members> requestHelper;

    public MembersIterable(
        Optional<String> startPage,
        HttpClient requestHttpClient,
        RequestHelper<Members> requestHelper) {
      super(startPage);
      this.requestHttpClient = requestHttpClient;
      this.requestHelper = requestHelper;
    }

    @Override
    public Page<Member, String> getPage(Optional<String> nextPage) throws IOException {
      Request<Members> allMembers =
          new GetRequest.Builder<Members>()
              .setRequestURL(new URL(nextPage.get()))
              .setResponseClass(Members.class)
              .build();
      Members members = requestHelper.executeRequest(allMembers, requestHttpClient);
      List<Member> membersToReturn = members.getValue();
      return new Page<>(
          membersToReturn == null ? Collections.emptyList() : membersToReturn,
          Optional.ofNullable(members.getOdataNextlink()));
    }
  }

  // TODO(tvartak): Move to common utility class to be shared by multiple connectors.
  static class O365RequestHelperFactory {
    <T extends GenericJson> RequestHelper<T> getO365RequestHelper(
        @SuppressWarnings("unused") Class<T> responseClass) {
      return new RequestHelper<T>();
    }
  }
}
