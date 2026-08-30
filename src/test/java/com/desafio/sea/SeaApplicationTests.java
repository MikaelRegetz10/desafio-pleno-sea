package com.desafio.sea;

import com.desafio.sea.repository.AuditLogRepository;
import com.desafio.sea.repository.SolicitationRepository;
import com.desafio.sea.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SeaApplicationTests {

	@MockitoBean
	private AuditLogRepository auditLogRepository;

	@MockitoBean
	private SolicitationRepository solicitationRepository;

	@MockitoBean
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

}
