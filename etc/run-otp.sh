#!/bin/bash
set -o pipefail -o noclobber -o nounset
otpdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
otpdatadir=/var/otp
router=nl
graphdir=$otpdatadir/graphs/$router
# If the server crashes on a built of the graph, be sure to reinstall the old version.
if [[ ! -f $graphdir/Graph.obj ]] && [[ -f $graphdir/Graph.obj.old ]]; then
	echo "No Graph file found, restore previous version."
	mv -f $graphdir/Graph.obj.old $graphdir/Graph.obj
	# Just to keep track
	touch $graphdir/Graph.restored
fi
/usr/bin/java -Xmx8G -Xverify:none -jar $otpdir/otp-1.5.0-SNAPSHOT-shaded.jar --basePath $otpdatadir --router $router --server --bindAddress 0.0.0.0 --port 8080

