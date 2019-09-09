// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

(function() {
  'use strict';

  const NOT_FOUND_MESSAGE = 'Not Found';

  Polymer({
    is: 'gr-serviceuser-detail',
    _legacyUndefinedCheck: true,

    properties: {
      _restApi: Object,
      _serviceUserId: String,
      _serviceUser: Object,
      _loading: {
        type: Boolean,
        value: true,
      },
      _statusButtonText: {
        type: String,
        value: 'Activate',
      },
      _prefsChanged: {
        type: Boolean,
        value: false,
      },
      _changingPrefs: {
        type: Boolean,
        value: false,
      },
      _isAdmin: {
        type: Boolean,
        value: false,
      },
      _allowEmail: {
        type: Boolean,
        value: false,
      },
      _allowOwner: {
        type: Boolean,
        value: false,
      },
      _allowHttpPassword: {
        type: Boolean,
        value: false,
      },
      _newFullName: String,
      _newEmail: String,
      _availableOwners: Array,
      _newOwner: String,
      _ownerChangeWarning: String,
      _query: {
        type: Function,
        value() {
          return this._getGroupSuggestions.bind(this);
        },
      },
    },

    behaviors: [
      Gerrit.ListViewBehavior,
    ],

    attached() {
      this._extractUserId();
      this._loadServiceUser();
    },

    _loadServiceUser() {
      if (!this._serviceUserId) { return; }

      const promises = [];

      promises.push(this._getPluginConfig());
      promises.push(this._getServiceUser());

      Promise.all(promises).then(() => {
        this.$.sshEditor.loadData(this._restApi, this._serviceUser);
        this.$.httpPass.loadData(this._restApi, this._serviceUser);

        this.fire('title-change', {title: this._serviceUser.name});
        this._computeStatusButtonText();
        this._loading = false;
      });
    },

    _computeLoadingClass(loading) {
      return loading ? 'loading' : '';
    },

    _extractUserId() {
      this._serviceUserId = this.baseURI.split('/').pop();
    },

    _getPermissions() {
      return this.plugin.restApi('/accounts/self/capabilities/').get('')
          .then(capabilities => {
            this._isAdmin = capabilities && capabilities.administrateServer;
          });
    },

    _getPluginConfig() {
      return Promise.resolve(this._getPermissions()).then(() => {
        this.plugin.restApi('/config/server/serviceuser~config/').get('')
            .then(config => {
              if (!config) {
                return;
              }
              this._allowEmail = config.allow_email || this._isAdmin;
              this._allowOwner = config.allow_owner || this._isAdmin;
              this._allowHttpPassword = config.allow_http_password
                || this._isAdmin;
            });
      });
    },

    _getServiceUser() {
      this._restApi = this.plugin.restApi(
          '/config/server/serviceuser~serviceusers/');
      return this._restApi.get(this._serviceUserId)
          .then(serviceUser => {
            if (!serviceUser) {
              this._serviceUser = {};
              return;
            }
            this._serviceUser = serviceUser;
          });
    },

    _active(serviceUser) {
      if (!serviceUser) {
        return NOT_FOUND_MESSAGE;
      }

      return serviceUser.inactive === true ? 'Inactive' : 'Active';
    },

    _computeStatusButtonText() {
      if (!this._serviceUser) {
        return;
      }

      this._statusButtonText = this._serviceUser.inactive === true
        ? 'Activate'
        : 'Deactivate';
    },

    _toggleStatus() {
      if (this._serviceUser.inactive === true) {
        this._restApi.put(`${this._serviceUser._account_id}/active`)
            .then(() => {
              this._loadServiceUser();
            });
      } else {
        this._restApi.delete(`${this._serviceUser._account_id}/active`)
            .then(() => {
              this._loadServiceUser();
            });
      }
    },

    _getCreator(serviceUser) {
      if (!serviceUser || !serviceUser.created_by) {
        return NOT_FOUND_MESSAGE;
      }

      if (serviceUser.created_by.username != undefined) {
        return serviceUser.created_by.username;
      }

      if (serviceUser.created_by._account_id != -1) {
        return serviceUser.created_by._account_id;
      }

      return NOT_FOUND_MESSAGE;
    },

    _getOwnerGroup(serviceUser) {
      return serviceUser && serviceUser.owner
        ? serviceUser.owner.name
        : NOT_FOUND_MESSAGE;
    },

    _isEmailValid(email) {
      if (!email) {
        return false;
      }
      return email.includes('@');
    },

    _getGroupSuggestions(input) {
      let query;
      if (!input || input === this._getOwnerGroup(this._serviceUser)) {
        query = '';
      } else {
        query = `?suggest=${input}`;
      }

      return this.plugin.restApi('/a/groups/').get(query)
          .then(response => {
            const groups = [];
            for (const key in response) {
              if (!response.hasOwnProperty(key)) { continue; }
              groups.push({
                name: key,
                value: decodeURIComponent(response[key].id),
              });
            }
            this._availableOwners = groups;
            return groups;
          });
    },

    _isOwnerValid(owner) {
      if (!owner) {
        return false;
      }

      return this._getOwnerName(owner);
    },

    _isNewOwner() {
      return this._getOwnerName(this._newOwner)
         === this._getOwnerGroup(this._serviceUser);
    },

    _getOwnerName(id) {
      return this._availableOwners.find(o => { return o.value === id; }).name;
    },

    _computeOwnerWarning() {
      let message = 'If ';
      message += this._getOwnerGroup(this._serviceUser) != NOT_FOUND_MESSAGE
        ? 'the owner group is changed' : 'an owner group is set';
      message += ' only members of the ';
      message += this._getOwnerGroup(this._serviceUser) != NOT_FOUND_MESSAGE
        ? 'new ' : '';
      message += 'owner group can see and administrate the service user.';
      message += this._getOwnerGroup(this._serviceUser) != NOT_FOUND_MESSAGE
        ? '' : ' The creator of the service user can no'
          + ' longer see and administrate the service user if she/he'
          + ' is not member of the owner group.';
      this._ownerChangeWarning = message;
    },

    _computePrefsChanged() {
      if (this.loading || this._changingPrefs) {
        return;
      }

      if (!this._newOwner && !this._newEmail && !this._newFullName) {
        this._prefsChanged = false;
        return;
      }

      if (this._newEmail && !this._isEmailValid(this._newEmail)) {
        this._prefsChanged = false;
        return;
      }

      if (this._newOwner
          && (this._isNewOwner() || !this._isOwnerValid(this._newOwner))) {
        this._prefsChanged = false;
        return;
      }

      if (this._newOwner) {
        this._computeOwnerWarning();
      }

      this._prefsChanged = true;
    },

    _applyNewFullName() {
      return this._restApi
          .put(`${this._serviceUser._account_id}/name`,
               {name: this._newFullName})
          .then(() => {
            this.$.serviceUserFullNameInput.value = '';
          });
    },

    _applyNewEmail(email) {
      if (!this._isEmailValid(email)) {
        return;
      }
      return this._restApi
          .put(`${this._serviceUser._account_id}/email`, {email})
          .then(() => {
            this.$.serviceUserEmailInput.value = '';
          });
    },

    _applyNewOwner(owner) {
      if (this._isNewOwner() || !this._isOwnerValid(this._newOwner)) {
        return;
      }
      return this._restApi
          .put(`${this._serviceUser._account_id}/owner`, {group: owner})
          .then(() => {
            this.$.serviceUserOwnerInput.text = this._getOwnerGroup(
                this._serviceUser);
          });
    },

    _handleSavePreferences() {
      const promises = [];
      this._changingPrefs = true;

      if (this._newFullName) {
        promises.push(this._applyNewFullName());
      }

      if (this._newEmail) {
        promises.push(this._applyNewEmail(this._newEmail));
      }

      if (this._newOwner) {
        promises.push(this._applyNewOwner(this._newOwner));
      }

      Promise.all(promises).then(() => {
        this._changingPrefs = false;
        this._prefsChanged = false;
        this._ownerChangeWarning = '';
        this._loadServiceUser();
      });
    },
  });
})();
