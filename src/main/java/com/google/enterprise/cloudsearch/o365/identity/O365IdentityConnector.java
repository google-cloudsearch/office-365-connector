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

import com.google.enterprise.cloudsearch.sdk.identity.FullSyncIdentityConnector;
import com.google.enterprise.cloudsearch.sdk.identity.IdentityApplication;

/** Entry point for connector syncing identities from O365. */
public class O365IdentityConnector {
  public static void main(String[] args) throws InterruptedException {
    IdentityApplication application =
        new IdentityApplication.Builder(
                new FullSyncIdentityConnector(new O365IdentityRepository()), args)
            .build();
    application.start();
  }
}
