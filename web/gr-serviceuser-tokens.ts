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

import {customElement, property, query, state} from 'lit/decorators.js';
import {css, html, CSSResult, LitElement} from 'lit';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';
import {Timestamp} from '@gerritcodereview/typescript-api/rest-api';

export interface BindValueChangeEventDetail {
  value: string | undefined;
}
export type BindValueChangeEvent = CustomEvent<BindValueChangeEventDetail>;

export interface TokenInfo {
  id: string;
  token?: string;
  expiration?: Timestamp;
}

//TODO(Thomas): Remove after updated Typescript API was released with Gerrit 3.13
export declare interface ServerInfo {
  auth: AuthInfo;
}

export declare interface AuthInfo {
  max_token_lifetime?: string;
}

@customElement('gr-serviceuser-tokens')
export class GrServiceUserTokens extends LitElement {
  @property()
  pluginRestApi!: RestPluginApi;

  @property({type: String})
  serviceUserId?: string;  @query('#generatedAuthTokenModal')

  @query('#generatedAuthTokenModal')
  generatedAuthTokenModal?: HTMLDialogElement;

  @state()
  username?: string;

  @state()
  generatedAuthToken?: TokenInfo;

  @state()
  status?: string;

  @state()
  maxLifetime: string = 'unlimited';

  @property({type: Array})
  tokens: TokenInfo[] = [];

  @property({type: String})
  newTokenId = '';

  @property({type: String})
  newLifetime = '';

  @query('#generateButton') generateButton!: HTMLButtonElement;

  @query('#newToken') tokenInput!: HTMLInputElement;

  @query('#lifetime') tokenLifetime!: HTMLInputElement;

  async loadData(pluginRestApi: RestPluginApi) {
    this.pluginRestApi = pluginRestApi;
    this.serviceUserId = this.baseURI.split('/').pop();
    await pluginRestApi
      .get<ServerInfo>('/a/config/server/info')
      .then(config => {
        this.maxLifetime = config?.auth?.max_token_lifetime || 'unlimited';
      });
    await this.fetchTokens();
  }

  private fetchTokens() {
    this.pluginRestApi
      .get<TokenInfo[]>(`/a/config/server/serviceuser~serviceusers/${this.serviceUserId}/tokens`)
      .then(tokens => {
        this.tokens = tokens;
      });
  }

  static override get styles() {
    return [
      window.Gerrit.styles.form as CSSResult,
      window.Gerrit.styles.modal as CSSResult,
      css`
        .token {
          font-family: var(--monospace-font-family);
          font-size: var(--font-size-mono);
          line-height: var(--line-height-mono);
        }
        #generatedAuthTokenModal {
          padding: var(--spacing-xxl);
          width: 50em;
        }
        #generatedAuthTokenDisplay {
          margin: var(--spacing-l) 0;
        }
        #generatedAuthTokenDisplay .title {
          width: unset;
        }
        #generatedAuthTokenDisplay .value {
          font-family: var(--monospace-font-family);
          font-size: var(--font-size-mono);
          line-height: var(--line-height-mono);
        }
        #authTokenWarning {
          font-style: italic;
          text-align: center;
        }
        #existing {
          margin-top: var(--spacing-l);
          margin-bottom: var(--spacing-l);
        }
        #existing .idColumn {
          min-width: 15em;
          width: auto;
        }
        .closeButton {
          bottom: 2em;
          position: absolute;
          right: 2em;
        }
        .expired {
          color: var(--negative-red-text-color);
        }
        .lifeTimeInput {
          min-width: 23em;
        }

        # Remove when material styles are available for plugins
        md-outlined-text-field {
          background-color: var(--view-background-color);
          color: var(--primary-text-color);
          --md-sys-color-primary: var(--primary-text-color);
          --md-sys-color-on-surface: var(--primary-text-color);
          --md-sys-color-on-surface-variant: var(--deemphasized-text-color);
          --md-outlined-text-field-label-text-color: var(--deemphasized-text-color);
          --md-outlined-text-field-focus-label-text-color: var(
            --deemphasized-text-color
          );
          --md-outlined-text-field-hover-label-text-color: var(
            --deemphasized-text-color
          );
          border-radius: var(--border-radius);
          --md-outlined-text-field-container-shape: var(--border-radius);
          --md-outlined-text-field-focus-outline-color: var(
            --prominent-border-color,
            var(--border-color)
          );
          --md-outlined-text-field-outline-color: var(
            --prominent-border-color,
            var(--border-color)
          );
          --md-outlined-text-field-hover-outline-color: var(
            --prominent-border-color,
            var(--border-color)
          );
          --md-sys-color-outline: var(--prominent-border-color, var(--border-color));
          --md-outlined-field-top-space: var(--spacing-s);
          --md-outlined-field-bottom-space: var(--spacing-s);
          --md-outlined-text-field-outline-width: 1px;
          --md-outlined-text-field-hover-outline-width: 1px;
          --md-outlined-text-field-focus-outline-width: 0;
          --md-outlined-field-leading-space: 8px;
        }

        md-outlined-text-field.showBlueFocusBorder {
          --md-outlined-text-field-focus-outline-width: 2px;
          --md-outlined-text-field-focus-outline-color: var(
            --input-focus-border-color
          );
        }
      `,
    ];
  }

  override render() {
    return html`
      <div class="gr-form-styles">
        <fieldset id="existing">
          <table>
            <thead>
              <tr>
                <th class="idColumn">ID</th>
                <th class="expirationColumn">Expiration Date</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${this.tokens.map(tokenInfo => this.renderToken(tokenInfo))}
            </tbody>
            <tfoot>
              ${this.renderFooterRow()}
            </tfoot>
          </table>
        </fieldset>
      </div>
      <dialog
        tabindex="-1"
        id="generatedAuthTokenModal"
        @closed=${this.generatedAuthTokenModalClosed}
      >
        <div class="gr-form-styles">
          <section id="generatedAuthTokenDisplay">
            <span class="title">New Token:</span>
            <span class="value"
              >${this.status || this.generatedAuthToken?.token}</span
            >
            <gr-copy-clipboard
              hasTooltip=""
              buttonTitle="Copy token to clipboard"
              hideInput=""
              .text=${this.status ? '' : this.generatedAuthToken?.token}
            >
            </gr-copy-clipboard>
          </section>
          <section
            id="authTokenWarning"
            ?hidden=${!this.generatedAuthToken?.expiration}
          >
            This token will be valid until &nbsp;
            <gr-date-formatter
              showDateAndTime
              withTooltip
              .dateStr=${this.generatedAuthToken?.expiration}
            ></gr-date-formatter>
            .
          </section>
          <section id="authTokenWarning">
            This token will not be displayed again.<br />
            If you lose it, you will need to generate a new one.
          </section>
          <gr-button link="" class="closeButton" @click=${this.closeModal}
            >Close</gr-button
          >
        </div>
      </dialog>`;
  }

  private renderToken(tokenInfo: TokenInfo) {
    return html` <tr class=${this.isTokenExpired(tokenInfo) ? 'expired' : ''}>
      <td class="idColumn">${tokenInfo.id}</td>
      <td class="expirationColumn">
        <gr-date-formatter
          withTooltip
          showDateAndTime
          dateFormat="STD"
          .dateStr=${tokenInfo.expiration}
        ></gr-date-formatter>
      </td>
      <td>
        <gr-button
          id="deleteButton"
          @click=${() => this.handleDeleteTap(tokenInfo.id)}
          >Delete</gr-button
        >
      </td>
    </tr>`;
  }

  private renderFooterRow() {
    return html`
      <tr>
        <th style="vertical-align: top;">
          <md-outlined-text-field
            id="newToken"
            class="showBlueFocusBorder"
            placeholder="New Token ID"
            .value=${this.newTokenId ?? ''}
            @input=${(e: InputEvent) => {
              const target = e.target as HTMLInputElement;
              this.newTokenId = target.value;
            }}
            @keydown=${this.handleInputKeydown}
          >
          </md-outlined-text-field>
        </th>
        <th style="vertical-align: top;">
          <md-outlined-text-field
            id="lifetime"
            class="lifeTimeInput showBlueFocusBorder"
            placeholder="Lifetime (e.g. 30d)"
            .value=${this.newLifetime ?? ''}
            @input=${(e: InputEvent) => {
              const target = e.target as HTMLInputElement;
              this.newLifetime = target.value;
            }}
            @keydown=${this.handleInputKeydown}
          >
          </md-outlined-text-field>
          </br>
          (Max. allowed lifetime: ${this.formatDuration(this.maxLifetime)})
        </th>
        <th>
          <gr-button
            id="generateButton"
            link=""
            ?disabled=${!this.newTokenId.length}
            @click=${this.handleGenerateTap}
            >Generate</gr-button
          >
        </th>
      </tr>
    `;
  }

  private formatDuration(durationMinutes: string) {
    if (!durationMinutes) return '';
    if (durationMinutes === 'unlimited') return 'unlimited';
    let minutes = parseInt(durationMinutes, 10);
    let hours = Math.floor(minutes / 60);
    minutes = minutes % 60;
    let days = Math.floor(hours / 24);
    hours = hours % 24;
    const years = Math.floor(days / 365);
    days = days % 365;
    let formatted = '';
    if (years) formatted += `${years}y `;
    if (days) formatted += `${days}d `;
    if (hours) formatted += `${hours}h `;
    if (minutes) formatted += `${minutes}m`;
    return formatted;
  }

  private isTokenExpired(tokenInfo: TokenInfo) {
    if (!tokenInfo.expiration) return false;
    return new Date(tokenInfo.expiration.replace(' ', 'T') + 'Z') < new Date();
  }

  private handleInputKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      e.stopPropagation();
      this.handleGenerateTap();
    }
  }

  private handleGenerateTap() {
    this.generateButton.disabled = true;
    this.status = 'Generating...';
    this.generatedAuthTokenModal?.showModal();
    this.pluginRestApi
      .put<TokenInfo>(
        `/a/config/server/serviceuser~serviceusers/${this.serviceUserId}/tokens/${this.newTokenId}`,
        {
          id: this.newTokenId,
          lifetime: this.newLifetime,
        },
      )
      .catch(err => {
        this.closeModal();
        this.dispatchEvent(
          new CustomEvent('show-error', {
            detail: {message: err},
            composed: true,
            bubbles: true,
          })
        );
      })
      .then(newToken => {
        if (newToken) {
          this.generatedAuthToken = newToken;
          this.status = undefined;
          this.fetchTokens();
          this.tokenInput.value = '';
          this.tokenLifetime.value = '';
        } else {
          this.status = 'Failed to generate';
        }
      })
      .finally(() => {
        this.generateButton.disabled = false;
      });
  }

  private handleDeleteTap(id: string) {
    this.pluginRestApi
      .delete(
        `/a/config/server/serviceuser~serviceusers/${this.serviceUserId}/tokens/${id}`)
      .catch(err => {
        this.dispatchEvent(
          new CustomEvent('show-error', {
            detail: {message: err},
            composed: true,
            bubbles: true,
          })
        );
      })
      .then(() => {
        this.fetchTokens();
      });
  }

  private generatedAuthTokenModalClosed() {
    this.status = undefined;
    this.generatedAuthToken = undefined;
  }

  private closeModal() {
    this.generatedAuthTokenModal?.close();
  }
}
