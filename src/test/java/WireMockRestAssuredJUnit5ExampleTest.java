import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.http.Header;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WireMockRestAssuredJUnit5ExampleTest {

    public static WireMockServer wireMockServer;

    @BeforeAll
    public static void setup() {
        wireMockServer = new WireMockServer(8451);
        wireMockServer.start();
        createStubs();
    }

    @AfterAll
    public static void teardown() {
        wireMockServer.stop();
    }

    public static void createStubs() {
        wireMockServer.stubFor(get(urlEqualTo("/organization/employees"))
                .willReturn(aResponse().withHeader("Content-Type", "text/plain")
                        .withStatus(200)
                        .withBodyFile("json/employees.json")));

        wireMockServer.stubFor(get(urlEqualTo("/another/endpoint"))
                .withHeader("Accept", matching("application/json"))
                .willReturn(aResponse().
                        withStatus(200).
                        withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"OK\"}")
                        .withFixedDelay(1984)));

        wireMockServer.stubFor(get(urlEqualTo("/another/endpoint"))
                .withHeader("Accept", matching("text/plain"))
                .willReturn(aResponse().
                        withStatus(406).
                        withHeader("Content-Type", "text/html").
                        withBody("406 Not Acceptable")));
    }

    @Test
    public void checkEndpointStatusCodeTest() {
        given().
                when().
                get("http://localhost:8451/organization/employees").
                then().
                assertThat().statusCode(200);
    }

    @Test
    public void checkEmployeeFirstNameTest() {
        Response response = given().when().get("http://localhost:8451/organization/employees");
        String firstName = response.jsonPath().get("employees.find { it.id == 2 }.first_name");
        assertEquals("Ray", firstName);
    }

    @Test
    public void checkNotFoundStatusCodeTest() {
        given().
                when().
                get("http://localhost:8451/invalid/endpoint").
                then().
                assertThat().statusCode(404);
    }

    @Test
    public void checkAcceptHeaderPositiveTest() {
        Response response = given()
                .header(new Header("Accept", "application/json"))
                .when()
                .get("http://localhost:8451/another/endpoint");
        assertEquals(HttpStatus.SC_OK, response.statusCode());
        assertEquals("OK", response.jsonPath().getString("status"));
    }

    @Test
    public void checkAcceptHeaderNegativeTest() {
        Response response = given()
                .header(new Header("Accept", "text/plain"))
                .when()
                .get("http://localhost:8451/another/endpoint");
        assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.statusCode());
        assertEquals("406 Not Acceptable", response.getBody().asString());
    }

}