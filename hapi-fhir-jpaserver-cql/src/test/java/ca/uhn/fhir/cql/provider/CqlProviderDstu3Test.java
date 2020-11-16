package ca.uhn.fhir.cql.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.cql.BaseCqlDstu3Test;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.rp.dstu3.LibraryResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.MeasureResourceProvider;
import ca.uhn.fhir.jpa.rp.dstu3.ValueSetResourceProvider;
import ca.uhn.fhir.util.StopWatch;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.opencds.cqf.dstu3.providers.MeasureOperationsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CqlProviderDstu3Test extends BaseCqlDstu3Test implements CqlProviderTestBase {
	private static final Logger ourLog = LoggerFactory.getLogger(CqlProviderDstu3Test.class);

	@Autowired
	CqlProviderLoader myCqlProviderLoader;
	@Autowired
	DaoRegistry myDaoRegistry;
	@Autowired
	IFhirResourceDao<Patient> myPatientDao;
	@Autowired
	FhirContext myFhirContext;
	@Autowired
	IFhirSystemDao mySystemDao;
	@Autowired
	private LibraryResourceProvider myLibraryResourceProvider;
	@Autowired
	private MeasureResourceProvider myMeasureResourceProvider;
	@Autowired
	private ValueSetResourceProvider myValueSetResourceProvider;

	MeasureOperationsProvider myProvider;

	@BeforeEach
	public void before() throws IOException {
		// FIXME KBD Can we find a way to remove these?
		myMeasureResourceProvider.setDao(myDaoRegistry.getResourceDao("Measure"));
		myLibraryResourceProvider.setDao(myDaoRegistry.getResourceDao("Library"));
		myValueSetResourceProvider.setDao(myDaoRegistry.getResourceDao("ValueSet"));

		myProvider = myCqlProviderLoader.buildDstu3Provider();

		// Load terminology for measure tests (HEDIS measures)
		loadBundle("hedis-valuesets-bundle.json");

		// Load libraries
		loadResource("library/library-fhir-model-definition.json", myFhirContext, myDaoRegistry);
		loadResource("library/library-fhir-helpers.json", myFhirContext, myDaoRegistry);
		loadResource("library/library-asf-logic.json", myFhirContext, myDaoRegistry);

		// load test data and conversion library for $apply operation tests
		loadResource("general-practitioner.json", myFhirContext, myDaoRegistry);
		loadResource("general-patient.json", myFhirContext, myDaoRegistry);
	}

	// FIXME KBD
	//@Test
	@Disabled
	public void evaluateMeasure() throws IOException {
		Patient patient = new Patient();
		// FIXME KBD add something to patient we want to measure
		loadResource("measure-asf.json", myFhirContext, myDaoRegistry);

		IIdType patientId = myPatientDao.create(patient).getId().toVersionless();

		Patient patientInstance = myDaoRegistry.getResourceDao(Patient.class).read(patientId);

		// FIXME KBD
		String periodStart = "0";
		String periodEnd = StringUtils.defaultToString(System.currentTimeMillis());
		String subject = "Patient";
		MeasureReport measureReport = myProvider.evaluateMeasure((IdType) patientId.toVersionless(), periodStart,
			periodEnd, null, "patient", subject, null, null,
			null, null, null, null);
		assertNotNull(measureReport);
		// FIXME KBD assert on stuff in measureReport
	}

	/*
	See library-asf-cql.txt to see the cql encoded within library-asf-logic.json
	See library-asf-elm.xml to see the elm encoded within library-asf-logic.json
	 */

	/*
	To help explain what's being measured here.  Specifically how to interpret the contents of library-asf-logic.json.
	From https://www.ncqa.org/wp-content/uploads/2020/02/20200212_17_ASF.pdf

• ValueSet: "Alcohol Counseling and Treatment": 'http://ncqa.org/hedis/ValueSet/2.16.840.1.113883.3.464.1004.1437'
• ValueSet: "Alcohol Screening": 'http://ncqa.org/hedis/ValueSet/2.16.840.1.113883.3.464.1004.1337'
• ValueSet: "Alcohol use disorder": 'http://ncqa.org/hedis/ValueSet/2.16.840.1.113883.3.464.1004.1339'
• ValueSet: "Dementia": 'http://ncqa.org/hedis/ValueSet/2.16.840.1.113883.3.464.1004.1074'
• Diagnosis: Alcohol Use Disorder (2.16.840.1.113883.3.464.1004.1339)
• Diagnosis: Dementia (2.16.840.1.113883.3.464.1004.1074)
• Encounter, Performed: Hospice Encounter (2.16.840.1.113883.3.464.1004.1761)
• Intervention, Order: Hospice Intervention (2.16.840.1.113883.3.464.1004.1762)
• Intervention, Performed: Alcohol Counseling or Other Follow Up Care
(2.16.840.1.113883.3.464.1004.1437)
• Intervention, Performed: Hospice Intervention (2.16.840.1.113883.3.464.1004.1762)
Direct Reference Codes:
• Assessment, Performed: How often have you had five or more drinks in one day during the past year
[Reported] (LOINC version 2.63 Code 88037-7)
• Assessment, Performed: How often have you had four or more drinks in one day during the past year
[Reported] (LOINC version 2.63 Code 75889-6)
• Assessment, Performed: Total score [AUDIT-C] (LOINC version 2.63 Code 75626-2)
	 */

	@Test
	public void evaluatePatientMeasure() throws IOException {
		// Load the measure for ASF: Unhealthy Alcohol Use Screening and Follow-up (ASF)
		loadResource("measure-asf.json", myFhirContext, myDaoRegistry);
		Bundle result = loadBundle("test-patient-6529-data.json");
		assertNotNull(result);
		List<Bundle.BundleEntryComponent> entries = result.getEntry();
		assertThat(entries, hasSize(22));
		assertEquals(entries.get(0).getResponse().getStatus(), "201 Created");
		assertEquals(entries.get(21).getResponse().getStatus(), "201 Created");

		IdType measureId = new IdType("Measure", "measure-asf");
		String patient = "Patient/Patient-6529";
		String periodStart = "2003-01-01";
		String periodEnd = "2003-12-31";

		// First run to absorb startup costs
		MeasureReport report = myProvider.evaluateMeasure(measureId, periodStart, periodEnd, null, null,
			patient, null, null, null, null, null, null);
		// Assert it worked
		assertThat(report.getGroup(), hasSize(1));
		assertThat(report.getGroup().get(0).getPopulation(), hasSize(3));
		for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
			for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
				assertTrue(population.getCount() > 0);
			}
		}
		ourLog.info(myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(report));

		// Now timed runs
		int runCount = 10;
		StopWatch sw = new StopWatch();
		for (int i = 0; i < runCount; ++i) {
			myProvider.evaluateMeasure(measureId, periodStart, periodEnd, null, null,
				patient, null, null, null, null, null, null);
		}

		ourLog.info("Called evaluateMeasure() {} times: average time per call: {}", runCount, sw.formatMillisPerOperation(runCount));

	}

	@Test
	public void evaluatePopulationMeasure() throws IOException {
		// Load the measure for ASF: Unhealthy Alcohol Use Screening and Follow-up (ASF)
		loadResource("measure-asf.json", myFhirContext, myDaoRegistry);
		loadBundle("test-patient-6529-data.json");
		// Add a second patient with the same data
		loadBundle("test-patient-9999-x-data.json");

		IdType measureId = new IdType("Measure", "measure-asf");
		String periodStart = "2003-01-01";
		String periodEnd = "2003-12-31";

		// First run to absorb startup costs
		MeasureReport report = myProvider.evaluateMeasure(measureId, periodStart, periodEnd, null, "population",
			null, null, null, null, null, null, null);
		// Assert it worked
		assertThat(report.getGroup(), hasSize(1));
		assertThat(report.getGroup().get(0).getPopulation(), hasSize(3));
		for (MeasureReport.MeasureReportGroupComponent group : report.getGroup()) {
			for (MeasureReport.MeasureReportGroupPopulationComponent population : group.getPopulation()) {
				assertTrue(population.getCount() > 0);
			}
		}
		ourLog.info(myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(report));

		// Now timed runs
		int runCount = 10;
		StopWatch sw = new StopWatch();
		for (int i = 0; i < runCount; ++i) {
			myProvider.evaluateMeasure(measureId, periodStart, periodEnd, null, "population",
				null, null, null, null, null, null, null);
		}

		ourLog.info("Called evaluateMeasure() {} times: average time per call: {}", runCount, sw.formatMillisPerOperation(runCount));

	}

	private Bundle loadBundle(String theLocation) throws IOException {
		String json = stringFromResource(theLocation);
		Bundle bundle = (Bundle) myFhirContext.newJsonParser().parseResource(json);
		Bundle result = (Bundle) mySystemDao.transaction(null, bundle);
		return result;
	}
}
