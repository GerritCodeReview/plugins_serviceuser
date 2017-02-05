load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "serviceuser",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    gwt_module = "com.googlesource.gerrit.plugins.serviceuser.CreateServiceUserForm",
    manifest_entries = [
        "Gerrit-PluginName: serviceuser",
        "Gerrit-Module: com.googlesource.gerrit.plugins.serviceuser.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.serviceuser.HttpModule",
        "Gerrit-SshModule: com.googlesource.gerrit.plugins.serviceuser.SshModule",
    ],
    deps = [
        "@commons_codec//jar:neverlink",
    ],
)
