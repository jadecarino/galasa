/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.zosbatch.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dev.galasa.zosbatch.ZosBatchException;

/**
 * Locates and parses the JOB statement in JCL that already contains its own JOB statement,
 * and builds the JCL to be submitted. Shared by the z/OS Batch Manager implementations
 * so that they behave identically.
 * <p>
 * The first record of the JCL that is not blank and not a comment (<code>//*</code>) must be the JOB statement,
 * <code>//jobname JOB ...</code>. The JOB statement may be continued over more than one record. 
 * A statement is continued when the operand field ends with a comma or a quoted string is not closed. 
 * Each continuation record must start with <code>//</code> followed by a blank. 
 * Comment records within a continued statement are considered part of the statement.
 */
public final class ZosBatchSuppliedJobcard {

    private static final Log logger = LogFactory.getLog(ZosBatchSuppliedJobcard.class);

    /*
     * Matches the first record of a JOB statement, for example "//MYJOB    JOB (ACCT),'PROG',":
     *   ^//                                 the record starts with //. A comment record, //*, does not match as * is not a valid name character
     *   ([A-Za-z#$@][A-Za-z0-9#$@]{0,7})    capture group 1 is the job name: 1 to 8 characters, the first of which must be
     *                                       a letter or one of # $ @, the rest may also be digits
     *   [ \t]+                              one or more blanks separating the name field from the operation field
     *   JOB                                 the JOB operation
     *   (?=[ \t]|$)                         followed by a blank or the end of the record, so that JOBX does not match
     * The match is case-insensitive and is not anchored to the end of the record, as the operands follow.
     */
    private static final Pattern JOB_STATEMENT = Pattern.compile("^//([A-Za-z#$@][A-Za-z0-9#$@]{0,7})[ \\t]+JOB(?=[ \\t]|$)", Pattern.CASE_INSENSITIVE);

    private static final String COMMENT_PREFIX = "//*";
    private static final String JOBPARM_PREFIX = "/*JOBPARM";
    private static final String SYSAFF_KEYWORD = "SYSAFF=";
    private static final String DEFAULT_EOL = "\n";

    private final String jcl;
    private final String jobname;
    private final int endOffset;
    private final String eol;

    private ZosBatchSuppliedJobcard(String jcl, String jobname, int endOffset, String eol) {
        this.jcl = jcl;
        this.jobname = jobname;
        this.endOffset = endOffset;
        this.eol = eol;
    }

    /**
     * @return the job name from the JOB statement, in upper case
     */
    public String getJobname() {
        return this.jobname;
    }

    /**
     * @return the offset into the parsed JCL immediately after the last record of the JOB statement, including its line terminator if it has one
     */
    public int getEndOffset() {
        return this.endOffset;
    }

    /**
     * Parse the JOB statement from the supplied JCL. The returned instance represents the supplied JCL, 
     * and {@link #build(String)} builds the JCL to be submitted from it.
     * 
     * @param jcl the JCL, which must start with a JOB statement
     * @return the parsed JOB statement
     * @throws ZosBatchException if a valid JOB statement is not found
     */
    public static ZosBatchSuppliedJobcard parse(String jcl) throws ZosBatchException {
        jcl = (jcl == null) ? "" : jcl;
        List<Record> records = splitRecords(jcl);

        int firstRecordIndex = 0;
        while (firstRecordIndex < records.size() && (records.get(firstRecordIndex).text.trim().isEmpty() || records.get(firstRecordIndex).text.startsWith(COMMENT_PREFIX))) {
            firstRecordIndex++;
        }
        if (firstRecordIndex == records.size()) {
            throw new ZosBatchException("No JOB statement found in the supplied JCL. The first record that is not blank or a comment must be of the form //jobname JOB ...");
        }

        String firstRecordText = records.get(firstRecordIndex).text;
        Matcher matcher = JOB_STATEMENT.matcher(firstRecordText);
        if (!matcher.find()) {
            // The record is not included in the message as a JOB statement can contain sensitive data, for example PASSWORD=
            throw new ZosBatchException("No valid JOB statement found in the supplied JCL. The first record that is not blank or a comment, at line " + (firstRecordIndex + 1)
                    + ", must be of the form //jobname JOB ... where jobname is 1 to 8 characters");
        }
        String jobname = matcher.group(1).toUpperCase(Locale.ROOT);

        OperandsEnd operandsEnd = scanOperands(firstRecordText.substring(matcher.end()), false);
        int lastRecordIndex = firstRecordIndex;
        while (operandsEnd != OperandsEnd.COMPLETE) {
            int nextRecordIndex = lastRecordIndex + 1;
            while (nextRecordIndex < records.size() && records.get(nextRecordIndex).text.startsWith(COMMENT_PREFIX)) {
                nextRecordIndex++;
            }
            if (nextRecordIndex >= records.size()) {
                throw new ZosBatchException("The JOB statement for job " + jobname + " is continued but the supplied JCL ends before the continuation record");
            }
            String nextRecordText = records.get(nextRecordIndex).text;
            if (!nextRecordText.startsWith("//") || (nextRecordText.length() > 2 && nextRecordText.charAt(2) != ' ')) {
                throw new ZosBatchException("The JOB statement for job " + jobname + " is continued but the record at line " + (nextRecordIndex + 1)
                        + " is not a valid continuation record. A continuation record must start with // followed by a blank");
            }
            operandsEnd = scanOperands(nextRecordText.substring(2), operandsEnd == OperandsEnd.INSIDE_QUOTE);
            lastRecordIndex = nextRecordIndex;
        }

        Record lastRecord = records.get(lastRecordIndex);
        return new ZosBatchSuppliedJobcard(jcl, jobname, lastRecord.end, lastRecord.eol.isEmpty() ? DEFAULT_EOL : lastRecord.eol);
    }

    /**
     * Build the JCL to be submitted from the JCL that this instance was parsed from. The JCL is unchanged except that:
     * <ul>
     * <li>if a SYSAFF image is provided and the JCL does not already contain a <code>/*JOBPARM</code> record with 
     * <code>SYSAFF=</code>, a <code>/*JOBPARM SYSAFF=[image]</code> record is inserted immediately after the last record of the JOB statement</li>
     * <li>a line terminator is added to the end if there is not one</li>
     * </ul>
     * 
     * @param sysaffImageId the image ID to use for SYSAFF, or null to not add a SYSAFF record
     * @return the JCL to be submitted
     */
    public String build(String sysaffImageId) {
        StringBuilder result = new StringBuilder();
        
        boolean sysaffSupplied = sysaffImageId != null && containsSysaff(jcl.substring(endOffset));
        if (sysaffSupplied) {
            logger.info("The supplied JCL for job " + jobname + " already specifies SYSAFF=, so /*JOBPARM SYSAFF=" + sysaffImageId + " has not been added");
        }
        if (sysaffImageId != null && !sysaffSupplied) {
            result.append(jcl, 0, endOffset);
            if (!endsWithNewline(result)) {
                result.append(eol);
            }
            result.append(JOBPARM_PREFIX).append(" ").append(SYSAFF_KEYWORD).append(sysaffImageId).append(eol);
            result.append(jcl, endOffset, jcl.length());
        } else {
            result.append(jcl);
        }
        
        if (!endsWithNewline(result)) {
            result.append(eol);
        }
        return result.toString();
    }

    /**
     * @param jcl the JCL to search
     * @return true if the JCL contains a <code>/*JOBPARM</code> record with <code>SYSAFF=</code>
     */
    static boolean containsSysaff(String jcl) {
        for (Record record : splitRecords(jcl)) {
            String text = record.text.toUpperCase(Locale.ROOT);
            if (text.startsWith(JOBPARM_PREFIX) && text.contains(SYSAFF_KEYWORD)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scan the operand field of a record (the text after the JOB keyword or after the // of a continuation record). 
     * The field ends at the first blank that is not within a quoted string.
     * 
     * @param text the text of the record after the JOB keyword or after the // of a continuation record
     * @param startsInsideQuote true if the previous record ended inside a quoted string
     * @return how the operand field ended, which determines whether the statement is continued on the next record
     */
    private static OperandsEnd scanOperands(String text, boolean startsInsideQuote) {
        boolean inQuote = startsInsideQuote;
        int length = text.length();
        int i = 0;
        while (i < length && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        char lastChar = 0;
        for (; i < length; i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                inQuote = !inQuote;
            } else if (!inQuote && (c == ' ' || c == '\t')) {
                break;
            }
            lastChar = c;
        }
        if (inQuote) {
            return OperandsEnd.INSIDE_QUOTE;
        }
        return (lastChar == ',') ? OperandsEnd.AFTER_COMMA : OperandsEnd.COMPLETE;
    }

    private static boolean endsWithNewline(CharSequence text) {
        return text.length() > 0 && text.charAt(text.length() - 1) == '\n';
    }

    private static List<Record> splitRecords(String jcl) {
        List<Record> records = new ArrayList<>();
        int length = jcl.length();
        int position = 0;
        while (position < length) {
            int newline = jcl.indexOf('\n', position);
            int next = (newline < 0) ? length : newline + 1;
            int contentEnd = (newline < 0) ? length : newline;
            if (contentEnd > position && jcl.charAt(contentEnd - 1) == '\r') {
                contentEnd--;
            }
            records.add(new Record(jcl.substring(position, contentEnd), jcl.substring(contentEnd, next), next));
            position = next;
        }
        return records;
    }

    /**
     * How the operand field of a record of the JOB statement ended
     */
    private enum OperandsEnd {
        /** The operand field is complete, so the statement ends with this record */
        COMPLETE,
        /** The operand field ends with a comma, so the statement continues on the next record */
        AFTER_COMMA,
        /** A quoted string has not been closed, so the statement continues on the next record */
        INSIDE_QUOTE
    }

    private static final class Record {
        private final String text;
        private final String eol;
        private final int end;

        private Record(String text, String eol, int end) {
            this.text = text;
            this.eol = eol;
            this.end = end;
        }
    }
}
