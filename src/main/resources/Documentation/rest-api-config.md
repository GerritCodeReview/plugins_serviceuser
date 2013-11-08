@PLUGIN@ - /config/ REST API
============================

This page describes the REST endpoints that are added by the @PLUGIN@.

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
----
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


<a id="json-entities">JSON Entities
-----------------------------------

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
