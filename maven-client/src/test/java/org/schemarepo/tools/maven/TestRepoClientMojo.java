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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.schemarepo.InMemoryRepository;
import org.schemarepo.SchemaEntry;
import org.schemarepo.Subject;
import org.schemarepo.client.RESTRepositoryClient;
import org.schemarepo.config.Config;
import org.schemarepo.server.RepositoryServer;

/**
 * Testing the execution of the mojo.
 * maven-test-harness -- the "proper" way to unit-test mojos -- is so broken in the latest versions of maven,
 * hence the somewhat clunky way this class employs.
 */
public class TestRepoClientMojo {

  private static final int JETTY_PORT = 32876;
  private static final String REPO_URL = "http://localhost:" + JETTY_PORT + Config.getDefault(Config.JETTY_PATH);

  private static RepositoryServer server;

  @BeforeClass
  public static void startServer() throws Exception {
    Properties props = new Properties();
    props.setProperty(Config.REPO_CLASS, InMemoryRepository.class.getName());
    props.setProperty(Config.JETTY_PORT, JETTY_PORT+"");
    props.setProperty(Config.JETTY_GRACEFUL_SHUTDOWN, "100");
    server = new RepositoryServer(props);
    server.start();
    Thread.sleep(100);
  }

  @AfterClass
  public static void stopServer() throws Exception {
    server.stop();
  }

  public RepoClientMojo createMojo(File schemaDir, String schemaFileExt) {
    RepoClientMojo mojo = new RepoClientMojo();
    MavenProject project = new MavenProject();
    project.setFile(new File("src/test/resources/schema/dummy-file"));
    mojo.setLog(new SystemStreamLog() {
      @Override
      public boolean isDebugEnabled() {
        return true;
      }
    });
    mojo.project = project;
    mojo.schemaFileExt = schemaFileExt;
    mojo.schemaDir = schemaDir;
    mojo.schemaFileExt = schemaFileExt != null ? schemaFileExt : RepoClientMojo.DEFAULT_SCHEMA_FILE_EXT;
    mojo.subjectNameStrategyClass = RepoClientMojo.DEFAULT_SUBJECT_NAME_STRATEGY_CLASS;
    mojo.serverURL = REPO_URL;
    return mojo;
  }

  @Test
  public void testRegistration() throws Exception {
    RESTRepositoryClient client = new RESTRepositoryClient(REPO_URL, true);
    // no schemas
    RepoClientMojo mojo = createMojo(new File("v1/"), null);
    mojo.execute();
    assertFalse("Expected no schemas registered", client.subjects().iterator().hasNext());

    mojo = createMojo(new File("v1/"), ".properties");
    mojo.execute();
    assertSchema(client, "test_schema_1", "1");
    assertSchema(client, "test_schema_2", "1");
  }

  private void assertSchema(RESTRepositoryClient client, String subjectName, String latestVersion) throws IOException {
    Subject subject = client.lookup(subjectName);
    assertNotNull("Expected subject to be registered: " + subjectName, subject);
    SchemaEntry entry = subject.latest();
    assertNotNull("No latest version", entry);
    Properties properties = new Properties();
    properties.load(new StringReader(entry.getSchema()));
    assertEquals(subjectName, properties.getProperty("name"));
    assertEquals(latestVersion, properties.getProperty("version"));
  }

}
