load("//tools/bzl:classpath.bzl", "classpath_collector")
load(
    "//tools/bzl:plugin.bzl",
    "GWT_PLUGIN_DEPS",
    "PLUGIN_DEPS",
)

classpath_collector(
    name = "main_classpath_collect",
    testonly = 1,
    deps = PLUGIN_DEPS + GWT_PLUGIN_DEPS + [
        "//external:gwt-dev",
        "//external:gwt-user",
        "//:serviceuser__plugin",
    ],
)
