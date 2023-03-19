package org.openmrs.module.automaticdisposition.api.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.annotation.OpenmrsProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component("automaticDispositionEventListener")
@OpenmrsProfile(openmrsPlatformVersion = "2.0.0 - 2.*")
public class AutomaticDispositionEventListener implements ApplicationListener<ContextRefreshedEvent> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticDispositionEventListener.class);
	
	@Autowired
	private PlatformTransactionManager springPlatformTransactionManager;
	
	public AutomaticDispositionEventListener() {
		LOGGER.error("Init");
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		debugEvent(event);
	}
	
	protected void debugEvent(ApplicationEvent event) {
		final ObjectMapper objectMapper = new ObjectMapper();
		LOGGER.error("Created AtomFeed event with {} UUID", event.toString());
	}
}
