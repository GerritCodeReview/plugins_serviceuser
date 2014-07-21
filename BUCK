include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/java_doc.bucklet')
include_defs('//bucklets/maven_package.bucklet')
include_defs(align_path('serviceuser', '//VERSION'))

MODULE = 'com.googlesource.gerrit.plugins.serviceuser.CreateServiceUserForm'
SRCS = glob(['src/main/java/**/*.java'])
RSRCS = glob(['src/main/**/*'])

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
  srcs = SRCS,
  resources = RSRCS,
  gwt_module = MODULE,
  manifest_entries = [
    'Gerrit-PluginName: serviceuser',
    'Gerrit-Module: com.googlesource.gerrit.plugins.serviceuser.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.serviceuser.HttpModule',
    'Gerrit-SshModule: com.googlesource.gerrit.plugins.serviceuser.SshModule',
  ],
  provided_deps = DEPS,
)

java_library(
  name = 'classpath',
  deps = [':serviceuser__plugin'],
)

maven_package(
  repository = 'gerrit-maven-repository',
  url = 'gs://gerrit-maven',
  version = PLUGIN_VERSION,
  group = 'com.googlesource.gerrit.plugins.serviceuser',
  jar = {'serviceuser': ':serviceuser__plugin'},
  src = {'serviceuser': ':serviceuser-src'},
  doc = {'serviceuser': ':serviceuser-javadoc'},
)

java_sources(
  name = 'serviceuser-src',
  srcs = SRCS + RSRCS,
)

java_doc(
  name = 'serviceuser-javadoc',
  title = 'Serviceuser API Documentation',
  pkg = 'com.googlesource.gerrit.plugins.serviceuser',
  paths = ['src/main/java'],
  srcs = glob([n + '**/*.java' for n in SRCS]),
  deps = GERRIT_PLUGIN_API + GERRIT_GWT_API + DEPS + [
    ':serviceuser__plugin',
    '//lib/gwt:user',
    '//lib/gwt:dev',
  ],
)
