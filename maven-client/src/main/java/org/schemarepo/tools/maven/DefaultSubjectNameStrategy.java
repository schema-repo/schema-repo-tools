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
import java.util.Properties;

/**
 * Default strategy responsible for determining schema-repo subject name based on the schema file name and path.
 * Uses the file's name without extension.
 */
public class DefaultSubjectNameStrategy implements SubjectNameStrategy {

  @Override
  public String getSubjectName(final Path schemaPath) {
    final String path = schemaPath.getFileName().toString();
    final int dot = path.lastIndexOf('.');
    return dot > -1 ? path.substring(0, dot) : path;
  }

  @Override
  public void configure(final Properties properties) {
  }

}
