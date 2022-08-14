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

import {htmlTemplate} from './gr-serviceuser-list_html.js';

const NOT_FOUND_MESSAGE = 'Not Found';

export class GrServiceUserList extends Polymer.GestureEventListeners(
    Polymer.Element) {
  /** @returns {?} template for this component */
  static get template() { return htmlTemplate; }

  /** @returns {string} name of the component */
  static get is() { return 'gr-serviceuser-list'; }

  /**
   * Defines properties of the component
   *
   * @returns {?}
   */
  static get properties() {
    return {
      _canCreate: {
        type: Boolean,
        value: false,
      },
      _serviceUsers: Array,
      _loading: {
        type: Boolean,
        value: true,
      },
    };
  }

  static get behaviors() {
    return [
      Gerrit.ListViewBehavior,
    ];
  }

  connectedCallback() {
    super.connectedCallback();
    this.dispatchEvent(
        new CustomEvent(
            'title-change',
            {detail: {title: 'Service Users'}, bubbles: true, composed: true}));
    this._getPermissions();
    this._getServiceUsers();
  }

  _getPermissions() {
    return this.plugin.restApi('/accounts/self/capabilities/').get('')
        .then(capabilities => {
          this._canCreate = capabilities
              && (capabilities.administrateServer
                  || capabilities['serviceuser-createServiceUser']);
        });
  }

  _getServiceUsers() {
    return this.plugin.restApi('/a/config/server/serviceuser~serviceusers/')
        .get('')
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
  }

  _computeLoadingClass(loading) {
    return loading ? 'loading' : '';
  }

  _active(item) {
    if (!item) {
      return NOT_FOUND_MESSAGE;
    }

    return item.inactive === true ? 'Inactive' : 'Active';
  }

  _getCreator(item) {
    if (!item || !item.created_by) {
      return NOT_FOUND_MESSAGE;
    }

    if (item.created_by.username != undefined) {
      return item.created_by.username;
    }

    if (item.created_by._account_id != -1) {
      return item.created_by._account_id;
    }

    return NOT_FOUND_MESSAGE;
  }

  _getOwnerGroup(item) {
    return item && item.owner ? item.owner.name : NOT_FOUND_MESSAGE;
  }

  _computeServiceUserUrl(id) {
    return `${this.plugin.screenUrl()}/user/${id}`;
  }

  _createNewServiceUser() {
    page.show(this.plugin.screenUrl() + '/create');
  }
}

customElements.define(GrServiceUserList.is, GrServiceUserList);
