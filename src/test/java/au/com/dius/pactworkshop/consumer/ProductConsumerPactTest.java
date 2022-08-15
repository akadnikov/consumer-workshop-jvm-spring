package au.com.dius.pactworkshop.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.PactTestExecutionContext;
import au.com.dius.pact.consumer.PactTestRun;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.Interaction;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.pact.PactRequest;
import com.atlassian.oai.validator.pact.PactResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArrayMinLike;
import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(PactConsumerTestExt.class)
public class ProductConsumerPactTest {

    @Pact(consumer = "FrontendApplication", provider = "ProductService")
    RequestResponsePact getAllProducts(PactDslWithProvider builder) {
        return builder.given("products exist")
                .uponReceiving("get all products")
                .method("GET").path("/products")
                .matchHeader("Authorization", "Bearer (19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0123]):[0-5][0-9]")
                .willRespondWith()
                .status(200)
                .headers(headers())
                .body(newJsonArrayMinLike(2, array -> array.object(object -> {
                    object.stringType("id", "09");
                    object.stringType("type", "CREDIT_CARD");
                    object.stringType("name", "Gem Visa");
                })).build())
                .toPact();
    }

    @Pact(consumer = "FrontendApplication", provider = "ProductService")
    RequestResponsePact noProductsExist(PactDslWithProvider builder) {
        return builder.given("no products exist")
                .uponReceiving("get all products")
                .method("GET")
                .path("/products")
                .matchHeader("Authorization", "Bearer (19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0123]):[0-5][0-9]")
                .willRespondWith()
                .status(200)
                .headers(headers())
                .body("[]").toPact();
    }

    @Pact(consumer = "FrontendApplication", provider = "ProductService")
    RequestResponsePact allProductsNoAuthToken(PactDslWithProvider builder) {
        return builder.given("products exist")
                .uponReceiving("get all products with no auth token")
                .method("GET")
                .path("/products")
                .willRespondWith()
                .status(401)
                .toPact();
    }

    @Pact(consumer = "FrontendApplication", provider = "ProductService")
    RequestResponsePact getOneProduct(PactDslWithProvider builder) {
        return builder.given("product with ID 10 exists")
                .uponReceiving("get product with ID 10")
                .method("GET").path("/product/10")
                .matchHeader("Authorization", "Bearer (19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0123]):[0-5][0-9]")
                .willRespondWith()
                .status(200)
                .headers(headers())
                .body(newJsonBody(object -> {
                    object.stringType("id", "10");
                    object.stringType("type", "CREDIT_CARD");
                    object.stringType("name", "28 Degrees");
                }).build())
                .toPact();
    }

    @Pact(consumer = "FrontendApplication", provider = "ProductService")
    RequestResponsePact productDoesNotExist(PactDslWithProvider builder) {
        return builder.given("product with ID 11 does not exist")
                .uponReceiving("get product with ID 11")
                .method("GET")
                .path("/product/11")
                .matchHeader("Authorization", "Bearer (19|20)\\d\\d-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0123]):[0-5][0-9]")
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Pact(consumer = "FrontendApplication", provider = "ProductService")
    RequestResponsePact singleProductnoAuthToken(PactDslWithProvider builder) {
        return builder.given("product with ID 10 exists")
                .uponReceiving("get product by ID 10 with no auth token")
                .method("GET")
                .path("/product/10")
                .willRespondWith()
                .status(401)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getAllProducts", pactVersion = PactSpecVersion.V3)
    void getAllProducts_whenProductsExist(MockServer mockServer, RequestResponsePact requestResponsePact) {
        Product product = new Product();
        product.setId("09");
        product.setType("CREDIT_CARD");
        product.setName("Gem Visa");
        List<Product> expected = Arrays.asList(product, product);

        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(mockServer.getUrl()).build();
        List<Product> products = new ProductService(restTemplate).getAllProducts();
        assertEquals(expected, products);

        File spec = new File("oas/schema.json");
        validateWithOAS(requestResponsePact, spec.getAbsolutePath());
    }

    @Test
    @PactTestFor(pactMethod = "noProductsExist", pactVersion = PactSpecVersion.V3)
    void getAllProducts_whenNoProductsExist(MockServer mockServer, RequestResponsePact requestResponsePact) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(mockServer.getUrl()).build();
        List<Product> products = new ProductService(restTemplate).getAllProducts();

        assertEquals(Collections.emptyList(), products);

        File spec = new File("oas/schema.json");
        validateWithOAS(requestResponsePact, spec.getAbsolutePath());
    }

    @Test
    @PactTestFor(pactMethod = "allProductsNoAuthToken", pactVersion = PactSpecVersion.V3)
    void getAllProducts_whenNoAuth(MockServer mockServer, RequestResponsePact requestResponsePact) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(mockServer.getUrl()).build();

        HttpClientErrorException e = assertThrows(HttpClientErrorException.class, () -> new ProductService(restTemplate).getAllProducts());
        assertEquals(401, e.getRawStatusCode());

        File spec = new File("oas/schema.json");
        validateWithOAS(requestResponsePact, spec.getAbsolutePath());
    }

    @Test
    @PactTestFor(pactMethod = "getOneProduct", pactVersion = PactSpecVersion.V3)
    void getProductById_whenProductWithId10Exists(MockServer mockServer, RequestResponsePact requestResponsePact) {
        Product expected = new Product();
        expected.setId("10");
        expected.setType("CREDIT_CARD");
        expected.setName("28 Degrees");

        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(mockServer.getUrl()).build();
        Product product = new ProductService(restTemplate).getProduct("10");

        assertEquals(expected, product);

        File spec = new File("oas/schema.json");
        validateWithOAS(requestResponsePact, spec.getAbsolutePath());
    }

    @Test
    @PactTestFor(pactMethod = "productDoesNotExist", pactVersion = PactSpecVersion.V3)
    void getProductById_whenProductWithId11DoesNotExist(MockServer mockServer, RequestResponsePact requestResponsePact) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(mockServer.getUrl()).build();

        HttpClientErrorException e = assertThrows(HttpClientErrorException.class, () -> new ProductService(restTemplate).getProduct("11"));
        assertEquals(404, e.getRawStatusCode());

        File spec = new File("oas/schema.json");
        validateWithOAS(requestResponsePact, spec.getAbsolutePath());
    }

    @Test
    @PactTestFor(pactMethod = "singleProductnoAuthToken", pactVersion = PactSpecVersion.V3)
    void getProductById_whenNoAuth(MockServer mockServer, RequestResponsePact requestResponsePact) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(mockServer.getUrl()).build();

        HttpClientErrorException e = assertThrows(HttpClientErrorException.class, () ->
                new ProductService(restTemplate).getProduct("10"));
        assertEquals(401, e.getRawStatusCode());

        File spec = new File("oas/schema.json");
        validateWithOAS(requestResponsePact, spec.getAbsolutePath());
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        return headers;
    }

    private void validateWithOAS(RequestResponsePact requestResponsePact, String specificationUrl) {

        RequestResponseInteraction interaction = (RequestResponseInteraction) requestResponsePact.getInteractions().stream().findFirst().orElseThrow();
        OpenApiInteractionValidator validator = OpenApiInteractionValidator.createForSpecificationUrl(specificationUrl).build();
        ValidationReport report = validator.validate(PactRequest.of(interaction.getRequest()), PactResponse.of(interaction.getResponse()));

        assertThat(report.hasErrors()).withFailMessage(report.getMessages().toString()).isFalse();
    }
}
