# Database Service

The Database Service DSL is a wrapper for the Database Service module provided by CSW.
You can refer to detailed documentation of the Database Service provided by CSW @extref[here](csw:services/database).

This DSL provides APIs to create the connection to database.

## Create Database Connection (Read Access)

This API allows creating connection to database with default read access. The username and password for read access
is picked from environment variables set on individual's machine i.e. `DB_READ_USERNAME` and `DB_READ_PASSWORD`.
It is expected that developers set these variables before calling this method. It returns Jooq's `DSLContext` or fails with
DatabaseException. DSLContext provide methods like `fetchAsync`, `executeAsync`, `executeBatch`, etc. Moreover, see
`JooqHelper` in CSW which provides wrapper methods on Jooq's `DSLContext`. 


