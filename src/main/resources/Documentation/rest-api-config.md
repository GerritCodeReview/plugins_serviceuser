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

As response a map is returned that maps the username to an
[AccountInfo](../../../Documentation/rest-api-accounts.html#account-info)
entity.

#### Response

```
  HTTP/1.1 201 Created
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "GlobalVerifier": {
      "_account_id": 1000107,
      "name": "GlobalVerifier",
      "username": "GlobalVerifier",
      "avatars": []
    },
    "JenkinsVoter": {
      "_account_id": 1000195,
      "name": "JenkinsVoter",
      "username": "JenkinsVoter",
      "avatars": []
    }
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
