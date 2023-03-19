package org.openmrs.module.automaticdisposition.advice;

import org.apache.commons.beanutils.PropertyUtils;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.automaticdisposition.api.impl.TransactionManager;
import org.openmrs.module.emrapi.encounter.EmrEncounterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.openmrs.api.context.Context;

import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EncounterSaveAdvice implements AfterReturningAdvice {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EncounterSaveAdvice.class);
	
	public static final String TITLE = "Encounter";
	
	public static final String CATEGORY = "Encounter";
	
	private static final String SAVE_METHOD = "save";
	
	private final TransactionManager transactionManager;
	
	private final EncounterService encounterService;
	
	public EncounterSaveAdvice() throws SQLException {
		PlatformTransactionManager platformTransactionManager = getSpringPlatformTransactionManager();
		transactionManager = new TransactionManager(platformTransactionManager);
		encounterService = getEncounterService();
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
					Encounter encounter = encounterService.getEncounterByUuid(encounterUuid);
					
					Set<Obs> allObs = encounter.getAllObs();
					
					for (Obs obs : allObs) {
						String name = obs.getConcept().getName().getName();
						
						if ("Disposition".equals(name)) {
							Concept valueCoded = obs.getValueCoded();
							
							Collection<ConceptMap> conceptMappings = valueCoded.getConceptMappings();
							
							String dispositonLocation = null;
							String dispositonvisitType = null;
							
							for (ConceptMap conceptMap : conceptMappings) {
								
								ConceptReferenceTerm conceptReferenceTerm = conceptMap.getConceptReferenceTerm();
								
								String source = conceptReferenceTerm.getConceptSource().getName();
								
								if ("Disposition Location".equals(source)) {
									dispositonLocation = conceptReferenceTerm.getCode();
								}
								
								if ("Disposition Visit Type".equals(source)) {
									dispositonvisitType = conceptReferenceTerm.getCode();
								}
							}
							
							if (dispositonLocation != null && dispositonvisitType != null) {
								LOGGER.error("Move to " + dispositonLocation + " and start visit via " + dispositonvisitType);
							}
						}
						
						LOGGER.info(name);
					}
					
					LOGGER.error(encounter.toString());
				}
				
				@Override
				public PropagationDefinition getTxPropagationDefinition() {
					return PropagationDefinition.PROPAGATION_REQUIRED;
				}
			});
			
			LOGGER.error(encounterUuid.toString());
		}
	}
	
	private EncounterService getEncounterService() {
		return Context.getEncounterService();
	}
	
	private PlatformTransactionManager getSpringPlatformTransactionManager() {
		List<PlatformTransactionManager> platformTransactionManagers = Context
		        .getRegisteredComponents(PlatformTransactionManager.class);
		return platformTransactionManagers.get(0);
	}
	
}
