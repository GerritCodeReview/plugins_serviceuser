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
import {css, CSSResult, html, LitElement, PropertyValues} from 'lit';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';

export interface BindValueChangeEventDetail {
  value: string | undefined;
}
export type BindValueChangeEvent = CustomEvent<BindValueChangeEventDetail>;

// TODO: Remove when it is released with typescript API
export interface SshKeyInfo {
  seq: number;
  ssh_public_key: string;
  encoded_key: string;
  algorithm: string;
  comment?: string;
  valid: boolean;
}

@customElement('gr-serviceuser-ssh-panel')
export class GrServiceUserSshPanel extends LitElement {
  @query('#addButton') addButton!: HTMLButtonElement;

  @query('#newKey') newKeyEditor!: HTMLTextAreaElement;

  @query('#viewKeyModal') viewKeyModal!: HTMLDialogElement;

  @property({type: Boolean})
  hasUnsavedChanges = false;

  @property()
  pluginRestApi!: RestPluginApi;

  @property({type: String})
  serviceUserId?: String;

  @property({type: Array})
  keys: SshKeyInfo[] = [];

  @property({type: Object})
  keyToView?: SshKeyInfo;

  @property({type: String})
  newKey = '';

  @property({type: Array})
  keysToRemove: SshKeyInfo[] = [];

  @state() prevHasUnsavedChanges = false;

  static override get styles() {
    return [
      window.Gerrit.styles.form as CSSResult,
      window.Gerrit.styles.modal as CSSResult,
      css`
        .statusHeader {
          width: 4em;
        }
        .keyHeader {
          width: 7.5em;
        }
        #viewKeyModal {
          padding: var(--spacing-xxl);
          width: 50em;
        }
        .publicKey {
          font-family: var(--monospace-font-family);
          font-size: var(--font-size-mono);
          line-height: var(--line-height-mono);
          overflow-x: scroll;
          overflow-wrap: break-word;
          width: 30em;
        }
        .closeButton {
          bottom: 2em;
          position: absolute;
          right: 2em;
        }
        #existing {
          margin-bottom: var(--spacing-l);
        }
        #existing .commentColumn {
          min-width: 27em;
          width: auto;
        }
        iron-autogrow-textarea {
          background-color: var(--view-background-color);
        }
      `,
    ];
  }

  override updated(changedProperties: PropertyValues) {
    if (changedProperties.has('hasUnsavedChanges')) {
      if (this.prevHasUnsavedChanges === this.hasUnsavedChanges) return;
      this.prevHasUnsavedChanges = this.hasUnsavedChanges;
      this.dispatchEvent(
        new CustomEvent('has-unsaved-changes-changed', {
          detail: {value: this.hasUnsavedChanges},
          composed: true,
          bubbles: true,
        })
      );
    }
  }

  override render() {
    return html`
      <div class="gr-form-styles">
        <fieldset id="existing">
          <table>
            <thead>
              <tr>
                <th class="commentColumn">Comment</th>
                <th class="statusHeader">Status</th>
                <th class="keyHeader">Public key</th>
                <th></th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${this.keys.map((key, index) => this.renderKey(key, index))}
            </tbody>
          </table>
          <dialog id="viewKeyModal" tabindex="-1">
            <fieldset>
              <section>
                <span class="title">Algorithm</span>
                <span class="value">${this.keyToView?.algorithm}</span>
              </section>
              <section>
                <span class="title">Public key</span>
                <span class="value publicKey"
                  >${this.keyToView?.encoded_key}</span
                >
              </section>
              <section>
                <span class="title">Comment</span>
                <span class="value">${this.keyToView?.comment}</span>
              </section>
            </fieldset>
            <gr-button
              class="closeButton"
              @click=${() => this.viewKeyModal.close()}
              >Close</gr-button
            >
          </dialog>
          <gr-button
            @click=${() => this.save()}
            ?disabled=${!this.hasUnsavedChanges}
            >Save changes</gr-button
          >
        </fieldset>
        <fieldset>
          <section>
            <span class="title">New SSH key</span>
            <span class="value">
              <iron-autogrow-textarea
                id="newKey"
                autocomplete="on"
                placeholder="New SSH Key"
                .bindValue=${this.newKey}
                @bind-value-changed=${(e: BindValueChangeEvent) => {
                  this.newKey = e.detail.value ?? '';
                }}
              ></iron-autogrow-textarea>
            </span>
          </section>
          <gr-button
            id="addButton"
            link=""
            ?disabled=${!this.newKey.length}
            @click=${() => this.handleAddKey()}
            >Add new SSH key</gr-button
          >
        </fieldset>
      </div>
    `;
  }

  private renderKey(key: SshKeyInfo, index: number) {
    return html` <tr>
      <td class="commentColumn">${key.comment}</td>
      <td>${key.valid ? 'Valid' : 'Invalid'}</td>
      <td>
        <gr-button
          link=""
          @click=${(e: Event) => this.showKey(e)}
          data-index=${index}
          >Click to View</gr-button
        >
      </td>
      <td>
        <gr-copy-clipboard
          hasTooltip=""
          .buttonTitle=${'Copy SSH public key to clipboard'}
          hideInput=""
          .text=${key.ssh_public_key}
        >
        </gr-copy-clipboard>
      </td>
      <td>
        <gr-button
          link=""
          data-index=${index}
          @click=${(e: Event) => this.handleDeleteKey(e)}
          >Delete</gr-button
        >
      </td>
    </tr>`;
  }

  loadData(pluginRestApi: RestPluginApi) {
    this.pluginRestApi = pluginRestApi;
    this.serviceUserId = this.baseURI.split('/').pop();
    return this.pluginRestApi
      .get<Array<SshKeyInfo>>(
        `/config/server/serviceuser~serviceusers/${this.serviceUserId}/sshkeys`
      )
      .then(keys => {
        if (!keys) {
          this.keys = [];
          return;
        }
        this.keys = keys;
      });
  }

  private save() {
    const promises = this.keysToRemove.map(key =>
      this.pluginRestApi.delete(
        `/config/server/serviceuser~serviceusers/${this.serviceUserId}/sshkeys/${key.seq}`
      )
    );
    return Promise.all(promises).then(() => {
      this.keysToRemove = [];
      this.hasUnsavedChanges = false;
    });
  }

  private showKey(e: Event) {
    const el = e.target as HTMLBaseElement;
    const index = Number(el.getAttribute('data-index'));
    this.keyToView = this.keys[index];
    this.viewKeyModal.showModal();
  }

  private handleDeleteKey(e: Event) {
    const el = e.target as HTMLBaseElement;
    const index = Number(el.getAttribute('data-index')!);
    this.keysToRemove.push(this.keys[index]);
    this.keys.splice(index, 1);
    this.requestUpdate();
    this.hasUnsavedChanges = true;
  }

  private handleAddKey() {
    this.addButton.disabled = true;
    this.newKeyEditor.disabled = true;
    return this.pluginRestApi
      .post<SshKeyInfo>(
        `/config/server/serviceuser~serviceusers/${this.serviceUserId}/sshkeys`,
        this.newKey.trim(),
        undefined,
        'plain/text'
      )
      .then(key => {
        this.newKeyEditor.disabled = false;
        this.newKey = '';
        this.keys.push(key);
        this.requestUpdate();
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
        this.addButton.disabled = false;
        this.newKeyEditor.disabled = false;
      });
  }
}
