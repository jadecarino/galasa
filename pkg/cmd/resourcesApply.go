/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package cmd

import (
	"log"

	"github.com/galasa-dev/cli/pkg/api"
	"github.com/galasa-dev/cli/pkg/resources"
	"github.com/galasa-dev/cli/pkg/utils"
	"github.com/spf13/cobra"
)

// Variables set by cobra's command-line parsing.
type ResourcesApplyCmdValues struct {
}

type ResourcesApplyCommand struct {
	values       *ResourcesApplyCmdValues
	cobraCommand *cobra.Command
}

// ------------------------------------------------------------------------------------------------
// Constructors methods
// ------------------------------------------------------------------------------------------------
func NewResourcesApplyCommand(factory utils.Factory, resourcesCommand utils.GalasaCommand, rootCommand utils.GalasaCommand) (utils.GalasaCommand, error) {

	cmd := new(ResourcesApplyCommand)
	err := cmd.init(factory, resourcesCommand, rootCommand)
	return cmd, err
}

// ------------------------------------------------------------------------------------------------
// Public methods
// ------------------------------------------------------------------------------------------------
func (cmd *ResourcesApplyCommand) Name() string {
	return COMMAND_NAME_RESOURCES_APPLY
}

func (cmd *ResourcesApplyCommand) CobraCommand() *cobra.Command {
	return cmd.cobraCommand
}

func (cmd *ResourcesApplyCommand) Values() interface{} {
	return cmd.values
}

// ------------------------------------------------------------------------------------------------
// Private methods
// ------------------------------------------------------------------------------------------------

func (cmd *ResourcesApplyCommand) init(factory utils.Factory, resourcesApplyCommand utils.GalasaCommand, rootCommand utils.GalasaCommand) error {

	var err error = nil

	cmd.values = &ResourcesApplyCmdValues{}
	cmd.cobraCommand = cmd.createCobraCommand(factory, resourcesApplyCommand, rootCommand.Values().(*RootCmdValues))

	return err
}

func (cmd *ResourcesApplyCommand) createCobraCommand(
	factory utils.Factory,
	resourcesCommand utils.GalasaCommand,
	rootCommandValues *RootCmdValues,
) *cobra.Command {

	resourcesApplyCommandValues := resourcesCommand.Values().(*ResourcesCmdValues)
	resourcesApplyCmd := &cobra.Command{
		Use:     "apply",
		Short:   "Apply file contents to the ecosystem.",
		Long:    "Create or Update resources from a given file in the Galasa Ecosystem",
		Args:    cobra.NoArgs,
		Aliases: []string{"resources apply"},
		RunE: func(cmd *cobra.Command, args []string) error {
			return executeResourcesApply(factory,
				resourcesApplyCommandValues, rootCommandValues)
		},
	}

	resourcesCommand.CobraCommand().AddCommand(resourcesApplyCmd)

	return resourcesApplyCmd
}

func executeResourcesApply(factory utils.Factory,
	resourcesCmdValues *ResourcesCmdValues,
	rootCmdValues *RootCmdValues,
) error {
	action := "apply"

	err := loadAndPassDataIntoResourcesApi(action, factory, resourcesCmdValues, rootCmdValues)

	return err
}

func loadAndPassDataIntoResourcesApi(action string, factory utils.Factory, resourcesCmdValues *ResourcesCmdValues, rootCmdValues *RootCmdValues) error {
	var err error
	// Operations on the file system will all be relative to the current folder.
	fileSystem := factory.GetFileSystem()

	err = utils.CaptureLog(fileSystem, rootCmdValues.logFileName)

	if err == nil {
		rootCmdValues.isCapturingLogs = true

		log.Println("Galasa CLI -", action, "Resources Command")

		// Get the ability to query environment variables.
		env := factory.GetEnvironment()

		var galasaHome utils.GalasaHome
		galasaHome, err = utils.NewGalasaHome(fileSystem, env, rootCmdValues.CmdParamGalasaHomePath)

		if err == nil {
			// Read the bootstrap properties.
			var urlService *api.RealUrlResolutionService = new(api.RealUrlResolutionService)
			var bootstrapData *api.BootstrapData
			bootstrapData, err = api.LoadBootstrap(galasaHome, fileSystem, env, resourcesCmdValues.bootstrap, urlService)
			if err == nil {

				apiServerUrl := bootstrapData.ApiServerURL
				log.Printf("The API server is at '%s'\n", apiServerUrl)

				var bearerToken string
				authenticator := factory.GetAuthenticator(
					apiServerUrl,
					galasaHome,
				)
				bearerToken, err = authenticator.GetBearerToken()

				if err == nil {
					err = resources.ApplyResources(
						action,
						resourcesCmdValues.filePath,
						fileSystem,
						apiServerUrl,
						bearerToken,
					)
				}
			}

		}

	}

	return err
}
