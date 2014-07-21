include_defs('//bucklets/gerrit_plugin.bucklet')
MODULE = 'com.googlesource.gerrit.plugins.serviceuser.CreateServiceUserForm'

if __standalone_mode__:
  DEPS = ['//lib/gerrit:gwtexpui']
else:
  DEPS = [
    '//gerrit-gwtexpui:Clippy',
    '//gerrit-gwtexpui:GlobalKey',
    '//gerrit-gwtexpui:SafeHtml',
    '//gerrit-gwtexpui:UserAgent',
  ]

gerrit_plugin(
  name = 'serviceuser',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  gwt_module = MODULE,
  manifest_entries = [
    'Gerrit-PluginName: serviceuser',
    'Gerrit-Module: com.googlesource.gerrit.plugins.serviceuser.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.serviceuser.HttpModule',
    'Gerrit-SshModule: com.googlesource.gerrit.plugins.serviceuser.SshModule',
  ],
  provided_deps = DEPS,
)
