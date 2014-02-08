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

### <a id="get-service-user"> Get Service User
_GET /config/server/@PLUGIN@~serviceusers/\{username\}_

Gets a service user.

In order to be able to see a service user the caller must have created
that service user or be a member of a group that is granted the
'Administrate Server' capability.

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

In order to see a service user the caller must have created that service
user or be a member of a group that is granted the 'Administrate Server'
capability.

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

* _info_: HTML formatted message that should be displayed in the
  service user creation dialog.
* _on\_success_: HTML formatted message that should be displayed after
  a service user was successfully created.
* _allow\_email_: Whether it is allowed to provide an email address for
  a service user (not set if `false`).

### <a id="service-user-info"></a>ServiceUserInfo

The `ServiceUserInfo` entity contains information about a service user.
It has the same fields as a detailed
[AccountInfo](../../../Documentation/rest-api-accounts.html#account-info)
and in addition the following fields:

* _created\_by_: The username of the user that created this service
  user.
* _created\_at_: The date when the service user was created in the
  format 'EEE, dd MMM yyyy HH:mm:ss Z'.

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
