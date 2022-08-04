package au.com.dius.pactworkshop.consumer;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureStubRunner(ids = "au.com.dius.pactworkshop:provider:+:stubs:8085", stubsMode = StubRunnerProperties.StubsMode.LOCAL)
class ProductConsumerSCCTest {

    @Autowired
    private ProductService productService;

    @Test
    void getProduct() {
        Product product = productService.getProduct("10");
        assertThat(product.getId(), is("10"));
    }

    @Test
    void getProducts() {

        List<Product> products = productService.getAllProducts();
        assertThat(products.get(0).getId(), is("09"));
    }

    @Test
    void getProduct404() {
        HttpClientErrorException e = assertThrows(HttpClientErrorException.class,
                () -> productService.getProduct("11"));
        assertEquals(404, e.getRawStatusCode());
    }

}