package com.sancanji.mealsapi.controller;

import com.sancanji.mealsapi.dto.ApiResponse;
import com.sancanji.mealsapi.entity.Category;
import com.sancanji.mealsapi.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<Category>> getCategories() {
        return ApiResponse.success(categoryService.getAllCategories());
    }
}