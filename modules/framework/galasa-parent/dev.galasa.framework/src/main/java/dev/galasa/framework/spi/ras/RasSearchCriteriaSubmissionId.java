/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.spi.ras;

import javax.validation.constraints.NotNull;

import dev.galasa.framework.spi.teststructure.TestStructure;

public class RasSearchCriteriaSubmissionId implements IRasSearchCriteria {

	private static final String CRITERIA_NAME = "submissionId";
	private final String[] submissionIds;

	public RasSearchCriteriaSubmissionId(@NotNull String... submissionIdCriteria) {
		this.submissionIds = submissionIdCriteria;
	}

	@Override
	public boolean criteriaMatched(@NotNull TestStructure structure) {

		boolean isMatched = false;
		if (structure != null && submissionIds != null) {
			for (String submissionId : submissionIds) {
				if (submissionId.equals(structure.getSubmissionId())) {
					isMatched = true;
					break;
				}
			}
		}

		return isMatched;
	}

    public String[] getSubmissionIds() {
        return this.submissionIds;
    }

	@Override
	public String getCriteriaName() {
		return CRITERIA_NAME;
	}

	@Override
	public String[] getCriteriaContent() {
		return this.submissionIds;
	}
}
