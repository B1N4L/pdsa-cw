#!/bin/sh
# Installs AMOffline for the current user (no sudo needed).
#   Install:    sh install.sh
#   Uninstall:  sh ~/.local/share/amoffline/install.sh --uninstall
# Uses the Java included in this folder (64-bit PCs), or the computer's own Java 17+.
# Your appointments are kept in ~/AMOffline and are not removed by uninstalling.
set -e

DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}"
APP_DIR="$DATA_HOME/amoffline"
BIN_DIR="$HOME/.local/bin"
DESKTOP_FILE="$DATA_HOME/applications/amoffline.desktop"
ICON_FILE="$DATA_HOME/icons/hicolor/256x256/apps/amoffline.png"
HERE="$(cd "$(dirname "$0")" && pwd)"

refresh_menus() {
    command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$DATA_HOME/applications" 2>/dev/null || true
    command -v gtk-update-icon-cache >/dev/null 2>&1 && gtk-update-icon-cache -q "$DATA_HOME/icons/hicolor" 2>/dev/null || true
}

if [ "$1" = "--uninstall" ]; then
    rm -f "$BIN_DIR/amoffline" "$DESKTOP_FILE" "$ICON_FILE"
    rm -rf "$APP_DIR"
    refresh_menus
    echo "AMOffline has been removed. Your appointments are still in $HOME/AMOffline"
    exit 0
fi

if [ "$HERE" = "$APP_DIR" ]; then
    echo "To reinstall, run install.sh from the downloaded AMOffline folder (this copy is only for uninstalling)."
    exit 1
fi

echo "Installing AMOffline..."

# Java: the included runtime if it suits this computer, otherwise the system's Java 17+
BUNDLED=no
case "$(uname -m)" in
    x86_64|amd64)
        if [ -x "$HERE/runtime/bin/java" ]; then BUNDLED=yes; fi ;;
esac
if [ "$BUNDLED" = no ]; then
    if ! command -v java >/dev/null 2>&1; then
        echo "AMOffline needs Java 17 or newer, and Java was not found."
        echo "Install it, then run this again:"
        echo "  Ubuntu / Debian / Mint:  sudo apt install openjdk-21-jre"
        echo "  Fedora:                  sudo dnf install java-21-openjdk"
        echo "  Arch / Manjaro:          sudo pacman -S jre-openjdk"
        exit 1
    fi
    JAVA_MAJOR=$(java -version 2>&1 | head -n 1 | sed -E 's/[^"]*"([0-9]+).*/\1/')
    case "$JAVA_MAJOR" in
        ''|*[!0-9]*) JAVA_MAJOR=0 ;;
    esac
    if [ "$JAVA_MAJOR" -lt 17 ]; then
        echo "AMOffline needs Java 17 or newer, but this computer has Java $JAVA_MAJOR."
        echo "Install a newer Java (for example: sudo apt install openjdk-21-jre) and run this again."
        exit 1
    fi
fi

# copy the app (replacing an older version)
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR" "$BIN_DIR" "$(dirname "$DESKTOP_FILE")" "$(dirname "$ICON_FILE")"
cp "$HERE/AMOffline.jar" "$HERE/install.sh" "$APP_DIR/"
cp "$HERE/amoffline.png" "$ICON_FILE"
if [ "$BUNDLED" = yes ]; then
    cp -R "$HERE/runtime" "$APP_DIR/runtime"
    chmod +x "$APP_DIR/runtime/bin/"* "$APP_DIR/runtime/lib/jspawnhelper" 2>/dev/null || true
    JAVA_CMD="\"$APP_DIR/runtime/bin/java\""
else
    JAVA_CMD="java"
fi

cat > "$BIN_DIR/amoffline" <<EOF
#!/bin/sh
exec $JAVA_CMD -jar "$APP_DIR/AMOffline.jar" "\$@"
EOF
chmod +x "$BIN_DIR/amoffline"

cat > "$DESKTOP_FILE" <<EOF
[Desktop Entry]
Type=Application
Name=AMOffline
Comment=Organise appointments offline
Exec="$BIN_DIR/amoffline"
Icon=amoffline
Terminal=false
Categories=Office;
Keywords=appointments;schedule;calendar;
StartupWMClass=ams-AMSWindow
EOF
refresh_menus

echo "AMOffline is installed$( [ "$BUNDLED" = yes ] && echo ' (Java included)')."
echo "Open \"AMOffline\" from your applications menu, or run: amoffline"
case ":$PATH:" in
    *":$BIN_DIR:"*) ;;
    *) echo "(To use the 'amoffline' command, add $BIN_DIR to your PATH or log out and back in.)" ;;
esac
echo "To uninstall later: sh $APP_DIR/install.sh --uninstall"
