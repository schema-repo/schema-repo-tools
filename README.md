Schema-Repo Tools
===

Schema-Repo Tools houses a number of tools / addenda which support and supplement [Schema-Repo](http://schema-repo.org) project.

## Maven Client plugin (work in progress)

This is a maven plugin which wraps schema-repo client and allows new schema(s) deployment to happen as part of maven build.

A typical use case will involve a source repository which contains schemas, and a build process acting on the schemas. One of the desirable actions is automatic deployment of the most recent version of each schema (or [Subject](https://github.com/schema-repo/schema-repo/blob/master/common/src/main/java/org/schemarepo/Subject.java) in schema-repo's terminology) to the schema-repo.