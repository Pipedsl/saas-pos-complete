package com.saaspos.api.controller;

import com.saaspos.api.model.Category;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.CategoryRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryController(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    // GET: Listar mis categorías
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(categoryRepository.findByTenantIdAndIsActiveTrue(tenantId));
    }

    // POST: Crear nueva categoría
    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        UUID tenantId = getCurrentTenantId();

        category.setTenantId(tenantId);
        category.setActive(true);

        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(saved);
    }

    // Helper privado (Duplicado por ahora, luego lo refactorizaremos a una clase base)
    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (user.getTenant() == null) throw new RuntimeException("Sin Tenant");
        return user.getTenant().getId();
    }
}