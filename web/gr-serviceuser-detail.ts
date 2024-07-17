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

import {customElement, property, query, state} from 'lit/decorators';
import {css, CSSResult, html, LitElement} from 'lit';
import {PluginApi} from '@gerritcodereview/typescript-api/plugin';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';
import {GroupInfo} from '@gerritcodereview/typescript-api/rest-api';

import {AccountCapabilityInfo} from './plugin';
import {ConfigInfo, ServiceUserInfo} from './gr-serviceuser-create';
import {GrServiceUserSshPanel} from './gr-serviceuser-ssh-panel';
import {GrServiceUserHttpPassword} from './gr-serviceuser-http-password';

import './gr-serviceuser-ssh-panel';
import './gr-serviceuser-http-password';

const NOT_FOUND_MESSAGE = 'Not Found';

@customElement('gr-serviceuser-detail')
export class GrServiceUserDetail extends LitElement {
  @query('#sshEditor')
  sshEditor!: GrServiceUserSshPanel;

  @query('#httpPass')
  httpPass!: GrServiceUserHttpPassword;

  @query('#serviceUserFullNameInput')
  serviceUserFullNameInput!: HTMLInputElement;

  @query('#serviceUserEmailInput')
  serviceUserEmailInput!: HTMLInputElement;

  @query('#serviceUserOwnerInput')
  serviceUserOwnerInput!: HTMLInputElement;

  @property({type: Object})
  plugin!: PluginApi;

  @property()
  pluginRestApi!: RestPluginApi;

  @property({type: String})
  serviceUserId?: String;

  @property({type: Object})
  serviceUser!: ServiceUserInfo;

  @state()
  loading = true;

  @state()
  statusButtonText = 'Activate';

  @state()
  prefsChanged = false;

  @state()
  changingPrefs = false;

  @state()
  isAdmin = false;

  @state()
  emailAllowed = false;

  @state()
  ownerAllowed = false;

  @state()
  httpPasswordAllowed = false;

  @property({type: String})
  fullName?: String;

  @property({type: String})
  email?: String;

  @property({type: Array})
  availableOwners?: Array<GroupInfo>;

  @property({type: String})
  owner = NOT_FOUND_MESSAGE;

  @property({type: String})
  ownerChangeWarning?: String;

  override connectedCallback() {
    super.connectedCallback();
    this.pluginRestApi = this.plugin.restApi();
  }

  override firstUpdated() {
    this.extractUserId();
    this.loadServiceUser();
  }

  static override get styles() {
    return [
      window.Gerrit.styles.font as CSSResult,
      window.Gerrit.styles.form as CSSResult,
      window.Gerrit.styles.subPage as CSSResult,
      css`
        main {
          margin: 2em auto;
          max-width: 50em;
        }

        .heading {
          font-size: x-large;
          font-weight: 500;
        }

        h1#Title {
          margin-bottom: 1em;
        }

        p#ownerChangeWarning {
          margin-top: 1em;
          margin-bottom: 1em;
        }

        span#gr_serviceuser_activity {
          border-radius: 1em;
          width: 10em;
          padding: 0.3em;
          font-weight: bold;
          text-align: center;
        }

        span.value {
          width: 50%;
        }

        input.wide {
          width: var(--paper-input-container-shared-input-style_-_width);
        }

        span.Active {
          background-color: #9fcc6b;
        }

        span.Inactive {
          background-color: #f7a1ad;
        }
      `,
    ];
  }

  override render() {
    return html`
      <main class="gr-form-styles read-only">
        <div id="loading" class="${this.computeLoadingClass()}">Loading...</div>
        <div id="loadedContent" class="${this.computeLoadingClass()}">
          <h1 id="Title" class="heading">
            Service User "${this.serviceUser?.name}"
          </h1>
          <div id="form">
            <fieldset>
              <fieldset>
                <h2 id="accountState" class="heading-2">Account State</h2>
                <section>
                  <span class="title">Current State</span>
                  <span
                    id="gr_serviceuser_activity"
                    class="value ${this.active()}"
                  >
                    ${this.active()}
                  </span>
                </section>
                <gr-button
                  id="statusToggleButton"
                  @click="${this.toggleStatus}"
                  ?disabled="${this.loading}"
                >
                  ${this.statusButtonText}
                </gr-button>
              </fieldset>
              <fieldset>
                <h2 id="userDataHeader" class="heading-2">User Data</h2>
                <section>
                  <span class="title">Username</span>
                  <span class="value">${this.serviceUser?.username}</span>
                </section>
                ${this.renderFullNameFormSection()}
                <section>
                  <span class="title">Email Address</span>
                  <span class="value"> ${this.renderEmailFormContent()} </span>
                </section>
                <section>
                  <span class="title">Owner Group</span>
                  <span class="value">
                    ${this.renderOwnerGroupFormContent()}
                  </span>
                </section>
                <p id="ownerChangeWarning" class="style-scope gr-settings-view">
                  ${this.ownerChangeWarning}
                </p>
                <gr-button
                  id="savePrefs"
                  @click="${this.handleSavePreferences}"
                  ?disabled="${!this.prefsChanged}"
                >
                  Save changes
                </gr-button>
              </fieldset>
              <fieldset>
                <h2 id="creationHeader" class="heading-2">Creation</h2>
                <section>
                  <span class="title">Created By</span>
                  <span class="value">${this.getCreator()}</span>
                </section>
                <section>
                  <span class="title">Created At</span>
                  <span class="value">${this.serviceUser?.created_at}</span>
                </section>
              </fieldset>
              <fieldset>
                <h2 id="credentialsHeader" class="heading-2">Credentials</h2>
                ${this.renderHttpCredentialsForm()}
                <fieldset>
                  <h3 id="SSHKeys">SSH keys</h3>
                  <gr-serviceuser-ssh-panel
                    id="sshEditor"
                  ></gr-serviceuser-ssh-panel>
                </fieldset>
              </fieldset>
            </fieldset>
          </div>
        </div>
      </main>
    `;
  }

  private renderFullNameFormSection() {
    return html`
      <section>
        <span class="title">Full Name</span>
        <span class="value">
          <input
            id="serviceUserFullNameInput"
            type="text"
            class="wide"
            .value="${this.fullName}"
            .placeholder="${this.serviceUser?.name}"
            ?disabled="${this.changingPrefs}"
            @input="${this.fullNameChanged}"
          />
        </span>
      </section>
    `;
  }

  private renderEmailFormContent() {
    if (this.emailAllowed) {
      return html`
        <input
          id="serviceUserEmailInput"
          type="text"
          class="wide"
          .value="${this.email}"
          .placeholder="${this.serviceUser.email ?? ''}"
          ?disabled="${this.changingPrefs}"
          @input="${this.emailChanged}"
        />
      `;
    }

    return html`${this.serviceUser?.email}`;
  }

  private renderOwnerGroupFormContent() {
    if (this.ownerAllowed) {
      return html`
        <gr-autocomplete
          id="serviceUserOwnerInput"
          .text="${this.owner}"
          .value="${this.owner}"
          .query="${(input: string) => this.getGroupSuggestions(input)}"
          ?disabled="${this.changingPrefs}"
          @value-changed="${this.ownerChanged}"
          @text-changed="${this.ownerChanged}"
        >
          ${this.getCurrentOwnerGroup()}
        </gr-autocomplete>
      `;
    }

    return html`${this.getCurrentOwnerGroup()}`;
  }

  private renderHttpCredentialsForm() {
    if (this.httpPasswordAllowed) {
      return html`
        <fieldset>
          <h3 id="HTTPCredentials">HTTP Credentials</h3>
          <fieldset>
            <gr-serviceuser-http-password id="httpPass">
              </gr-http-password>
          </fieldset>
        </fieldset>
      `;
    }

    return html``;
  }

  private loadServiceUser() {
    if (!this.serviceUserId) {
      return;
    }

    const promises = [];

    promises.push(this.getPluginConfig());
    promises.push(this.getServiceUser());

    Promise.all(promises).then(() => {
      this.sshEditor.loadData(this.pluginRestApi);
      if (this.httpPasswordAllowed) {
        this.httpPass.loadData(this.pluginRestApi);
      }

      this.dispatchEvent(
        new CustomEvent('title-change', {
          detail: {title: this.serviceUser?.name},
          bubbles: true,
          composed: true,
        })
      );
      this.computeStatusButtonText();
      this.loading = false;
      this.fullName = this.serviceUser?.name;
      this.email = this.serviceUser.email ?? '';
      this.owner = this.getCurrentOwnerGroup() ?? NOT_FOUND_MESSAGE;
    });
  }

  private computeLoadingClass() {
    return this.loading ? 'loading' : '';
  }

  private extractUserId() {
    this.serviceUserId = this.baseURI.split('/').pop();
  }

  private getPermissions() {
    return this.pluginRestApi
      .get<AccountCapabilityInfo>('/accounts/self/capabilities/')
      .then(capabilities => {
        if (!capabilities) {
          this.isAdmin = false;
        } else {
          this.isAdmin =
            capabilities.administrateServer === undefined ? false : true;
        }
      });
  }

  private getPluginConfig() {
    return this.getPermissions()
      .then(() =>
        this.pluginRestApi.get<ConfigInfo>('/config/server/serviceuser~config/')
      )
      .then(config => {
        if (!config) {
          return;
        }
        this.emailAllowed = config.allow_email || this.isAdmin;
        this.ownerAllowed = config.allow_owner || this.isAdmin;
        this.httpPasswordAllowed = config.allow_http_password || this.isAdmin;
      });
  }

  private getServiceUser() {
    return this.pluginRestApi
      .get(`/a/config/server/serviceuser~serviceusers/${this.serviceUserId}`)
      .then(serviceUser => {
        if (!serviceUser) {
          this.serviceUser = {};
          return;
        }
        this.serviceUser = serviceUser;
      });
  }

  private active() {
    if (!this.serviceUser) {
      return NOT_FOUND_MESSAGE;
    }

    return this.serviceUser?.inactive === true ? 'Inactive' : 'Active';
  }

  private computeStatusButtonText() {
    if (!this.serviceUser) {
      return;
    }

    this.statusButtonText =
      this.serviceUser?.inactive === true ? 'Activate' : 'Deactivate';
  }

  private toggleStatus() {
    if (this.serviceUser?.inactive === true) {
      this.pluginRestApi
        .put(
          `/config/server/serviceuser~serviceusers/${this.serviceUser?._account_id}/active`
        )
        .then(() => {
          this.loadServiceUser();
        });
    } else {
      this.pluginRestApi
        .delete(
          `/config/server/serviceuser~serviceusers/${this.serviceUser?._account_id}/active`
        )
        .then(() => {
          this.loadServiceUser();
        });
    }
  }

  private getCreator() {
    if (!this.serviceUser || !this.serviceUser?.created_by) {
      return NOT_FOUND_MESSAGE;
    }

    if (this.serviceUser?.created_by.username !== undefined) {
      return this.serviceUser?.created_by.username;
    }

    if (this.serviceUser?.created_by._account_id !== -1) {
      return this.serviceUser?.created_by._account_id;
    }

    return NOT_FOUND_MESSAGE;
  }

  private getCurrentOwnerGroup() {
    return this.serviceUser && this.serviceUser?.owner
      ? this.serviceUser?.owner.name
      : NOT_FOUND_MESSAGE;
  }

  // private isEmailValid(email: String) {
  //   return email.includes('@') || email.length == 0;
  // }

  private getGroupSuggestions(input: String) {
    return this.pluginRestApi
      .get<Object>(`/a/groups/?n=10&suggest=${input}`)
      .then(response => {
        this.availableOwners = Object.values(response);
        return Object.keys(response).map(name => {
          return {name, value: name};
        });
      });
  }

  private isNewOwner() {
    if (this.owner === NOT_FOUND_MESSAGE) {
      return false;
    }
    return this.owner !== this.getCurrentOwnerGroup();
  }

  private computeOwnerWarning() {
    let message = 'If ';
    message +=
      this.getCurrentOwnerGroup() !== NOT_FOUND_MESSAGE
        ? 'the owner group is changed'
        : 'an owner group is set';
    message += ' only members of the ';
    message += this.getCurrentOwnerGroup() !== NOT_FOUND_MESSAGE ? 'new ' : '';
    message += 'owner group can see and administrate the service user.';
    message +=
      this.getCurrentOwnerGroup() !== NOT_FOUND_MESSAGE
        ? ''
        : ' The creator of the service user can no' +
          ' longer see and administrate the service user if she/he' +
          ' is not member of the owner group.';
    this.ownerChangeWarning = message;
  }

  private fullNameChanged() {
    this.fullName = this.serviceUserFullNameInput.value;
    this.computePrefsChanged();
  }

  private emailChanged() {
    this.email = this.serviceUserEmailInput.value;
    this.computePrefsChanged();
  }

  private ownerChanged() {
    this.owner = this.serviceUserOwnerInput.value;

    if (this.isNewOwner()) {
      this.computeOwnerWarning();
    }

    this.computePrefsChanged();
  }

  private computePrefsChanged() {
    if (this.loading || this.changingPrefs) {
      return;
    }

    if (
      this.owner === this.getCurrentOwnerGroup() &&
      !this.isNewValidEmail(this.email!) &&
      this.fullName === this.serviceUser.name
    ) {
      this.prefsChanged = false;
      return;
    }

    this.prefsChanged = true;
  }

  private isNewValidEmail(email: String) {
    if (!this.serviceUser.email) {
      return email.includes('@');
    }
    return email !== this.serviceUser.email && (email.includes('@') || email.length == 0);
  }

  private applyNewFullName() {
    return this.pluginRestApi.put(
      `/config/server/serviceuser~serviceusers/${this.serviceUser?._account_id}/name`,
      {name: this.fullName}
    );
  }

  private applyNewEmail() {
    if (!this.isNewValidEmail(this.email!)) {
      return;
    }
    return this.pluginRestApi.put(
      `/config/server/serviceuser~serviceusers/${this.serviceUser?._account_id}/email`,
      {email: this.email}
    );
  }

  private applyNewOwner() {
    if (!this.isNewOwner()) {
      return;
    }
    if (this.owner === '') {
      return this.pluginRestApi.delete(
        `/config/server/serviceuser~serviceusers/${this.serviceUser?._account_id}/owner`
      );
    }
    return this.pluginRestApi.put(
      `/config/server/serviceuser~serviceusers/${this.serviceUser?._account_id}/owner`,
      {group: this.owner}
    );
  }

  private handleSavePreferences() {
    const promises = [];
    this.changingPrefs = true;

    if (this.fullName !== this.serviceUser.name) {
      promises.push(this.applyNewFullName());
    }

    if (this.email !== this.serviceUser.email) {
      promises.push(this.applyNewEmail());
    }

    if (this.owner !== this.serviceUser.owner?.name) {
      promises.push(this.applyNewOwner());
    }

    Promise.all(promises)
      .then(() => {
        this.prefsChanged = false;
        this.ownerChangeWarning = '';
        this.loadServiceUser();
      })
      .catch(error => {
        this.dispatchEvent(
          new CustomEvent('show-error', {
            detail: {message: error},
            composed: true,
            bubbles: true,
          })
        );
      })
      .finally(() => {
        this.changingPrefs = false;
      });
  }
}
