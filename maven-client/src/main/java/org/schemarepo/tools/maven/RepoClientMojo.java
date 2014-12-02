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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static java.lang.String.format;

/**
 * Implements regiter goal of schema-repo plugin.
 * Will recursively scan the schema directory (<pre>schemaDir</pre>) for <pre>schemeFileExt</pre>
 * files (defaults to <pre>.avsc</pre>) and attempts to register them
 * with the schema-repo identified by <pre>url</pre>.
 * Files starting with dot ('.') will be ignored.
 * <p>If the corresponding subject does not exist (e.g. first version of the given schema),
 * the subject will be created.</p>
 * <p>Subject name is derived from schema file name using Strategy pattern, with specific strategy
 * class specified in the user's POM. Default stategy uses file name without extension as subject name.</p>
 */
@Mojo( name = "register", defaultPhase = LifecyclePhase.DEPLOY)
public class RepoClientMojo extends AbstractMojo {

  private static final String IDL_EXT = ".avdl";
  private static final String SCHEMA_EXT = ".avsc";

  @Parameter(required = true, readonly = true, defaultValue = "${project}")
  private MavenProject project;

  @Parameter(property = "schema-repo.register.schemaDir")
  private File schemaDir;

  @Parameter(property = "schema-repo.register.schemaFileExt", defaultValue = ".avsc")
  private String schemaFileExt;

  @Parameter(property = "schema-repo.register.subjectNameStrategyClass",
      defaultValue = "org.schemarepo.tools.maven.DefaultSubjectNameStrategy")
  private String subjectNameStrategyClass;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!schemaDir.isAbsolute()) {
      schemaDir = new File(project.getBasedir(), schemaDir.getPath());
    }
    if (!schemaDir.isDirectory()) {
      throw new MojoExecutionException(format("Invalid <schemaDir> parameter value %s -- not a directory", schemaDir.getAbsolutePath()));
    }
    schemaFileExt = schemaFileExt != null ? schemaFileExt : "";
    getLog().info(format("Looking for %s files in %s", schemaFileExt.length() > 0 ? schemaFileExt : "all", schemaDir.getAbsolutePath()));

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
    boolean failures = false;
    for (Path schemaPath : schemaPaths) {
      String subjectName = subjectNameStrategy.getSubjectName(schemaPath);
      getLog().debug(format("Attempting to register %s under subject %s", schemaPath, subjectName));
    }

    if (failures) {
      throw new MojoFailureException(this, "Schema registration failed",
          "One or more schemas failed to get registered with the schema-repo, see above errors");
    }
  }

}
