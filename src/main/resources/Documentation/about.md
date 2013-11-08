This plugin allows to create service users in Gerrit.

A service user is a user that is used by another service to communicate
with Gerrit. E.g. a service user is needed to run the
[Gerrit Trigger Plugin](https://wiki.jenkins-ci.org/display/JENKINS/Gerrit+Trigger)
in Jenkins. A service user is not able to login into the Gerrit WebUI
and it cannot push commits or tags.

This plugin supports the creation of service users via [SSH](cmd-create.html),
[REST](rest-api-config.html) and in the [WebUI](#webui).

To create a service user a user must be a member of a group that is
granted the 'Create Service User' capability (provided by this plugin)
or the 'Administrate Server' capability.

The plugin can be [configured to automatically add new service users to
groups](config.html#group). This allows to automatically assign or
block certain access rights for the service users.

<a id="webui"></a>
Create Service User in WebUI
----------------------------
In the `People` top menu there is a menu item `Create Service User`
that opens a dialog for creating a service user.
