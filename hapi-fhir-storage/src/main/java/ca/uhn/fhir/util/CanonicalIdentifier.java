/*-
 * #%L
 * HAPI FHIR Storage api
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
package ca.uhn.fhir.util;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.model.api.IElement;
import ca.uhn.fhir.model.base.composite.BaseIdentifierDt;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.model.primitive.UriDt;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hl7.fhir.instance.model.api.IBase;

import java.util.List;

/**
 * Version independent identifier
 */
public class CanonicalIdentifier extends BaseIdentifierDt {
	UriDt mySystem;
	StringDt myValue;

	@Override
	public UriDt getSystemElement() {
		return mySystem;
	}

	@Override
	public StringDt getValueElement() {
		return myValue;
	}

	@Override
	public CanonicalIdentifier setSystem(String theUri) {
		mySystem = new UriDt((theUri));
		return this;
	}

	@Override
	public CanonicalIdentifier setValue(String theString) {
		myValue = new StringDt(theString);
		return this;
	}

	@Override
	public <T extends IElement> List<T> getAllPopulatedChildElementsOfType(Class<T> theType) {
		throw new UnsupportedOperationException(Msg.code(1488));
	}

	@Override
	public boolean isEmpty() {
		if (mySystem != null && !mySystem.isEmpty()) {
			return false;
		}
		if (myValue != null && !myValue.isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean equals(Object theO) {
		if (this == theO) return true;

		if (theO == null || getClass() != theO.getClass()) return false;

		CanonicalIdentifier that = (CanonicalIdentifier) theO;

		return new EqualsBuilder()
				.append(mySystem, that.mySystem)
				.append(myValue, that.myValue)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(mySystem).append(myValue).toHashCode();
	}

	public static CanonicalIdentifier fromIdentifier(IBase theIdentifier) {
		CanonicalIdentifier retval = new CanonicalIdentifier();

		// TODO add other fields like "use" etc
		if (theIdentifier instanceof org.hl7.fhir.dstu3.model.Identifier) {
			org.hl7.fhir.dstu3.model.Identifier ident = (org.hl7.fhir.dstu3.model.Identifier) theIdentifier;
			retval.setSystem(ident.getSystem()).setValue(ident.getValue());
		} else if (theIdentifier instanceof org.hl7.fhir.r4.model.Identifier) {
			org.hl7.fhir.r4.model.Identifier ident = (org.hl7.fhir.r4.model.Identifier) theIdentifier;
			retval.setSystem(ident.getSystem()).setValue(ident.getValue());
		} else if (theIdentifier instanceof org.hl7.fhir.r5.model.Identifier) {
			org.hl7.fhir.r5.model.Identifier ident = (org.hl7.fhir.r5.model.Identifier) theIdentifier;
			retval.setSystem(ident.getSystem()).setValue(ident.getValue());
		} else {
			throw new InternalErrorException(Msg.code(1486) + "Expected 'Identifier' type but was '"
					+ theIdentifier.getClass().getName() + "'");
		}
		return retval;
	}
}
