/**
 * @license
 * Copyright (C) 2019 The Android Open Source Project
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
    is: 'gr-serviceuser-http-password',
    _legacyUndefinedCheck: true,

    properties: {
      _restApi: Object,
      _serviceUser: Object,
      _generatedPassword: String,
      _passwordUrl: String,
    },

    loadData(restApi, serviceUser) {
      this._restApi = restApi;
      this._serviceUser = serviceUser;
    },

    _handleGenerateTap() {
      this._generatedPassword = 'Generating...';
      this.$.generatedPasswordOverlay.open();
      this._restApi
          .put(`${this._serviceUser._account_id}/password.http`,
               {generate: true})
          .then(newPassword => {
            this._generatedPassword = newPassword;
          });
    },

    _closeOverlay() {
      this.$.generatedPasswordOverlay.close();
    },

    _generatedPasswordOverlayClosed() {
      this._generatedPassword = '';
    },

    _handleDelete() {
      this._restApi.delete(`${this._serviceUser._account_id}/password.http`);
    },
  });
})();
