/**
 * @license
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function() {
  'use strict';

  Polymer({
    is: 'gr-serviceuser-create',
    _legacyUndefinedCheck: true,

    properties: {
      _infoMessageEnabled: {
        type: Boolean,
        value: false,
      },
      _infoMessage: String,
      _successMessageEnabled: {
        type: Boolean,
        value: false,
      },
      _successMessage: String,
      _newUsername: String,
      _emailEnabled: {
        type: Boolean,
        value: false,
      },
      _newEmail: String,
      _newKey: String,
      _dataValid: {
        type: Boolean,
        value: false,
      },
      _isAdding: {
        type: Boolean,
        value: false,
      },
      _enableButton: {
        type: Boolean,
        value: false,
      },
      _accountId: String,
    },

    attached() {
      this._getConfig();
    },

    _forwardToDetails() {
      page.show(
          this.plugin.screenUrl()
          + '/user/'
          + this._accountId);
    },

    _getConfig() {
      return this.plugin.restApi('/config/server/serviceuser~config/').get('')
          .then(config => {
            if (!config) {
              return;
            }

            if (config.info && config.info != '') {
              this._infoMessageEnabled = true;
              this._infoMessage = config.info;
              this.$.infoMessage.innerHTML = this._infoMessage;
            }

            if (config.on_success && config.on_success != '') {
              this._successMessageEnabled = true;
              this._successMessage = config.on_success;
              this.$.successMessage.innerHTML = this._successMessage;
            }

            this._emailEnabled = config.allow_email;
          });
    },

    _validateData() {
      this._dataValid = this._validateName(this._newUsername)
        && this._validateEmail(this._newEmail)
        && this._validateKey(this._newKey);
      this._computeButtonEnabled();
    },

    _validateName(username) {
      if (username && username.trim().length > 0) {
        return true;
      }

      return false;
    },

    _validateEmail(email) {
      if (!email || email.trim().length == 0 || email.includes('@')) {
        return true;
      }

      return false;
    },

    _validateKey(key) {
      if (!key || !key.trim()) {
        return false;
      }

      return true;
    },

    _computeButtonEnabled() {
      this._enableButton = this._dataValid && !this._isAdding;
    },

    _handleCreateServiceUser() {
      this._isAdding = true;
      this._computeButtonEnabled();
      const body = {
        ssh_key: this._newKey.trim(),
        email: this._newEmail ? this._newEmail.trim() : null,
      };
      return this.plugin.restApi('/config/server/serviceuser~serviceusers/')
          .post(this._newUsername, body)
          .then(response => {
            this._accountId = response._account_id;
            if (this._successMessage) {
              this.$.successDialogOverlay.open();
            } else {
              this._forwardToDetails();
            }
          }).catch(response => {
            this.fire('show-error', {message: response});
            this._isAdding = false;
            this._computeButtonEnabled();
          });
    },
  });
})();
