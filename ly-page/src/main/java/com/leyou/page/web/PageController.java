package com.leyou.page.web;

import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author 虎哥
 */
@Controller
public class PageController {

    @Autowired
    private PageService pageService;

    @GetMapping("item/{id}.html")
    public String toItemHtml(@PathVariable("id") Long spuId, Model model){
        // 查询商品数据
        Map<String, Object> itemData = pageService.loadItemData(spuId);
        // 添加模型数据
        model.addAllAttributes(itemData);
        // 返回视图名称
        return "item";
    }

    @GetMapping("hello/{msg}")
    public String hello(Model model, @PathVariable("msg") String msg) {
        // 添加模型数据
        model.addAttribute("msg", "Hi," + msg);
        // 返回视图名称
        return "hello";
    }
}
