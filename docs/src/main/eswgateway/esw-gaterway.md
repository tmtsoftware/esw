# Esw Gateway

This is provided to give access to all CSW and ESW services and components from browser-based user interfaces

## Esw Gateway with authentication and authorization

Esw Gateway is accessible to public network and exposes API endpoints through Http interface, hence we need to
protected its endpoints from unauthorized access.
  
### Protection on Commands service endpoints on Gateway 
 
There are commands which are more restrictive which need eng or above role and some commands which just need user
level role. Also, these more restrictive eng commands need a fined grained control mechanism so that they can be
safely executed by authorized person having specific role at subsystem level. To achieve this we need to have a
role hierarchy based on subsystem along with a config table containing the mapping between more restrictive
commands and subsystem level roles. Diagram below shows such a role hierarchy, this type of role hierarchy need
to be created in Keycloak.
     
![Role Hierarchy](../images/eswgateway/Role-Hierarchy.png)

As per this role hierarchy there should be three roles present for each subsystem, composed in specific order on
keycloak. E.g TCS-admin -> TCS-eng -> TCS-user. 

* When you assign a user TCS-eng role, keycloak will automatically add TCS-user role to user
* When you assign a user TCS-admin role, keycloak will automatically add TCS-eng and TCS-user role user

Also, there are special roles. OSW-admin, OSW-eng and OSW-user which are composed of all respective subsystem level roles. 
 * When you assign a user OSW-eng role, keycloak will automatically add TCS-eng, APS-eng, etc role to user and these
  roles will automatically add their respective lower roles to this user TCS-user, APS-user, etc.

Below shown are example entries in config table with commands and roles who can execute those commands.

```
IRIS.filter.wheel.startExposure: [IRIS-user, APS-eng]
IRIS.filter.wheel.stopExposure: [IRIS-user, APS-eng]
```

We need to create a config containing role mapping entries like shown above and use it when starting esw-gateway server. 

#### Examples:

Use command role mapping file from local file system
```
./esw-gateway-server start --local --command-role-config-path /local/path/filename.conf
```
or
```
./esw-gateway-server start -l -c /local/path/filename.conf
```

Use command role mapping file from config service
```
./esw-gateway-server start --command-role-config-path /path/filename.conf
```
or
```
./esw-gateway-server start -c /path/filename.conf
```

### Protection on Sequencer endpoints on Gateway.  

On protected endpoints of sequencer commands in esw-gateway, subsystem-user role check is performed. 
E.g. If current sequence to be executed is for esw.primary(this is obtained form componentId.prefix.subsystem) then user should have minimum ESW-user role