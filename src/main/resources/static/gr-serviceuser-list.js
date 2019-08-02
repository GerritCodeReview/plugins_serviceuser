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

(function () {
  'use strict';

  Polymer({
    is: 'gr-serviceuser-list',
    _legacyUndefinedCheck: true,

    properties: {
      _serviceUsers: Array,
      _loading: {
        type: Boolean,
        value: true,
      },
    },

    behaviors: [Gerrit.ListViewBehavior,],

    attached() {
      this.fire('title-change', { title: 'Service Users' });
      this._getServiceUsers();
    },

    _getServiceUsers() {
      const errFn = response => {
        this.fire('page-error', { response });
      };
      return this.plugin.restApi('/config/server/serviceuser~serviceusers/').get('')
        .then(serviceUsers => {
          if (!serviceUsers) {
            this._serviceUsers = [];
            return;
          }
          this._serviceUsers = Object.keys(serviceUsers)
            .map(key => {
              const serviceUser = serviceUsers[key];
              serviceUser.username = key;
              return serviceUser;
            });
          this._loading = false;
        });
    },

    _active(item) {
      return item.inactive === true ? 'Inactive' : 'Active';
    },

    _getCreator(item) {
      if (item.created_by.username != undefined) {
        return item.created_by.username;
      }
      if (item.created_by._account_id != -1) {
        return item.created_by._account_id;
      }

      return "N/A";
    },

    _getOwnerGroup(item) {
      if (item.owner) {
        return item.owner.name;
      }
    },
  })
})();
