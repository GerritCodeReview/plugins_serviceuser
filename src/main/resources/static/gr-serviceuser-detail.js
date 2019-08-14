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
    },

    behaviors: [
      Gerrit.ListViewBehavior,
    ],

    attached() {
      this._extractUserId();

      if (!this._serviceUserId) { return; }

      Promise.resolve(this._getServiceUser()).then(() => {
        this.$.sshEditor.loadData(this._restApi, this._serviceUser);

        this.fire('title-change', {title: this._serviceUser.name});
        this._loading = false;
      });
    },

    _computeLoadingClass(loading) {
      return loading ? 'loading' : '';
    },

    _extractUserId() {
      this._serviceUserId = this.baseURI.split('/').pop();
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
  });
})();
