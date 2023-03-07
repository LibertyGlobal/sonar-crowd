/*
 * SonarQube Crowd Plugin
 * Copyright (C) 2023 Liberty Global
 * mailto:info AT libertyglobal DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.atlassian.crowd.integration.rest.service;

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.integration.rest.entity.ErrorEntity;
import com.atlassian.crowd.service.client.AuthenticationMethod;
import com.atlassian.crowd.service.client.ClientProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import javax.xml.bind.DataBindingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * @author Esteban Lopez Valecky
 */
public class JacksonBasicAuthRestExecutor extends BasicAuthRestExecutor {

  private final String baseUrl;

  private static final ObjectMapper mapper = new XmlMapper()
    .registerModule(new JaxbAnnotationModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static JacksonBasicAuthRestExecutor createFrom(ClientProperties clientProperties, CloseableHttpClient httpClient) {
    Preconditions.checkArgument(clientProperties.getAuthenticationMethod() == AuthenticationMethod.BASIC_AUTH, "Client properties should specify Basic auth as the authentication method");
    String baseUrl = createBaseUrl(clientProperties.getBaseURL());
    HttpHost httpHost = createHttpHost(clientProperties);
    CredentialsProvider credsProvider = createCredentialsProvider(clientProperties);
    Preconditions.checkNotNull(clientProperties.getApplicationName(), "Missing required Crowd client application name");
    Preconditions.checkNotNull(clientProperties.getApplicationPassword(), "Missing required Crowd client application password");
    credsProvider.setCredentials(new AuthScope(httpHost), new UsernamePasswordCredentials(clientProperties.getApplicationName(), clientProperties.getApplicationPassword()));
    return new JacksonBasicAuthRestExecutor(baseUrl, httpHost, credsProvider, httpClient);
  }

  JacksonBasicAuthRestExecutor(String baseUrl, HttpHost httpHost, CredentialsProvider credsProvider, CloseableHttpClient client) {
    super(baseUrl, httpHost, credsProvider, client);
    this.baseUrl = Preconditions.checkNotNull(baseUrl);
  }

  @Override
  protected MethodExecutor createMethodExecutor(HttpUriRequest request, Set<Integer> statusCodesWithoutErrorEntity) {
    return new JacksonMethodExecutor(request, statusCodesWithoutErrorEntity);
  }

  @Override
  MethodExecutor post(Object body, String format, Object... args) {
    HttpPost method = new HttpPost(buildUrl(this.baseUrl, format, args));
    this.setBody(method, body);
    return this.createMethodExecutor(method);
  }

  @Override
  MethodExecutor put(Object body, String format, Object... args) {
    HttpPut method = new HttpPut(buildUrl(this.baseUrl, format, args));
    this.setBody(method, body);
    return this.createMethodExecutor(method);
  }

  private void setBody(HttpEntityEnclosingRequestBase method, Object body) {
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    try {
      mapper.writeValue(bs, body);
    } catch (IOException e) {
      throw new DataBindingException("Cannot marshall object " + body, e);
    }
    method.setEntity(new ByteArrayEntity(bs.toByteArray(), ContentType.APPLICATION_XML));
  }

  class JacksonMethodExecutor extends MethodExecutor {

    protected JacksonMethodExecutor(HttpUriRequest request, Set<Integer> statusCodesWithoutErrorEntity) {
      super(request, statusCodesWithoutErrorEntity);
    }

    @Override
    <T> T andReceive(Class<T> returnType) throws ApplicationPermissionException, InvalidAuthenticationException, OperationFailedException, CrowdRestException {
      this.request.setHeader("Accept", "application/xml");
      this.request.setHeader("X-Atlassian-Token", "no-check");

      try {
        int statusCode = this.executeCrowdServiceMethod();
        if (!isSuccessRange(statusCode)) {
          this.throwError(statusCode);
          throw new OperationFailedException(this.response.getStatusLine().getReasonPhrase());
        }
        return mapper.readValue(this.response.getEntity().getContent(), returnType);
      } catch (IOException e) {
        throw new OperationFailedException(e);
      } finally {
        if (this.response != null) {
          EntityUtils.consumeQuietly(this.response.getEntity());
        }
      }
    }

    private boolean isSuccessRange(int statusCode) {
      return statusCode >= 200 && statusCode < 300;
    }

    @Override
    void throwError(int errorCode) throws ApplicationPermissionException, InvalidAuthenticationException, OperationFailedException, CrowdRestException {
      String reasonPhrase;
      if (Strings.isNullOrEmpty(this.response.getStatusLine().getReasonPhrase())) {
        reasonPhrase = EnglishReasonPhraseCatalog.INSTANCE.getReason(this.response.getStatusLine().getStatusCode(), Locale.getDefault());
      } else {
        reasonPhrase = this.response.getStatusLine().getReasonPhrase();
      }

      try {
        if (errorCode == 403) {
          throw new ApplicationPermissionException(RestExecutor.getExceptionMessageFromResponse(this.response));
        }

        if (errorCode == 401) {
          throw new InvalidAuthenticationException(RestExecutor.getExceptionMessageFromResponse(this.response));
        }

        if (errorCode >= 300) {
          if (this.statusCodesWithoutErrorEntity.contains(errorCode)) {
            throw new CrowdRestException("HTTP error: " + errorCode + " " + reasonPhrase + ". Response body: " + EntityUtils.toString(this.response.getEntity()), (ErrorEntity)null, errorCode);
          }
          ErrorEntity errorEntity = mapper.readValue(this.response.getEntity().getContent(), ErrorEntity.class);
          throw new CrowdRestException(errorEntity.getMessage(), errorEntity, errorCode);
        }
      } catch (IOException e) {
        throw new OperationFailedException(reasonPhrase);
      } catch (DataBindingException e) {
        throw new OperationFailedException(reasonPhrase, e);
      } finally {
        EntityUtils.consumeQuietly(this.response.getEntity());
      }
    }
  }
}
