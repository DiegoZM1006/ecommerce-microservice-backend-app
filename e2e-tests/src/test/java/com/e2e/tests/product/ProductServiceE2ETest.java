package com.e2e.tests.product;

import com.e2e.tests.util.E2ESuite;
import com.e2e.tests.util.TestRestFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = E2ESuite.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ProductServiceE2ETest extends E2ESuite {

    //@Autowired
    //private TestRestTemplate testRestTemplate;
    @Autowired
    private TestRestFacade restFacade; // Autowire your facade

    @Value("${product.service.url}") // This property is set by E2ESuite.Initializer
    private String productServiceUrl;

//    @Value("${user.service.url}") // This property is set by E2ESuite.Initializer
//    private String userServiceUrl;

    @Test
    void shouldGetAllProducts() {
        ResponseEntity<String> response = restFacade.get(productServiceUrl + "/product-service/api/categories", String.class);
//        System.out.println("Response: " + response.getBody());
//        System.out.println("Status Code: " + response.getStatusCode());
        assertTrue(response.getStatusCode().is2xxSuccessful(), "Unexpected status code: " + response.getStatusCode());
    }
}
