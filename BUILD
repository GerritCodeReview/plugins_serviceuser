load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")

gerrit_plugin(
    name = "serviceuser",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: serviceuser",
        "Gerrit-Module: com.googlesource.gerrit.plugins.serviceuser.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.serviceuser.HttpModule",
        "Gerrit-SshModule: com.googlesource.gerrit.plugins.serviceuser.SshModule",
    ],
    resource_jars = ["//plugins/serviceuser/gr-serviceuser:serviceuser"],
    resources = glob(["src/main/resources/**/*"]),
)

junit_tests(
    name = "serviceuser_tests",
    testonly = 1,
    srcs = glob([
        "src/test/java/**/*Test.java",
    ]),
    tags = ["serviceuser"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":serviceuser__plugin",
    ],
)
