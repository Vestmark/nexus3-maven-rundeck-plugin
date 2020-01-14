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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.rest.Resource;

@SuppressWarnings("unchecked")
public abstract class BaseMavenResource
    extends ComponentSupport
    implements Resource
{

  protected static final String MAVEN2 = "maven2";
  protected static final String MAVEN2_BASE_VERSION = "baseVersion";
  protected static final String ASSETS = "assets";
  protected static final String ATTRIBUTES = "attributes";
  protected static final String CONTENT = "content";
  protected static final String LAST_MODIFIED = "last_modified";

  protected final SearchService searchService;
  protected final RepositoryManager repositoryManager;

  protected BaseMavenResource(SearchService searchService, RepositoryManager repositoryManager)
  {
    this.searchService = searchService;
    this.repositoryManager = repositoryManager;
  }

  protected SearchResponse searchMavenArtifacts(
      String repository,
      String groupId,
      String artifactId,
      String classifier,
      String extension,
      String baseVersion,
      int limit)
  {
    if (log.isDebugEnabled()) {
      log.debug(
          "searchMavenArtifacts: repository: {}, limit: {}, groupId: {}, artifactId: {}, classifier: {}, extension: {},  baseVersion: {}",
          repository,
          limit,
          groupId,
          artifactId,
          classifier,
          extension,
          baseVersion);
    }

    List<Repository> repos = Collections.emptyList();
    if (StringUtils.isNotBlank(repository)) {
      Repository repo = repositoryManager.get(repository);
      GroupFacet groupFacet = repo.optionalFacet(GroupFacet.class).orElse(null);
      if (groupFacet != null) {
        repos = groupFacet.allMembers();
      }
      else {
        repos = Collections.singletonList(repo);
      }
    }
    if (log.isDebugEnabled()) {
      log.debug("searchMavenArtifacts repositories: {}", repos);
    }

    BoolQueryBuilder query = QueryBuilders.boolQuery();
    query.filter(QueryBuilders.termQuery("format", MAVEN2));
    if (!repos.isEmpty()) {
      query.filter(
          QueryBuilders
              .termsQuery("repository_name", repos.stream().map(Repository::getName).collect(Collectors.toList())));
    }
    if (StringUtils.isNotBlank(groupId)) {
      query.filter(QueryBuilders.termQuery("attributes.maven2.groupId", groupId));
    }
    if (StringUtils.isNotBlank(artifactId)) {
      query.filter(QueryBuilders.termQuery("attributes.maven2.artifactId", artifactId));
    }
    if (StringUtils.isNotBlank(baseVersion)) {
      query.filter(QueryBuilders.termQuery("attributes.maven2.baseVersion", baseVersion));
    }
    if (StringUtils.isNotBlank(classifier)) {
      query.filter(QueryBuilders.termQuery("assets.attributes.maven2.classifier", classifier));
    }
    if (StringUtils.isNotBlank(extension)) {
      query.filter(QueryBuilders.termQuery("assets.attributes.maven2.extension", extension));
    }

    if (log.isDebugEnabled()) {
      log.debug("searchMavenArtifacts query: {}", query);
    }

    SearchResponse response = searchService.searchUnrestricted(
        query,
        Collections
            .singletonList(new FieldSortBuilder("assets.attributes.content.last_modified").order(SortOrder.DESC)),
        0,
        limit);

    if (log.isDebugEnabled()) {
      log.debug("searchMavenArtifacts total hits: {}", response.getHits().getTotalHits());
      for (SearchHit hit : response.getHits().hits()) {
        log.debug(hit.getSourceAsString());
      }
    }

    return response;
  }

  protected List<MavenVersion> listVersions(
      int limit,
      String repository,
      String groupId,
      String artifactId,
      String classifier,
      String extension)
  {
    SearchResponse result = searchMavenArtifacts(repository, groupId, artifactId, classifier, extension, null, limit);
    Map<String, MavenVersion> versionMap = new HashMap<>();
    for (SearchHit hit : result.getHits().hits()) {
      Map<String, Object> attributes = getAttributes(hit);
      String baseVersion = getBaseVersion(attributes);
      MavenVersion existing = versionMap.computeIfAbsent(baseVersion, MavenVersion::new);
      String assetVersion = (String) hit.getSource().get("version");
      Date lastModified = getLastModified(attributes);
      existing.addAsset(new NexusAsset(assetVersion, lastModified));
    }
    return versionMap.values().stream().sorted().collect(Collectors.toList());
  }

  private Map<String, Object> getAttributes(SearchHit hit)
  {
    List<Map<String, Object>> assets = (List<Map<String, Object>>) hit.getSource().get(ASSETS);
    return (Map<String, Object>) assets.get(0).get(ATTRIBUTES);
  }

  protected String getBaseVersion(Map<String, Object> attributes)
  {
    Map<String, Object> maven = (Map<String, Object>) attributes.get(MAVEN2);
    return (String) maven.get(MAVEN2_BASE_VERSION);
  }

  private Date getLastModified(Map<String, Object> attributes)
  {
    Map<String, Object> content = (Map<String, Object>) attributes.get(CONTENT);
    if (content != null && content.containsKey(LAST_MODIFIED)) {
      Long lastModified = (Long) content.get(LAST_MODIFIED);
      return new Date(lastModified);
    }
    return null;
  }
}
