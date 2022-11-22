#!/bin/bash
# Rebuild the graph for OpenTripPlanner. The process takes 15 to 20 minutes.
set -o pipefail -o noclobber -o nounset
otpdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
graphdir=/var/otp/graphs/nl
file=Graph.obj
if [[ ! -w $graphdir ]]; then
	echo "Cannot write to $graphdir, did you use sudo?"
	exit 1
fi
if [[ ! -s $graphdir/gtfs-nl.zip ]]; then
	echo "File $graphdir/gtfs-nl.zip is non-existent or empty"
	exit 1
fi
if [[ ! -s $graphdir/netherlands.osm.pbf ]]; then
	echo "File $graphdir/netherlands.osm.pbf is non-existent or empty"
	exit 1
fi
if [[ -f $graphdir/$file ]]; then
	mv -f $graphdir/$file $graphdir/${file}.old
fi
echo "Building graph..."
if [[ -f /var/log/otp/build.log ]]; then
	mv -f /var/log/otp/build.log /var/log/otp/build.log.1
fi
# Note: the noclobber option prevents overwriting files through redirection. Use >| to override
echo $(pwd)
java -Xmx12G -Xverify:none -jar $otpdir/otp-1.5.0-SNAPSHOT-shaded.jar --build $graphdir >/var/log/otp/build.log
status=$?
if [[ $status -ne 0 ]]; then
	echo "An error occurred in building $file, restoring old situation"
	if [[ -f $graphdir/${file}.old ]]; then
		mv -f  $graphdir/${file}.old $graphdir/$file
	fi
else
	echo "Building of graph finished sucessfully"
fi
exit $status
