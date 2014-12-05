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

import static java.lang.String.format;
import static org.schemarepo.tools.maven.PropertyKeys.REPO_CLIENT_PROPERTY_PREFIX;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.schemarepo.SchemaEntry;
import org.schemarepo.Subject;
import org.schemarepo.client.RESTRepositoryClient;
import org.schemarepo.config.Config;

/**
 * Implements register goal of schema-repo plugin.
 * Will recursively scan the schema directory (<pre>schemaDir</pre>) for <pre>schemeFileExt</pre>
 * files (defaults to <pre>.avsc</pre>) and attempts to register them
 * with the schema-repo identified by <pre>url</pre>.
 * Files starting with dot ('.') will be ignored.
 * <p>If the corresponding subject does not exist (e.g. first version of the given schema),
 * the subject will be created.</p>
 * <p>Subject name is derived from schema file name using Strategy pattern, with specific strategy
 * class specified in the user's POM. Default strategy uses file name without extension as subject name.</p>
 */
@Mojo( name = "register-schemas", defaultPhase = LifecyclePhase.DEPLOY)
public class RepoClientMojo extends AbstractMojo {

  static final String DEFAULT_SCHEMA_FILE_EXT = ".avsc";
  static final String DEFAULT_SUBJECT_NAME_STRATEGY_CLASS = "org.schemarepo.tools.maven.DefaultSubjectNameStrategy";

  @Parameter(required = true, readonly = true, defaultValue = "${project}")
  MavenProject project;

  @Parameter(required = true, property = REPO_CLIENT_PROPERTY_PREFIX + "schemaDir")
  File schemaDir;

  @Parameter(property = REPO_CLIENT_PROPERTY_PREFIX + "schemaFileExt", defaultValue = DEFAULT_SCHEMA_FILE_EXT)
  String schemaFileExt;

  @Parameter(property = REPO_CLIENT_PROPERTY_PREFIX + "subjectNameStrategyClass", defaultValue = DEFAULT_SUBJECT_NAME_STRATEGY_CLASS)
  String subjectNameStrategyClass;

  @Parameter(required = true, property = Config.CLIENT_SERVER_URL)
  String serverURL;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    verifySchemaDir();
    final SubjectNameStrategy subjectNameStrategy = createSubjectNameStrategy();
    final List<Path> schemaPaths = collectSchemas();
    final RESTRepositoryClient client = configureRepositoryClient();

    final Map<String, Subject> subjectMap = new HashMap<>();
    for (Subject subject : client.subjects()) {
      subjectMap.put(subject.getName(), subject);
    }
    getLog().info(format("Schema repo instance currently contains definitions for %s schemas", subjectMap.size()));

    int failuresCnt = 0;
    for (Path schemaPath : schemaPaths) {
      String subjectName = null;
      try {
        subjectName = subjectNameStrategy.getSubjectName(schemaPath);
        Subject subject = subjectMap.get(subjectName);
        if (subject == null) {
          getLog().debug(format("Creating subject %s", subjectName));
          subject = client.register(subjectName, null);
          subjectMap.put(subjectName, subject);
        } else {
          getLog().debug(format("subject %s is already registered", subjectName));
        }
        SchemaEntry schemaEntry = subject.register(new String(Files.readAllBytes(schemaPath)));
        getLog().debug(format("Registered %s under subject %s with ID %s", schemaPath, subjectName, schemaEntry.getId()));
      } catch (Exception e) {
        failuresCnt++;
        getLog().error(format("Failed to register %s under subject %s", schemaPath, subjectName), e);
      }
    }

    if (failuresCnt > 0) {
      throw new MojoFailureException(this, "Schema registration failed",
          format("%s schemas failed to get registered with the schema-repo, see above errors", failuresCnt));
    }
  }

  private void verifySchemaDir() throws MojoExecutionException {
    if (!schemaDir.isAbsolute()) {
      schemaDir = new File(project.getBasedir(), schemaDir.getPath());
    }
    if (!schemaDir.isDirectory()) {
      throw new MojoExecutionException(format("Invalid <schemaDir> parameter value %s -- not a directory", schemaDir.getAbsolutePath()));
    }
    schemaFileExt = schemaFileExt != null ? schemaFileExt : "";
    getLog().info(format("Looking for %s files in %s", schemaFileExt.length() > 0 ? schemaFileExt : "all", schemaDir.getAbsolutePath()));
  }

  private SubjectNameStrategy createSubjectNameStrategy() throws MojoExecutionException {
    SubjectNameStrategy subjectNameStrategy;
    String step = null;
    try {
      step = "resolve/instantiate";
      subjectNameStrategy = getClass().getClassLoader().loadClass(subjectNameStrategyClass).asSubclass(SubjectNameStrategy.class).newInstance();
      step = "configure";
      subjectNameStrategy.configure(project.getProperties());
    } catch (Exception e) {
      throw new MojoExecutionException(format(
          "Invalid <subjectNameStrategyClass> parameter value %s -- failed to %s strategy", subjectNameStrategyClass, step));
    }
    getLog().info("Using " + subjectNameStrategy);
    return subjectNameStrategy;
  }

  private RESTRepositoryClient configureRepositoryClient() {
    final RESTRepositoryClient client = new RESTRepositoryClient(serverURL, false);
    getLog().info(format("Connecting to schema-repo at %s", serverURL));
    return client;
  }

  private List<Path> collectSchemas() throws MojoExecutionException {
    final List<Path> schemaPaths = new ArrayList<>();
    try {
      Files.walkFileTree(Paths.get(schemaDir.getAbsolutePath()), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {
          if (path.toString().endsWith(schemaFileExt) && path.getFileName().toString().charAt(0) != '.') {
            schemaPaths.add(path);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new MojoExecutionException(format("Failed to walk %s", schemaDir), e);
    }
    getLog().info(format("Found %s schema files", schemaPaths.size()));
    return schemaPaths;
  }

}
