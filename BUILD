load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "serviceuser",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: serviceuser",
        "Gerrit-Module: com.googlesource.gerrit.plugins.serviceuser.Module",
        "Gerrit-SshModule: com.googlesource.gerrit.plugins.serviceuser.SshModule",
    ],
    resources = glob(["src/main/**/*"]),
)
