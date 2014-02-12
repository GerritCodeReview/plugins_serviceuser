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

As response a [ServiceUserInfo](#service-user-info) entity is returned
that describes the created account.

#### Response

```
  HTTP/1.1 201 Created
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "created_by": "jdoe",
    "created_at": "Thu, 21 Nov 2013 15:00:55 +0100",
    "_account_id": 1000195,
    "name": "JenkinsVoter",
    "avatars": []
  }
```

### <a id="get-service-user"> Get Service User
_GET /config/server/@PLUGIN@~serviceusers/\{username\}_

Gets a service user.

In order to be able to see a service user the caller must be

* a member of the owner group,
* the creator of the service user if no owner group is assigned or
* a member of a group that is granted the 'Administrate Server' capability.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/JenkinsVoter HTTP/1.0
```

As response a [ServiceUserInfo](#service-user-info) entity is returned
that describes the service user.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "created_by": "jdoe",
    "created_at": "Thu, 21 Nov 2013 15:00:55 +0100",
    "_account_id": 1000195,
    "name": "JenkinsVoter",
    "username": "JenkinsVoter",
    "avatars": []
  }
```

### <a id="list-service-users"> List Service Users
GET /config/server/@PLUGIN@~serviceusers/_

Lists service users.

In order to be able to see a service user the caller must be

* a member of the owner group,
* the creator of the service user if no owner group is assigned or
* a member of a group that is granted the 'Administrate Server' capability.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/ HTTP/1.0
```

As response a map is returned that maps the username to a
[ServiceUserInfo](#service-user-info) entity. The username in
ServiceUserInfo is not set since it is already available as map key.

#### Response

```
  HTTP/1.1 201 Created
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "GlobalVerifier": {
      "created_by": "jdoe",
      "created_at": "Mon, 27 Jan 2014 21:00:12 +0100",
      "_account_id": 1000107,
      "name": "GlobalVerifier",
      "avatars": []
    },
    "JenkinsVoter": {
      "created_by": "jdoe",
      "created_at": "Thu, 21 Nov 2013 15:00:55 +0100",
      "_account_id": 1000195,
      "name": "JenkinsVoter",
      "avatars": []
    }
  }
```

### <a id="list-ssh-keys"> List SSH keys
GET /config/server/@PLUGIN@~serviceusers/\{username\}/sshkeys/_

Lists the SSH keys of a service user.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/JenkinsVoter/sshkeys/ HTTP/1.0
```

As response a list of [SshKeyInfo](../../../Documentation/rest-api-accounts.html#ssh-key-info)
entities is returned.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  [
    {
      "seq": 1,
      "ssh_public_key": "ssh-rsa AAAAB3NzaC1...",
      "encoded_key": "AAAAB3NzaC1...",
      "algorithm": "ssh-rsa",
      "comment": "jenkins.voter@gerrit.com",
      "valid": true
    }
  ]
```

### <a id="get-ssh-key"> Get SSH key
GET /config/server/@PLUGIN@~serviceusers/\{username\}/sshkeys/[\{ssh-key-id\}](../../../Documentation/rest-api-accounts.html#ssh-key-id)_

Gets an SSH key of a service user.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/JenkinsVoter/sshkeys/1 HTTP/1.0
```

As response an [SshKeyInfo](../../../Documentation/rest-api-accounts.html#ssh-key-info)
entity is returned.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "seq": 1,
    "ssh_public_key": "ssh-rsa AAAAB3NzaC1...",
    "encoded_key": "AAAAB3NzaC1...",
    "algorithm": "ssh-rsa",
    "comment": "jenkins.voter@gerrit.com",
    "valid": true
  }
```

### <a id="add-ssh-key"> Add SSH key
POST /config/server/@PLUGIN@~serviceusers/\{username\}/sshkeys_

Adds an SSH key for a service user.

#### Request

```
  POST /config/server/@PLUGIN@~serviceusers/JenkinsVoter/sshkeys HTTP/1.0
  Content-Type: plain/text

  AAAAB3NzaC1yc2EAAAABIwAAAQEA0T...YImydZAw\u003d\u003d
```

As response an [SshKeyInfo](../../../Documentation/rest-api-accounts.html#ssh-key-info)
entity is returned that describes the new SSH key.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  [
    {
      "seq": 2,
      "ssh_public_key": "ssh-rsa AAAAB1NzaA2...",
      "encoded_key": "AAAAB1NzaA2...",
      "algorithm": "ssh-rsa",
      "comment": "jenkins.voter@gerrit.com",
      "valid": true
    }
  ]
```

### <a id="delete-ssh-key"> Delete SSH key
DELETE /config/server/@PLUGIN@~serviceusers/\{username\}/sshkeys/[\{ssh-key-id\}](../../../Documentation/rest-api-accounts.html#ssh-key-id)_

Deletes an SSH key of a service user.

#### Request

```
  DELETE /config/server/@PLUGIN@~serviceusers/JenkinsVoter/sshkeys/2 HTTP/1.0
```

#### Response

```
  HTTP/1.1 204 No Content
```

### <a id="get-full-name"> Get Full Name
GET /config/server/@PLUGIN@~serviceusers/\{username\}/name_

Retrieves the full name of a service user.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/JenkinsVoter/name HTTP/1.0
```

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  "Jenkins Voter"
```

If the service user does not have a name an empty string is returned.

### <a id="set-full-name"> Set Full Name
PUT /config/server/@PLUGIN@~serviceusers/\{username\}/name_

Sets the full name of a service user.

The new full name must be provided in the request body inside a
[AccountNameInput](../../../Documentation/rest-api-accounts.html#account-name-input)
entity.

#### Request

```
  PUT /config/server/@PLUGIN@~serviceusers/JenkinsVoter/name HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "name": "Jenkins Voter"
  }
```

As response the new full name is returned.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  "Jenkins Voter"
```

If the name was deleted the response is "`204 No Content`".

Some realms may not allow to modify the full name. In this case the
request is rejected with "`405 Method Not Allowed`".

### <a id="delete-full-name"> Delete Full Name
DELETE /config/server/@PLUGIN@~serviceusers/\{username\}/name_

Deletes the full name of a service user.

#### Request

```
  DELETE /config/server/@PLUGIN@~serviceusers/JenkinsVoter/name HTTP/1.0
```

As response the new full name is returned.

#### Response

```
  HTTP/1.1 204 No Content
```

Some realms may not allow to modify the full name. In this case the
request is rejected with "`405 Method Not Allowed`".

### <a id="get-email"> Get Email
GET /config/server/@PLUGIN@~serviceusers/\{username\}/email_

Retrieves the (preferred) email of a service user.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/JenkinsVoter/email HTTP/1.0
```

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  "jenkins.voter@gerrit.com"
```

If the service user does not have an email address an empty string is returned.

### <a id="set-email"> Set Email
PUT /config/server/@PLUGIN@~serviceusers/\{username\}/email_

Sets the (preferred) email of a service user.

The new email must be provided in the request body inside a
[EmailInput](#email-input) entity.

#### Request

```
  PUT /config/server/@PLUGIN@~serviceusers/JenkinsVoter/email HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "email": "jenkins.voter@gerrit.com"
  }
```

As response the new email is returned.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  "jenkins.voter@gerrit.com"
```

If the email was deleted the response is "`204 No Content`".

Some realms may not allow to modify the email. In this case the
request is rejected with "`405 Method Not Allowed`".

### <a id="delete-email"> Delete Email
DELETE /config/server/@PLUGIN@~serviceusers/\{username\}/email_

Deletes the email of a service user.

#### Request

```
  DELETE /config/server/@PLUGIN@~serviceusers/JenkinsVoter/email HTTP/1.0
```

As response the new email is returned.

#### Response

```
  HTTP/1.1 204 No Content
```

Some realms may not allow to modify the email. In this case the
request is rejected with "`405 Method Not Allowed`".

### <a id="get-active"> Get Active
GET /config/server/@PLUGIN@~serviceusers/\{username\}/active_

Checks if a service user is active.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/JenkinsVoter/active HTTP/1.0
```

If the service user is active the string `ok` is returned.

#### Response

```
  HTTP/1.1 200 OK

  ok
```

If the service user is inactive the response is `204 No Content`.

### <a id="set-active"> Set Active
PUT /config/server/@PLUGIN@~serviceusers/\{username\}/active_

Sets the service user state to active.

#### Request

```
  PUT /config/server/@PLUGIN@~serviceusers/JenkinsVoter/active HTTP/1.0
```

#### Response

```
  HTTP/1.1 201 Created
```

If the service user was already active the response is `200 OK`.

### <a id="delete-active"> Delete Active
DELETE /config/server/@PLUGIN@~serviceusers/\{username\}/active_

Sets the service user state to inactive.

#### Request

```
  DELETE /config/server/@PLUGIN@~serviceusers/JenkinsVoter/active HTTP/1.0
```

#### Response

```
  HTTP/1.1 204 No Content
```

### <a id="get-owner"> Get Owner
GET /config/server/@PLUGIN@~serviceusers/\{username\}/owner_

Retrieves the owner group of the service user.

#### Request

```
  GET /config/server/@PLUGIN@~serviceusers/JenkinsVoter/owner HTTP/1.0
```

The owner group is returned as a
[GroupInfo](../../../Documentation/rest-api-groups.html#group-info)
entity.

#### Response

```
  HTTP/1.1 200 OK
  Content-Type: application/json;charset=UTF-8

  {
    "kind": "gerritcodereview#group",
    "url": "#/admin/groups/uuid-2a97064e13ebc5c64b963d09a66219e539854226",
    "options": {},
    "description": "Jenkins Administrators",
    "group_id": 10,
    "owner": "JenkinsAdmins",
    "owner_id": "2a97064e13ebc5c64b963d09a66219e539854226",
    "id": "2a97064e13ebc5c64b963d09a66219e539854226",
    "name": "JenkinsAdmins"
  }
```

If no owner group for the service user is set the response is `200 OK`
without any content in the request body.

If an owner group is set but the group is not visible to the caller or
doesn't exist anymore the response is `404 Not Found`.

### <a id="set-owner"> Set Owner
PUT /config/server/@PLUGIN@~serviceusers/\{username\}/owner_

Sets the owner group for a service user.

The owner group must be specified in the request body as a
[OwnerInput](#owner-input) entity.

#### Request

```
  PUT /config/server/@PLUGIN@~serviceusers/JenkinsVoter/owner HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "group": "JenkinsAdmins"
  }
```

As response the new owner group is returned as a
[GroupInfo](../../../Documentation/rest-api-groups.html#group-info)
entity.

#### Response

```
  HTTP/1.1 200 OK
  Content-Type: application/json;charset=UTF-8

  {
    "kind": "gerritcodereview#group",
    "url": "#/admin/groups/uuid-2a97064e13ebc5c64b963d09a66219e539854226",
    "options": {},
    "description": "Jenkins Administrators",
    "group_id": 10,
    "owner": "JenkinsAdmins",
    "owner_id": "2a97064e13ebc5c64b963d09a66219e539854226",
    "id": "2a97064e13ebc5c64b963d09a66219e539854226",
    "name": "JenkinsAdmins"
  }
```

If the service user didn't have an owner group before the response is
`201 Created`.

If the owner group of the service user is deleted the response is
`204 No Content`.

### <a id="delete-owner"> Delete Owner
DELETE /config/server/@PLUGIN@~serviceusers/\{username\}/owner_

Delete the owner group of a service user.

#### Request

```
  DELETE /config/server/@PLUGIN@~serviceusers/JenkinsVoter/owner HTTP/1.0
```

#### Response

```
  HTTP/1.1 204 No Content
```

### <a id="get-config"> Get Config
_GET /config/server/@PLUGIN@~config_

Gets the configuration of the @PLUGIN@ plugin.

#### Request

```
  GET /config/server/@PLUGIN@~config HTTP/1.0
```

As response a [ConfigInfo](#config-info) entity is returned that
contains the configuration.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "on_success": "Don\u0027t forget to assign \u003ca href\u003d\"Documentation/access-control.html\"\u003eaccess rights\u003c/a\u003e to the service user."
  }
```

### <a id="put-config"> Put Config
_PUT /config/server/@PLUGIN@~config_

Sets the configuration of the @PLUGIN@ plugin.

The new configuration must be specified as a [ConfigInfo](#config-info)
entity in the request body. Not setting a parameter leaves the parameter
unchanged.

#### Request

```
  PUT /config/server/@PLUGIN@~config HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "info": "Please find more information about service users in the <a href\"wiki.html\">wiki</a>."
  }
```


<a id="json-entities">JSON Entities
-----------------------------------

### <a id="config-info"></a>ConfigInfo

The `ConfigInfo` entity contains configuration of the @PLUGIN@ plugin.

* _info_: HTML formatted message that should be displayed on the
  service user creation screen.
* _on\_success_: HTML formatted message that should be displayed after
  a service user was successfully created.
* _allow\_email_: Whether it is allowed to provide an email address for
  a service user (not set if `false`).

### <a id="email-input"></a>EmailInput

The `EmailInput` entity contains a new email address.

* _email_: The new email address.

### <a id="owner-input"></a>OwnerInput

The `OwnerInput` entity contains a group that should own a service user.

* _group_: A group. This can be the UUID of the group, the legacy
  numeric ID of the group or the name of the group if it is unique.

### <a id="service-user-info"></a>ServiceUserInfo

The `ServiceUserInfo` entity contains information about a service user.
It has the same fields as a detailed
[AccountInfo](../../../Documentation/rest-api-accounts.html#account-info)
and in addition the following fields:

* _created\_by_: The username of the user that created this service
  user.
* _created\_at_: The date when the service user was created in the
  format 'EEE, dd MMM yyyy HH:mm:ss Z'.
* _inactive_: Whether the account state of the service user is
  inactive. Not set if the account is active.
* _owner_: The owner group of the service user as a
  [GroupInfo](../../../Documentation/rest-api-groups.html#group-info)
  entity. Not set if no owner group is assigned.

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
