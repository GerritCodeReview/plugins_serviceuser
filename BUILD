load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")
load("//tools/js:eslint.bzl", "eslint")

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

# Define the eslinter for the plugin
# The eslint macro creates 2 rules: lint_test and lint_bin
eslint(
    name = "lint",
    srcs = glob([
        "gr-serviceuser/**/*.js",
    ]),
    config = ".eslintrc.json",
    data = [],
    extensions = [
        ".js",
    ],
    ignore = ".eslintignore",
    plugins = [
        "@npm//eslint-config-google",
        "@npm//eslint-plugin-html",
        "@npm//eslint-plugin-import",
        "@npm//eslint-plugin-jsdoc",
    ],
)
