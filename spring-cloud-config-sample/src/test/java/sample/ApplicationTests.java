package sample;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.config.server.test.ConfigServerTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class,
// Normally spring.cloud.config.enabled:true is the default but since we have the config
// server on the classpath we need to set it explicitly
	properties = { "spring.cloud.config.enabled:true",
			"management.security.enabled=false", "endpoints.default.web.enabled=true" }, webEnvironment = RANDOM_PORT)
public class ApplicationTests {

	private static int configPort = SocketUtils.findAvailableTcpPort();

	@LocalServerPort
	private int port;

	private static ConfigurableApplicationContext server;

	@BeforeClass
	public static void startConfigServer() throws IOException {
		String baseDir = ConfigServerTestUtils
				.getBaseDirectory("spring-cloud-config-sample");
		String repo = ConfigServerTestUtils.prepareLocalRepo(baseDir, "target/repos",
				"config-repo", "target/config");
		server = SpringApplication.run(
				org.springframework.cloud.config.server.ConfigServerApplication.class,
				"--server.port=" + configPort, "--spring.config.name=server",
				"--spring.cloud.config.server.git.uri=" + repo);
		/*FIXME configPort = ((EmbeddedWebApplicationContext) server)
				.getEmbeddedServletContainer().getPort();*/
		System.setProperty("config.port", "" + configPort);
	}

	@AfterClass
	public static void close() {
		System.clearProperty("config.port");
		if (server != null) {
			server.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void contextLoads() {
		Map res = new TestRestTemplate()
				.getForObject("http://localhost:" + port + "/application/env/info.foo", Map.class);
		assertThat(res).containsKey("propertySources");
		List<Map<String, Object>> sources = (List<Map<String,Object>>) res.get("propertySources");
		Optional<Map<String, Object>> properties = sources.stream()
				.map(map -> (Map<String, Object>) map.get("properties"))
				.filter(map -> map.containsKey("info.foo"))
				.map(map -> (Map<String, Object>) map.get("info.foo"))
				.findFirst();
		assertThat(properties.get()).containsEntry("value", "bar");
	}

	public static void main(String[] args) throws IOException {
		configPort = 8888;
		startConfigServer();
		SpringApplication.run(Application.class, args);
	}

}
