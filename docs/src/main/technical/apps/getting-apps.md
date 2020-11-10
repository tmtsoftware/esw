# Getting and Running ESW Applications

ESW applications are installed locally using the `coursier` application, a standalone program that makes using the
JVM and Scala/Java applications easier to use and maintain.

To use *any* ESW application, `coursier` must be installed on your machine.

## 1. Install `coursier`

The installation process is documented in the [coursier installation document](https://get-coursier.io/docs/cli-installation).

This blog post: [Single Command Scala Setup](https://alexarchambault.github.io/posts/2020-09-21-cs-setup.html)
by Alex Archambault, is a more friendly introduction to `coursier`.

## 2. Add TMT Apps channel to `coursier`

TMT apps are installed using a `coursier` channel. The channel must be added to the local installation of `coursier`
using the `cs install` option. The apps channel is maintained on the TMT GitHub site.  To install the TMT Apps channel
use one of the following commands.

For developer machine setup, type:

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

For a production machine setup, type:

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

## 3. Starting CSW Services

Often when using an ESW application for development, it is necessary to also run one or more CSW services
such as Location Service, Event Service, etc. The CSW documentation provides the information needed to
start CSW services @extref[here](csw:commons/apps).

## 4. Install and Run ESW Apps

`cs install` command will be used to install executables/launchers of each of ESW apps.
**This Step varies for each application. Each app section explains installation and running procedure in detail.**

For example following command shows installation for agent app,
```bash
cs install agent-app:<version | SHA>
```
