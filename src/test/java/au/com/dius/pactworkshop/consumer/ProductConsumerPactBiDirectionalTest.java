package au.com.dius.pactworkshop.consumer;

import com.atlassian.ta.wiremockpactgenerator.WireMockPactGenerator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductConsumerPactBiDirectionalTest {

    private ProductService productService;

    private WireMockServer wireMockServer;

    @BeforeAll
    void configureWiremockPactGenerator() {
        String provider = System.getenv().getOrDefault("PACT_PROVIDER", "pactflow-example-bi-directional-provider-restassured");

        wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
        wireMockServer.start();

        wireMockServer.addMockServiceRequestListener(
                WireMockPactGenerator
                        .builder("pactflow-example-bi-directional-consumer-wiremock", provider)
                        .build());

        RestTemplate restTemplate = new RestTemplateBuilder()
                //.rootUri("http://localhost:" + wireMockServer.port())
                .rootUri(wireMockServer.baseUrl())
                .build();

        productService = new ProductService(restTemplate);
    }


    @AfterAll
    void closeWireMock() {
        wireMockServer.stop();
    }

    @Test
    void getProduct() throws IOException {

        // Arrange
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/product/10"))
                .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{ \"name\": \"pizza\", \"id\": \"10\", \"type\": \"food\" }")));

        // Act
        Product product = productService.getProduct("10");

        // Assert
        assertThat(product.getId(), is("10"));
    }

    @Test
    void getProducts() throws IOException {

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/products"))
                .willReturn(aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[{ \"name\": \"pizza\", \"id\": \"10\", \"type\": \"food\", \"version\":  \"100\" }]")));

        List<Product> products = productService.getAllProducts();
        assertThat(products.get(0).getId(), is("10"));
    }
}