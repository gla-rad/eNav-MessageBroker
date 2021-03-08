package org.grad.eNav.msgBroker;

import org.geotools.data.DataStore;
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

	/**
	 * MOck a Geomesa Data Store bean so that we pretend we have a connection
	 * while the actual GS Data Store configuration is not enabled.
	 *
	 * @return the Geomesa Data Store bean
	 */
	@Bean
	DataStore gsDataStore() {
		return mock(DataStore.class);
	}

}
