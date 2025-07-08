/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.api.ras.internal.routes;

import static dev.galasa.framework.api.common.ServletErrorMessage.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import dev.galasa.framework.api.ras.internal.common.RunActionJson;
import dev.galasa.framework.api.ras.internal.common.RunActionStatus;
import dev.galasa.framework.api.ras.internal.common.RunResultUtility;
import dev.galasa.framework.api.common.HttpRequestContext;
import dev.galasa.framework.api.common.InternalServletException;
import dev.galasa.framework.api.common.QueryParameters;
import dev.galasa.framework.api.common.ResponseBuilder;
import dev.galasa.framework.api.common.RunStatusUpdate;
import dev.galasa.framework.api.common.ServletError;
import dev.galasa.api.ras.RasRunResult;
import dev.galasa.framework.spi.DynamicStatusStoreException;
import dev.galasa.framework.spi.FrameworkException;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.IRunResult;
import dev.galasa.framework.spi.ResultArchiveStoreException;
import dev.galasa.framework.spi.rbac.BuiltInAction;
import dev.galasa.framework.spi.rbac.RBACException;
import dev.galasa.framework.spi.utils.GalasaGson;

/*
 * Implementation to return details for a given run based on its runId.
 */
public class RunDetailsRoute extends RunsRoute {

   private IFramework framework;

   static final GalasaGson gson = new GalasaGson();

   protected static final String path = "\\/runs\\/([A-Za-z0-9.\\-=]+)\\/?";

   public RunDetailsRoute(ResponseBuilder responseBuilder, IFramework framework) throws RBACException {
      //  Regex to match endpoint: /ras/runs/{runid}
      super(responseBuilder, path, framework);
      this.framework = framework;
   }

   @Override
   public HttpServletResponse handleGetRequest(String pathInfo, QueryParameters queryParams, HttpRequestContext requestContext, HttpServletResponse res) throws ServletException, IOException, FrameworkException {
      HttpServletRequest request = requestContext.getRequest();
      String runId = getRunIdFromPath(pathInfo);
      try{
         RasRunResult run = getRunFromFramework(runId);
         String outputString = gson.toJson(run);
         return getResponseBuilder().buildResponse(request, res, "application/json", outputString, HttpServletResponse.SC_OK );
      }catch(ResultArchiveStoreException ex){
         ServletError error = new ServletError(GAL5002_INVALID_RUN_ID,runId);
         throw new InternalServletException(error, HttpServletResponse.SC_NOT_FOUND, ex);
      }
   }

   @Override
   public HttpServletResponse handlePutRequest(String pathInfo, HttpRequestContext requestContext, HttpServletResponse response) throws DynamicStatusStoreException, FrameworkException, IOException {
      HttpServletRequest request = requestContext.getRequest();
      String runId = getRunIdFromPath(pathInfo);
      String runName = getRunNameFromRunId(runId);

      RunStatusUpdate runStatusUpdate = new RunStatusUpdate(framework);
      RunActionJson runAction = getUpdatedRunActionFromRequestBody(request);
      
      return getResponseBuilder().buildResponse(request, response, "text/plain", updateRunStatus(runName, runAction, runStatusUpdate), HttpServletResponse.SC_ACCEPTED);
   } 


   @Override
   public HttpServletResponse handleDeleteRequest(String pathInfo, HttpRequestContext requestContext, HttpServletResponse response ) throws ServletException, IOException, FrameworkException {
      HttpServletRequest request = requestContext.getRequest();
      String runId = getRunIdFromPath(pathInfo);
      IRunResult run = getRunByRunId(runId);

      String runRequestor = run.getTestStructure().getRequestor();
      String requestUsername = requestContext.getUsername();
      
      // Check if the user sending this request is allowed to delete runs submitted by other users
      if (!runRequestor.equals(requestUsername) && !isActionPermitted(BuiltInAction.RUNS_DELETE_OTHER_USERS, requestUsername)) {
         ServletError error = new ServletError(GAL5125_ACTION_NOT_PERMITTED, BuiltInAction.RUNS_DELETE_OTHER_USERS.getAction().getId());
         throw new InternalServletException(error, HttpServletResponse.SC_FORBIDDEN);
      }
      
      run.discard();

      response = getResponseBuilder().buildResponse(request, response, HttpServletResponse.SC_NO_CONTENT);
      return response;
   } 

   private String updateRunStatus(String runName, RunActionJson runAction, RunStatusUpdate runStatusUpdate) throws InternalServletException, ResultArchiveStoreException {
      String responseBody = "";
      RunActionStatus status = RunActionStatus.getfromString(runAction.getStatus());
      String result = runAction.getResult();
      
      if (status == null) {
         ServletError error = new ServletError(GAL5045_INVALID_STATUS_UPDATE_REQUEST, runAction.getStatus());
         throw new InternalServletException(error, HttpServletResponse.SC_BAD_REQUEST);
      } else if (status == RunActionStatus.QUEUED) {
         runStatusUpdate.resetRun(runName);
         logger.info("Run reset by external source.");
         responseBody = String.format("The request to reset run %s has been received.", runName);
      } else if (status == RunActionStatus.FINISHED) {
         runStatusUpdate.cancelRun(runName, result);
         logger.info("Run cancelled by external source.");
         responseBody = String.format("The request to cancel run %s has been received.", runName);
      } 
      return responseBody;
   }

   private @NotNull RasRunResult getRunFromFramework(@NotNull String id) throws ResultArchiveStoreException, InternalServletException {
      IRunResult run = getRunByRunId(id);
      return RunResultUtility.toRunResult(run, false);
   }

   private String getRunIdFromPath(String pathInfo) throws InternalServletException {
      Matcher matcher = this.getPathRegex().matcher(pathInfo);
      matcher.matches();
      String runId = matcher.group(1);
      return runId;
   }

   /**
    * 
    * @param runId
    * @return The short run name of the run.
    * @throws ResultArchiveStoreException
    * @throws InternalServletException If the runID was not found.
    */
   private String getRunNameFromRunId(@NotNull String runId) throws ResultArchiveStoreException, InternalServletException {
      IRunResult run = getRunByRunId(runId);
      String runName = run.getTestStructure().getRunName();
      return runName;
   }
   
   private RunActionJson getUpdatedRunActionFromRequestBody(HttpServletRequest request) throws IOException {
      ServletInputStream body = request.getInputStream();
      String jsonString = new String(body.readAllBytes(), StandardCharsets.UTF_8);
      body.close();
      RunActionJson runAction = gson.fromJson(jsonString, RunActionJson.class);
      return runAction;
   }

}