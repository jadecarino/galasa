#! /usr/bin/env bash 

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#

#-----------------------------------------------------------------------------------------                   
#
# Objectives: Sets the version number of this component.
#
# Environment variable over-rides:
# None
# 
#-----------------------------------------------------------------------------------------                   

# Where is this script executing from ?
BASEDIR=$(dirname "$0");pushd $BASEDIR 2>&1 >> /dev/null ;BASEDIR=$(pwd);popd 2>&1 >> /dev/null
# echo "Running from directory ${BASEDIR}"
export ORIGINAL_DIR=$(pwd)
# cd "${BASEDIR}"

cd "${BASEDIR}/.."
WORKSPACE_DIR=$(pwd)


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
underline() { printf "${underline}${bold}%s${reset}\n" "$@" ;}
h1() { printf "\n${underline}${bold}${blue}%s${reset}\n" "$@" ;}
h2() { printf "\n${underline}${bold}${white}%s${reset}\n" "$@" ;}
debug() { printf "${white}%s${reset}\n" "$@" ;}
info() { printf "${white}➜ %s${reset}\n" "$@" ;}
success() { printf "${green}✔ %s${reset}\n" "$@" ;}
error() { printf "${red}✖ %s${reset}\n" "$@" ;}
warn() { printf "${tan}➜ %s${reset}\n" "$@" ;}
bold() { printf "${bold}%s${reset}\n" "$@" ;}
note() { printf "\n${underline}${bold}${blue}Note:${reset} ${blue}%s${reset}\n" "$@" ;}

#-----------------------------------------------------------------------------------------                   
# Functions
#-----------------------------------------------------------------------------------------                   
function usage {
    h1 "Syntax"
    cat << EOF
set-version.sh [OPTIONS]
Options are:
-v | --version xxx : Mandatory. Set the version number to something explicitly. 
    Re-builds the release.yaml based on the contents of sub-projects.
    For example '--version 0.29.0'
EOF
}

#-----------------------------------------------------------------------------------------                   
# Process parameters
#-----------------------------------------------------------------------------------------                   
component_version=""

while [ "$1" != "" ]; do
    case $1 in
        -v | --version )        shift
                                export component_version=$1
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     error "Unexpected argument $1"
                                usage
                                exit 1
    esac
    shift
done

if [[ -z $component_version ]]; then 
    error "Missing mandatory '--version' argument."
    usage
    exit 1
fi

#-------------------------------------------------------------------------------
function update_release_yaml {

    source_file=$1
    target_file=$2
    temp_dir=$3
    match_regex="$4" # The regex we are looking for, beyond which we expect a version line.
    indent="$5"

    h1 "Updating the release.yaml so the OBR version gets set. For regex $match_regex"



    version_regex="^.*version[:].*$"

    # Read through the release yaml and set the version of the framework bundle explicitly.
    # It's on the line after the line containing 'release:'
    # The line we need to change looks like this: version: 0.29.0
    is_line_supressed=false
    while IFS= read -r line
    do
        if [[ "$line" =~ $match_regex ]]; then
            # We found the marker, so the next line needs supressing.
            echo "$line"
            is_line_supressed=true
        else
            if [[ $is_line_supressed == true ]]; then

                if [[ "$line" =~ $version_regex ]]; then
                    # This line contains a "version" property, and it follows a section header we want to target.
                    # So supress this line.
                    # Don't echo this line, but we only want to supress one line.
                    is_line_supressed=false
                    echo "${indent}version: $component_version"
                else 
                    # This line follows something we are trying to target.
                    # But the line does not contain a 'version'.
                    is_line_supressed=false
                    echo "$line"
                fi
            else
                # Nothing special about this line, so echo it.
                echo "$line"
            fi
        fi

    done < $source_file > $target_file
    rc=$?; if [[ "${rc}" != "0" ]]; then error "Failed to set version into file."; exit 1; fi

    # Copy the temp files back to where they belong...
    cp $temp_dir/release.yaml ${BASEDIR}/release.yaml

    success "OBR release.yaml updated OK."
}


function update_dependency_versions {
    h1 "Updating the version in the dependencies so we pull in the correct managers, framework...etc."
    temp_dir=$1

    set -o pipefail

    temp_file="$temp_dir/dependency-build.gradle"
    source_file="${BASEDIR}/dependency-download/build.gradle"
    info "Using temporary file $temp_file"
    info "Updating file $source_file"

    cat $source_file | sed "s/^version[ ]*=[ ]*\".*\"[ ]*$/version = \"$component_version\"/1" > $temp_file
    rc=$?; if [[ "${rc}" != "0" ]]; then error "Failed to set version into dependency-download build.gradle file."; exit 1; fi
    cp $temp_file ${source_file}
    rc=$?; if [[ "${rc}" != "0" ]]; then error "Failed to overwrite new version of dependency-download build.gradle file."; exit 1; fi

    success "Dependency versions updated OK."
}


temp_dir=$BASEDIR/temp/versions
rm -fr $temp_dir
mkdir -p $temp_dir

update_release_yaml ${BASEDIR}/release.yaml $temp_dir/release.yaml $temp_dir "^.*release[ ]*[:][ ]*$" "  "
update_release_yaml ${BASEDIR}/release.yaml $temp_dir/release.yaml $temp_dir "^.*artifact[:] dev[.]galasa[.]wrapping[.]gson.*$" "    "
update_release_yaml ${BASEDIR}/release.yaml $temp_dir/release.yaml $temp_dir "^.*artifact[:] dev[.]galasa[.]wrapping[.]httpclient-osgi.*$" "    "

update_dependency_versions $temp_dir
