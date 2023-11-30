/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cmd

import (
	"github.com/galasa-dev/cli/pkg/embedded"
	"github.com/spf13/cobra"
)

type RootCmdValues struct {
	// The file to which logs are being directed, if any. "" if not.
	logFileName string

	// We don't trace anything until this flag is true.
	// This means that any errors which occur in the cobra framework are not
	// followed by stack traces all the time.
	isCapturingLogs bool

	// The path to GALASA_HOME. Over-rides the environment variable.
	CmdParamGalasaHomePath string
}

type RootCommand struct {
	values       *RootCmdValues
	cobraCommand *cobra.Command
}

// -------------------------------------------------------------------------------
// Constructor
// -------------------------------------------------------------------------------
func NewRootCommand(factory Factory) (*RootCommand, error) {
	cmd := new(RootCommand)

	err := cmd.init(factory)

	return cmd, err
}

// -------------------------------------------------------------------------------
// Public methods
// -------------------------------------------------------------------------------

func (cmd *RootCommand) GetName() string {
	return COMMAND_NAME_ROOT
}

func (cmd *RootCommand) GetCobraCommand() *cobra.Command {
	return cmd.cobraCommand
}

func (cmd *RootCommand) GetValues() interface{} {
	return cmd.values
}

//-------------------------------------------------------------------------------
// Private methods
//-------------------------------------------------------------------------------

func (cmd *RootCommand) init(factory Factory) error {

	var err error = nil

	// Flags parsed by this command put values into this instance of the structure.
	cmd.values = &RootCmdValues{
		isCapturingLogs: false,
	}

	cmd.cobraCommand, err = cmd.newRootCobraCommand(factory, cmd.values)

	return err
}

func (cmd *RootCommand) newRootCobraCommand(factory Factory, rootCmdValues *RootCmdValues) (*cobra.Command, error) {
	version, err := embedded.GetGalasaCtlVersion()
	var rootCmd *cobra.Command
	if err == nil {

		rootCmd = &cobra.Command{
			Use:     "galasactl",
			Short:   "CLI for Galasa",
			Long:    `A tool for controlling Galasa resources using the command-line.`,
			Version: version,
		}

		rootCmd.SetErr(factory.GetStdErrConsole())
		rootCmd.SetOut(factory.GetStdOutConsole())

		var galasaCtlVersion string
		galasaCtlVersion, err = embedded.GetGalasaCtlVersion()
		if err == nil {

			rootCmd.Version = galasaCtlVersion

			rootCmd.PersistentFlags().StringVarP(&(rootCmdValues.logFileName), "log", "l", "",
				"File to which log information will be sent. Any folder referred to must exist. "+
					"An existing file will be overwritten. "+
					"Specify \"-\" to log to stderr. "+
					"Defaults to not logging.")

			rootCmd.SetHelpCommand(&cobra.Command{Hidden: true})

			rootCmd.PersistentFlags().StringVarP(&(rootCmdValues.CmdParamGalasaHomePath), "galasahome", "", "",
				"Path to a folder where Galasa will read and write files and configuration settings. "+
					"The default is '${HOME}/.galasa'. "+
					"This overrides the GALASA_HOME environment variable which may be set instead.",
			)

		}
	}
	return rootCmd, err
}
