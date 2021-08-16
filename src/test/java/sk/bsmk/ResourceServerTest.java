package sk.bsmk;

import com.github.tomakehurst.wiremock.WireMockServer;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.web.header.Header;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResourceServerTest {

  @Autowired private TestRestTemplate restTemplate;

  private static WireMockServer wireMockServer;

  private static final RSAKey jwk;

  static {
    try {
      jwk =
          new RSAKeyGenerator(2048)
              .keyUse(KeyUse.SIGNATURE)
              .keyID(UUID.randomUUID().toString())
              .generate();
    } catch (JOSEException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  public static void initAuthorizationServer() {
    wireMockServer = new WireMockServer(8081);
    wireMockServer.start();
    wireMockServer.stubFor(
        get(urlEqualTo("/issuer/.well-known/openid-configuration")).willReturn(ok(jwk.toPublicJWK().toJSONString())));
  }

  @AfterAll
  public static void teardownAuthorizationServer() {
    wireMockServer.stop();
    wireMockServer = null;
  }

  @Test
  public void thatSecuredResourcesCanBeAccessed() {
    final String token = "TODO";
    final RequestEntity<Void> request = RequestEntity.get(ResourceServerApp.PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

    final ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(ResourceServerApp.DATA);
  }
}
