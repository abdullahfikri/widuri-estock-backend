package dev.mfikri.widuriestock.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthenticationEventListener {

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        log.info("{} bad credential", event.getAuthentication().getName());
    }
}
