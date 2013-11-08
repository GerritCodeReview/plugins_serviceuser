@PLUGIN@ create
===============

NAME
----
@PLUGIN@ create - Creates a new service user

SYNOPSIS
--------
```
ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ create
  --ssh-key - | <KEY>
  <USERNAME>
```

DESCRIPTION
-----------
Creates a service user.

ACCESS
------
Caller must be a member of a group that is granted the
'Create Service User' capability (provided by this plugin) or the
'Administrate Server' capability.

SCRIPTING
---------
This command is intended to be used in scripts.

OPTIONS
-------

`--ssh-key`
:	Content of the public SSH key to load into the account's
	keyring.  If `-` the key is read from stdin, rather than
	from the command line.

EXAMPLES
--------
Create a service user:

```
  $ cat ~/.ssh/id_rsa.pub | ssh -p @SSH_PORT@ @SSH_HOST@ @PLUGIN@ create --ssh-key - JenkinsVoter
```
