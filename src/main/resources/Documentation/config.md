Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    group = Service Users
```

<a id="block">
`plugin.@PLUGIN@.block`
:	A username which is forbidden to be used as name for a service
	user. The blocked username is case insensitive. Multiple
	usernames can be blocked by specifying multiple
	`plugin.@PLUGIN@.block` entries.

<a id="group">
`plugin.@PLUGIN@.group`
:	A group to which newly created service users should be
    automatically added. Multiple groups can be specified by having
    multiple `plugin.@PLUGIN@.group` entries.
