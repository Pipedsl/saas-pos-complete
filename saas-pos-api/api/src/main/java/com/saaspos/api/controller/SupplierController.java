package com.saaspos.api.controller;

import com.saaspos.api.model.Category;
import com.saaspos.api.model.Supplier;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.SupplierRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    public SupplierController(SupplierRepository supplierRepository, UserRepository userRepository) {
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
    }

    // GET: Listar mis proveedores
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        UUID tenantId = getCurrentTenantId();
        return ResponseEntity.ok(supplierRepository.findByTenantId(tenantId));
    }

    // POST: Crear nuevo proveedor
    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        UUID tenantId = getCurrentTenantId();

        supplier.setTenantId(tenantId);
        // Validaciones extra (como RUT único por tenant) podrían ir aquí

        Supplier saved = supplierRepository.save(supplier);
        return ResponseEntity.ok(saved);
    }

    // Helper privado para seguridad
    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (user.getTenant() == null) throw new RuntimeException("Usuario sin Tenant asignado");
        return user.getTenant().getId();
    }
}
