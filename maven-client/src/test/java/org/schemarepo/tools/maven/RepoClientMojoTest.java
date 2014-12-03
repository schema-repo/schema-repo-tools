package org.schemarepo.tools.maven;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.junit.Test;

/**
 * maven-test-harness -- the "proper" way to unit-test mojos -- is so broken in the latest versions of maven.
 */
public class RepoClientMojoTest {

  @Test
  public void testRegistration() throws Exception {
    RepoClientMojo mojo = new RepoClientMojo();
    MavenProject project = new MavenProject();
    project.setFile(new File("src/test/resources/schema/dummy-file"));
    mojo.execute();
  }

}
