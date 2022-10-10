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
    <style include="gr-subpage-styles"></style>
    <style include="gr-font-styles"></style>
    <style include="gr-form-styles"></style>
    <style>
      main {
        margin: 2em auto;
        max-width: 50em;
      }

      .heading {
        font-size: x-large;
        font-weight: 500;
      }
    </style>
    <main class="gr-form-styles read-only">
      <div class="topHeader">
        <h1 class="heading">Create Service User</h1>
      </div>
      <fieldset id="infoMessage"
           hidden$="[[!_infoMessageEnabled]]">
      </fieldset>
      <fieldset>
        <section>
          <span class="title">Username</span>
          <span class="value">
            <iron-input bind-value="{{_newUsername}}">
              <input id="serviceUserNameInput"
                     value="{{_newUsername::input}}"
                     type="text"
                     on-keyup="_validateData">
            </iron-input>
          </span>
        </section>
        <section hidden$="[[!_emailEnabled]]">
          <span class="title">Email</span>
          <span class="value">
            <iron-input bind-value="{{_newEmail}}">
              <input id="serviceUserEmailInput"
                     value="{{_newEmail::input}}"
                     type="text"
                     on-keyup="_validateData">
            </iron-input>
          </span>
        </section>
      </fieldset>
      <fieldset>
        <section>
          <span class="title">Public SSH key</span>
          <span class="value">
            <iron-autogrow-textarea id="newKey"
                                    bind-value="{{_newKey}}"
                                    placeholder="New SSH Key"
                                    on-keyup="_validateData">
            </iron-autogrow-textarea>
          </span>
        </section>
      </fieldset>
      <gr-button id="createButton"
                 on-click="_handleCreateServiceUser"
                 disabled="[[!_enableButton]]">
        Create
      </gr-button>
      <gr-overlay id="successDialogOverlay" with-backdrop>
        <gr-dialog id="successDialog"
                   confirm-label="OK"
                   cancel-label=""
                   on-confirm="_forwardToDetails"
                   confirm-on-enter>
          <div slot="header">
            Success
          </div>
          <div id="successMessage" slot="main">
          </div>
        </gr-dialog>
      </gr-overlay>
    </main>
`;
