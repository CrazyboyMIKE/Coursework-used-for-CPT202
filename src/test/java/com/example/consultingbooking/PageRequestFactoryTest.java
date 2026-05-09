package com.example.consultingbooking;

import com.example.consultingbooking.controller.PageRequestFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PageRequestFactoryTest {

    @Test
    void normalizesPageAndSizeWithoutChangingSort() {
        PageRequest request = PageRequestFactory.of(
                -5,
                999,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Assertions.assertEquals(0, request.getPageNumber());
        Assertions.assertEquals(50, request.getPageSize());
        Assertions.assertEquals(Sort.Direction.DESC, request.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void keepsValidPageAndSize() {
        PageRequest request = PageRequestFactory.of(
                2,
                8,
                Sort.by(Sort.Direction.ASC, "fullName")
        );

        Assertions.assertEquals(2, request.getPageNumber());
        Assertions.assertEquals(8, request.getPageSize());
        Assertions.assertEquals(Sort.Direction.ASC, request.getSort().getOrderFor("fullName").getDirection());
    }
}
