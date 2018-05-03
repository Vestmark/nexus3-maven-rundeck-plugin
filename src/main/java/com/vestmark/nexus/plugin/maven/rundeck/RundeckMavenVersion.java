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

import java.io.Serializable;
import java.text.SimpleDateFormat;

import com.vestmark.nexus.plugin.maven.MavenVersion;
import com.vestmark.nexus.plugin.maven.NexusAsset;

public class RundeckMavenVersion
    implements Comparable<RundeckMavenVersion>, Serializable
{

  static final long serialVersionUID = 1L;
  private static final SimpleDateFormat lastUpdatedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private String name;
  private String value;

  RundeckMavenVersion(MavenVersion mavenVersion)
  {
    value = mavenVersion.getBaseVersion();
    NexusAsset latestAsset = mavenVersion.getAssets().first();
    if (latestAsset != null && latestAsset.getLastUpdated() != null) {
      name = String
          .format("%s (%s)", mavenVersion.getBaseVersion(), lastUpdatedFormat.format(latestAsset.getLastUpdated()));
    }
    else {
      name = mavenVersion.getBaseVersion();
    }
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getValue()
  {
    return value;
  }

  public void setValue(String value)
  {
    this.value = value;
  }

  @Override
  public int compareTo(RundeckMavenVersion o)
  {
    return value.compareTo(o.getValue());
  }
}
