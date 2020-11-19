#!/bin/bash 
# Downloads the files needed to rebuild the graph for the OpenTripPlanner.
# The script is designed in way to avoid an inconsistent state.
set -o errexit -o pipefail -o noclobber -o nounset
graphdir=/var/otp/graphs/nl
osm_file="netherlands.osm.pbf"
osm_site="http://download.openstreetmap.fr/extracts/europe"
#osm_file=test1.txt
#osm_site=http://localhost
gtfs_file="gtfs-nl.zip"
gtfs_site="http://gtfs.ovapi.nl/nl"
#gtfs_file=test2.txt
#gtfs_site=http://localhost
if [ ! -w $graphdir ]; then
	echo "Cannot write to $graphdir, did you use sudo?"
	exit 1
fi

cd $graphdir
# Download OpenStreetMap data
echo "Downloading $osm_site/$osm_file"
wget -q -O ${osm_file}.new $osm_site/$osm_file
# Download GTFS data netherlands
echo "Downloading $gtfs_site/$gtfs_file"
wget -q -O ${gtfs_file}.new $gtfs_site/$gtfs_file
# Rename current files
if [[ -f "$osm_file" ]]; then
	mv -f "$osm_file" ${osm_file}.old
fi
if [[ -f "$gtfs_file" ]]; then
	mv -f "$gtfs_file" ${gtfs_file}.old
fi
# Install downloaded files
mv -f "${osm_file}.new" ${osm_file}
mv -f "${gtfs_file}.new" ${gtfs_file}
echo "OSM and GTFS downloads completed"
