load("@aspect_rules_ts//ts:defs.bzl", "ts_config", "ts_project")
load("@com_googlesource_gerrit_bazlets//js:defs.bzl", "gerrit_js_bundle")
load("//tools/js:eslint.bzl", "plugin_eslint")

package(default_visibility = [":visibility"])

package_group(
    name = "visibility",
    packages = ["//plugins/serviceuser/..."],
)

ts_config(
    name = "tsconfig",
    src = "tsconfig.json",
    deps = [
        "//plugins:tsconfig-plugins-base",
    ],
)

ts_project(
    name = "serviceuser-ts",
    srcs = glob(["*.ts"]),
    incremental = True,
    out_dir = "_bazel_ts_out",
    tsconfig = ":tsconfig",
    deps = [
        "//plugins:node_modules",
    ],
)

gerrit_js_bundle(
    name = "serviceuser",
    srcs = [":serviceuser-ts"],
    entry_point = "_bazel_ts_out/plugin.js",
)

# Run the lint tests with `bazel test plugins/serviceuser/web:lint_test`.
plugin_eslint()
