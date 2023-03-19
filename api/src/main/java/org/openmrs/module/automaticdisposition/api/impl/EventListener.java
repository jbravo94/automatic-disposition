package org.openmrs.module.automaticdisposition.api.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

public class EventListener implements ApplicationListener<ContextRefreshedEvent> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);
	
	@Autowired
	private PlatformTransactionManager springPlatformTransactionManager;
	
	public EventListener() {
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
