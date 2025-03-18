#!/bin/sh

set -x
curl -s https://get.sdkman.io | bash
source "/home/runner/.sdkman/bin/sdkman-init.sh"
sdk install kotlin 2.1.0
sudo ln -s $(which kotlinc) /usr/local/bin/kotlinc
