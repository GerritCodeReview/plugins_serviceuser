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

import {customElement, property, query} from 'lit/decorators';
import {css, CSSResult, html, LitElement} from 'lit';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';

@customElement('gr-serviceuser-http-password')
export class GrServiceUserHttpPassword extends LitElement {
  @query('#generatedPasswordModal')
  generatedPasswordModal?: HTMLDialogElement;

  @property()
  pluginRestApi!: RestPluginApi;

  @property({type: String})
  serviceUserId?: String;

  @property({type: String})
  generatedPassword?: String;

  loadData(pluginRestApi: RestPluginApi) {
    this.pluginRestApi = pluginRestApi;
    this.serviceUserId = this.baseURI.split('/').pop();
  }

  static override get styles() {
    return [
      window.Gerrit.styles.font as CSSResult,
      window.Gerrit.styles.form as CSSResult,
      window.Gerrit.styles.modal as CSSResult,
      css`
        .password {
          font-family: var(--monospace-font-family);
          font-size: var(--font-size-mono);
          line-height: var(--line-height-mono);
        }
        #generatedPasswordModal {
          padding: var(--spacing-xxl);
          width: 50em;
        }
        #generatedPasswordDisplay {
          margin: var(--spacing-l) 0;
        }
        #generatedPasswordDisplay .title {
          width: unset;
        }
        #generatedPasswordDisplay .value {
          font-family: var(--monospace-font-family);
          font-size: var(--font-size-mono);
          line-height: var(--line-height-mono);
        }
        #passwordWarning {
          font-style: italic;
          text-align: center;
        }
        .closeButton {
          bottom: 2em;
          position: absolute;
          right: 2em;
        }
      `,
    ];
  }

  override render() {
    return html` <div class="gr-form-styles">
        <div>
          <gr-button id="generateButton" @click=${this.handleGenerateTap}
            >Generate new password</gr-button
          >
          <gr-button id="deleteButton" @click="${this.handleDelete}"
            >Delete password</gr-button
          >
        </div>
      </div>
      <dialog
        tabindex="-1"
        id="generatedPasswordModal"
        @closed=${this.generatedPasswordModalClosed}
      >
        <div class="gr-form-styles">
          <section id="generatedPasswordDisplay">
            <span class="title">New Password:</span>
            <span class="value">${this.generatedPassword}</span>
            <gr-copy-clipboard
              hasTooltip=""
              buttonTitle="Copy password to clipboard"
              hideInput=""
              .text=${this.generatedPassword}
            >
            </gr-copy-clipboard>
          </section>
          <section id="passwordWarning">
            This password will not be displayed again.<br />
            If you lose it, you will need to generate a new one.
          </section>
          <gr-button link="" class="closeButton" @click=${this.closeModal}
            >Close</gr-button
          >
        </div>
      </dialog>`;
  }

  private handleGenerateTap() {
    this.generatedPassword = 'Generating...';
    this.generatedPasswordModal?.showModal();
    this.pluginRestApi
      .put<String>(`/a/accounts/${this.serviceUserId}/password.http`, {
        generate: true,
      })
      .then(newPassword => {
        this.generatedPassword = newPassword;
      });
  }

  private closeModal() {
    this.generatedPasswordModal?.close();
  }

  private generatedPasswordModalClosed() {
    this.generatedPassword = '';
  }

  private handleDelete() {
    this.pluginRestApi.delete(
      `/a/accounts/${this.serviceUserId}/password.http`
    );
  }
}
