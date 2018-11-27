load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps(omit_commons_codec = True):
    if not omit_commons_codec:
        maven_jar(
            name = "commons-codec",
            artifact = "commons-codec:commons-codec:1.4",
            sha1 = "4216af16d38465bbab0f3dff8efa14204f7a399a",
        )
