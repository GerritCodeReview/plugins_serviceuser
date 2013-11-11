MODULE = 'com.googlesource.gerrit.plugins.serviceuser.CreateServiceUserForm'

#
# this currently produces two artifacts:
# o serviceuser.jar
# o serviceuser-static.zip
#

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
  deps = ['//plugins/serviceuser:serviceuser-static'],
)

genrule(
  name = 'serviceuser-static',
  cmd = 'mkdir -p $TMP/static; ' +
    'unzip -qd $TMP/static $(location //plugins/serviceuser:serviceuserform);' +
    'cd $TMP;' +
    'zip -qr $OUT .',
  out = 'serviceuser-static.zip',
  deps = ['//plugins/serviceuser:serviceuserform']
)

gwt_application(
  name = 'serviceuserform',
  module_target = MODULE,
  compiler_opts = [
    '-strict',
    '-style', 'OBF',
    '-optimize', '9',
    '-XdisableClassMetadata',
    '-XdisableCastChecking',
  ],
  deps = ['//plugins/serviceuser:serviceuser-lib']
)

java_library(
  name = 'serviceuser-lib',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  deps = [
    '//gerrit-plugin-gwtui:client',
    '//lib/gwt:user',
    '//:plugin-lib',
  ],
)
