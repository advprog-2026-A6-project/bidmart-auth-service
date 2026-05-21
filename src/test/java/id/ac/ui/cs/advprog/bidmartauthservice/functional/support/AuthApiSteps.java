package id.ac.ui.cs.advprog.bidmartauthservice.functional.support;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Step;

import java.util.Map;

import static net.serenitybdd.rest.SerenityRest.given;

public class AuthApiSteps {

    @Step("Register user {1} as {2}")
    public void registerUser(int port, String email, String role, String password) {
        given()
                .baseUri("http://localhost")
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "Functional Test User",
                        "email", email,
                        "role", role,
                        "password", password
                ))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);
    }

    @Step("Verify email using token")
    public void verifyEmail(int port, String token) {
        given()
                .baseUri("http://localhost")
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of("token", token))
                .when()
                .post("/api/auth/verify-email")
                .then()
                .statusCode(200);
    }

    @Step("Login as {1}")
    public Response login(int port, String email, String password, String userAgent) {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .header("User-Agent", userAgent)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "email", email,
                        "password", password
                ))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    @Step("Verify second factor with challenge token")
    public Response verifySecondFactor(int port, String challengeToken, String code, String userAgent) {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .header("User-Agent", userAgent)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "challengeToken", challengeToken,
                        "code", code
                ))
                .when()
                .post("/api/auth/verify-2fa")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }
}
