package org.openmrs.module.automaticdisposition.advice;

import org.apache.commons.beanutils.PropertyUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EncounterSaveAdvice implements AfterReturningAdvice {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EncounterSaveAdvice.class);
	
	public static final String TITLE = "Encounter";
	
	public static final String CATEGORY = "Encounter";
	
	private static final String SAVE_METHOD = "save";
	
	public EncounterSaveAdvice() throws SQLException {
	}
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object emrEncounterService)
	        throws Throwable {
		if (method.getName().equals(SAVE_METHOD)) {
			Object encounterUuid = PropertyUtils.getProperty(returnValue, "encounterUuid");
			LOGGER.error(encounterUuid.toString());
		}
	}
	
}
