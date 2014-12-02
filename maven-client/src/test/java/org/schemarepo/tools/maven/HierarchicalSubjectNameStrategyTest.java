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

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

public class HierarchicalSubjectNameStrategyTest {

  @Test
  public void testGetSubjectName() throws Exception {
    HierarchicalSubjectNameStrategy subjectNameStrategy = new HierarchicalSubjectNameStrategy();
    subjectNameStrategy.configure(new Properties());
    Map<Path, String> testData = new LinkedHashMap<>();
    testData.put(Paths.get("schema"), "schema");
    testData.put(Paths.get("path1", "schema.json"), "path1_schema");
    testData.put(Paths.get("path1", "path2", "schema."), "path2_schema");
    testGetSubjectNameParametrized(subjectNameStrategy, testData);

    subjectNameStrategy = new HierarchicalSubjectNameStrategy();
    Properties properties = new Properties();
    properties.setProperty("schema-repo.tools.registration.hierarchicalSubjectNameStrategy.separator", "|");
    properties.setProperty("schema-repo.tools.registration.hierarchicalSubjectNameStrategy.numberOfAncestors", "2");
    subjectNameStrategy.configure(properties);
    testData = new LinkedHashMap<>();
    testData.put(Paths.get("schema"), "schema");
    testData.put(Paths.get("path1", "schema.json"), "path1|schema");
    testData.put(Paths.get("path1", "path2", "schema."), "path1|path2|schema");
    testGetSubjectNameParametrized(subjectNameStrategy, testData);
  }

  private void testGetSubjectNameParametrized(HierarchicalSubjectNameStrategy subjectNameStrategy, Map<Path, String> testData) {
    for (Map.Entry<Path, String> entry : testData.entrySet()) {
      assertEquals("broken " + subjectNameStrategy, entry.getValue(), subjectNameStrategy.getSubjectName(entry.getKey()));
    }
  }

}