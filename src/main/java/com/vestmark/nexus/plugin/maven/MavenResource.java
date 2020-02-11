/*
 * Copyright 2018 Vestmark, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express 
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vestmark.nexus.plugin.maven;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.net.HttpHeaders;

@Named
@Singleton
@Path("/maven/")
@SuppressWarnings("unchecked")
public class MavenResource
    extends BaseMavenResource
{

  private static final Response invalidResponse = Response.status(400).build();
  private static final Response notFoundResponse = Response.status(404).build();

  private static final String LATEST = "LATEST";
  private static final String MAVEN2_EXTENSION = "extension";
  private static final String ASSET_NAME = "name";
  private static final String BLOBSTORE_CONTENT_TYPE = "BlobStore.content-type";

  @Inject
  public MavenResource(SearchService searchService, RepositoryManager repositoryManager)
  {
    super(searchService, repositoryManager);
  }

  @GET
  @Path("download")
  public Response download(
      @QueryParam("r") String repositoryName,
      @QueryParam("g") String groupId,
      @QueryParam("a") String artifactId,
      @QueryParam("v") String version,
      @QueryParam("c") String classifier,
      @QueryParam("e") @DefaultValue("jar") String extension)
  {
    if (StringUtils.isBlank(repositoryName) || StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId)
        || StringUtils.isBlank(version)) {
      log.warn("Missing required parameter(s): r={}, g={}, a={}, v={}", repositoryName, groupId, artifactId, version);
      return invalidResponse;
    }

    if (LATEST.equalsIgnoreCase(version)) {
      List<MavenVersion> versions = versions(1, repositoryName, groupId, artifactId, classifier, extension);
      if (versions.isEmpty()) {
        return notFoundResponse;
      }
      version = versions.get(0).getBaseVersion();
    }

    if (log.isDebugEnabled()) {
      log.debug("download version: {}", version);
    }

    Repository repository = repositoryManager.get(repositoryName);
    if (null == repository || !MAVEN2.equals(repository.getFormat().getValue())) {
      log.warn("Repository supplied: {} is not a maven repo", repositoryName);
      return invalidResponse;
    }

    StorageFacet facet = repository.facet(StorageFacet.class);
    Supplier<StorageTx> storageTxSupplier = facet.txSupplier();
    StorageTx storageTx = null;
    try {
      storageTx = storageTxSupplier.get();
      storageTx.begin();
      SearchResponse searchResponse = searchMavenArtifacts(
          repositoryName,
          groupId,
          artifactId,
          classifier,
          extension,
          version,
          1);

      if (searchResponse.getHits().getTotalHits() == 0) {
        return notFoundResponse;
      }

      SearchHit hit = searchResponse.getHits().getAt(0);
      List<Map<String, Object>> assetList = (List<Map<String, Object>>) hit.getSource().get(ASSETS);
      String assetName = null;
      for (Map<String, Object> asset : assetList) {
        Map<String, Object> attributes = (Map<String, Object>) asset.get(ATTRIBUTES);
        Map<String, Object> maven2 = (Map<String, Object>) attributes.get(MAVEN2);
        String assetExtension = (String) maven2.get(MAVEN2_EXTENSION);
        if (extension.equals(assetExtension)) {
          assetName = (String) asset.get(ASSET_NAME);
        }
      }

      if (log.isDebugEnabled()) {
        log.debug("download asset name: {}", assetName);
      }

      if (assetName == null) {
        return notFoundResponse;
      }

      List<Repository> repos = Collections.singletonList(repository);
      GroupFacet groupFacet = repository.optionalFacet(GroupFacet.class).orElse(null);
      if (groupFacet != null) {
        repos = groupFacet.allMembers();
      }
      if (log.isDebugEnabled()) {
        log.debug("download repositories: {}", repos);
      }

      Query.Builder builder = Query.builder().where(ASSET_NAME).eq(assetName);
      List<Asset> assets = StreamSupport.stream(storageTx.findAssets(builder.build(), repos).spliterator(), false)
          .collect(Collectors.toList());

      if (log.isDebugEnabled()) {
        log.debug("download assets found: {}", assets);
      }

      if (assets.isEmpty()) {
        return notFoundResponse;
      }

      Asset asset = assets.get(0);

      if (log.isDebugEnabled()) {
        log.debug("download asset: {}", assets);
      }
      if (null == asset) {
        return notFoundResponse;
      }

      asset.markAsDownloaded();
      storageTx.saveAsset(asset);
      Blob blob = storageTx.requireBlob(asset.requireBlobRef());
      Response.ResponseBuilder response = Response.ok(blob.getInputStream());
      response.header(HttpHeaders.CONTENT_TYPE, blob.getHeaders().get(BLOBSTORE_CONTENT_TYPE));
      String fileName = String.format(
          "%s-%s%s.%s",
          artifactId,
          version,
          StringUtils.isBlank(classifier) ? "" : "-" + classifier,
          extension);
      response.header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment;filename=\"%s\"", fileName));
      return response.build();
    }
    catch (Exception e) {
      log.debug("Exception: {}", e.toString());
      storageTx.rollback();
      return null;
    }
    finally {
      if (storageTx != null) {
        if (storageTx.isActive()) {
          storageTx.commit();
        }
        storageTx.close();
      }
    }
  }

  @GET
  @Path("versions")
  @Produces(MediaType.APPLICATION_JSON)
  public List<MavenVersion> versions(
      @QueryParam("l") @DefaultValue("10") int limit,
      @QueryParam("r") String repository,
      @QueryParam("g") String groupId,
      @QueryParam("a") String artifactId,
      @QueryParam("c") String classifier,
      @QueryParam("e") String extension)
  {
    return super.listVersions(limit, repository, groupId, artifactId, classifier, extension);
  }
}
