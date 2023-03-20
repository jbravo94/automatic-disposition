package org.openmrs.module.automaticdisposition.advice;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.automaticdisposition.exception.DispositionAbortedException;
import org.openmrs.module.automaticdisposition.persistence.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.transaction.PlatformTransactionManager;
import static org.openmrs.module.automaticdisposition.AutomaticDispositionConstants.*;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class EncounterSaveAdvice implements AfterReturningAdvice {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EncounterSaveAdvice.class);
	
	public static final String TITLE = "Encounter";
	
	public static final String CATEGORY = "Encounter";
	
	private static final String SAVE_METHOD = "save";
	
	private final TransactionManager transactionManager;
	
	private final EncounterService encounterService;
	
	private final VisitService visitService;
	
	private final LocationService locationService;
	
	public EncounterSaveAdvice() throws SQLException {
		PlatformTransactionManager platformTransactionManager = getSpringPlatformTransactionManager();
		transactionManager = new TransactionManager(platformTransactionManager);
		encounterService = Context.getEncounterService();
		visitService = Context.getVisitService();
		locationService = Context.getLocationService();
	}
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object emrEncounterService)
	        throws Throwable {
		if (method.getName().equals(SAVE_METHOD)) {
			Object encounterUuidObject = PropertyUtils.getProperty(returnValue, "encounterUuid");
			String encounterUuid = encounterUuidObject.toString();
			
			transactionManager.executeWithTransaction(new AFTransactionWorkWithoutResult() {
				
				@Override
				protected void doInTransaction() {
					LOGGER.info("Starting patient disposition.");
					
					try {
						Encounter encounter = encounterService.getEncounterByUuid(encounterUuid);
						Patient patient = encounter.getPatient();
						Visit visit = encounter.getVisit();
						
						Obs dispositionObs = findDispositionObs(encounter);
						
						Pair<String, String> dispositionLocationAndVisitType = findDispositionLocationAndVisitTypeMappings(dispositionObs);
						String dispositonLocation = dispositionLocationAndVisitType.getLeft();
						String dispositonVisitType = dispositionLocationAndVisitType.getRight();
						
						Location actualDispositonLocation = findLocation(locationService, dispositonLocation);
						VisitType actualDispositonVisitType = findVisitType(visitService, dispositonVisitType);
						
						Visit dispositionedVisit = new Visit(patient, actualDispositonVisitType, new Date());
						dispositionedVisit.setLocation(actualDispositonLocation);
						
						LOGGER.info("Trying to move to " + dispositonLocation + " and start visit via "
						        + dispositonVisitType);
						
						visitService.endVisit(visit, new Date());
						visitService.saveVisit(dispositionedVisit);
						
					}
					catch (DispositionAbortedException e) {
						LOGGER.error(
						    "Disposition was aborted because precondition were not met. Look at the stacktrace for further information.",
						    e);
					}
					
					LOGGER.info("Successfully performed patient disposition.");
				}
				
				@Override
				public PropagationDefinition getTxPropagationDefinition() {
					return PropagationDefinition.PROPAGATION_REQUIRED;
				}
			});
		}
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
	
	private Obs findDispositionObs(Encounter encounter) throws DispositionAbortedException {
		Set<Obs> allObs = encounter.getAllObs();
		
		for (Obs obs : allObs) {
			String name = obs.getConcept().getName().getName();
			
			if ("Disposition".equals(name)) {
				return obs;
			}
		}
		
		throw new DispositionAbortedException("Could not find disposition obs. No action needed.");
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
