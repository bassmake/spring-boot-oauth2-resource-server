package sk.bsmk;

import com.github.tomakehurst.wiremock.WireMockServer;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.web.header.Header;

import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResourceServerTest {

  @Autowired private TestRestTemplate restTemplate;

  private static WireMockServer wireMockServer;

  private static final RSAKey rsaJWK;
  private static final JWSSigner signer;

  static {
    try {
      rsaJWK =
          new RSAKeyGenerator(2048)
              .keyUse(KeyUse.SIGNATURE)
              .keyID(UUID.randomUUID().toString())
              .generate();

      signer = new RSASSASigner(rsaJWK);
    } catch (JOSEException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  public static void initAuthorizationServer() {
    wireMockServer = new WireMockServer(8081);
    wireMockServer.start();
    wireMockServer.stubFor(
        get(urlEqualTo("/issuer/.well-known/jwks.json"))
            .willReturn(ok("{\"keys\": [" + rsaJWK.toPublicJWK().toJSONString() + "]}")));
  }

  @AfterAll
  public static void teardownAuthorizationServer() {
    wireMockServer.stop();
    wireMockServer = null;
  }

  @Test
  public void thatSecuredResourcesCanBeAccessed() throws Exception {
    final String token = generateToken();
    final RequestEntity<Void> request =
        RequestEntity.get(ResourceServerApp.PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .build();

    final ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(ResourceServerApp.DATA);
  }

  private String generateToken() throws Exception {
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject("alice")
            .issuer("http://localhost:8081/issuer")
            .expirationTime(new Date(new Date().getTime() + 60 * 1000))
            .build();

    SignedJWT signedJWT =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), claimsSet);

    signedJWT.sign(signer);

    return signedJWT.serialize();
  }
}
