// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.serviceuser.client;

import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;

public class SshKeyHelpPanel extends FlowPanel {

  SshKeyHelpPanel() {
    DisclosurePanel dp = new DisclosurePanel("How to generate an SSH Key");
    StringBuilder b = new StringBuilder();
    b.append("<ol>")
        .append("<li>From the Terminal or Git Bash, run <em>ssh-keygen</em></li>")
        .append("<li>")
        .append(
            "Enter a path for the key, e.g. <em>id_rsa</em>. If you are generating the key<br />")
        .append("on your local system take care to not overwrite your own SSH key.")
        .append("</li>")
        .append("<li>")
        .append("Enter a passphrase only if the service where you intend to use this<br />")
        .append("service user is able to deal with passphrases, otherwise leave it blank.<br />")
        .append("Remember this passphrase, as you will need it to unlock the key.")
        .append("</li>")
        .append("<li>")
        .append(
            "Open <em>id_rsa.pub</em> and copy &amp; paste the contents into the box below.<br />")
        .append("Note that <em>id_rsa.pub</em> is your public key and can be shared,<br />")
        .append("while <em>id_rsa</em> is your private key and should be kept secret.")
        .append("</li>")
        .append("</ol>");
    dp.add(new HTML(b.toString()));
    add(dp);
  }
}
