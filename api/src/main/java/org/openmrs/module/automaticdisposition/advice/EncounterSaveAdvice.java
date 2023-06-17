package org.openmrs.module.automaticdisposition.advice;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.automaticdisposition.exception.DispositionAbortedException;
import org.openmrs.module.automaticdisposition.exception.DispositionPreconditionNotMetException;
import org.openmrs.module.emrapi.test.builder.ObsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static org.openmrs.module.automaticdisposition.AutomaticDispositionConstants.*;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EncounterSaveAdvice implements AfterReturningAdvice {

	private static final Logger LOGGER = LoggerFactory.getLogger(EncounterSaveAdvice.class);

	public static final String TITLE = "Encounter";

	public static final String CATEGORY = "Encounter";

	private static final String SAVE_METHOD = "save";

	private final PlatformTransactionManager platformTransactionManager;

	private final EncounterService encounterService;

	private final VisitService visitService;

	private final DefaultTransactionDefinition definition;

	private final ObsService obsService;

	private final LocationService locationService;

	private final AdministrationService administrationService;

	public EncounterSaveAdvice() throws SQLException {

		definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		platformTransactionManager = getSpringPlatformTransactionManager();
		encounterService = Context.getEncounterService();
		visitService = Context.getVisitService();
		locationService = Context.getLocationService();
		obsService = Context.getObsService();
		administrationService = Context.getAdministrationService();
	}

	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object emrEncounterService)
			throws Throwable {
		if (method.getName().equals(SAVE_METHOD)) {
			Object encounterUuidObject = PropertyUtils.getProperty(returnValue, "encounterUuid");
			String encounterUuid = encounterUuidObject.toString();

			TransactionStatus status = platformTransactionManager.getTransaction(definition);

			Pair<Visit, Set<Obs>> result = null;

			try {
				result = executeLogicForEncounterUuid(encounterUuid);
				platformTransactionManager.commit(status);
			} catch (Exception ex) {
				LOGGER.error("Error occured in transaction. Trying to rollback.", ex);
				platformTransactionManager.rollback(status);

				LOGGER.info("Successfully rolled back erroneous transaction.");
			}

			if (result == null) {
				return;
			}

			status = platformTransactionManager.getTransaction(definition);

			try {
				Visit visit = result.getLeft();
				Set<Obs> obs = result.getRight();

				if (isVisitAllowed(visit)) {
					addObs(obs, visit);
				}

				platformTransactionManager.commit(status);
			} catch (Exception ex) {
				LOGGER.error("Error occured in transaction. Trying to rollback.", ex);
				platformTransactionManager.rollback(status);

				LOGGER.info("Successfully rolled back erroneous transaction.");
			}

		}
	}

	private Pair<Visit, Set<Obs>> executeLogicForEncounterUuid(String encounterUuid) {
		LOGGER.info("Starting patient disposition.");

		try {
			Encounter encounter = encounterService.getEncounterByUuid(encounterUuid);
			Patient patient = encounter.getPatient();
			Visit visit = encounter.getVisit();

			Obs dispositionObs = findDispositionObs(encounter);

			Pair<String, String> dispositionLocationAndVisitType = findDispositionLocationAndVisitTypeMappings(
					dispositionObs);
			String dispositonLocation = dispositionLocationAndVisitType.getLeft();
			String dispositonVisitType = dispositionLocationAndVisitType.getRight();

			Location actualDispositonLocation = findLocation(locationService, dispositonLocation);
			VisitType actualDispositonVisitType = findVisitType(visitService, dispositonVisitType);

			Visit dispositionedVisit = new Visit(patient, actualDispositonVisitType, new Date());
			dispositionedVisit.setLocation(actualDispositonLocation);

			LOGGER.info("Trying to move to " + dispositonLocation + " and start visit via " + dispositonVisitType);

			visitService.endVisit(visit, new Date());
			Visit savedDispositionedVisit = visitService.saveVisit(dispositionedVisit);

			Set<Obs> obsToCopy = getFilteredObs(getAllObsForVisit(visit));

			LOGGER.info("Successfully performed patient disposition.");

			return Pair.of(savedDispositionedVisit, obsToCopy);
		} catch (DispositionPreconditionNotMetException e) {
			LOGGER.info(
					"Disposition was aborted because precondition were not met. Look at the stacktrace for further information.",
					e);
		} catch (DispositionAbortedException e) {
			LOGGER.error("Disposition was aborted. Look at the stacktrace for further information.", e);
		}

		return null;
	}

	private Set<Obs> getAllObsForVisit(Visit visit) {
		Set<Obs> allObs = new HashSet<>();

		for (Encounter encounter : visit.getEncounters()) {
			allObs.addAll(encounter.getAllObs());
		}

		return allObs;
	}

	private Set<Obs> getFilteredObs(Set<Obs> allObs) {
		Set<Obs> filteredObs = new HashSet<>();

		String conceptsString = administrationService
				.getGlobalProperty(DISPOSITION_GLOBAL_PROPERTY_OBS_CONCEPTS_TO_COPY);

		if (conceptsString == null) {
			return filteredObs;
		}

		String[] concepts = conceptsString.split(",");

		for (String concept : concepts) {
			Obs obs = findObs(allObs, concept);

			if (obs == null) {
				continue;
			}

			filteredObs.add(obs);
		}

		return filteredObs;
	}

	private void addObs(Set<Obs> allObs, Visit visit) {

		Patient patient = visit.getPatient();
		Location location = visit.getLocation();

		Encounter encounter = new Encounter();

		encounter.setEncounterDatetime(new Date());
		encounter.setEncounterType(encounterService.getEncounterType(DISPOSITION_COPIED_OBS_ENCOUNTER_TYPE));
		encounter.setPatient(patient);
		encounter.setLocation(location);

		Encounter savedEncounter = encounterService.saveEncounter(encounter);

		for (Obs obs : allObs) {
			Obs copiedObs = Obs.newInstance(obs);

			Date now = new Date();

			copiedObs.setDateCreated(now);
			copiedObs.setObsDatetime(now);
			copiedObs.setLocation(location);
			copiedObs.setEncounter(savedEncounter);

			Obs savedCopiedObs = obsService.saveObs(copiedObs,
					"Obs replication for new visit created by automatic disposition.");
			savedEncounter.addObs(savedCopiedObs);
		}

		savedEncounter = encounterService.saveEncounter(encounter);

		visit.addEncounter(savedEncounter);

		visitService.saveVisit(visit);
	}

	private boolean isVisitAllowed(Visit visit) {

		String visitTypesString = administrationService
				.getGlobalProperty(DISPOSITION_GLOBAL_PROPERTY_ALLOWED_VISIT_TYPES_FOR_OBS_COPY);

		if (visitTypesString == null) {
			return true;
		}

		String currentVisitType = visit.getVisitType().getName();

		String[] visitTypes = visitTypesString.split(",");

		for (String visitType : visitTypes) {
			if (currentVisitType.equals(visitType)) {
				return true;
			}
		}
		return false;
	}

	private Pair<String, String> findDispositionLocationAndVisitTypeMappings(Obs dispositionObs)
			throws DispositionAbortedException {
		Concept valueCoded = dispositionObs.getValueCoded();

		Collection<ConceptMap> conceptMappings = valueCoded.getConceptMappings();

		String dispositonLocation = null;
		String dispositonVisitType = null;

		for (ConceptMap conceptMap : conceptMappings) {

			ConceptReferenceTerm conceptReferenceTerm = conceptMap.getConceptReferenceTerm();

			String source = conceptReferenceTerm.getConceptSource().getName();

			if (DISPOSITION_LOCATION_CONCEPT_SOURCE.equals(source)) {
				dispositonLocation = conceptReferenceTerm.getCode();
			}

			if (DISPOSITION_VISIT_TYPE_CONCEPT_SOURCE.equals(source)) {
				dispositonVisitType = conceptReferenceTerm.getCode();
			}
		}

		if (dispositonLocation != null && dispositonVisitType != null) {
			return Pair.of(dispositonLocation, dispositonVisitType);
		} else {
			throw new DispositionAbortedException(
					"Could not find 'Disposition Location' and 'Disposition Visit Type' Mappings - Please check their correct entry.");
		}
	}

	private Obs findObs(Set<Obs> obsSet, String conceptName) {
		for (Obs obs : obsSet) {
			Concept concept = obs.getConcept();
			String name = concept.getName().getName();

			if (conceptName.equals(name)) {
				return obs;
			}
		}
		return null;
	}

	private Obs findDispositionObs(Encounter encounter) throws DispositionAbortedException {
		Set<Obs> allObs = encounter.getAllObs();

		Obs disposition = findObs(allObs, DISPOSITION_CONCEPT_NAME);

		if (disposition != null) {
			return disposition;
		}

		throw new DispositionPreconditionNotMetException("Could not find disposition obs. No action needed.");
	}

	private VisitType findVisitType(VisitService visitService, String dispositonVisitType)
			throws DispositionAbortedException {
		List<VisitType> allVisitTypes = visitService.getAllVisitTypes();

		for (VisitType visitType : allVisitTypes) {
			if (dispositonVisitType.equals(visitType.getName())) {
				return visitType;
			}
		}

		throw new DispositionAbortedException(
				"Could not find actual Visit Type object for String: Please check disposition mappings.");
	}

	private Location findLocation(LocationService locationService, String dispositonLocation)
			throws DispositionAbortedException {
		List<Location> allLocations = locationService.getAllLocations();

		for (Location location : allLocations) {
			if (dispositonLocation.equals(location.getName())) {
				return location;
			}
		}

		throw new DispositionAbortedException(
				"Could not find actual Location object for String: Please check disposition mappings.");
	}

	private PlatformTransactionManager getSpringPlatformTransactionManager() {
		List<PlatformTransactionManager> platformTransactionManagers = Context
				.getRegisteredComponents(PlatformTransactionManager.class);
		return platformTransactionManagers.get(0);
	}
}
