# Database Service

The Database Service DSL is a wrapper for the Database Service module provided by CSW.
You can refer to detailed documentation of the Database Service provided by CSW @extref[here](csw:services/database).

This DSL provides APIs to create the connection to database. It uses Jooq library underneath for crating database connections
and query. APIs for creating database connection expose `DSLContext`. All the database related functionality is available using Java APIs
exposed on `DSLContext`.

@@@ note
`Jooq` is a Java library that provides a higher level API for accessing data i.e. DDL support, DML support, fetch,
batch execution, prepared statements, safety against sql injection, connection pooling, etc. To know more about Jooq and
its features, please refer to this [link](https://www.jooq.org/learn/).
@@@


## Create Database Connection (Read Access)

This API allows creating connection to database with default read access. The username and password for read access
is picked from environment variables set on individual's machine i.e. `DB_READ_USERNAME` and `DB_READ_PASSWORD`.
It is expected that developers set these variables before calling this method. It returns Jooq's `DSLContext` or fails with
DatabaseException. DSLContext provide methods like `fetchAsync`, `executeAsync`, `executeBatch`, etc. Moreover, see
`JooqHelper` in CSW which provides wrapper methods on Jooq's `DSLContext`. For methods returning `Future`, script will need
to await for sequential flow.

Following example shows creating database connection and querying database.

Kotlin
:   @@snip [DatabaseServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DatabaseServiceDslExample.kts) { #dsl-jooq-helper }

## Create Database Connection (with provided access)

This API allows creating the connection to database with credentials picked from environment variables. Names of these
environment variables is expected as method parameters and developers are expected to set these variables before
calling this method. If user provides names for write credentials environment variables, then database connection will be created
with write access. It returns Jooq's `DSLContext` or fails with DatabaseException. DSLContext provide methods like `fetchAsync`, `executeAsync`, `executeBatch`, etc.
Moreover, see `JooqHelper` in CSW which provides wrapper methods on Jooq's `DSLContext`. For methods returning `Future`, script will need
to await for sequential flow. 

Following example shows creating database connection with write access.

Kotlin
:   @@snip [DatabaseServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DatabaseServiceDslExample.kts) { #dsl-context-with-write-access }

### Source code for examples

* [Database Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/DatabaseServiceDslExample.kts)

@@@ note
After getting handle to `DSLContext`, methods like `fetchAsync`, `executeAsync` etc are available for querying database. If these
methods return `Future` then script writer will need to await for sequential flow.
@@@
