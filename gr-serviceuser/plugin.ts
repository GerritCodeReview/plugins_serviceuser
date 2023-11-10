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

import '@gerritcodereview/typescript-api/gerrit';

import './gr-serviceuser-create';
import './gr-serviceuser-detail';
import './gr-serviceuser-list';

export interface AccountCapabilityInfo {
  administrateServer: boolean;
  'serviceuser-createServiceUser': boolean;
}

window.Gerrit.install(plugin => {
  plugin
    .restApi()
    .get<AccountCapabilityInfo>('/accounts/self/capabilities/')
    .then(capabilities => {
      if (
        capabilities &&
        (capabilities['administrateServer'] ||
          capabilities['serviceuser-createServiceUser'])
      ) {
        plugin.screen('create', 'gr-serviceuser-create');
      }
      plugin.screen('list', 'gr-serviceuser-list');
      plugin.screen('user', 'gr-serviceuser-detail');
      plugin.admin().addMenuLink('Service Users', '/x/serviceuser/list');
    });
});
