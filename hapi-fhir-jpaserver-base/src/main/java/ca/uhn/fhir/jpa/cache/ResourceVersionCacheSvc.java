package ca.uhn.fhir.jpa.cache;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.dao.data.IResourceTableDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.storage.ResourcePersistentId;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceVersionCacheSvc {
	private static final Logger myLogger = LoggerFactory.getLogger(ResourceVersionMap.class);

	@Autowired
	DaoRegistry myDaoRegistry;
	@Autowired
	IResourceTableDao myResourceTableDao;

	public ResourceVersionMap getVersionLookup(String theResourceName, SearchParameterMap theSearchParamMap) {
		IFhirResourceDao<?> dao = myDaoRegistry.getResourceDao(theResourceName);

		myLogger.info("About to search for '" + theResourceName + "' objects using params '" + theSearchParamMap + "'.");
		List<Long> matchingIds = dao.searchForIds(theSearchParamMap, null).stream()
			.map(ResourcePersistentId::getIdAsLong)
			.collect(Collectors.toList());
		myLogger.info("Found " + matchingIds.size() + " '" + theResourceName + "' objects using params '" + theSearchParamMap + "'.");

		ResourceVersionMap retval = new ResourceVersionMap();
		myResourceTableDao.findAllById(matchingIds)
			.forEach(entity -> retval.add(entity.getIdDt()));
		return retval;
	}
}