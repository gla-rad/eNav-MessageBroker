package org.grad.eNav.msgBroker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.security.auth.message.config.RegistrationListener;

import static org.mockito.Mockito.mock;

/**
 * The type Test configuration.
 */
@Configuration
public class TestConfiguration {

	/**
	 * Feign depends on the Eureka Registration Listener bean so let's mock one
	 * up.
	 *
	 * @return the Eureka Registration Listener bean
	 */
	@Bean
	RegistrationListener registrationListener() {
		return mock(RegistrationListener.class);
	}
}
