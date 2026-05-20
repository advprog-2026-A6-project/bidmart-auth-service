package id.ac.ui.cs.advprog.bidmartauthservice.config;

import id.ac.ui.cs.advprog.bidmartauthservice.model.AuthEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthEventLogListener {

    @EventListener
    public void onAuthEvent(AuthEvent event) {
        log.info("Auth event published: type={}, aggregateType={}, aggregateId={}, payload={}",
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getPayload());
    }
}
