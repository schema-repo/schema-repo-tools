Schema-Repo Tools
===

Schema-Repo Tools houses a number of tools / addenda which support and supplement [Schema-Repo](http://schema-repo.org) project.

## Maven Client plugin (work in progress)

This is a maven plugin which wraps schema-repo client and allows new schema(s) deployment to happen as part of maven build.

A typical use case will involve a source repository which contains schemas, and a build process acting on the schemas.
One of the desirable actions is automatic deployment of the most recent version of each schema
(or [Subject](https://github.com/schema-repo/schema-repo/blob/master/common/src/main/java/org/schemarepo/Subject.java) in schema-repo's terminology)
to the schema-repo.

### Plugin's properties
* `schema-repo.tools.registration.schemaDir` : directory where to start looking for schema files.
If the path is relative, prepends it with `project.basedir`. Required.
* `schema-repo.tools.registration.schemaFileExt` : schema files extension to match. Defaults to `.avsc` (Avro JSON schema)
* `schema-repo.tools.registration.subjectNameStrategyClass` : which subject name strategy class to use, see below.
Defaults to `org.schemarepo.tools.maven.DefaultSubjectNameStrategy`
* `schema-repo.rest-client.server-url` : HTTP URL to the running schema-repo. Required.

### Subject name strategies
Responsible for determining schema-repo subject name based on the schema file name and path.

* `org.schemarepo.tools.maven.DefaultSubjectNameStrategy` : Uses the file's name without extension. No additional properties.
* `org.schemarepo.tools.maven.HierarchicalSubjectNameStrategy` :
    Combines DefaultSubjectNameStrategy with one or more ancestor folders names
    joined by the separator into single string.
    For example, with the default settings, `parent/schema.avsc` will generate `parent_schema` name. Supports additional properties:
    * `schema-repo.tools.registration.hierarchicalSubjectNameStrategy.numberOfAncestors` : how many ancestors to include, default is 1
    * `schema-repo.tools.registration.hierarchicalSubjectNameStrategy.separator` : separator used when joining strings, default is underscore

### Sample usage

    <plugin>
        <groupId>org.schemarepo</groupId>
        <artifactId>schema-repo-maven-plugin</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <executions>
            <execution>
                <phase>deploy</phase>
                <goals>
                    <goal>register-schemas</goal>
                </goals>
                <configuration>
                    <schemaDir>src/main/schema</schemaDir>
                    <schemaRepoURL>http://my-schema-repo:2876/schema-repo</schemaRepoURL>
                </configuration>
            </execution>
        </executions>
    </plugin>

