Build
=====

This plugin is built with Buck.

Two build modes are supported: Standalone and in Gerrit tree. Standalone
build mode is recommended, as this mode doesn't require local Gerrit
tree to exist.

Build standalone
----------------

Clone bucklets library:

```
  git clone https://gerrit.googlesource.com/bucklets

```
and link it to serviceuser directory:

```
  cd serviceuser && ln -s ../bucklets .
```

Add link to the .buckversion file:

```
  cd serviceuser && ln -s bucklets/buckversion .buckversion
```

To build the plugin, issue the following command:


```
  buck build plugin
```

The output is created in

```
  buck-out/gen/serviceuser/serviceuser.jar
```

Build in Gerrit tree
--------------------

Clone or link this plugin to the plugins directory of Gerrit's source
tree, and issue the command:

```
  buck build plugins/serviceuser
```

The output is created in

```
  buck-out/gen/plugins/serviceuser/serviceuser.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```
