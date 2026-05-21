package id.ac.ui.cs.advprog.bidmartauthservice.functional.support;

import io.restassured.http.ContentType;
import net.serenitybdd.annotations.Step;

import java.util.Map;

import static net.serenitybdd.rest.SerenityRest.given;

public class AdminApiSteps {

    @Step("Deactivate user {1}")
    public void deactivateUser(int port, Long userId, String adminAccessToken, String reason) {
        given()
                .baseUri("http://localhost")
                .port(port)
                .header("Authorization", "Bearer " + adminAccessToken)
                .contentType(ContentType.JSON)
                .body(Map.of("reason", reason))
                .when()
                .post("/api/admin/users/{userId}/deactivate", userId)
                .then()
                .statusCode(200);
    }

    @Step("Expect old access token to be rejected")
    public void expectProfileUnauthorized(int port, String accessToken) {
        given()
                .baseUri("http://localhost")
                .port(port)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/profile")
                .then()
                .statusCode(401);
    }
}
