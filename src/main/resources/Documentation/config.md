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
:	The name of an internal group to which newly created service users
	should be automatically added. Multiple groups can be specified by
	having multiple `plugin.@PLUGIN@.group` entries.

<a id="infoMessage">
`plugin.@PLUGIN@.infoMessage`
:	HTML formatted message that should be displayed on the service user
	creation screen.

<a id="onSuccessMessage">
`plugin.@PLUGIN@.onSuccessMessage`
:	HTML formatted message that should be displayed after a service
	user was successfully created.

<a id="allowEmail">
`plugin.@PLUGIN@.allowEmail`
:	Whether it is allowed to provide an email address for
	a service user. By default false.

<a id="allowOwner">
`plugin.@PLUGIN@.allowOwner`
:	Whether it is allowed to set an owner group for a service user.
	By default false.

<a id="createNotes">
`plugin.@PLUGIN@.createNotes`
:	Whether commits of a service user should be annotated by a Git note
	that contains information about the current owners of the service
	user. This allows to find a real person that is responsible for
	this commit. To get such a Git note for each commit of a service
	user the 'Forge Committer' access right must be blocked for service
	users. By default true.

<a id="createNotes">
`plugin.@PLUGIN@.createNotesAsync`
:	Whether the Git notes on commits that are pushed by a service user
	should be created asynchronously. By default false.
