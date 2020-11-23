package ca.uhn.fhir.jpa.searchparam.registry;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.rest.api.Constants;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class ReadOnlySearchParamCache {
	private static final Logger ourLog = LoggerFactory.getLogger(ReadOnlySearchParamCache.class);
	// resourceName -> searchParamName -> searchparam
	protected final Map<String, Map<String, RuntimeSearchParam>> myMap;

	ReadOnlySearchParamCache() {
		myMap = new HashMap<>();
	}

	private ReadOnlySearchParamCache(RuntimeSearchParamCache theRuntimeSearchParamCache) {
		myMap = theRuntimeSearchParamCache.myMap;
	}

	public static ReadOnlySearchParamCache fromFhirContext(FhirContext theFhirContext) {
		ReadOnlySearchParamCache retval = new ReadOnlySearchParamCache();

		Set<String> resourceNames = theFhirContext.getResourceTypes();

		for (String resourceName : resourceNames) {
			RuntimeResourceDefinition nextResDef = theFhirContext.getResourceDefinition(resourceName);
			String nextResourceName = nextResDef.getName();
			HashMap<String, RuntimeSearchParam> nameToParam = new HashMap<>();
			retval.myMap.put(nextResourceName, nameToParam);

			for (RuntimeSearchParam nextSp : nextResDef.getSearchParams()) {
				nameToParam.put(nextSp.getName(), nextSp);
			}
		}
		return retval;
	}

	public static ReadOnlySearchParamCache fromRuntimeSearchParamCache(RuntimeSearchParamCache theRuntimeSearchParamCache) {
		return new ReadOnlySearchParamCache(theRuntimeSearchParamCache);
	}

	public Stream<RuntimeSearchParam> getSearchParamStream() {
		return myMap.values().stream().flatMap(entry -> entry.values().stream());
	}

	protected Map<String, RuntimeSearchParam> getSearchParamMap(String theResourceName) {
		Map<String, RuntimeSearchParam> retval = myMap.get(theResourceName);
		if (retval == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(myMap.get(theResourceName));
	}

	public Collection<String> getValidSearchParameterNamesIncludingMeta(String theResourceName) {
		TreeSet<String> retval;
		Map<String, RuntimeSearchParam> searchParamMap = myMap.get(theResourceName);
		if (searchParamMap == null) {
			retval = new TreeSet<>();
		} else {
			retval = new TreeSet<>(searchParamMap.keySet());
		}
		retval.add(IAnyResource.SP_RES_ID);
		retval.add(Constants.PARAM_LASTUPDATED);
		return retval;
	}

	public int size() {
		return myMap.size();
	}
}
