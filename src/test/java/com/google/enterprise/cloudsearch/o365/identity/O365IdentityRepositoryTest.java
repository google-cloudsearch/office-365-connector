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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudidentity.v1.model.EntityKey;
import com.google.api.services.cloudidentity.v1.model.Membership;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.enterprise.cloudsearch.o365.ApiConnection;
import com.google.enterprise.cloudsearch.o365.GetRequest;
import com.google.enterprise.cloudsearch.o365.Request;
import com.google.enterprise.cloudsearch.o365.Request.RequestHelper;
import com.google.enterprise.cloudsearch.o365.identity.O365IdentityRepository.O365RequestHelperFactory;
import com.google.enterprise.cloudsearch.o365.model.Group;
import com.google.enterprise.cloudsearch.o365.model.Groups;
import com.google.enterprise.cloudsearch.o365.model.Member;
import com.google.enterprise.cloudsearch.o365.model.Members;
import com.google.enterprise.cloudsearch.o365.model.User;
import com.google.enterprise.cloudsearch.o365.model.Users;
import com.google.enterprise.cloudsearch.sdk.CheckpointCloseableIterable;
import com.google.enterprise.cloudsearch.sdk.config.Configuration.ResetConfigRule;
import com.google.enterprise.cloudsearch.sdk.config.Configuration.SetupConfigRule;
import com.google.enterprise.cloudsearch.sdk.identity.IdentityGroup;
import com.google.enterprise.cloudsearch.sdk.identity.IdentityUser;
import com.google.enterprise.cloudsearch.sdk.identity.RepositoryContext;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link O365IdentityRepository} */
@RunWith(MockitoJUnitRunner.class)
public class O365IdentityRepositoryTest {
  @Rule public ExpectedException thrown = ExpectedException.none();
  @Rule public ResetConfigRule resetConfig = new ResetConfigRule();
  @Rule public SetupConfigRule setupConfig = SetupConfigRule.uninitialized();

  @Mock private RepositoryContext repositoryContext;

  @Test
  public void testInit() throws Exception {
    O365IdentityRepository identityRepository = new O365IdentityRepository();
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
  }

  @Test
  public void testListUsers() throws Exception {
    O365RequestHelperFactory spyRquestHelperFactory = spy(new O365RequestHelperFactory());
    O365IdentityRepository identityRepository = new O365IdentityRepository(spyRquestHelperFactory);
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
    RequestHelper<Users> usersRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Users.class))
        .thenReturn(usersRequestHelper);
    Request<Users> initialUsersRequest =
        new GetRequest.Builder<Users>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.USERS_ENDPOINT_FORMAT,
                        O365IdentityRepository.DEFAULT_USER_PAGINATION_SIZE)))
            .setResponseClass(Users.class)
            .build();
    User user1 =
        new User.Builder()
            .setId("id1")
            .setMail("user1@googledomain.com")
            .setUserPrincipalName("user1@o365domain.com")
            .build();
    doAnswer(invocation -> new Users.Builder().setValue(ImmutableList.of(user1)).build())
        .when(usersRequestHelper)
        .executeRequest(eq(initialUsersRequest), any());
    IdentityUser identityUser1 = new IdentityUser.Builder()
        .setGoogleIdentity("user1@googledomain.com")
        .setUserIdentity("user1@o365domain.com")
        .setSchema("schema")
        .setAttribute("attribute")
        .build();
    when(repositoryContext.buildIdentityUser("user1@googledomain.com", "user1@o365domain.com"))
        .thenReturn(
            identityUser1);
    CheckpointCloseableIterable<IdentityUser> listUsers = identityRepository.listUsers(null);
    assertEquals(ImmutableList.of(identityUser1), ImmutableList.copyOf(listUsers));
    assertNull(listUsers.getCheckpoint());
    assertFalse(listUsers.hasMore());
  }

  @Test
  public void testListUsersInvalidPayload() throws Exception {
    O365RequestHelperFactory spyRquestHelperFactory = spy(new O365RequestHelperFactory());
    O365IdentityRepository identityRepository = new O365IdentityRepository(spyRquestHelperFactory);
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
    RequestHelper<Users> usersRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Users.class))
        .thenReturn(usersRequestHelper);
    Request<Users> initialUsersRequest =
        new GetRequest.Builder<Users>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.USERS_ENDPOINT_FORMAT,
                        O365IdentityRepository.DEFAULT_USER_PAGINATION_SIZE)))
            .setResponseClass(Users.class)
            .build();
    User user1 =
        new User.Builder()
            .setId("id1")
            .setMail("user1@googledomain.com")
            .setUserPrincipalName("user1@o365domain.com")
            .build();
    doAnswer(invocation -> new Users.Builder().setValue(ImmutableList.of(user1)).build())
        .when(usersRequestHelper)
        .executeRequest(eq(initialUsersRequest), any());
    IdentityUser identityUser1 =
        new IdentityUser.Builder()
            .setGoogleIdentity("user1@googledomain.com")
            .setUserIdentity("user1@o365domain.com")
            .setSchema("schema")
            .setAttribute("attribute")
            .build();
    when(repositoryContext.buildIdentityUser("user1@googledomain.com", "user1@o365domain.com"))
        .thenReturn(identityUser1);
    CheckpointCloseableIterable<IdentityUser> listUsers =
        identityRepository.listUsers("invalid".getBytes(UTF_8));
    assertEquals(ImmutableList.of(identityUser1), ImmutableList.copyOf(listUsers));
    assertNull(listUsers.getCheckpoint());
    assertFalse(listUsers.hasMore());
  }

  @Test
  public void testListUsersMultiPages() throws Exception {
    O365RequestHelperFactory spyRquestHelperFactory = spy(new O365RequestHelperFactory());
    O365IdentityRepository identityRepository = new O365IdentityRepository(spyRquestHelperFactory);
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
    RequestHelper<Users> usersRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Users.class))
        .thenReturn(usersRequestHelper);
    Request<Users> initialUsersRequest =
        new GetRequest.Builder<Users>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.USERS_ENDPOINT_FORMAT,
                        O365IdentityRepository.DEFAULT_USER_PAGINATION_SIZE)))
            .setResponseClass(Users.class)
            .build();
    User user1 =
        new User.Builder()
            .setId("id1")
            .setMail("user1@googledomain.com")
            .setUserPrincipalName("user1@o365domain.com")
            .build();
    doAnswer(
            invocation ->
                new Users.Builder()
                    .setValue(ImmutableList.of(user1))
                    .setOdataNextlink(O365IdentityRepository.USERS_ENDPOINT_FORMAT + "#next")
                    .build())
        .when(usersRequestHelper)
        .executeRequest(eq(initialUsersRequest), any());
    IdentityUser identityUser1 =
        new IdentityUser.Builder()
            .setGoogleIdentity("user1@googledomain.com")
            .setUserIdentity("user1@o365domain.com")
            .setSchema("schema")
            .setAttribute("attribute")
            .build();
    when(repositoryContext.buildIdentityUser("user1@googledomain.com", "user1@o365domain.com"))
        .thenReturn(identityUser1);
    CheckpointCloseableIterable<IdentityUser> listUsers = identityRepository.listUsers(null);
    assertEquals(ImmutableList.of(identityUser1), ImmutableList.copyOf(listUsers));
    assertEquals(
        O365IdentityRepository.USERS_ENDPOINT_FORMAT + "#next",
        new String(listUsers.getCheckpoint(), UTF_8));
    assertTrue(listUsers.hasMore());
  }

  @Test
  public void testListUsersCheckpoint() throws Exception {
    byte[] checkpoint = (O365IdentityRepository.USERS_ENDPOINT_FORMAT + "#next").getBytes();
    O365RequestHelperFactory spyRquestHelperFactory = spy(new O365RequestHelperFactory());
    O365IdentityRepository identityRepository = new O365IdentityRepository(spyRquestHelperFactory);
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
    RequestHelper<Users> usersRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Users.class))
        .thenReturn(usersRequestHelper);
    Request<Users> initialUsersRequest =
        new GetRequest.Builder<Users>()
            .setRequestURL(new URL(O365IdentityRepository.USERS_ENDPOINT_FORMAT + "#next"))
            .setResponseClass(Users.class)
            .build();
    User user1 =
        new User.Builder()
            .setId("id1")
            .setMail("user1@googledomain.com")
            .setUserPrincipalName("user1@o365domain.com")
            .build();
    doAnswer(
            invocation ->
                new Users.Builder()
                    .setValue(ImmutableList.of(user1))
                    .build())
        .when(usersRequestHelper)
        .executeRequest(eq(initialUsersRequest), any());
    IdentityUser identityUser1 =
        new IdentityUser.Builder()
            .setGoogleIdentity("user1@googledomain.com")
            .setUserIdentity("user1@o365domain.com")
            .setSchema("schema")
            .setAttribute("attribute")
            .build();
    when(repositoryContext.buildIdentityUser("user1@googledomain.com", "user1@o365domain.com"))
        .thenReturn(identityUser1);
    CheckpointCloseableIterable<IdentityUser> listUsers = identityRepository.listUsers(checkpoint);
    assertEquals(ImmutableList.of(identityUser1), ImmutableList.copyOf(listUsers));
    assertNull(listUsers.getCheckpoint());
    assertFalse(listUsers.hasMore());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testListGroups() throws Exception {
    O365RequestHelperFactory spyRquestHelperFactory = spy(new O365RequestHelperFactory());
    O365IdentityRepository identityRepository = new O365IdentityRepository(spyRquestHelperFactory);
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
    RequestHelper<Groups> groupsRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Groups.class))
        .thenReturn(groupsRequestHelper);
    RequestHelper<Members> membersRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Members.class))
        .thenReturn(membersRequestHelper);
    Request<Groups> initialUsersRequest =
        new GetRequest.Builder<Groups>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.GROUPS_ENDPOINT_FORMAT,
                        O365IdentityRepository.DEFAULT_GROUP_PAGINATION_SIZE)))
            .setResponseClass(Groups.class)
            .build();
    Group group1 = new Group.Builder().setId("o365Group1").build();
    doAnswer(invocation -> new Groups.Builder().setValue(ImmutableList.of(group1)).build())
        .when(groupsRequestHelper)
        .executeRequest(eq(initialUsersRequest), any());

    Request<Members> membersRequest =
        new GetRequest.Builder<Members>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.MEMBERS_ENDPOINT_FORMAT,
                        "o365Group1",
                        O365IdentityRepository.DEFAULT_MEMBER_PAGINATION_SIZE)))
            .setResponseClass(Members.class)
            .build();
    Member memberUser =
        new Member.Builder()
            .setId("user1")
            .setMail("user1@googledomain.com")
            .setMemberType("#microsoft.graph.user")
            .build();
    Membership userMembership =
        new Membership()
            .setPreferredMemberKey(new EntityKey().setId("user1@googledomain.com"))
            .setRoles(O365IdentityRepository.MEMBER_ROLES);

    Member memberGroup =
        new Member.Builder().setId("o365Group2").setMemberType("#microsoft.graph.group").build();
    EntityKey groupMemberKey = new EntityKey().setId("o365Group2").setNamespace("ns");
    when(repositoryContext.buildEntityKeyForGroup("o365Group2")).thenReturn(groupMemberKey);
    Membership groupMembership =
        new Membership()
            .setPreferredMemberKey(groupMemberKey)
            .setRoles(O365IdentityRepository.MEMBER_ROLES);

    doAnswer(
            invocation ->
                new Members.Builder().setValue(ImmutableList.of(memberUser, memberGroup)).build())
        .when(membersRequestHelper)
        .executeRequest(eq(membersRequest), any());
    IdentityGroup identityGroup1 =
        new IdentityGroup.Builder()
            .setGroupIdentity("o365Group1")
            .setGroupKey(new EntityKey().setId("o365Group1").setNamespace("ns1"))
            .setMembers(ImmutableSet.of(userMembership, groupMembership))
            .build();
    doAnswer(
            invocation -> {
              assertEquals(
                  ImmutableSet.of(userMembership, groupMembership),
                  ((Supplier<Set<Membership>>) invocation.getArgument(1)).get());
              return identityGroup1;
            })
        .when(repositoryContext)
        .buildIdentityGroup(eq("o365Group1"), any());
    CheckpointCloseableIterable<IdentityGroup> listGroups = identityRepository.listGroups(null);
    assertEquals(ImmutableList.of(identityGroup1), ImmutableList.copyOf(listGroups));
    assertNull(listGroups.getCheckpoint());
    assertFalse(listGroups.hasMore());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testListGroupsInvalidPayload() throws Exception {
    O365RequestHelperFactory spyRquestHelperFactory = spy(new O365RequestHelperFactory());
    O365IdentityRepository identityRepository = new O365IdentityRepository(spyRquestHelperFactory);
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
    RequestHelper<Groups> groupsRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Groups.class))
        .thenReturn(groupsRequestHelper);
    RequestHelper<Members> membersRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Members.class))
        .thenReturn(membersRequestHelper);
    Request<Groups> initialUsersRequest =
        new GetRequest.Builder<Groups>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.GROUPS_ENDPOINT_FORMAT,
                        O365IdentityRepository.DEFAULT_GROUP_PAGINATION_SIZE)))
            .setResponseClass(Groups.class)
            .build();
    Group group1 = new Group.Builder().setId("o365Group1").build();
    doAnswer(invocation -> new Groups.Builder().setValue(ImmutableList.of(group1)).build())
        .when(groupsRequestHelper)
        .executeRequest(eq(initialUsersRequest), any());

    Request<Members> membersRequest =
        new GetRequest.Builder<Members>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.MEMBERS_ENDPOINT_FORMAT,
                        "o365Group1",
                        O365IdentityRepository.DEFAULT_MEMBER_PAGINATION_SIZE)))
            .setResponseClass(Members.class)
            .build();
    Member memberUser =
        new Member.Builder()
            .setId("user1")
            .setMail("user1@googledomain.com")
            .setMemberType("#microsoft.graph.user")
            .build();
    Membership userMembership =
        new Membership()
            .setPreferredMemberKey(new EntityKey().setId("user1@googledomain.com"))
            .setRoles(O365IdentityRepository.MEMBER_ROLES);

    Member memberGroup =
        new Member.Builder().setId("o365Group2").setMemberType("#microsoft.graph.group").build();
    EntityKey groupMemberKey = new EntityKey().setId("o365Group2").setNamespace("ns");
    when(repositoryContext.buildEntityKeyForGroup("o365Group2")).thenReturn(groupMemberKey);
    Membership groupMembership =
        new Membership()
            .setPreferredMemberKey(groupMemberKey)
            .setRoles(O365IdentityRepository.MEMBER_ROLES);

    doAnswer(
            invocation ->
                new Members.Builder().setValue(ImmutableList.of(memberUser, memberGroup)).build())
        .when(membersRequestHelper)
        .executeRequest(eq(membersRequest), any());
    IdentityGroup identityGroup1 =
        new IdentityGroup.Builder()
            .setGroupIdentity("o365Group1")
            .setGroupKey(new EntityKey().setId("o365Group1").setNamespace("ns1"))
            .setMembers(ImmutableSet.of(userMembership, groupMembership))
            .build();
    doAnswer(
            invocation -> {
              assertEquals(
                  ImmutableSet.of(userMembership, groupMembership),
                  ((Supplier<Set<Membership>>) invocation.getArgument(1)).get());
              return identityGroup1;
            })
        .when(repositoryContext)
        .buildIdentityGroup(eq("o365Group1"), any());
    CheckpointCloseableIterable<IdentityGroup> listGroups =
        identityRepository.listGroups("invalid".getBytes(UTF_8));
    assertEquals(ImmutableList.of(identityGroup1), ImmutableList.copyOf(listGroups));
    assertNull(listGroups.getCheckpoint());
    assertFalse(listGroups.hasMore());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testListGroupsWithPagination() throws Exception {
    O365RequestHelperFactory spyRquestHelperFactory = spy(new O365RequestHelperFactory());
    O365IdentityRepository identityRepository = new O365IdentityRepository(spyRquestHelperFactory);
    setupBaseConfiguration();
    identityRepository.init(repositoryContext);
    RequestHelper<Groups> groupsRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Groups.class))
        .thenReturn(groupsRequestHelper);
    RequestHelper<Members> membersRequestHelper = spy(new RequestHelper<>());
    when(spyRquestHelperFactory.getO365RequestHelper(Members.class))
        .thenReturn(membersRequestHelper);
    Request<Groups> initialUsersRequest =
        new GetRequest.Builder<Groups>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.GROUPS_ENDPOINT_FORMAT,
                        O365IdentityRepository.DEFAULT_GROUP_PAGINATION_SIZE)))
            .setResponseClass(Groups.class)
            .build();
    Group group1 = new Group.Builder().setId("o365Group1").build();
    doAnswer(
            invocation ->
                new Groups.Builder()
                    .setValue(ImmutableList.of(group1))
                    .setOdataNextlink(O365IdentityRepository.GROUPS_ENDPOINT_FORMAT + "#NEXT")
                    .build())
        .when(groupsRequestHelper)
        .executeRequest(eq(initialUsersRequest), any());

    Request<Members> membersRequest =
        new GetRequest.Builder<Members>()
            .setRequestURL(
                new URL(
                    String.format(
                        O365IdentityRepository.MEMBERS_ENDPOINT_FORMAT,
                        "o365Group1",
                        O365IdentityRepository.DEFAULT_MEMBER_PAGINATION_SIZE)))
            .setResponseClass(Members.class)
            .build();
    Member memberUser =
        new Member.Builder()
            .setId("user1")
            .setMail("user1@googledomain.com")
            .setMemberType("#microsoft.graph.user")
            .build();
    Membership userMembership =
        new Membership()
            .setPreferredMemberKey(new EntityKey().setId("user1@googledomain.com"))
            .setRoles(O365IdentityRepository.MEMBER_ROLES);

    Member memberGroup =
        new Member.Builder().setId("o365Group2").setMemberType("#microsoft.graph.group").build();
    EntityKey groupMemberKey = new EntityKey().setId("o365Group2").setNamespace("ns");
    when(repositoryContext.buildEntityKeyForGroup("o365Group2")).thenReturn(groupMemberKey);
    Membership groupMembership =
        new Membership()
            .setPreferredMemberKey(groupMemberKey)
            .setRoles(O365IdentityRepository.MEMBER_ROLES);

    doAnswer(
            invocation ->
                new Members.Builder().setValue(ImmutableList.of(memberUser, memberGroup)).build())
        .when(membersRequestHelper)
        .executeRequest(eq(membersRequest), any());
    IdentityGroup identityGroup1 =
        new IdentityGroup.Builder()
            .setGroupIdentity("o365Group1")
            .setGroupKey(new EntityKey().setId("o365Group1").setNamespace("ns1"))
            .setMembers(ImmutableSet.of(userMembership, groupMembership))
            .build();
    doAnswer(
            invocation -> {
              assertEquals(
                  ImmutableSet.of(userMembership, groupMembership),
                  ((Supplier<Set<Membership>>) invocation.getArgument(1)).get());
              return identityGroup1;
            })
        .when(repositoryContext)
        .buildIdentityGroup(eq("o365Group1"), any());
    CheckpointCloseableIterable<IdentityGroup> listGroups = identityRepository.listGroups(null);
    assertEquals(ImmutableList.of(identityGroup1), ImmutableList.copyOf(listGroups));
    assertEquals(
        O365IdentityRepository.GROUPS_ENDPOINT_FORMAT + "#NEXT",
        new String(listGroups.getCheckpoint(), UTF_8));
    assertTrue(listGroups.hasMore());
  }

  private void setupBaseConfiguration() {
    Properties config = new Properties();
    config.put(ApiConnection.CLIENT_ID, "client id");
    config.put(ApiConnection.CLIENT_SECRET, "secret");
    config.put(ApiConnection.TENANT, "tenant");
    setupConfig.initConfig(config);
  }
}
