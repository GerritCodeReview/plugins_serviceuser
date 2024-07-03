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

import {customElement, property, state} from 'lit/decorators';
import {css, CSSResult, html, LitElement} from 'lit';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';
import {PluginApi} from '@gerritcodereview/typescript-api/plugin';
import {AccountId} from '@gerritcodereview/typescript-api/rest-api';

import {AccountCapabilityInfo} from './plugin';
import {ServiceUserInfo} from './gr-serviceuser-create';

const NOT_FOUND_MESSAGE = 'Not Found';

@customElement('gr-serviceuser-list')
export class GrServiceUserList extends LitElement {
  @property()
  plugin!: PluginApi;

  @property()
  pluginRestApi!: RestPluginApi;

  @state()
  loading = true;

  @state()
  canCreate = false;

  @property({type: Array})
  serviceUsers = new Array<ServiceUserInfo>();

  static override get styles() {
    return [
      window.Gerrit.styles.font as CSSResult,
      window.Gerrit.styles.table as CSSResult,
      css`
        .topHeader {
          padding: 8px;
        }

        .heading {
          font-size: x-large;
          font-weight: 500;
        }

        #topContainer {
          align-items: center;
          display: flex;
          height: 3rem;
          justify-content: space-between;
          margin: 0 1em;
        }

        #createNewContainer {
          display: block;
        }
      `,
    ];
  }

  override render() {
    return html`
      <div id="topContainer">
        <div>
          <h1 class="heading">Service Users</h1>
        </div>
        ${this.renderCreateButton()}
      </div>
      <table id="list" class="genericList">
        <tr class="headerRow">
          <th class="name topHeader">Username</th>
          <th class="fullName topHeader">Full Name</th>
          <th class="email topHeader">Email</th>
          <th class="owner topHeader">Owner</th>
          <th class="createdBy topHeader">Created By</th>
          <th class="createdAt topHeader">Created At</th>
          <th class="accountState topHeader">Account State</th>
        </tr>
        <tr id="loading" class="loadingMsg ${this.computeLoadingClass()}">
          <td>Loading...</td>
        </tr>
        <tbody class="${this.computeLoadingClass()}">
          ${this.serviceUsers.map(serviceUser =>
            this.renderServiceUserList(serviceUser)
          )}
        </tbody>
      </table>
    `;
  }

  private renderCreateButton() {
    if (this.canCreate) {
      return html`
        <div id="createNewContainer">
          <gr-button
            primary
            link
            id="createNew"
            @click="${this.createNewServiceUser}"
          >
            Create New
          </gr-button>
        </div>
      `;
    }
    return html``;
  }

  private renderServiceUserList(serviceUser: ServiceUserInfo) {
    if (!serviceUser._account_id) {
      return;
    }
    return html`
      <tr class="table">
        <td class="name">
          <a href="${this.computeServiceUserUrl(serviceUser._account_id)}"
            >${serviceUser.name}</a
          >
        </td>
        <td class="fullName">${serviceUser.name}</td>
        <td class="email">${serviceUser.email}</td>
        <td class="owner">${this.getOwnerGroup(serviceUser)}</td>
        <td class="createdBy">${this.getCreator(serviceUser)}</td>
        <td class="createdAt">${serviceUser.created_at}</td>
        <td class="accountState">${this.active(serviceUser)}</td>
      </tr>
    `;
  }

  override connectedCallback() {
    super.connectedCallback();
    this.pluginRestApi = this.plugin.restApi();
    this.dispatchEvent(
      new CustomEvent('title-change', {
        detail: {title: 'Service Users'},
        bubbles: true,
        composed: true,
      })
    );
    Promise.all(Array.of(this.getPermissions(), this.getServiceUsers())).then(
      () => (this.loading = false)
    );
  }

  private getPermissions() {
    return this.pluginRestApi
      .get<AccountCapabilityInfo>('/accounts/self/capabilities/')
      .then(capabilities => {
        this.canCreate =
          capabilities &&
          (capabilities.administrateServer ||
            capabilities['serviceuser-createServiceUser']);
      });
  }

  private getServiceUsers() {
    return this.pluginRestApi
      .get<Object>('/a/config/server/serviceuser~serviceusers/')
      .then(serviceUsers => {
        new Map<String, ServiceUserInfo>(Object.entries(serviceUsers)).forEach(
          v => this.serviceUsers.push(v)
        );
      });
  }

  private computeLoadingClass() {
    return this.loading ? 'loading' : '';
  }

  private active(item: ServiceUserInfo) {
    if (!item) {
      return NOT_FOUND_MESSAGE;
    }

    return item.inactive === true ? 'Inactive' : 'Active';
  }

  private getCreator(item: ServiceUserInfo) {
    if (!item || !item.created_by) {
      return NOT_FOUND_MESSAGE;
    }

    if (item.created_by.username !== undefined) {
      return item.created_by.username;
    }

    if (item.created_by._account_id !== -1) {
      return item.created_by._account_id;
    }

    return NOT_FOUND_MESSAGE;
  }

  private getOwnerGroup(item: ServiceUserInfo) {
    return item && item.owner ? item.owner.name : NOT_FOUND_MESSAGE;
  }

  private computeServiceUserUrl(id: AccountId) {
    return `${this.getPluginBaseURL()}/user/${id}`;
  }

  private createNewServiceUser() {
    window.location.href = `${this.getPluginBaseURL()}/create`;
  }

  private getPluginBaseURL() {
    var href = window.location.href;
    return href.substring(0, href.lastIndexOf('/list'));
  }
}
