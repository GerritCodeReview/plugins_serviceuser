include_defs('//bucklets/gerrit_plugin.bucklet')
MODULE = 'com.googlesource.gerrit.plugins.serviceuser.CreateServiceUserForm'

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
  provided_deps = [
    '//lib/commons:codec',
  ],
)

java_library(
  name = 'classpath',
  deps = [':serviceuser__plugin'],
)
