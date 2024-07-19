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
import {unsafeHTML} from 'lit/directives/unsafe-html';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';
import {
  AccountId,
  AccountInfo,
  GroupInfo,
} from '@gerritcodereview/typescript-api/rest-api';
import {PluginApi} from '@gerritcodereview/typescript-api/plugin';

export interface ConfigInfo {
  info: string;
  on_success: string;
  allow_email: boolean;
  allow_owner: boolean;
  allow_http_password: boolean;
}

export interface ServiceUserInfo extends AccountInfo {
  created_by?: AccountInfo;
  created_at?: string;
  owner?: GroupInfo;
}

declare interface ServiceUserInput {
  username?: string;
  name?: string;
  ssh_key?: string;
  email?: string;
}

@customElement('gr-serviceuser-create')
export class GrServiceUserCreate extends LitElement {
  @query('#successDialogModal')
  successDialogModal!: HTMLDialogElement;

  @query('#serviceUserNameInput')
  serviceUserNameInput!: HTMLInputElement;

  @query('#serviceUserEmailInput')
  serviceUserEmailInput!: HTMLInputElement;

  @query('#serviceUserKeyInput')
  serviceUserKeyInput!: HTMLInputElement;

  @property()
  plugin!: PluginApi;

  @property()
  pluginRestApi!: RestPluginApi;

  @state()
  infoMessageEnabled = false;

  @state()
  successMessageEnabled = false;

  @state()
  emailAllowed = false;

  @state()
  dataValid = false;

  @state()
  isAdding = false;

  @property({type: String})
  infoMessage = '';

  @property({type: String})
  successMessage = '';

  @property({type: String})
  username?: String;

  @property({type: String})
  email?: String;

  @property({type: String})
  key?: String;

  @property({type: Object})
  accountId?: AccountId;

  static override get styles() {
    return [
      window.Gerrit.styles.font as CSSResult,
      window.Gerrit.styles.form as CSSResult,
      window.Gerrit.styles.modal as CSSResult,
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
      `,
    ];
  }

  override render() {
    return html`
      <main class="gr-form-styles read-only">
        <div class="topHeader">
          <h1 class="heading">Create Service User</h1>
        </div>
        ${this.renderInfoMessage()}
        <fieldset>
          <section>
            <span class="title">Username</span>
            <span class="value">
              <input
                id="serviceUserNameInput"
                value="${this.username}"
                type="text"
                @input="${this.validateData}"
              />
            </span>
          </section>
          ${this.renderEmailInputSection()}
        </fieldset>
        <fieldset>
          <section>
            <span class="title">Public SSH key</span>
            <span class="value">
              <iron-autogrow-textarea
                id="serviceUserKeyInput"
                .bind-value="${this.key}"
                placeholder="New SSH Key"
                @bind-value-changed=${this.validateData}
              >
              </iron-autogrow-textarea>
            </span>
          </section>
        </fieldset>
        <gr-button
          id="createButton"
          @click=${this.handleCreateServiceUser}
          ?disabled="${!this.dataValid || this.isAdding}"
        >
          Create
        </gr-button>
        <dialog id="successDialogModal">
          <gr-dialog
            id="successDialog"
            confirm-label="OK"
            cancel-label=""
            @confirm="${this.forwardToDetails}"
            confirm-on-enter
          >
            <div slot="header">Success</div>
            <div id="successMessage" slot="main">
              ${this.renderSuccessMessage()}
            </div>
          </gr-dialog>
        </dialog>
      </main>
    `;
  }

  private renderSuccessMessage() {
    return html`${unsafeHTML(this.successMessage)}`;
  }

  private renderInfoMessage() {
    if (this.infoMessageEnabled) {
      return html`
        <fieldset id="infoMessage">${unsafeHTML(this.infoMessage)}</fieldset>
      `;
    }

    return html``;
  }

  private renderEmailInputSection() {
    if (this.emailAllowed) {
      return html`
        <section>
          <span class="title">Email</span>
          <span class="value">
            <input
              id="serviceUserEmailInput"
              value="${this.email}"
              type="text"
              @input="${this.validateData}"
            />
          </span>
        </section>
      `;
    }

    return html``;
  }

  override connectedCallback() {
    super.connectedCallback();
    this.pluginRestApi = this.plugin.restApi();
    this.getConfig();
  }

  private forwardToDetails() {
    window.location.href = `${this.getPluginBaseURL()}/user/${this.accountId}`;
  }

  private getPluginBaseURL() {
    return `${window.location.origin}${window.CANONICAL_PATH || ''}/x/${this.plugin.getPluginName()}`;
  }

  private getConfig() {
    return this.pluginRestApi
      .get<ConfigInfo>('/config/server/serviceuser~config/')
      .then(config => {
        if (!config) {
          return;
        }

        if (config.info && config.info !== '') {
          this.infoMessageEnabled = true;
          this.infoMessage = config.info;
        }

        if (config.on_success && config.on_success !== '') {
          this.successMessageEnabled = true;
          this.successMessage = config.on_success;
        }

        this.emailAllowed = config.allow_email;
      });
  }

  private validateData() {
    this.dataValid =
      this.validateName(this.serviceUserNameInput.value) &&
      this.validateEmail(this.serviceUserEmailInput?.value) &&
      this.validateKey(this.serviceUserKeyInput.value);
  }

  private validateName(username: String | undefined) {
    if (username && username.trim().length > 0) {
      this.username = username;
      return true;
    }

    return false;
  }

  private validateEmail(email: String | undefined) {
    if (!email || email.trim().length === 0 || email.includes('@')) {
      this.email = email;
      return true;
    }

    return false;
  }

  private validateKey(key: String | undefined) {
    if (!key?.trim()) {
      return false;
    }

    this.key = key;
    return true;
  }

  private handleCreateServiceUser() {
    this.isAdding = true;
    const body: ServiceUserInput = {
      ssh_key: this.key ? this.key.trim() : '',
      email: this.email ? this.email.trim() : '',
    };
    return this.plugin
      .restApi()
      .post<ServiceUserInfo>(
        `/a/config/server/serviceuser~serviceusers/${this.username}`,
        body
      )
      .then(response => {
        this.accountId = response._account_id;
        if (this.successMessage) {
          this.successDialogModal?.showModal();
        } else {
          this.forwardToDetails();
        }
      })
      .catch(response => {
        this.dispatchEvent(
          new CustomEvent('show-error', {
            detail: {message: response},
            bubbles: true,
            composed: true,
          })
        );
        this.isAdding = false;
      });
  }
}
