Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    group = Service Users
```

<a id="block"></a>
`plugin.@PLUGIN@.block`
:	A username which is forbidden to be used as name for a service
	user. The blocked username is case insensitive. The match can
	either be exact, have a wildcard ('*') at the end or use regular
	expressions, which have to start with '^'. If the regex pattern is not
	ending with '$', every username starting with a matching prefix will be
	blocked. Multiple usernames can be blocked by specifying multiple
	`plugin.@PLUGIN@.block` entries.
	Examples:

```
   [plugin "serviceuser"]
        block = johndoe
        block = jane*
        block = ^gerrit[0-9]*
```

<a id="group"></a>
`plugin.@PLUGIN@.group`
:	The name of an internal group to which newly created service users
	should be automatically added. Multiple groups can be specified by
	having multiple `plugin.@PLUGIN@.group` entries.

<a id="infoMessage"></a>
`plugin.@PLUGIN@.infoMessage`
:	HTML formatted message that should be displayed on the service user
	creation screen.

<a id="onSuccessMessage"></a>
`plugin.@PLUGIN@.onSuccessMessage`
:	Message that should be displayed after a service user was
	successfully created.

<a id="allowSsh"></a>
`plugin.@PLUGIN@.allowSsh`
:	Whether it is allowed for service user owners to set ssh keys
	for their service users. Independent of this setting
	Gerrit administrators are always able to set email addresses for
	any service user.
	By default false.

<a id="allowEmail"></a>
`plugin.@PLUGIN@.allowEmail`
:	Whether it is allowed for service user owners to set email
	addresses for their service users. Independent of this setting
	Gerrit administrators are always able to set email addresses for
	any service user.
	By default false.

<a id="allowHttpPassword"></a>
`plugin.@PLUGIN@.allowHttpPassword`
:	Whether it is allowed for service user owners to generate HTTP
    passwords for their service users. Independent of this setting
    Gerrit administrators are always able to set/generate HTTP
    passwords for any service user.
    By default false.

<a id="allowCustomHttpPassword"></a>
`plugin.@PLUGIN@.allowCustomHttpPassword`
:	Whether it is allowed for service user owners to set custom HTTP
	passwords for their service users. This option requires
	`plugin.@PLUGIN@.allowHttpPassword` to be true. Independent of this
	setting Gerrit administrators are always able to set custom HTTP
	passwords for any service user.
	By default false.

<a id="allowOwner"></a>
`plugin.@PLUGIN@.allowOwner`
:	Whether it is allowed to set an owner group for a service user.
	By default false.

<a id="createNotes"></a>
`plugin.@PLUGIN@.createNotes`
:	Whether commits of a service user should be annotated by a Git note
	that contains information about the current owners of the service
	user. This allows to find a real person that is responsible for
	this commit. To get such a Git note for each commit of a service
	user the 'Forge Committer' access right must be blocked for service
	users. By default true.

<a id="createNotes"></a>
`plugin.@PLUGIN@.createNotesAsync`
:	Whether the Git notes on commits that are pushed by a service user
	should be created asynchronously. By default false.

Control what service users can do
---------------------------------

The @PLUGIN@ plugin provides a self-service for creating service users.
Project owners can then grant access rights to the service users on
their projects. Independent of these access rights Gerrit
administrators have some control over what service users can do.

### Git over SSH / Access to SSH API

Every service user for which a public SSH key is uploaded can access
Gerrit projects via Git over SSH (if the
[Read](access-control.md#category_read) permission is granted).

In addition these service users can make use of the Gerrit
[SSH API](cmd-index.md#user_commands).

E.g. this enables service users to be used for continuous integration
builds: They can clone projects, fetch open changes and then vote and
comment on the changes (for voting the corresponding
[label permission](access-control.md#category_review_labels) must be
assigned on the project).

There is no setting to disable SSH access for service users.

### Git over HTTP / Access to REST API

To be able to do Git operations over HTTP and to access the Gerrit
[REST API](rest-api.md) service users must have an HTTP password.

Gerrit administrators can control by the
[allowHttpPassword](#allowHttpPassword) plugin configuration parameter
whether service user owners can generate HTTP passwords for their
service users. As a consequence of setting this option to `false`
by default service users can neither do git operations over HTTP nor
access the Gerrit REST API. Still Gerrit administrators may approve
access for certain service users by explicitly generating a HTTP
passwords for them. This can be done on the service user screen.

*WARNING*: If access to the REST API is enabled, service users can use
the [Create Email REST endpoint](../../../Documentation/rest-api-accounts.html#create-account-email)
in Gerrit core to create an email address even if
[allowEmail](#allowEmail) is set to `false`, unless
[registration of email addresses in Gerrit is disabled](../../../Documentation/config-gerrit.html#sendemail.allowRegisterNewEmail).

### Git Push

To be able to push to Gerrit service users must have an email address.

Gerrit administrators can control by the
[allowEmail](#allowEmail) plugin configuration parameter whether
service user owners can set email addresses for their service
users. As a consequence of setting this option to `false` git push is
by default not allowed for service users. Still Gerrit administrators
may approve git push for certain service users by explicitly setting
email addresses for them. This can be done on the service user screen.

When git push is allowed, the plugin can be configured to
[create a git note](#createNotes) on each commit pushed by a service
user which records the service user owners at that point in time. This
allows to track back which person is responsible for the commits done
by the service user.

### Block access rights for service users

By automatically adding newly created service users to a Gerrit
[group](#group) Gerrit administrators can use this group to globally
block certain access rights for this group on the `All-Projects`
project so that by default service users cannot do these operations.

E.g. blocking push on `refs/heads/*` and `refs/meta/config` would
prevent service users from pushing commits, while they still may push
tags.

Gerrit administrators can make exceptions for certain service users by
removing them from the group for which access rights are blocked.
