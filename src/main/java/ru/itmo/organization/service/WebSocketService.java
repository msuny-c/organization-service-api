package ru.itmo.organization.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastOrganizationsUpdate() {
        messagingTemplate.convertAndSend("/topic/organizations", "update");
    }

    public void broadcastAddressesUpdate() {
        messagingTemplate.convertAndSend("/topic/addresses", "update");
    }

    public void broadcastCoordinatesUpdate() {
        messagingTemplate.convertAndSend("/topic/coordinates", "update");
    }

    public void broadcastLocationsUpdate() {
        messagingTemplate.convertAndSend("/topic/locations", "update");
    }
}
