#!/bin/sh
# One-command installer for AMS Appointment Manager on Linux (no sudo needed):
#     curl -fsSL __AMS_BASE_URL__/install.sh | sh
# Uninstall:
#     curl -fsSL __AMS_BASE_URL__/install.sh | sh -s -- --uninstall
# Downloads AMS (with Java included on 64-bit PCs) and runs its installer.
set -e

BASE="${AMS_BASE_URL:-__AMS_BASE_URL__}"
case "$(uname -m)" in
    x86_64|amd64) FILE=AMS-Linux-x64.tar.gz ;;
    *) FILE=AMS-Linux.tar.gz ;;  # other processors use the computer's own Java
esac

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT INT TERM

echo "Downloading AMS ($FILE)..."
if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$BASE/downloads/$FILE" -o "$TMP/ams.tar.gz"
elif command -v wget >/dev/null 2>&1; then
    wget -qO "$TMP/ams.tar.gz" "$BASE/downloads/$FILE"
else
    echo "Please install curl or wget first."
    exit 1
fi
tar -xzf "$TMP/ams.tar.gz" -C "$TMP"
sh "$TMP/AMS/install.sh" "$@"
