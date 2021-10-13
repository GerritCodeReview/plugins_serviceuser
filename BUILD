load("@npm//@bazel/rollup:index.bzl", "rollup_bundle")
load("//tools/bzl:js.bzl", "polygerrit_plugin")
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/js:eslint.bzl", "eslint")
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
    resource_jars = [":gr-serviceuser-static"],
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

genrule2(
    name = "gr-serviceuser-static",
    srcs = [":gr-serviceuser"],
    outs = ["gr-serviceuser-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :gr-serviceuser) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

rollup_bundle(
    name = "serviceuser-bundle",
    srcs = glob(["gr-serviceuser/*.js"]),
    entry_point = "gr-serviceuser/gr-serviceuser.js",
    format = "iife",
    rollup_bin = "//tools/node_tools:rollup-bin",
    sourcemap = "hidden",
    deps = [
        "@tools_npm//rollup-plugin-node-resolve",
    ],
)

polygerrit_plugin(
    name = "gr-serviceuser",
    app = "serviceuser-bundle.js",
    plugin_name = "serviceuser",
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
