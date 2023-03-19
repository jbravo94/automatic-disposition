package org.openmrs.module.automaticdisposition.api.impl;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openmrs.event.EventListener;
import org.openmrs.event.Event.Action;

// A simple event listener that just keeps a count of all created, updated and purged items
public class AutomaticDispositionEventHandler implements EventListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticDispositionEventHandler.class);
	
	private int createdCount = 0;
	
	private int updatedCount = 0;
	
	private int purgedCount = 0;
	
	public int getCreatedCount() {
		return createdCount;
	}
	
	public int getUpdatedCount() {
		return updatedCount;
	}
	
	public int getPurgedCount() {
		return purgedCount;
	}
	
	@Override
	public void onMessage(Message message) {
		try {
			MapMessage mapMessage = (MapMessage) message;
			LOGGER.error(message.toString());
			if (Action.CREATED.toString().equals(mapMessage.getString("action")))
				createdCount++;
			else if (Action.UPDATED.toString().equals(mapMessage.getString("action")))
				updatedCount++;
			else if (Action.PURGED.toString().equals(mapMessage.getString("action")))
				purgedCount++;
			
			//..... Keep counts for more event actions
		}
		catch (JMSException e) {
			System.out.println("Ooops! some error occurred");
		}
	}
	
}
