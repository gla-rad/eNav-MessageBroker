package org.grad.eNav.msgBroker;

import org.geotools.data.DataStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import javax.xml.crypto.Data;

import static org.mockito.Mockito.mock;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
class MessageBrokerTests {

	@Test
	void contextLoads() {

	}

}
