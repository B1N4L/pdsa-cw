#!/usr/bin/env bash
# Makes the website's generated files: the one-command installers website/install.ps1 (Windows)
# and website/install.sh (Linux) with the site address filled in, and downloads/release.js.
# Netlify runs this for every deploy (see netlify.toml); it needs no Java.
#   SITE_URL=https://amoffline.netlify.app bash AMS/packaging/build-site.sh
set -euo pipefail

PACKAGING="$(cd "$(dirname "$0")" && pwd)"
SITE_DIR="$PACKAGING/../../website"
SITE_URL="${SITE_URL:-${URL:-http://localhost:8137}}"   # Netlify sets URL to the site's main address
SITE_URL="${SITE_URL%/}"
VERSION="${VERSION:-$(sed -n 's/^VERSION="\${VERSION:-\([^}]*\)}".*/\1/p' "$PACKAGING/build-release.sh")}"

fill() {
    sed "s|__AMS_BASE_URL__|$SITE_URL|g; s|__AMS_VERSION__|$VERSION|g" "$1"
}

mkdir -p "$SITE_DIR/downloads"
# Windows scripts need CRLF line endings
fill "$PACKAGING/windows/install.ps1" | sed 's/\r$//; s/$/\r/' > "$SITE_DIR/install.ps1"
fill "$PACKAGING/linux/install-online.sh" | sed 's/\r$//' > "$SITE_DIR/install.sh"
# The page reads the real version and file sizes from the latest GitHub release; this is the fallback
{
    echo "// Written by packaging/build-site.sh"
    echo "window.AMS_RELEASE = { version: \"$VERSION\", site: \"$SITE_URL\", sizes: {} };"
} > "$SITE_DIR/downloads/release.js"

echo "Website installers written for $SITE_URL (version $VERSION)"
