package de.metas.material.dispo.service.event;

import de.metas.material.dispo.Candidate;
import de.metas.material.dispo.Candidate.Status;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventUtil
{
	public static Candidate.Status getCandidateStatus(final String docStatus)
	{
		final Candidate.Status candidateStatus;
		if ("DR".equals(docStatus) || "IP".equals(docStatus))
		{
			candidateStatus = Status.doc_created;
		}
		else if ("CO".equals(docStatus))
		{
			candidateStatus = Status.doc_completed;
		}
		else if ("CL".equals(docStatus))
		{
			candidateStatus = Status.doc_closed;
		}
		else
		{
			candidateStatus = Status.unexpected;
		}
		return candidateStatus;
	}
}
