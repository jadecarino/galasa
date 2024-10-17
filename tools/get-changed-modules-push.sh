#!/usr/bin/env bash

#
# Copyright contributors to the Galasa project
#
# SPDX-License-Identifier: EPL-2.0
#


#-----------------------------------------------------------------------------------------                   
#
# Objectives: Get all modules that have been changed in a Push (Merge of a Pull Request).
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
    info "Syntax: get-changed-modules-push.sh [OPTIONS]"
    cat << EOF
Options are:
-h | --help : Display this help text
--something...
EOF
}

module_names=(\
    "buildutils" \
    "wrapping" \
    "gradle" \
    "maven" \
    "framework" \
    "extensions" \
    "managers" \
    "obr" \
)

#-----------------------------------------------------------------------------------------                   
# Process parameters
#-----------------------------------------------------------------------------------------
pr_number=""
while [ "$1" != "" ]; do
    case $1 in
        -h | --help )       usage
                            exit
                            ;;

        --pr-number )       pr_number="$2"
                            shift
                            ;;

        * )                 error "Unexpected argument $1"
                            usage
                            exit 1
    esac
    shift
done

#-----------------------------------------------------------------------------------------                   
# Functions
#-----------------------------------------------------------------------------------------  

# # Set outputs to false as default value.
# echo "BUILDUTILS_CHANGED=false" >> $GITHUB_OUTPUT
# echo "WRAPPING_CHANGED=false" >> $GITHUB_OUTPUT
# echo "GRADLE_CHANGED=false" >> $GITHUB_OUTPUT
# echo "MAVEN_CHANGED=false" >> $GITHUB_OUTPUT
# echo "FRAMEWORK_CHANGED=false" >> $GITHUB_OUTPUT
# echo "EXTENSIONS_CHANGED=false" >> $GITHUB_OUTPUT
# echo "MANAGERS_CHANGED=false" >> $GITHUB_OUTPUT
# echo "OBR_CHANGED=false" >> $GITHUB_OUTPUT

# Temporary while testing - set to true as default.
echo "BUILDUTILS_CHANGED=true" >> $GITHUB_OUTPUT
echo "WRAPPING_CHANGED=true" >> $GITHUB_OUTPUT
echo "GRADLE_CHANGED=true" >> $GITHUB_OUTPUT
echo "MAVEN_CHANGED=true" >> $GITHUB_OUTPUT
echo "FRAMEWORK_CHANGED=true" >> $GITHUB_OUTPUT
echo "EXTENSIONS_CHANGED=true" >> $GITHUB_OUTPUT
echo "MANAGERS_CHANGED=true" >> $GITHUB_OUTPUT
echo "OBR_CHANGED=true" >> $GITHUB_OUTPUT
