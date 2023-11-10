load("//tools/bzl:js.bzl", "gerrit_js_bundle")
load("//tools/js:eslint.bzl", "plugin_eslint")
load("@npm//@bazel/typescript:index.bzl", "ts_config", "ts_project")

package(default_visibility = [":visibility"])

package_group(
    name = "visibility",
    packages = ["//plugins/serviceuser/..."],
)

ts_config(
    name = "tsconfig",
    src = "tsconfig.json",
    deps = [
        "//plugins:tsconfig-plugins-base.json",
    ],
)

ts_project(
    name = "serviceuser-ts",
    srcs = glob(["*.ts"]),
    incremental = True,
    out_dir = "_bazel_ts_out",
    supports_workers = True,
    tsc = "//tools/node_tools:tsc-bin",
    tsconfig = ":tsconfig",
    deps = [
        "@plugins_npm//@gerritcodereview/typescript-api",
        "@plugins_npm//lit",
    ],
)

gerrit_js_bundle(
    name = "serviceuser",
    srcs = [":serviceuser-ts"],
    entry_point = "_bazel_ts_out/plugin.js",
)

# Run the lint tests with `bazel test plugins/serviceuser/gr-serviceuser:lint_test`.
plugin_eslint()
