/*-
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.term;

import ca.uhn.fhir.jpa.entity.TermConcept;
import ca.uhn.fhir.jpa.entity.TermConceptDesignation;
import ca.uhn.fhir.jpa.entity.TermConceptParentChildLink;
import ca.uhn.fhir.jpa.entity.TermConceptProperty;
import ca.uhn.fhir.jpa.model.entity.EntityIndexStatusEnum;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Date;

public class TermConceptDaoSvc {
	private static final Logger ourLog = LoggerFactory.getLogger(TermConceptDaoSvc.class);

	@Autowired
	private EntityManager myEntityManager;

	private boolean mySupportLegacyLob = false;

	public int saveConcept(TermConcept theConcept) {
		int retVal = 0;

		/*
		 * If the concept has an ID, we're reindexing, so there's no need to
		 * save parent concepts first (it's way too slow to do that)
		 */
		if (theConcept.getId() == null) {
			boolean needToSaveParents = false;
			for (TermConceptParentChildLink next : theConcept.getParents()) {
				if (next.getParent().getId() == null) {
					needToSaveParents = true;
					break;
				}
			}
			if (needToSaveParents) {
				retVal += ensureParentsSaved(theConcept.getParents());
			}
		}

		if (theConcept.getId() == null || theConcept.getIndexStatus() == null) {
			retVal++;
			theConcept.setIndexStatus(EntityIndexStatusEnum.INDEXED_ALL);
			theConcept.setUpdated(new Date());
			theConcept.flagForLegacyLobSupport(mySupportLegacyLob);
			myEntityManager.persist(theConcept);

			for (TermConceptProperty next : theConcept.getProperties()) {
				next.performLegacyLobSupport(mySupportLegacyLob);
				myEntityManager.persist(next);
			}

			for (TermConceptDesignation next : theConcept.getDesignations()) {
				myEntityManager.persist(next);
			}
		}

		ourLog.trace("Saved {} and got PID {}", theConcept.getCode(), theConcept.getId());
		return retVal;
	}

	public TermConceptDaoSvc setSupportLegacyLob(boolean theSupportLegacyLob) {
		mySupportLegacyLob = theSupportLegacyLob;
		return this;
	}

	private int ensureParentsSaved(Collection<TermConceptParentChildLink> theParents) {
		ourLog.trace("Checking {} parents", theParents.size());
		int retVal = 0;

		for (TermConceptParentChildLink nextLink : theParents) {
			if (nextLink.getRelationshipType() == TermConceptParentChildLink.RelationshipTypeEnum.ISA) {
				TermConcept nextParent = nextLink.getParent();
				retVal += ensureParentsSaved(nextParent.getParents());
				if (nextParent.getId() == null) {
					nextParent.setUpdated(new Date());
					nextParent.flagForLegacyLobSupport(mySupportLegacyLob);
					myEntityManager.persist(nextParent);
					retVal++;
					ourLog.debug("Saved parent code {} and got id {}", nextParent.getCode(), nextParent.getId());
				}
			}
		}

		return retVal;
	}
}
