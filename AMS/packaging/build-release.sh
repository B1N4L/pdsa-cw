#!/usr/bin/env bash
# Builds the downloadable files and installers for the AMS website.
#   SITE_URL=https://your-site.example bash packaging/build-release.sh
# (run from anywhere; needs JDK 17 or newer. SITE_URL is the address the website will be served from;
#  it is written into the online installers. It defaults to the local preview address.)
#
# Output (in AMS/release, and copied to website/downloads):
#   AMS-Windows.zip       Windows app with Java included + "Install AMS.cmd" (only when built on Windows)
#   AMS-Linux-x64.tar.gz  Linux app with Java included + install.sh (needs the Linux JDK's jmods, see below)
#   AMS-Linux.tar.gz      Linux app without Java (for other processors) + install.sh
#   AMS.jar               runs on any OS with Java 17+
# and the one-command installers website/install.ps1 (Windows) and website/install.sh (Linux).
#
# The Linux runtime is made from the Linux JDK's jmods, of the same Java version as this JDK. Put the
# Linux JDK .tar.gz in AMS/out/linux-jdk-download/jdk-linux.tar.gz (or set LINUX_JMODS to its jmods folder).
set -euo pipefail

VERSION="1.2.0"
SITE_URL="${SITE_URL:-http://localhost:8137}"
SITE_URL="${SITE_URL%/}"
AMS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SITE_DIR="$AMS_DIR/../website"
BUILD="$AMS_DIR/out/release-build"
RELEASE="$AMS_DIR/release"
MODULES="java.base,java.desktop"
JLINK_OPTIONS=(--strip-debug --no-header-files --no-man-pages --compress=zip-6)

if [ -n "${JAVA_HOME:-}" ]; then
    JBIN="$JAVA_HOME/bin/"
else
    JBIN=""
fi

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) OS=windows ;;
    Darwin*) OS=mac ;;
    *) OS=linux ;;
esac

# Fill in the site address and version in an installer script
fill() {
    sed "s|__AMS_BASE_URL__|$SITE_URL|g; s|__AMS_VERSION__|$VERSION|g" "$1"
}

# Windows scripts need CRLF line endings
crlf() {
    sed -i 's/\r$//; s/$/\r/' "$@"
}

# Make a .tar.gz of folder $1/AMS where install.sh and the Java programs are executable
make_tar() {
    local dir="$1" out="$2"
    if tar --version 2>/dev/null | grep -q "GNU tar"; then
        tar -C "$dir" --owner=0 --group=0 --mode='u=rwX,go=rX' \
            --exclude='AMS/install.sh' --exclude='AMS/runtime/bin/*' --exclude='AMS/runtime/lib/jspawnhelper' \
            -cf "$out.tar" AMS
        local executables=(AMS/install.sh)
        if [ -d "$dir/AMS/runtime/bin" ]; then
            executables+=($(cd "$dir" && ls AMS/runtime/bin/* AMS/runtime/lib/jspawnhelper 2>/dev/null))
        fi
        tar -C "$dir" --owner=0 --group=0 --mode=0755 -rf "$out.tar" "${executables[@]}"
        gzip -9 -c "$out.tar" > "$out"
        rm -f "$out.tar"
    else
        chmod +x "$dir/AMS/install.sh"
        tar -C "$dir" -czf "$out" AMS
    fi
}

echo "== Cleaning"
rm -rf -- "${BUILD:?}" "${RELEASE:?}" "${SITE_DIR:?}/downloads"
mkdir -p "$BUILD/classes" "$BUILD/jar" "$BUILD/icons" "$RELEASE"

echo "== Compiling"
"${JBIN}javac" --release 17 -nowarn -d "$BUILD/classes" "$AMS_DIR"/src/ams/*.java

echo "== Icons"
"${JBIN}java" -cp "$BUILD/classes" "$AMS_DIR/packaging/tools/MakeIcons.java" "$BUILD/icons"

echo "== AMS.jar"
"${JBIN}jar" --create --file "$BUILD/jar/AMS.jar" --main-class ams.AMSWindow -C "$BUILD/classes" .
cp "$BUILD/jar/AMS.jar" "$RELEASE/AMS.jar"

# Linux bundle without Java, then (if the Linux jmods are available) with Java
linux_bundle() {
    local dir="$1"
    mkdir -p "$dir/AMS"
    cp "$BUILD/jar/AMS.jar" "$BUILD/icons/ams.png" "$AMS_DIR/packaging/linux/README.txt" "$dir/AMS/"
    sed 's/\r$//' "$AMS_DIR/packaging/linux/install.sh" > "$dir/AMS/install.sh"
}
echo "== AMS-Linux.tar.gz (uses the computer's Java)"
linux_bundle "$BUILD/linux"
make_tar "$BUILD/linux" "$RELEASE/AMS-Linux.tar.gz"

LINUX_JMODS="${LINUX_JMODS:-}"
if [ -z "$LINUX_JMODS" ]; then
    DOWNLOAD="$AMS_DIR/out/linux-jdk-download/jdk-linux.tar.gz"
    if ! ls -d "$AMS_DIR"/out/linux-jdk/*/jmods >/dev/null 2>&1 && [ -f "$DOWNLOAD" ]; then
        echo "== Unpacking the Linux JDK's jmods"
        mkdir -p "$AMS_DIR/out/linux-jdk"
        tar -xzf "$DOWNLOAD" -C "$AMS_DIR/out/linux-jdk" --wildcards '*/jmods/*'
    fi
    LINUX_JMODS="$(ls -d "$AMS_DIR"/out/linux-jdk/*/jmods 2>/dev/null | head -n 1 || true)"
fi
if [ -n "$LINUX_JMODS" ] && [ -f "$LINUX_JMODS/java.base.jmod" ]; then
    echo "== AMS-Linux-x64.tar.gz (Java included)"
    linux_bundle "$BUILD/linux-x64"
    "${JBIN}jlink" --module-path "$LINUX_JMODS" --add-modules "$MODULES" "${JLINK_OPTIONS[@]}" --output "$BUILD/linux-x64/AMS/runtime"
    make_tar "$BUILD/linux-x64" "$RELEASE/AMS-Linux-x64.tar.gz"
else
    echo "== Skipping AMS-Linux-x64.tar.gz (no Linux jmods; see the note at the top)"
fi

if [ "$OS" = windows ]; then
    # The shortcuts run the signed javaw.exe from the included Java, so Smart App Control allows it
    # (a generated, unsigned launcher .exe would be blocked).
    echo "== AMS-Windows.zip (Java included)"
    W="$BUILD/windows/AMS"
    mkdir -p "$W/app" "$W/setup"
    "${JBIN}jlink" --add-modules "$MODULES" "${JLINK_OPTIONS[@]}" --output "$W/runtime"
    cp "$BUILD/jar/AMS.jar" "$W/app/"
    cp "$BUILD/icons/ams.ico" "$W/"
    fill "$AMS_DIR/packaging/windows/install.ps1" > "$W/setup/install.ps1"
    cp "$AMS_DIR/packaging/windows/uninstall.ps1" "$W/setup/"
    cp "$AMS_DIR/packaging/windows/Install AMS.cmd" "$AMS_DIR/packaging/windows/README.txt" "$W/"
    crlf "$W/setup/install.ps1" "$W/setup/uninstall.ps1" "$W/Install AMS.cmd" "$W/README.txt"
    # Windows' own tar.exe writes .zip files
    (cd "$BUILD/windows" && "$(cygpath -u "${SYSTEMROOT:-C:\\Windows}")/System32/tar.exe" -a -c -f "$(cygpath -w "$RELEASE/AMS-Windows.zip")" AMS)
else
    echo "== Skipping AMS-Windows.zip (build on Windows to make it)"
fi

echo "== Website downloads and one-command installers ($SITE_URL)"
mkdir -p "$SITE_DIR/downloads" "$SITE_DIR/assets"
cp "$RELEASE"/* "$SITE_DIR/downloads/"
fill "$AMS_DIR/packaging/windows/install.ps1" > "$SITE_DIR/install.ps1"
crlf "$SITE_DIR/install.ps1"
fill "$AMS_DIR/packaging/linux/install-online.sh" | sed 's/\r$//' > "$SITE_DIR/install.sh"
cp "$BUILD/icons/icon-512.png" "$BUILD/icons/icon-180.png" "$BUILD/icons/icon-64.png" "$SITE_DIR/assets/"
# screenshots of the app with sample data (needs a desktop session; skipped if it fails)
for THEME in light dark; do
    SUFFIX=""
    [ "$THEME" = dark ] && SUFFIX="-dark"
    if "${JBIN}java" -cp "$BUILD/classes" -Dams.data="$BUILD/shot-data-$THEME" -Dams.theme="$THEME" -Dshot.scale=1.5 \
            "$AMS_DIR/packaging/tools/Screenshot.java" "$BUILD/shots-$THEME" > /dev/null 2>&1; then
        cp "$BUILD/shots-$THEME/app-main.png" "$SITE_DIR/assets/screenshot-main$SUFFIX.png"
        cp "$BUILD/shots-$THEME/app-form.png" "$SITE_DIR/assets/screenshot-form$SUFFIX.png"
    else
        echo "($THEME screenshots skipped)"
    fi
done
{
    echo "// Written by packaging/build-release.sh"
    echo "window.AMS_RELEASE = {"
    echo "  version: \"$VERSION\","
    echo "  built: \"$(date +%Y-%m-%d)\","
    echo "  site: \"$SITE_URL\","
    echo "  sizes: {"
    for f in "$SITE_DIR"/downloads/*.jar "$SITE_DIR"/downloads/*.gz "$SITE_DIR"/downloads/*.zip; do
        [ -f "$f" ] || continue
        echo "    \"$(basename "$f")\": $(wc -c < "$f" | tr -d ' '),"
    done
    echo "  }"
    echo "};"
} > "$SITE_DIR/downloads/release.js"

echo
echo "Done:"
ls -l "$RELEASE"
