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
(function () {
  'use strict';

  const JSON_PREFIX = ')]}\'';

  Polymer({
    is: 'gr-serviceuser-ssh-panel',
    _legacyUndefinedCheck: true,

    properties: {
      _restApi: Object,
      _serviceUser: Object,
      _keys: Array,
      /** @type {?} */
      _keyToView: Object,
      _newKey: {
        type: String,
        value: '',
      },
      _keysToRemove: {
        type: Array,
        value() { return []; },
      },
    },

    loadData(restApi, serviceUser) {
      this._restApi = restApi;
      this._serviceUser = serviceUser;
      return this._restApi.get(`${this._serviceUser._account_id}/sshkeys`)
        .then(keys => {
          if (!keys) {
            this._keys = [];
            return;
          }
          this._keys = keys;
        });
    },

    _getStatusLabel(isValid) {
      return isValid ? 'Valid' : 'Invalid';
    },

    _showKey(e) {
      const el = Polymer.dom(e).localTarget;
      const index = parseInt(el.getAttribute('data-index'), 10);
      this._keyToView = this._keys[index];
      this.$.viewKeyOverlay.open();
    },

    _closeOverlay() {
      this.$.viewKeyOverlay.close();
    },

    _handleDeleteKey(e) {
      const el = Polymer.dom(e).localTarget;
      const index = parseInt(el.getAttribute('data-index'), 10);
      this.push('_keysToRemove', this._keys[index]);

      const promises = this._keysToRemove.map(key => {
        this._restApi.delete(`${this._serviceUser._account_id}/sshkeys/${key.seq}`);
      });

      return Promise.all(promises).then(() => {
        this.splice('_keys', index, 1);
        this._keysToRemove = [];
      });
    },

    _handleAddKey() {
      this.$.addButton.disabled = true;
      this.$.newKey.disabled = true;
      return this.$.restAPI.send(
          'POST',
          `/config/server/serviceuser~serviceusers/${this._serviceUser._account_id}/sshkeys`,
          this._newKey.trim(),
          undefined,
          'plain/text')
        .then(response => {
          this.$.newKey.disabled = false;
          this._newKey = '';
          let key;
          response.text().then(text => {
            let result;
            try {
              result = JSON.parse(text.substring(JSON_PREFIX.length));
            } catch (_) {
              result = null;
            }
            this.push('_keys', result);
          })
        }).catch(() => {
          this.$.addButton.disabled = false;
          this.$.newKey.disabled = false;
        });
    },

    _computeAddButtonDisabled(newKey) {
      return !newKey.length;
    },
  });
})();
