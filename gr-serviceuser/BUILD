load("//tools/bzl:js.bzl", "gerrit_js_bundle")

package(default_visibility = [":visibility"])

package_group(
    name = "visibility",
    packages = ["//plugins/serviceuser/..."],
)

gerrit_js_bundle(
    name = "serviceuser",
    srcs = glob(["*.js"]),
    entry_point = "gr-serviceuser.js",
)
