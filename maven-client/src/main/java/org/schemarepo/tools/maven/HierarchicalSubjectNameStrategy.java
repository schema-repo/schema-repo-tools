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

import static org.schemarepo.tools.maven.PropertyKeys.HIERACHICAL_SUBJECT_NAME_STRATEGY_PROPERTIES_PREFIX;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Default strategy responsible for determining schema-repo subject name based on the schema file name and path.
 * Combines {@link org.schemarepo.tools.maven.DefaultSubjectNameStrategy} with one or more ancestor folders names
 * (<pre>numberOfAncestors</pre>, defaults to <pre>1</pre>) joined by <pre>separator</pre> (defaults to <pre>_</pre>) string.
 * <p>For example, with the default settings, <pre>parent/schema.avsc</pre> will generate <pre>parent_schema</pre> name.</p>
 */
public class HierarchicalSubjectNameStrategy extends DefaultSubjectNameStrategy {

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
    separator = properties.getProperty(HIERACHICAL_SUBJECT_NAME_STRATEGY_PROPERTIES_PREFIX + "separator", "_");
    numberOfAncestors = Integer.parseInt(
        properties.getProperty(HIERACHICAL_SUBJECT_NAME_STRATEGY_PROPERTIES_PREFIX + "numberOfAncestors", "1"));
    if (numberOfAncestors < 0) {
      throw new IllegalArgumentException("numberOfAncestors property must be a non-negative integer");
    }
  }

  @Override
  public String toString() {
    return String.format("%s[separator=\"%s\", numberOfAncestors=%s]", super.toString(), separator, numberOfAncestors);
  }

}
