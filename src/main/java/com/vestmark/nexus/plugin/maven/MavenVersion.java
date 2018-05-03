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

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class MavenVersion
    implements Comparable<MavenVersion>
{

  private static final Comparator<NexusAsset> assetComparator = (o1, o2) -> {
    if (o1.getLastUpdated() == null) {
      if (o2.getLastUpdated() == null) {
        return 0;
      }
      return 1;
    }
    else if (o2.getLastUpdated() == null) {
      return -1;
    }
    return o2.getLastUpdated().compareTo(o1.getLastUpdated());
  };

  private final String baseVersion;
  private SortedSet<NexusAsset> assets;

  public MavenVersion(String baseVersion)
  {
    this.baseVersion = baseVersion;
  }

  public String getBaseVersion()
  {
    return baseVersion;
  }

  public SortedSet<NexusAsset> getAssets()
  {
    if (assets == null) {
      assets = new TreeSet<>(assetComparator);
    }
    return assets;
  }

  public void addAsset(NexusAsset version)
  {
    getAssets().add(version);
  }

  @Override
  public int compareTo(MavenVersion o)
  {
    return baseVersion.compareTo(o.getBaseVersion());
  }
}
