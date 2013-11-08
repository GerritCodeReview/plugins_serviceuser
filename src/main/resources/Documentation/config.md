Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    group = Service Users
```

<a id="group">
`plugin.@PLUGIN@.group`
:	A group to which newly created service users should be
    automatically added. Multiple groups can be specified by having
    multiple `plugin.@PLUGIN@.group` entries.
