AMOffline (Linux)
=================================

Install (adds AMOffline to your applications menu, no sudo needed):
  sh install.sh

On 64-bit PCs (AMOffline-Linux-x64.tar.gz) Java is included, so nothing else is needed.
Other computers (AMOffline-Linux.tar.gz, e.g. Raspberry Pi) need Java 17 or newer:
  Ubuntu / Debian / Mint:  sudo apt install openjdk-21-jre
  Fedora:                  sudo dnf install java-21-openjdk
  Arch / Manjaro:          sudo pacman -S jre-openjdk

Uninstall:
  sh ~/.local/share/amoffline/install.sh --uninstall

Your appointments are saved automatically in ~/AMOffline (backups go in ~/AMOffline/Backups).
The text-menu version is also included:  java -cp AMOffline.jar ams.AMS
