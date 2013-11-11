gerrit_plugin(
  name = 'serviceuser',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: serviceuser',
    'Gerrit-Module: com.googlesource.gerrit.plugins.serviceuser.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.serviceuser.HttpModule',
    'Gerrit-SshModule: com.googlesource.gerrit.plugins.serviceuser.SshModule',
  ],
  compile_deps = [
    '//gerrit-plugin-gwtui:client',
    '//lib/gwt:user',
  ],
)
