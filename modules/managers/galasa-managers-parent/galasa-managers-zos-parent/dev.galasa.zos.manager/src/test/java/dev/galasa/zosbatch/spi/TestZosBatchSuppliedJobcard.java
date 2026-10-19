/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.zosbatch.spi;

import org.junit.Assert;
import org.junit.Test;

import dev.galasa.zosbatch.ZosBatchException;

public class TestZosBatchSuppliedJobcard {

    private static final String STEP = "//STEP1    EXEC PGM=IEFBR14\n";

    private static final String SINGLE_LINE_CARD = "//MYJOB    JOB (ACCT),'PROG',CLASS=A\n";

    private static final String MULTI_LINE_CARD =
            "//MYJOB    JOB (ACCT),'PROG',\n" +
            "//         CLASS=A,\n" +
            "//         MSGCLASS=X\n";

    private static final String SYSAFF_LINE = "/*JOBPARM SYSAFF=MVSA\n";

    // ---- parse: success cases

    @Test
    public void testSingleLineCard() throws Exception {
        String jcl = SINGLE_LINE_CARD + STEP;
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(jcl);
        Assert.assertEquals("MYJOB", jobcard.getJobname());
        Assert.assertEquals(SINGLE_LINE_CARD.length(), jobcard.getEndOffset());
    }

    @Test
    public void testCardWithNoOperands() throws Exception {
        String jcl = "//MYJOB JOB\n" + STEP;
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(jcl);
        Assert.assertEquals("MYJOB", jobcard.getJobname());
        Assert.assertEquals("//MYJOB JOB\n".length(), jobcard.getEndOffset());
    }

    @Test
    public void testCardWithNoOperandsAndNoOtherRecords() throws Exception {
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse("//MYJOB JOB");
        Assert.assertEquals("MYJOB", jobcard.getJobname());
        Assert.assertEquals("//MYJOB JOB".length(), jobcard.getEndOffset());
    }

    @Test
    public void testMultiLineCard() throws Exception {
        String jcl = MULTI_LINE_CARD + STEP;
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(jcl);
        Assert.assertEquals("MYJOB", jobcard.getJobname());
        Assert.assertEquals(MULTI_LINE_CARD.length(), jobcard.getEndOffset());
    }

    @Test
    public void testMultiLineCardWithTrailingComments() throws Exception {
        String card =
                "//MYJOB    JOB (ACCT),'PROG',     THIS IS A COMMENT\n" +
                "//         CLASS=A,               ANOTHER COMMENT\n" +
                "//         MSGCLASS=X             LAST ONE\n";
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(card + STEP);
        Assert.assertEquals(card.length(), jobcard.getEndOffset());
    }

    @Test
    public void testMultiLineCardWithCommentRecordInside() throws Exception {
        String card =
                "//MYJOB    JOB (ACCT),'PROG',\n" +
                "//* A comment between continuation records\n" +
                "//         CLASS=A\n";
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(card + STEP);
        Assert.assertEquals(card.length(), jobcard.getEndOffset());
    }

    @Test
    public void testCommaInsideQuotedStringDoesNotContinue() throws Exception {
        String card = "//MYJOB    JOB (ACCT),'SMITH, JOHN'\n";
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(card + STEP);
        Assert.assertEquals(card.length(), jobcard.getEndOffset());
    }

    @Test
    public void testBlankInsideQuotedStringDoesNotEndOperands() throws Exception {
        String card = "//MYJOB    JOB (ACCT),'SMITH JOHN',CLASS=A\n";
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(card + STEP);
        Assert.assertEquals(card.length(), jobcard.getEndOffset());
    }

    @Test
    public void testQuotedStringContinuedOverRecords() throws Exception {
        String card =
                "//MYJOB    JOB (ACCT),'A LONG PROGRAMMER NAME THAT RUNS TO THE END OF THE RECORD\n" +
                "//         AND CONTINUES',CLASS=A\n";
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(card + STEP);
        Assert.assertEquals(card.length(), jobcard.getEndOffset());
    }

    @Test
    public void testLeadingBlankAndCommentRecords() throws Exception {
        String prefix = "\n//* A comment\n   \n//* Another comment\n";
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(prefix + SINGLE_LINE_CARD + STEP);
        Assert.assertEquals("MYJOB", jobcard.getJobname());
        Assert.assertEquals(prefix.length() + SINGLE_LINE_CARD.length(), jobcard.getEndOffset());
    }

    @Test
    public void testLowerCaseCardIsUpperCasedName() throws Exception {
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse("//myjob job class=a\n" + STEP);
        Assert.assertEquals("MYJOB", jobcard.getJobname());
    }

    @Test
    public void testJobnameBoundaries() throws Exception {
        Assert.assertEquals("A", ZosBatchSuppliedJobcard.parse("//A JOB\n").getJobname());
        Assert.assertEquals("ABCDEFGH", ZosBatchSuppliedJobcard.parse("//ABCDEFGH JOB\n").getJobname());
        Assert.assertEquals("#$@A1", ZosBatchSuppliedJobcard.parse("//#$@A1 JOB\n").getJobname());
        Assert.assertEquals("@JOB", ZosBatchSuppliedJobcard.parse("//@JOB JOB\n").getJobname());
    }

    @Test
    public void testWindowsLineEndings() throws Exception {
        String card = "//MYJOB    JOB (ACCT),\r\n//         CLASS=A\r\n";
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(card + "//STEP1 EXEC PGM=IEFBR14\r\n");
        Assert.assertEquals("MYJOB", jobcard.getJobname());
        Assert.assertEquals(card.length(), jobcard.getEndOffset());
    }

    @Test
    public void testJobKeywordInLaterRecordIsIgnored() throws Exception {
        String jcl = SINGLE_LINE_CARD + "//OTHER    JOB CLASS=B\n" + STEP;
        ZosBatchSuppliedJobcard jobcard = ZosBatchSuppliedJobcard.parse(jcl);
        Assert.assertEquals("MYJOB", jobcard.getJobname());
        Assert.assertEquals(SINGLE_LINE_CARD.length(), jobcard.getEndOffset());
    }

    // ---- parse: failure cases

    @Test
    public void testNullJcl() {
        assertParseFails(null, "No JOB statement found");
    }

    @Test
    public void testEmptyJcl() {
        assertParseFails("", "No JOB statement found");
    }

    @Test
    public void testOnlyBlankAndCommentRecords() {
        assertParseFails("\n//* comment\n   \n", "No JOB statement found");
    }

    @Test
    public void testFirstRecordIsNotJob() {
        assertParseFails(STEP + SINGLE_LINE_CARD, "No valid JOB statement found");
    }

    @Test
    public void testJobnameTooLong() {
        assertParseFails("//ABCDEFGHI JOB\n" + STEP, "No valid JOB statement found");
    }

    @Test
    public void testJobnameStartsWithDigit() {
        assertParseFails("//1ABC JOB\n" + STEP, "No valid JOB statement found");
    }

    @Test
    public void testJobnameWithInvalidCharacter() {
        assertParseFails("//AB-C JOB\n" + STEP, "No valid JOB statement found");
    }

    @Test
    public void testMissingJobname() {
        assertParseFails("//         JOB CLASS=A\n" + STEP, "No valid JOB statement found");
    }

    @Test
    public void testJobKeywordMustBeSeparatedByBlank() {
        assertParseFails("//MYJOB JOBX CLASS=A\n" + STEP, "No valid JOB statement found");
    }

    @Test
    public void testNotAJclStatement() {
        assertParseFails("MYJOB JOB CLASS=A\n", "No valid JOB statement found");
    }

    @Test
    public void testContinuationRunsOffEndOfJcl() {
        assertParseFails("//MYJOB    JOB (ACCT),\n", "continued but the supplied JCL ends");
    }

    @Test
    public void testOnlyCommentsFollowContinuation() {
        assertParseFails("//MYJOB    JOB (ACCT),\n//* comment\n", "continued but the supplied JCL ends");
    }

    @Test
    public void testContinuationRecordDoesNotStartWithSlashes() {
        assertParseFails("//MYJOB    JOB (ACCT),\n" + "CLASS=A\n", "not a valid continuation record");
    }

    @Test
    public void testContinuationRecordHasNoBlankAfterSlashes() {
        assertParseFails("//MYJOB    JOB (ACCT),\n" + "//CLASS=A\n", "not a valid continuation record");
    }

    @Test
    public void testUnclosedQuoteRunsOffEndOfJcl() {
        assertParseFails("//MYJOB    JOB (ACCT),'PROGRAMMER\n", "continued but the supplied JCL ends");
    }

    // ---- build

    @Test
    public void testBuildWithoutSysaffIsUnchanged() throws Exception {
        String jcl = MULTI_LINE_CARD + STEP;
        Assert.assertEquals(jcl, ZosBatchSuppliedJobcard.parse(jcl).build(null));
    }

    @Test
    public void testBuildAddsSysaffAfterSingleLineCard() throws Exception {
        String result = ZosBatchSuppliedJobcard.parse(SINGLE_LINE_CARD + STEP).build("MVSA");
        Assert.assertEquals(SINGLE_LINE_CARD + SYSAFF_LINE + STEP, result);
    }

    @Test
    public void testBuildAddsSysaffAfterLastRecordOfMultiLineCard() throws Exception {
        String result = ZosBatchSuppliedJobcard.parse(MULTI_LINE_CARD + STEP).build("MVSA");
        Assert.assertEquals(MULTI_LINE_CARD + SYSAFF_LINE + STEP, result);
    }

    @Test
    public void testBuildAddsSysaffAfterLeadingComments() throws Exception {
        String prefix = "//* a comment\n";
        String result = ZosBatchSuppliedJobcard.parse(prefix + MULTI_LINE_CARD + STEP).build("MVSA");
        Assert.assertEquals(prefix + MULTI_LINE_CARD + SYSAFF_LINE + STEP, result);
    }

    @Test
    public void testBuildDoesNotAddSysaffWhenAlreadyPresent() throws Exception {
        String jcl = MULTI_LINE_CARD + "/*JOBPARM SYSAFF=MVSB\n" + STEP;
        Assert.assertEquals(jcl, ZosBatchSuppliedJobcard.parse(jcl).build("MVSA"));
    }

    @Test
    public void testBuildDoesNotAddSysaffWhenAlreadyPresentAndSysaffOff() throws Exception {
        String jcl = MULTI_LINE_CARD + "/*JOBPARM SYSAFF=MVSB\n" + STEP;
        Assert.assertEquals(jcl, ZosBatchSuppliedJobcard.parse(jcl).build(null));
    }

    @Test
    public void testBuildDoesNotAddSysaffWhenAlreadyPresentWithOtherJobparmKeywords() throws Exception {
        String jcl = SINGLE_LINE_CARD + "/*jobparm L=10,sysaff=MVSB\n" + STEP;
        Assert.assertEquals(jcl, ZosBatchSuppliedJobcard.parse(jcl).build("MVSA"));
    }

    @Test
    public void testBuildAddsSysaffWhenJobparmWithoutSysaffIsPresent() throws Exception {
        String jcl = SINGLE_LINE_CARD + "/*JOBPARM L=10\n" + STEP;
        Assert.assertEquals(SINGLE_LINE_CARD + SYSAFF_LINE + "/*JOBPARM L=10\n" + STEP, ZosBatchSuppliedJobcard.parse(jcl).build("MVSA"));
    }

    @Test
    public void testBuildIgnoresSysaffInOrdinaryRecord() throws Exception {
        String data = "SYSAFF=MVSB is in the data\n";
        String result = ZosBatchSuppliedJobcard.parse(SINGLE_LINE_CARD + STEP + data).build("MVSA");
        Assert.assertEquals(SINGLE_LINE_CARD + SYSAFF_LINE + STEP + data, result);
    }

    @Test
    public void testBuildAddsTrailingNewline() throws Exception {
        Assert.assertEquals(SINGLE_LINE_CARD + STEP, ZosBatchSuppliedJobcard.parse(SINGLE_LINE_CARD + "//STEP1    EXEC PGM=IEFBR14").build(null));
    }

    @Test
    public void testBuildCardIsLastRecordWithNoNewline() throws Exception {
        Assert.assertEquals("//MYJOB JOB\n" + SYSAFF_LINE, ZosBatchSuppliedJobcard.parse("//MYJOB JOB").build("MVSA"));
    }

    @Test
    public void testBuildPreservesWindowsLineEndings() throws Exception {
        String card = "//MYJOB    JOB (ACCT),\r\n//         CLASS=A\r\n";
        String step = "//STEP1 EXEC PGM=IEFBR14\r\n";
        Assert.assertEquals(card + "/*JOBPARM SYSAFF=MVSA\r\n" + step, ZosBatchSuppliedJobcard.parse(card + step).build("MVSA"));
    }

    @Test
    public void testBuildDoesNotAddGeneratedJobcard() throws Exception {
        String jcl = SINGLE_LINE_CARD + STEP;
        String result = ZosBatchSuppliedJobcard.parse(jcl).build("MVSA");
        Assert.assertTrue("Result should start with the supplied JOB statement", result.startsWith(SINGLE_LINE_CARD));
        Assert.assertEquals("There should be exactly one JOB statement", 1, result.split(" JOB ", -1).length - 1);
    }

    @Test
    public void testBuildWithInvalidJcl() {
        Assert.assertThrows(ZosBatchException.class, () -> ZosBatchSuppliedJobcard.parse(STEP).build("MVSA"));
        Assert.assertThrows(ZosBatchException.class, () -> ZosBatchSuppliedJobcard.parse(null).build("MVSA"));
    }

    private void assertParseFails(String jcl, String expectedMessageFragment) {
        ZosBatchException exception = Assert.assertThrows("expected exception should be thrown", ZosBatchException.class, () -> ZosBatchSuppliedJobcard.parse(jcl));
        Assert.assertTrue("exception message should contain '" + expectedMessageFragment + "' but was '" + exception.getMessage() + "'",
                exception.getMessage().contains(expectedMessageFragment));
    }
}
