This plugin allows to create service users in Gerrit.

A service user is a user that is used by another service to communicate
with Gerrit. E.g. a service user is needed to run the
[Gerrit Trigger Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Gerrit+Trigger)
in Jenkins. A service user is not able to login into the Gerrit WebUI
and it cannot push commits or tags.

This plugin supports the creation of service users via [SSH](cmd-create.md) and
[REST](rest-api-config.md).

To create a service user a user must be a member of a group that is
granted the 'Create Service User' capability (provided by this plugin)
or the 'Administrate Server' capability.

The plugin can be [configured to automatically add new service users to
groups](config.md#group). This allows to automatically assign or
block certain access rights for the service users.

For each created service user the plugin stores some
[properties](#properties).

<a id="properties"></a>
Service User Properties
-----------------------
The service user properties are stored in the `refs/meta/config` branch
of the `All-Projects` project in the file `@PLUGIN@.db`, which is a
Git config file:

```
  [user "build-bot"]
    createdBy = jdoe
    createdAt = Wed, 13 Nov 2013 14:31:11 +0100
  [user "voter"]
    createdBy = jroe
    createdAt = Wed, 13 Nov 2013 14:45:00 +0100
```

<a id="createdBy">
`user.<service-user-name>.createdBy`
: The username of the user who created the service user.

<a id="createdAt">
`user.<service-user-name>.createdAt`
: The date when the service user was created.

