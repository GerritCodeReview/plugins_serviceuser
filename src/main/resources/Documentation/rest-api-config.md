@PLUGIN@ - /config/ REST API
============================

This page describes the REST endpoints that are added by the @PLUGIN@
plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="project-endpoints"> Service User Endpoints
-------------------------------------------------

### <a id="create-service-user"> Create Service User
_POST /config/server/@PLUGIN@~serviceusers/\{username\}_

Creates a service user.

The public SSH key for creating the service user must be specified in
the request body as a [ServiceUserInput](#service-user-input) entity.

Caller must be a member of a group that is granted the 'Create Service
User' capability (provided by this plugin) or the 'Administrate Server'
capability.

#### Request

```
  PUT /config/server/@PLUGIN@~serviceusers/JenkinsVoter HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "ssh_key": "ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA0T...YImydZAw=="
  }
```

As response a detailed [AccountInfo](../../../Documentation/rest-api-accounts.html#account-info)
entity is returned that describes the created account.

#### Response

```
  HTTP/1.1 201 Created
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "_account_id": 1000195,
    "name": "JenkinsVoter",
    "avatars": []
  }
```

### <a id="get-messages"> Get Messages
_GET /config/server/@PLUGIN@~messages_

Gets help messages to be displayed for the service user creation in the
Web UI.

#### Request

```
  GET /config/server/@PLUGIN@~messages HTTP/1.0
```

As response a [MessagesInfo](#messages-info) entity is returned that
contains the messages.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "on_success": "Don't forget to assign \u003ca href\u003d\"Documentation/access-control.html\"\u003eaccess rights\u003c/a\u003e to the service user."
  }
```

### <a id="put-messages"> Put Messages
_PUT /config/server/@PLUGIN@~messages_

Sets the help messages that are displayed for the service user creation
in the Web UI.

The new messages must be specified as [MessagesInfo](#messages-info)
entity in the request body. Not setting a message leaves the message
unchanged.

#### Request

```
  PUT /config/server/@PLUGIN@~messages HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "info": "Please find more information about service users in the \u003ca href\u003d\"wiki.html\"\u003ewiki\u003c/a\u003e."
  }
```


<a id="json-entities">JSON Entities
-----------------------------------

### <a id="messages-info"></a>MessagesInfo

The `MessagesInfo` entity contains help messages that should be
displayed for the service user creation in the Web UI.

* _info_: HTML formatted message that should be displayed in the
  service user creation dialog.
* _on\_success_: HTML formatted message that should be displayed after
  a service user was successfully created.

### <a id="service-user-input"></a>ServiceUserInput

The `ServiceUserInput` entity contains options for creating a service
user.

* _ssh\_key_: Content of the public SSH key to load into the account's keyring.

SEE ALSO
--------

* [Config related REST endpoints](../../../Documentation/rest-api-config.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
