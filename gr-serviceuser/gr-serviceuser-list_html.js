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

export const htmlTemplate = Polymer.html`
    <style include="shared-styles"></style>
    <style include="gr-font-styles"></style>
    <style include="gr-table-styles"></style>
    <style>
      .topHeader {
        padding: 8px;
      }

      #topContainer {
        align-items: center;
        display: flex;
        height: 3rem;
        justify-content: space-between;
        margin: 0 1em;
      }
    </style>
    <div class="topHeader">
      <h1 class="heading-1">Service Users</h1>
    </div>
    <div id="topContainer">
      <div></div>
      <div id="createNewContainer"
           class$="[[_computeCreateClass(createNew)]]"
           hidden$="[[!_canCreate]]">
        <gr-button primary
                   link
                   id="createNew"
                   on-click="_createNewServiceUser">
          Create New
        </gr-button>
      </div>
    </div>
    <table id="list"
           class="genericList">
      <tr class="headerRow">
        <th class="name topHeader">Username</th>
        <th class="fullName topHeader">Full Name</th>
        <th class="email topHeader">Email</th>
        <th class="owner topHeader">Owner</th>
        <th class="createdBy topHeader">Created By</th>
        <th class="createdAt topHeader">Created At</th>
        <th class="accountState topHeader">Account State</th>
      </tr>
      <tr id="loading"
          class$="loadingMsg [[_computeLoadingClass(_loading)]]">
        <td>Loading...</td>
      </tr>
      <tbody class$="[[_computeLoadingClass(_loading)]]">
        <template is="dom-repeat"
                  items="[[_serviceUsers]]">
          <tr class="table">
            <td class="name">
              <a href$="[[_computeServiceUserUrl(item._account_id)]]">[[item.username]]</a>
            </td>
            <td class="fullName">[[item.name]]</td>
            <td class="email">[[item.email]]</td>
            <td class="owner">[[_getOwnerGroup(item)]]</td>
            <td class="createdBy">[[_getCreator(item)]]</td>
            <td class="createdAt">[[item.created_at]]</td>
            <td class="accountState">[[_active(item)]]</td>
          </tr>
        </template>
      </tbody>
    </table>
`;
