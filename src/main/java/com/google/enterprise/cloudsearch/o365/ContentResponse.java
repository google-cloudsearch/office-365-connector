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
package com.google.enterprise.cloudsearch.o365;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpResponse;
import com.google.enterprise.cloudsearch.o365.Request.RequestHelper;

/** Construct to hold response for {@link RequestHelper#executeContentRequest}. */
public class ContentResponse {
  private AbstractInputStreamContent content;
  private String contentType;

  private ContentResponse(ContentResponse.Builder builder) {
    this.content = builder.content;
    this.contentType = builder.contentType;
  }

  /**
   * Get content extracted from {@link HttpResponse} in response to {@link
   * RequestHelper#executeContentRequest}.
   *
   * @return content for extracted from {@link HttpResponse}
   */
  public AbstractInputStreamContent getContent() {
    return content;
  }

  /**
   * Get content type for {@link HttpResponse}
   *
   * @return content type for {@link HttpResponse}
   */
  public String getContentType() {
    return contentType;
  }

  /** Builder for {@link ContentResponse}. */
  public static class Builder {
    private AbstractInputStreamContent content;
    private String contentType;

    public ContentResponse.Builder setContent(
        AbstractInputStreamContent content) {
      this.content = content;
      return this;
    }

    public ContentResponse.Builder setContentType(
        String contentType) {
      this.contentType = contentType;
      return this;
    }

    public ContentResponse build() {
      checkNotNull(contentType, "content type can not be null");
      checkNotNull(content, "content can not be null");
      return new ContentResponse(this);
    }
  }
}