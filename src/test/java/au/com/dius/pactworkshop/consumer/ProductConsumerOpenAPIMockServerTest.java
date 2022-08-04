package au.com.dius.pactworkshop.consumer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.MediaType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.mock.OpenAPIExpectation.openAPIExpectation;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductConsumerOpenAPIMockServerTest {


    private ProductService productService;

    @BeforeAll
    void setUp(ClientAndServer clientAndServer) {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost:" + clientAndServer.getPort())
                .build();

        productService = new ProductService(restTemplate);
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void getAllProducts(ClientAndServer clientAndServer) {
        clientAndServer
                .upsert(
                        openAPIExpectation(
                                "./../oas/schema.json"
                        )
                );
        List<Product> expected = Arrays.asList(new Product("9", "CREDIT_CARD", "GEM Visa", "v2"),
                new Product("10", "CREDIT_CARD", "28 Degrees", "v1"));
        List<Product> products = productService.getAllProducts();
        assertEquals(expected, products);
    }

    @Test
    void getProductById(ClientAndServer clientAndServer) {
        clientAndServer
                .upsert(
                        openAPIExpectation(
                                "./../oas/schema.json"
                        )
                );
        Product expected = new Product("50", "CREDIT_CARD", "28 Degrees", "v1");
        Product product = productService.getProduct("50");
        assertEquals(expected, product);
    }

    @Test
    void getAllProducts_whenNoProductsExist(ClientAndServer clientAndServer) {
        clientAndServer.reset().when(
                        request()
                                .withMethod("GET")
                                .withPath("/products")
                )
                .respond(
                        response()
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withStatusCode(200)
                                .withBody("[]")
                );
        List<Product> products = productService.getAllProducts();
        assertEquals(Collections.emptyList(), products);
    }
}
