## Coding guidelines

Contributions to the CSync Java SDK should follow proper Java Style.

## Documentation

All code changes should include comments describing the design, assumptions, dependencies, and non-obvious aspects of the implementation.
Hopefully the existing code provides a good example of appropriate code comments.
If necessary, make the appropriate updates in the README.md and other documentation files.

## Contributing your changes

1. If one does not exist already, open an issue that your contribution is going to resolve or fix.
    1. Make sure to give the issue a clear title and a very focused description.
2. On the issue page, set the appropriate Pipeline, Label(s), Milestone, and assign the issue to
yourself.
    1. We use Zenhub to organize our issues and plan our releases. Giving as much information as to
    what your changes are help us organize PRs and streamline the committing process.
3. Make a branch from the master branch using the following naming convention:
    1. `YOUR_INITIALS/ISSUE#-DESCRIPTIVE-NAME`
    2. For example, `kb/94-create-contributingmd` was the branch that had the commit containing this
    tutorial.
4. Commit your changes!
5. When you have completed making all your changes, create a Pull Request (PR) from your git manager
or our Github repo from your branch to master.
6. Fill in the template for the PR.
7. Contributions require sign-off. We require that any contributers agree to the [Developer's Certificate of Origin 1.1 (DCO)](http://elinux.org/Developer_Certificate_Of_Origin), otherwise your pull request will be rejected.
    1. When committing using the command line you can sign off using the --signoff or -s flag. This adds a Signed-off-by line by the committer at the end of the commit log message.`git commit -s -m "Commit message"`
8. That's it, thanks for the contribution!

## Setting up your environment

You have probably got most of these set up already, but starting from scratch
you'll need:

* [Java JDK 1.8](gradkehttp://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Gradle](https://gradle.org/install)

## Running the tests

Set the following environmental variables: 
CSYNC_HOST to the host of the CSync Server
CSYNC_PORT for the port number
CSYNC_DEMO_PROVIDER is `demo` by default but can be changed on the server
CSYNC_DEMO_TOKEN is `demoToken` by default but can be changed on the server

Run the tests by running `gradle build`

### Dependency Table

| Name | URL |License Type | Version | Need/Reason | Release Date | Verification Code |
|------|-----|-------------|---------|-------------|--------------|-------------------|
| SLF4j | https://github.com/qos-ch/slf4j | MIT | 1.7.22  | Logging | 12/13/2016 |  |
| SLF4j Simple	| https://github.com/qos-ch/slf4j | MIT | 1.7.22 | Logging | 12/13/2016 | |
| OK HTTP-ws | https://github.com/square/okhttp | Apache2.0 | 3.4.2 | Web Socket | 11/03/2016 |  |
| Guava | https://github.com/google/guava | Apache2.0 | 20.0  | Various Utilities | 10/28/2016 |  |
| H2 Database | https://github.com/h2database/h2database | MPL 2.0 or EPL 1.0 | 1.4.193  | Database | 10/31/2016|  |
| GSON| https://github.com/google/gson | Apacha2.0 | 2.8  | Java Serialization/deserialization | 10/27/2016 |  |
| Junit | https://github.com/junit-team/junit4 | EPL 1.0 | 4.11 | Testing | 11/14/2012 |  |