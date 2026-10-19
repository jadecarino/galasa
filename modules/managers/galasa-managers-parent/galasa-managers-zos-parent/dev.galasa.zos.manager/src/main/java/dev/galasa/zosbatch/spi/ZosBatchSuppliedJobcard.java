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

    private static final Pattern JOB_STATEMENT = Pattern.compile("^//([A-Za-z#$@][A-Za-z0-9#$@]{0,7})[ \\t]+JOB(?=[ \\t]|$)", Pattern.CASE_INSENSITIVE);

    private static final String COMMENT_PREFIX = "//*";
    private static final String JOBPARM_PREFIX = "/*JOBPARM";
    private static final String SYSAFF_KEYWORD = "SYSAFF=";
    private static final String DEFAULT_EOL = "\n";

    private static final int MAX_RECORD_LENGTH_IN_MESSAGE = 80;

    private final String jobname;
    private final int endOffset;
    private final String eol;

    private ZosBatchSuppliedJobcard(String jobname, int endOffset, String eol) {
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
     * Parse the JOB statement from the supplied JCL
     * 
     * @param jcl the JCL, which must start with a JOB statement
     * @return the parsed JOB statement
     * @throws ZosBatchException if a valid JOB statement is not found
     */
    public static ZosBatchSuppliedJobcard parse(String jcl) throws ZosBatchException {
        List<Record> records = splitRecords(jcl == null ? "" : jcl);

        int first = 0;
        while (first < records.size() && (records.get(first).text.trim().isEmpty() || records.get(first).text.startsWith(COMMENT_PREFIX))) {
            first++;
        }
        if (first == records.size()) {
            throw new ZosBatchException("No JOB statement found in the supplied JCL. The first record that is not blank or a comment must be of the form //jobname JOB ...");
        }

        String firstText = records.get(first).text;
        Matcher matcher = JOB_STATEMENT.matcher(firstText);
        if (!matcher.find()) {
            throw new ZosBatchException("No valid JOB statement found in the supplied JCL. The first record that is not blank or a comment must be of the form //jobname JOB ... "
                    + "where jobname is 1 to 8 characters. Found: " + abbreviate(firstText));
        }
        String jobname = matcher.group(1).toUpperCase(Locale.ROOT);

        QuoteState quoteState = new QuoteState();
        boolean continued = scanOperands(firstText.substring(matcher.end()), quoteState);
        int last = first;
        while (continued) {
            int next = last + 1;
            while (next < records.size() && records.get(next).text.startsWith(COMMENT_PREFIX)) {
                next++;
            }
            if (next >= records.size()) {
                throw new ZosBatchException("The JOB statement for job " + jobname + " is continued but the supplied JCL ends before the continuation record");
            }
            String text = records.get(next).text;
            if (!text.startsWith("//") || (text.length() > 2 && text.charAt(2) != ' ')) {
                throw new ZosBatchException("The JOB statement for job " + jobname + " is continued but the next record is not a valid continuation record. Found: " + abbreviate(text));
            }
            continued = scanOperands(text.substring(2), quoteState);
            last = next;
        }

        Record lastRecord = records.get(last);
        return new ZosBatchSuppliedJobcard(jobname, lastRecord.end, lastRecord.eol.isEmpty() ? DEFAULT_EOL : lastRecord.eol);
    }

    /**
     * Build the JCL to be submitted from JCL that contains its own JOB statement. The JCL is unchanged except that:
     * <ul>
     * <li>if a SYSAFF image is provided and the JCL does not already contain a <code>/*JOBPARM</code> record with 
     * <code>SYSAFF=</code>, a <code>/*JOBPARM SYSAFF=[image]</code> record is inserted immediately after the last record of the JOB statement</li>
     * <li>a line terminator is added to the end if there is not one</li>
     * </ul>
     * 
     * @param jcl the JCL, which must start with a JOB statement
     * @param sysaffImageId the image ID to use for SYSAFF, or null to not add a SYSAFF record
     * @return the JCL to be submitted
     * @throws ZosBatchException if a valid JOB statement is not found
     */
    public static String build(String jcl, String sysaffImageId) throws ZosBatchException {
        ZosBatchSuppliedJobcard jobcard = parse(jcl);
        StringBuilder result = new StringBuilder();
        
        boolean sysaffSupplied = sysaffImageId != null && containsSysaff(jcl.substring(jobcard.endOffset));
        if (sysaffSupplied) {
            logger.info("The supplied JCL for job " + jobcard.jobname + " already specifies SYSAFF=, so /*JOBPARM SYSAFF=" + sysaffImageId + " has not been added");
        }
        if (sysaffImageId != null && !sysaffSupplied) {
            result.append(jcl, 0, jobcard.endOffset);
            if (!endsWithNewline(result)) {
                result.append(jobcard.eol);
            }
            result.append(JOBPARM_PREFIX).append(" ").append(SYSAFF_KEYWORD).append(sysaffImageId).append(jobcard.eol);
            result.append(jcl, jobcard.endOffset, jcl.length());
        } else {
            result.append(jcl);
        }
        
        if (!endsWithNewline(result)) {
            result.append(jobcard.eol);
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
     * @return true if the statement is continued on the next record
     */
    private static boolean scanOperands(String text, QuoteState quoteState) {
        int length = text.length();
        int i = 0;
        while (i < length && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        char lastChar = 0;
        for (; i < length; i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                quoteState.inQuote = !quoteState.inQuote;
            } else if (!quoteState.inQuote && (c == ' ' || c == '\t')) {
                break;
            }
            lastChar = c;
        }
        return quoteState.inQuote || lastChar == ',';
    }

    private static boolean endsWithNewline(CharSequence text) {
        return text.length() > 0 && text.charAt(text.length() - 1) == '\n';
    }

    private static String abbreviate(String text) {
        if (text.length() > MAX_RECORD_LENGTH_IN_MESSAGE) {
            return text.substring(0, MAX_RECORD_LENGTH_IN_MESSAGE) + "...";
        }
        return text;
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

    private static final class QuoteState {
        private boolean inQuote;
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
