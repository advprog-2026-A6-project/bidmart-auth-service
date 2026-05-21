package id.ac.ui.cs.advprog.bidmartauthservice.functional.support;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.serenitybdd.annotations.Step;

import static net.serenitybdd.rest.SerenityRest.given;

public class ProfileApiSteps {

    @Step("Read authenticated profile")
    public Response getProfile(int port, String accessToken) {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/profile")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    @Step("Enable email-based 2FA")
    public void enableEmail2fa(int port, String accessToken) {
        given()
                .baseUri("http://localhost")
                .port(port)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/profile/2fa/enable/email")
                .then()
                .statusCode(200);
    }
}
