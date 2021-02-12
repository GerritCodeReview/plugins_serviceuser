@PLUGIN@ register
=================

NAME
----
@PLUGIN@ register - Registers an existing user as a service user

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ register
  --creator <CREATOR>
  --owner <OWNER>
  <USERNAME>
```

DESCRIPTION
-----------
Registers an existing user as a service user.

ACCESS
------
Caller must be a member of a group that is granted the
'Create Service User' capability (provided by this plugin) or the
'Administrate Server' capability. If not possessing the 'Administrate
Server' capability, the user to be registered as a service user must
also be the caller.

SCRIPTING
---------
This command is intended to be used in scripts.

OPTIONS
-------

`--creator`
:	Username of the user that will be set as the creator of the
    serviceuser. Defaults to the caller.

`--owner`
:   ID or name of the group that will own the service user. Defaults
    to no owner group being set.

EXAMPLES
--------
Register a service user:

```
  $ ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ register --creator admin --owner Administrators
```
