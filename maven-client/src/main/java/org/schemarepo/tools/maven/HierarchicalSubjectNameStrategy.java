/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.schemarepo.tools.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Default strategy responsible for determining schema-repo subject name based on the schema file name and path.
 * Uses the file's name without extension.
 */
public class HierarchicalSubjectNameStrategy extends DefaultSubjectNameStrategy {

  /** part of the properties names */
  public static final String PROPETIES_SUBPREFIX = "hierarchicalSubjectNameStrategy.";

  private String separator;
  private int numberOfAncestors;

  @Override
  public String getSubjectName(final Path schemaPath) {
    final List<String> ancestors = new ArrayList<>();
    Path ancestorPath = schemaPath;
    for (int level = 0; level < numberOfAncestors; level++) {
      ancestorPath = ancestorPath.getParent();
      if (ancestorPath == null) {
        break;
      }
      ancestors.add(ancestorPath.getFileName().toString());
    }

    Collections.reverse(ancestors);
    final StringBuilder subjectName = new StringBuilder();
    for (String ancestor : ancestors) {
      subjectName.append(ancestor).append(separator);
    }
    return subjectName.append(super.getSubjectName(schemaPath)).toString();
  }

  @Override
  public void configure(final Properties properties) {
    separator = properties.getProperty(PROPERTIES_PREFIX + PROPETIES_SUBPREFIX + "separator", "_");
    numberOfAncestors = Integer.parseInt(
        properties.getProperty(PROPERTIES_PREFIX + PROPETIES_SUBPREFIX + "numberOfAncestors", "0"));
  }

  @Override
  public String toString() {
    return String.format("%s[separator=\"%s\", numberOfAncestors=%s]", super.toString(), separator, numberOfAncestors);
  }

}
