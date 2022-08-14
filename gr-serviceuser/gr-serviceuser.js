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

import {GrServiceUserList} from './gr-serviceuser-list.js';
import {GrServiceUserDetail} from './gr-serviceuser-detail.js';
import {GrServiceUserCreate} from './gr-serviceuser-create.js';

Gerrit.install(plugin => {
  plugin.restApi('/accounts/self/capabilities/').get('')
      .then(capabilities => {
        if (capabilities
            && (capabilities.administrateServer
                || capabilities['serviceuser-createServiceUser'])) {
          plugin.screen('create', GrServiceUserCreate.is);
        }
        plugin.screen('list', GrServiceUserList.is);
        plugin.screen('user', GrServiceUserDetail.is);
        plugin.admin()
            .addMenuLink(
                'Service Users',
                '/x/serviceuser/list');
      });
});
