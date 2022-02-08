/*
 * Copyright contributors to the Galasa project
 */

package dev.galasa.cicsts.ceci.manager.ivt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import dev.galasa.After;
import dev.galasa.AfterClass;
import dev.galasa.BeforeClass;
import dev.galasa.Test;
import dev.galasa.core.manager.IResourceString;
import dev.galasa.core.manager.ResourceString;
import dev.galasa.core.manager.Logger;
import dev.galasa.core.manager.TestProperty;
import dev.galasa.cicsts.CeciException;
import dev.galasa.cicsts.CemtException;
import dev.galasa.cicsts.CicsRegion;
import dev.galasa.cicsts.CicsTerminal;
import dev.galasa.cicsts.CicstsManagerException;
import dev.galasa.cicsts.ICeciResponse;
import dev.galasa.cicsts.ICicsRegion;
import dev.galasa.cicsts.ICicsTerminal;
import dev.galasa.cicsts.IExecInterfaceBlock;
import dev.galasa.zos3270.FieldNotFoundException;
import dev.galasa.zos3270.KeyboardLockedException;
import dev.galasa.zos3270.TerminalInterruptedException;
import dev.galasa.zos3270.TimeoutException;
import dev.galasa.zos3270.spi.NetworkException;
import dev.galasa.zosbatch.ZosBatchException;
import dev.galasa.zosbatch.IZosBatch;
import dev.galasa.zosbatch.IZosBatchJob;
import dev.galasa.zosbatch.ZosBatch;

import org.apache.commons.logging.Log;

@Test
public class CECIManagerIVT {

   @Logger
   public Log logger;

   @CicsRegion
   public ICicsRegion cics;

   @CicsTerminal
   public ICicsTerminal ceciTerminal;
   
   @CicsTerminal
   public ICicsTerminal otherTerminal;

   @ZosBatch(imageTag = "PRIMARY")
   public IZosBatch batch;
   
   @TestProperty(prefix = "IVT.RESOURCE.STRING", suffix = "VARNAME", required = false)
   public String providedResourceString1;
   @ResourceString(tag = "VARNAME", length = 8)
   public IResourceString resourceString1;
   
   public String variableName;
   
   @TestProperty(prefix = "IVT.RESOURCE.STRING", suffix = "QUEUENAME", required = false)
   public String providedResourceString2;
   @ResourceString(tag = "QUEUENAME", length = 8)
   public IResourceString resourceString2;
   
   public String queueName;
   
   @BeforeClass
   public void setUp() throws CeciException, CicstsManagerException {
	   cics.ceci().startCECISession(ceciTerminal);
	   
	   // Get and set unique resource strings
	   if (providedResourceString1 != null) {
	      variableName = providedResourceString1;
	   } else {
	      variableName = resourceString1.getString();
	   }
	   logger.info("Unique CECI variable name to be used in the tests: " + variableName);
	   
	   if (providedResourceString2 != null) {
	      queueName = providedResourceString2;
	   } else {
	      queueName = resourceString2.getString();
	   }
	   logger.info("Unique TSQUEUE name to be used in the tests: " + queueName);
   }

   /**
    * Ensures that we have an instance of CECI, CICS and a Terminal.
    * 
    * @throws CicstsManagerException 
    */
   @Test
   public void testNotNull() throws CicstsManagerException  {
      assertThat(logger).isNotNull();
      assertThat(cics).isNotNull();
      assertThat(cics.ceci()).isNotNull();
      assertThat(ceciTerminal).isNotNull();
      assertThat(otherTerminal).isNotNull();
      assertThat(batch).isNotNull();
   } 
   
   /**
    * Starts the CECI session.
    * 
    * @throws FieldNotFoundException 
    * @throws NetworkException 
    * @throws TerminalInterruptedException 
    * @throws KeyboardLockedException 
    * @throws TimeoutException 
    */
   @Test
   public void testStartCeciSession() throws TimeoutException, KeyboardLockedException, TerminalInterruptedException, NetworkException, FieldNotFoundException {
	   logger.info("If the CECI session started successfully, the terminal should be able to process a CECI command");
	   ceciTerminal.type("DEF").enter().waitForKeyboard();
	   assertThat(ceciTerminal.retrieveScreen().contains("ENTER ONE OF THE FOLLOWING")).isTrue();
   }
   
   /**
    * Defines a variable, then retrieves the variable and tests that the stored text is correct.
    * Then attempts to delete the variable and tests that it is deleted.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException 
    */
  @Test 
   public void testDefineRetrieveDeleteTextVariable() throws CeciException, CicstsManagerException {
	  logger.info("Defining, retrieving then deleting the variable " + variableName + " using CECI");
      String variableValue = "THIS IS A TEXT STRING";
      
      cics.ceci().defineVariableText(ceciTerminal, variableName, variableValue);
      assertThat(cics.ceci().retrieveVariableText(ceciTerminal, variableName)).isEqualTo(variableValue);

      cics.ceci().deleteVariable(ceciTerminal, variableName);
      assertThatThrownBy(() -> {
    	  cics.ceci().retrieveVariableText(ceciTerminal, variableName);
	  }).isInstanceOf(CeciException.class).hasMessageContaining("Unable to find variable &" + variableName);
   }
   
   /**
    * Tests that variables defined with a name longer than 10 characters are caught.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException 
    */
  @Test
   public void testLongVariableName() throws CeciException, CicstsManagerException  {
	  logger.info("Testing that defining a variable using CECI with a name longer than 10 characters throws an exception");
      String tenCharacterName = "ABCDEFGHIJ";
      String variableValue = "THIS IS A TEXT STRING";
      
      assertThatThrownBy(() -> {
    	  cics.ceci().defineVariableText(ceciTerminal, tenCharacterName, variableValue);
	  }).isInstanceOf(CeciException.class).hasMessageContaining("CECI variable name \"&ABCDEFGHIJ\" greater than maximum length of 10 characters including the leading \"&\"");
   }

   /**
    * Defines two variables of the same name and checks that the text is overwritten with the second definition.
    * 
    * @throws CeciException 
    * @throws CicstsManagerException 
    */
  @Test
   public void testDefineTwoVariablesWithSameName() throws CeciException, CicstsManagerException {
	  logger.info("Defining two variables with the name " + variableName + " using CECI. The value should be overwritten with the second definition");
      String value1 = "A VALUE";
      String value2 = "A LONGER VALUE";
      int length = 0;
      length = cics.ceci().defineVariableText(ceciTerminal, variableName, value1);
      assertThat(length).isEqualTo(value1.length());
      assertThat(cics.ceci().retrieveVariableText(ceciTerminal, variableName)).isEqualTo(value1);

      length = cics.ceci().defineVariableText(ceciTerminal, variableName, value2);
      assertThat(length).isEqualTo(value2.length());
      assertThat(cics.ceci().retrieveVariableText(ceciTerminal, variableName)).isEqualTo(value2);
   }

   /**
    * Tests defining a binary variable
    * 
    * @throws CeciException 
    * @throws CicstsManagerException 
    */
  @Test
   public void testBinaryDataTypeVariable() throws CeciException, CicstsManagerException {
	  logger.info("Defining a binary variable with the name " + variableName);
      String variableValue = "BinaryString";
      cics.ceci().defineVariableBinary(ceciTerminal, variableName, variableValue.toCharArray());
      String response = new String(cics.ceci().retrieveVariableBinary(ceciTerminal, variableName));
      assertThat(response).isEqualTo(variableValue);

      cics.ceci().deleteVariable(ceciTerminal, variableName);
      logger.info("Testing that when you define a binary data type variable longer than allowed that an exception is thrown");
      assertThatThrownBy(() -> {
    	  cics.ceci().defineVariableBinary(ceciTerminal, variableName, constructRandomString(32768).toCharArray());
	  }).isInstanceOf(CeciException.class).hasMessageContaining("CECI variable value length 32768 greater than maximum 32767");

   }

   /**
    * Tests defining a double data type variable
    * 
    * @throws CeciException 
    * @throws CicstsManagerException 
    */
  @Test
   public void testDoubleDataTypeVariable() throws CeciException, CicstsManagerException {
	  logger.info("Defining a double variable with the name " + variableName);
      long variableValue = 9223372036854775807L;
      cics.ceci().defineVariableDoubleWord(ceciTerminal, variableName, variableValue);
      long response = cics.ceci().retrieveVariableDoubleWord(ceciTerminal, variableName);
      assertThat(response).isEqualTo(variableValue);
    }

   /**
    * Tests defining a full data type variable
    * 
    * @throws CeciException 
    * @throws CicstsManagerException 
    */
  @Test
   public void testFullDataTypeVariable() throws CeciException, CicstsManagerException {
	  logger.info("Defining a full variable with the name " + variableName);
      int variableValue = Integer.MAX_VALUE;
      cics.ceci().defineVariableFullWord(ceciTerminal, variableName, variableValue);
      long response = cics.ceci().retrieveVariableFullWord(ceciTerminal, variableName);
      assertThat(response).isEqualTo(variableValue);
   }

   /**
    * Tests defining a half data type variable
    * 
    * @throws CeciException 
    * @throws CicstsManagerException 
    */
  @Test
   public void testHalfDataTypeVariable() throws CeciException, CicstsManagerException {
	  logger.info("Defining a half variable with the name " + variableName);
      int variableValue = 32767;
      cics.ceci().defineVariableHalfWord(ceciTerminal, variableName, variableValue);
      int response = cics.ceci().retrieveVariableHalfWord(ceciTerminal, variableName);
      assertThat(response).isEqualTo(variableValue);
   }
  
  /**
   * Tests defining a 4 byte packed data type variable.
   * 
   * @throws CicstsManagerException 
   * @throws CeciException 
   */
  @Test
  public void test4BytePackedVariable() throws CeciException, CicstsManagerException {
	  logger.info("Defining a 4-byte packed variable with the name " + variableName);
	  int variableValue = 9999999;
	  cics.ceci().defineVariable4BytePacked(ceciTerminal, variableName, variableValue);
	  int response = cics.ceci().retrieveVariable4BytePacked(ceciTerminal, variableName);
	  assertThat(response).isEqualTo(variableValue);
	 
	  cics.ceci().deleteVariable(ceciTerminal, variableName);
      logger.info("Testing that when you define a 4 byte packed data type variable longer than allowed that an exception is thrown");
      assertThatThrownBy(() -> {
    	  cics.ceci().defineVariable4BytePacked(ceciTerminal, variableName, 10000000);
	  }).isInstanceOf(CeciException.class).hasMessageContaining("CECI variable value length 9 greater than maximum of 8 for type \"P\"");
	  
  }
  
  /**
   * Tests defining an 8 byte packed data type variable.
   * 
   * @throws CicstsManagerException 
   * @throws CeciException 
   */
  @Test
  public void test8BytePackedVariable() throws CeciException, CicstsManagerException {
	  logger.info("Defining an 8-byte packed variable with the name " + variableName);
	  long variableValue = 999999999999999L;
	  cics.ceci().defineVariable8BytePacked(ceciTerminal, variableName, variableValue);
	  long response = cics.ceci().retrieveVariable8BytePacked(ceciTerminal, variableName);
	  assertThat(response).isEqualTo(variableValue);
	 
	  cics.ceci().deleteVariable(ceciTerminal, variableName);
      logger.info("Testing that when you define an 8 byte packed data type variable longer than allowed that an exception is thrown");
      assertThatThrownBy(() -> {
    	  cics.ceci().defineVariable8BytePacked(ceciTerminal, variableName, 1000000000000000L);
	  }).isInstanceOf(CeciException.class).hasMessageContaining("CECI variable value length 17 greater than maximum of 16 for type \"D\"");
	  
  }

   /**
    * Tests the execution of a basic CECI command.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException 
    */
   @Test
   public void testCommand() throws CeciException, CicstsManagerException  {
      String userVariable = variableName;
      String ceciCommand = "ASSIGN USERID(&" + userVariable + ")";
      cics.ceci().issueCommand(ceciTerminal, ceciCommand, false);
      String user = cics.ceci().retrieveVariableText(ceciTerminal, userVariable);
      assertThat(user).isEqualTo("CICSUSER");
   }

   /**
    * Tests the execution of a documentation CECI command.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException 
    * @throws ZosBatchException 
    */
   @Test
   public void testDocumentationCommand() throws CeciException, CicstsManagerException, ZosBatchException  {
	  String message = "GALASA TEST " + Instant.now().toString();
	  String ceciCommand = "EXEC CICS WRITE OPERATOR TEXT('" + message + "')";
	  ICeciResponse resp = cics.ceci().issueCommand(ceciTerminal, ceciCommand);
	  assertThat(resp.isNormal()).isTrue();

      logger.info("Checking that the message was written to the CICS log");
      boolean messageFound = false;
      List<IZosBatchJob> jobs = batch.getJobs(cics.getApplid(), "*");      
      for (IZosBatchJob job : jobs) {
         String output = job.getSpoolFile("JESMSGLG").getRecords();
         if (output.contains(message)) {
            messageFound = true;
            break;
         }
      }
      assertThat(messageFound).isTrue();
   }
   
   /**
    * Tests the execution of a CECI command with options.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException 
    */
   @Test
   public void testCommandWithOptions() throws CeciException, CicstsManagerException {
	   logger.info("Putting and getting data from a container into variable " + variableName);
	   
	   String ceciCommand = "PUT";
	   HashMap<String, Object> options = new HashMap<String, Object>();
	   options.put("CONTAINER", "FRED");
	   options.put("CHANNEL", "FRED");
	   options.put("FROM", "'HELLO'");
	   ICeciResponse resp = cics.ceci().issueCommand(ceciTerminal, ceciCommand, options);
	   assertThat(resp.isNormal()).isTrue();
	   
	   ceciCommand = "GET";
	   options.clear();
	   options.put("CONTAINER", "FRED");
	   options.put("CHANNEL", "FRED");
	   options.put("INTO", "&" + variableName);
	   resp = cics.ceci().issueCommand(ceciTerminal, ceciCommand, options);
	   assertThat(resp.isNormal()).isTrue();
	   assertThat(cics.ceci().retrieveVariableText(ceciTerminal, variableName)).isEqualTo("HELLO");
   }
   
   /**
    * Writes data to a Temporary Storage Queue, checks that it was written to the queue and then cleans up the queue. 
    * 
    * @throws CicstsManagerException 
    * @throws CeciException 
    * @throws NetworkException 
    * @throws TerminalInterruptedException 
    * @throws KeyboardLockedException 
    * @throws TimeoutException 
    * @throws FieldNotFoundException 
    */
  @Test
   public void testWriteToTSQ() throws CeciException, CicstsManagerException, TimeoutException, KeyboardLockedException, TerminalInterruptedException, NetworkException, FieldNotFoundException {
	  logger.info("Testing writing data to a Temporary Storage Queue called " + queueName + " from variable " + variableName);
      String dataToWrite = "THIS IS A GALASA TEST";
      
      cics.ceci().defineVariableText(ceciTerminal, variableName, dataToWrite);
      String command = "WRITEQ TS QUEUE('" + queueName + "') FROM(&" + variableName + ")";
      cics.ceci().issueCommand(ceciTerminal, command);
      
      otherTerminal.type("CEBR " + queueName).enter().waitForKeyboard();
      assertThat(otherTerminal.retrieveScreen()).containsIgnoringCase(dataToWrite);
      otherTerminal.pf3().waitForKeyboard();

      command = "DELETEQ TS QUEUE('" + queueName + "')";
      cics.ceci().issueCommand(ceciTerminal, command);

      otherTerminal.type("CEBR " + queueName).enter().waitForKeyboard();
      assertThat(otherTerminal.retrieveScreen().contains("DOES NOT EXIST")).isTrue();
      otherTerminal.resetAndClear();
   }

   /**
    * Test putting and getting data from container and linking a program to a channel.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException 
    * @throws FieldNotFoundException 
    * @throws NetworkException 
    * @throws TerminalInterruptedException 
    * @throws KeyboardLockedException 
    * @throws TimeoutException 
    */
   @Test
   public void testPutAndGetDataFromContainer() throws CeciException, CicstsManagerException, TimeoutException, KeyboardLockedException, TerminalInterruptedException, NetworkException, FieldNotFoundException  {
	  String programName = "CONTTEST";
	  String channelName = "MY-CHANNEL";
	  String containerName = "HOBBIT";
	  String content = "my-content";
		  
	  logger.info("Linking program " + programName + " to channel " + channelName + " then putting the output into variable " + variableName);
	  
	  // Retrieving the use count of Program to compare with use count after the test case
	  int programUseCountBefore = Integer.parseInt(cics.cemt().inquireResource(otherTerminal, "PROGRAM", programName).get("usecount"));
	  
	  otherTerminal.setUppercaseTranslation(false);
	  
	  ICeciResponse resp = cics.ceci().putContainer(ceciTerminal, channelName, containerName, content, null, null, null);
      resp.checkNormal();
	  
      resp = cics.ceci().linkProgramWithChannel(ceciTerminal, programName, channelName, null, null, false);
      resp.checkNormal();
	  
      resp = cics.ceci().getContainer(ceciTerminal, channelName, containerName, variableName, null, null);
      resp.checkNormal();
      assertThat(cics.ceci().retrieveVariableText(ceciTerminal, "&" + variableName)).isUpperCase();
      assertThat(cics.ceci().retrieveVariableText(ceciTerminal, "&" + variableName)).startsWith(content.toUpperCase());
      
      int programUseCountAfter = Integer.parseInt(cics.cemt().inquireResource(otherTerminal, "PROGRAM", programName).get("usecount"));
	  assertThat(programUseCountBefore < programUseCountAfter).isTrue();
   }

   /**
    * Test linking a program to a comm area.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException
    */
   @Test
   public void testLinkToProgramToCommarea() throws CeciException, CicstsManagerException  {
	   String programName = "APITEST";
      String variableValue = "galasa";
	   logger.info("Linking program " + programName + " to variable " + variableName);

      cics.ceci().defineVariableText(ceciTerminal, variableName, variableValue);
      ICeciResponse resp = cics.ceci().linkProgram(ceciTerminal, programName, "&" + variableName, null, null, false);
      assertThat(resp.getEIBRESP()).isZero();

      String outputData = cics.ceci().retrieveVariableText(ceciTerminal, "&" + variableName);
      assertThat(outputData).isUpperCase();
      assertThat(outputData).startsWith(variableValue.toUpperCase());
   } 
   
   /**
    * Tests that the EIB fields are retrieved correctly.
    * 
    * @throws CicstsManagerException 
    * @throws CeciException
    * @throws FieldNotFoundException 
    * @throws NetworkException 
    * @throws TerminalInterruptedException 
    * @throws KeyboardLockedException 
    * @throws TimeoutException 
    */
   @Test
   public void testGetEIBFields() throws CeciException, CicstsManagerException, TimeoutException, KeyboardLockedException, TerminalInterruptedException, NetworkException, FieldNotFoundException{
	   logger.info("Testing that fields from the Exec Interface Block are retrieved correctly, first testing on a good command");
	   
	   cics.ceci().issueCommand(ceciTerminal, "PUT CONTAINER(BOB) CHANNEL(BOB) FROM('HELLO')");
	   IExecInterfaceBlock eib = cics.ceci().getEIB(ceciTerminal);
	  
	   assertThat(eib.getResponse()).isEqualTo("NORMAL");
	   assertThat(eib.getEIBRESP()).isEqualTo(0);
	   assertThat(eib.getEIBRESP2()).isEqualTo(0);
	   
	   assertThat(eib.getEIBTRNID(false)).isEqualTo("CECI");
	  
	   int expectedEIBDate = getExpectedEIBDate();
	   assertThat(eib.getEIBDATE()).isEqualTo(expectedEIBDate);
	   
	   logger.info("Testing the EIB fields on a bad command - linking to a program that doesn't exist");
	   
	   cics.ceci().linkProgramWithChannel(ceciTerminal, "NOEXIST", "MY-CHANNEL", null, null, false);
	   eib = cics.ceci().getEIB(ceciTerminal);
	   
	   assertThat(eib.getResponse()).isEqualTo("PGMIDERR");
	   assertThat(eib.getEIBRESP()).isEqualTo(27);
	   assertThat(eib.getEIBRESP2()).isEqualTo(3);
   }
   
   private int getExpectedEIBDate() throws TimeoutException, KeyboardLockedException, TerminalInterruptedException, NetworkException, FieldNotFoundException, CicstsManagerException {
	   otherTerminal.clear().waitForKeyboard();
	   otherTerminal.type("CEMT INQUIRE SYSTEM").enter().waitForKeyboard();
	   String screen = otherTerminal.retrieveScreen();
	   if (!screen.contains("DATE:")) {
		   throw new CemtException("CEMT INQUIRE SYSTEM did not navigate to correct screen");
	   }
	   otherTerminal.resetAndClear();
	   String dateString = screen.substring(screen.lastIndexOf("DATE: ") + 6, screen.lastIndexOf("DATE: ") + 14);
	   DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
	   LocalDate date = LocalDate.parse(dateString, formatter);
	   // EIBDATE is in packed decimal format
	   String expectedEIBDate = (date.getYear() <= 1999 ? "0" : "1")
			   					+ Integer.toString(date.getYear()).substring(2)
			   					+ (date.getDayOfYear() <= 99 ? "0" + date.getDayOfYear() : date.getDayOfYear());
	   
	   return Integer.parseInt(expectedEIBDate);
   }

   private String constructRandomString(int length) {
      String alphabet = "abcdefghijklmnopqrstuvwxyz";
      StringBuilder sb = new StringBuilder();
      Random r = new Random();
      for (int a = 0; a < length; a++){
         sb.append(alphabet.charAt(r.nextInt(26)));
      }
      return sb.toString();
   }

   @After
   public void after() throws CeciException, CicstsManagerException {
	   cics.ceci().deleteVariable(ceciTerminal, variableName);
   }
   
   @AfterClass
      public void afterClass() throws CicstsManagerException {
      cics.ceci().deleteAllVariables(ceciTerminal);
   }
   
}
