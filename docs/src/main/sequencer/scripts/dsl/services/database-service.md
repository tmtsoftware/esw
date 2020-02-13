# Database Service

The Database Service DSL is a wrapper for the Database Service module provided by CSW.
You can refer to detailed documentation of the Database Service provided by CSW @extref[here](csw:services/database).

This DSL provides APIs to create the connection to a database. It uses a Jooq library underneath for creating database connections
and queries. APIs for creating the database connection expose a `DSLContext` object. All database-related functionality 
from the Database Service is available using the Java APIs exposed by `DSLContext`.

@@@ note
`Jooq` is a Java library that provides a higher level API for accessing data i.e. DDL support, DML support, fetch,
batch execution, prepared statements, safety against SQL injection, connection pooling, etc. To know more about Jooq and
its features, please refer to this [link](https://www.jooq.org/learn/).
@@@


## Create a Database Connection (Read Access)

This API allows creating a connection to a database with default read access. The username and password for read access
is picked from environment variables set on individual's machine i.e. `DB_READ_USERNAME` and `DB_READ_PASSWORD`.
It is expected that developers set these variables before calling this method. It returns a Jooq `DSLContext` or fails with
a DatabaseException. The `DSLContext` provides methods like `fetchAsync`, `executeAsync`, `executeBatch`, etc. Additionally, the CSW
[`JooqHelper`](https://tmtsoftware.github.io/csw/api/scala/csw/database/javadsl/JooqHelper$.html) and its wrapper methods
can be used.   For methods returning a `Future`, script will need
to explicitly await for the future to complete to achieve sequential flow.

THe following example shows the creation of a database connection and a query to the database.

Kotlin
:   @@snip [DatabaseServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DatabaseServiceDslExample.kts) { #dsl-jooq-helper }

## Create a Database Connection (Read/Write Access)

This API allows creating a connection to a database with read/write access.  User name and password credentials 
should be stored in environment variables, and the names of these environment variables are passed as method parameters.  
If the correct write access credentials can be obtained from these environment variables, then a database connection will be created
with write access. It returns a Jooq `DSLContext` or fails with a DatabaseException.

The following example shows the creation of a database connection with write access and alternative way of using the `DSLContext`
to do queries.

Kotlin
:   @@snip [DatabaseServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DatabaseServiceDslExample.kts) { #dsl-context-with-write-access }

### Source code for examples

* [Database Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DatabaseServiceDslExample.kts)

