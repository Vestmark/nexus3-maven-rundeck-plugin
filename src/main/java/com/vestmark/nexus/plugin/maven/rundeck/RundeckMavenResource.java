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
package com.vestmark.nexus.plugin.maven.rundeck;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SearchService;

import com.vestmark.nexus.plugin.maven.BaseMavenResource;

@Named
@Singleton
@Path("/maven/rundeck")
public class RundeckMavenResource
    extends BaseMavenResource
{

  @Inject
  public RundeckMavenResource(SearchService searchService, RepositoryManager repositoryManager)
  {
    super(searchService, repositoryManager);
  }

  @GET
  @Path("versions")
  @Produces(MediaType.APPLICATION_JSON)
  public List<RundeckMavenVersion> versions(
      @DefaultValue("10") @QueryParam("l") int limit,
      @QueryParam("r") String repository,
      @QueryParam("g") String groupId,
      @QueryParam("a") String artifactId,
      @QueryParam("c") String classifier,
      @QueryParam("e") String extension)
  {
    return super.listVersions(limit, repository, groupId, artifactId, classifier, extension).stream()
        .map(RundeckMavenVersion::new)
        .collect(Collectors.toList());
  }
}
