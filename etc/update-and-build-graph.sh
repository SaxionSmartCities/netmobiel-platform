#!/bin/bash
# Download the data file and rebuild the grpah for OpenTripPlanner. If successfull then
# restart the OpenTripPlanner service.
set -o pipefail -o noclobber -o nounset
otpdir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
runuser -l saxion -c "$otpdir/update-data.sh"
status=$?
if [[ $status -eq 0 ]]; then
	echo "Start building the graph..."
	runuser -l saxion -c "$otpdir/build-graph.sh"
	status=$?
fi
if [[ $status -eq 0 ]]; then
	echo "Restart OTP"
	systemctl restart open-trip-planner.service
	status=$?

fi
if [[ $status -ne 0 ]]; then
	echo "Build of OTP graph failed"
fi
exit $status
