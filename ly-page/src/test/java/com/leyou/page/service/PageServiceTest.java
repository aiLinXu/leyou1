package com.leyou.page.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PageServiceTest {


    @Autowired
    private PageService pageService;

    @Test
    public void createItemHtml() throws InterruptedException {
        List<Long> ids = Arrays.asList(114L, 141L, 96L, 145L, 124L, 76L);
        for (Long id : ids) {
            pageService.createItemHtml(id);
            Thread.sleep(200);
        }
    }
}