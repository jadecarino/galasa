#!/usr/bin/env bash

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#


#-----------------------------------------------------------------------------------------                   
#
# Objectives: Get the last successful workflow run ID to get artifacts from if the current
#             workflow run has not built a particular module.
# 
#-----------------------------------------------------------------------------------------                   

# Where is this script executing from ?
BASEDIR=$(dirname "$0");pushd $BASEDIR 2>&1 >> /dev/null ;BASEDIR=$(pwd);popd 2>&1 >> /dev/null
# echo "Running from directory ${BASEDIR}"
export ORIGINAL_DIR=$(pwd)
# cd "${BASEDIR}"

cd "${BASEDIR}/.."
PROJECT_DIR=$(pwd)

#-----------------------------------------------------------------------------------------                   
#
# Set Colors
#
#-----------------------------------------------------------------------------------------                   
bold=$(tput bold)
underline=$(tput sgr 0 1)
reset=$(tput sgr0)
red=$(tput setaf 1)
green=$(tput setaf 76)
white=$(tput setaf 7)
tan=$(tput setaf 202)
blue=$(tput setaf 25)

#-----------------------------------------------------------------------------------------                   
#
# Headers and Logging
#
#-----------------------------------------------------------------------------------------                   
underline() { printf "${underline}${bold}%s${reset}\n" "$@" ; }
h1() { printf "\n${underline}${bold}${blue}%s${reset}\n" "$@" ; }
h2() { printf "\n${underline}${bold}${white}%s${reset}\n" "$@" ; }
debug() { printf "${white}[.] %s${reset}\n" "$@" ; }
info()  { printf "${white}[➜] %s${reset}\n" "$@" ; }
success() { printf "${white}[${green}✔${white}] ${green}%s${reset}\n" "$@" ; }
error() { printf "${white}[${red}✖${white}] ${red}%s${reset}\n" "$@" ; }
warn() { printf "${white}[${tan}➜${white}] ${tan}%s${reset}\n" "$@" ; }
bold() { printf "${bold}%s${reset}\n" "$@" ; }
note() { printf "\n${underline}${bold}${blue}Note:${reset} ${blue}%s${reset}\n" "$@" ; }

#-----------------------------------------------------------------------------------------                   
# Functions
#-----------------------------------------------------------------------------------------                   
function usage {
    info "Syntax: get-last-successful-workflow-run-for-artifacts.sh [OPTIONS]"
    cat << EOF
Options are:
-h | --help : Display this help text
--repo          The repository for this GitHub Actions workflow
--module        The module we are looking for artifacts for
EOF
}

#-----------------------------------------------------------------------------------------
# Process parameters
#-----------------------------------------------------------------------------------------
repo=""
module=""
while [ "$1" != "" ]; do
    case $1 in
        -h | --help )       usage
                            exit
                            ;;

        --repo )            repo="$2"
                            shift
                            ;;

        --module )          module="$2"
                            shift
                            ;;

        * )                 error "Unexpected argument $1"
                            usage
                            exit 1
    esac
    shift
done

# A map of module names and the job name
# that states the module was unchanged.
# This will be used by the GitHub CLI to
# see if a workflow run skipped building
# a module and so will have no artifacts.
declare -A module_skipped_job_name_map

# Assign key-value pairs
module_skipped_job_name_map=(
    ["buildutils"]="Buildutils is unchanged"
    ["wrapping"]="Wrapping is unchanged"
    ["gradle"]="Gradle is unchanged"
    ["maven"]="Maven is unchanged"
    ["framework"]="Framework is unchanged"
    ["extensions"]="Extensions is unchanged"
    ["managers"]="Managers is unchanged"
    ["obr"]="OBR is unchanged"
)

skipped_job_name=${module_skipped_job_name_map[${module}]}

#-----------------------------------------------------------------------------------------
# Functions
#-----------------------------------------------------------------------------------------

function get_last_successful_workflow_id() {

    h1 "Getting the last successful workflow run ID that has stored artifacts for the ${module} module" 

    h2 "Using the GitHub CLI to list the successful runs of the Main Build Orchestrator workflow"
    gh run list --repo "${repo}" --workflow "Main Build Orchestrator" --status success | tee gh-run-list.log

    h2 "Extracting the run IDs from the output"
    output=$(cat gh-run-list.log 2>&1)
    run_ids=($(echo "$output" | grep -oE '[0-9]{11}'))

    echo "Extracted IDs: ${run_ids[@]}"

    for run_id in "${run_ids[@]}"; do
        echo "Checking if the run ${run_id} ran the ${module} build or skipped it"
        if gh run view ${run_id} --log 2>&1 | grep -q "${skipped_job_name}"; then
            echo "The build of module ${module} was skipped so will have no artifacts in this workflow run."
        else
            echo "The build of module ${module} was not skipped - artifacts should be available to download!"
            echo "LAST_SUCCESSFUL_WORKFLOW_ID=${run_id}" >> $GITHUB_OUTPUT
            break
        fi
    done

}

get_last_successful_workflow_id