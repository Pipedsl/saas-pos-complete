package com.saaspos.api.controller;

import com.saaspos.api.model.InventoryLog;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.InventoryLogRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final InventoryLogRepository inventoryLogRepository;
    private final UserRepository userRepository;

    public ReportController(InventoryLogRepository inventoryLogRepository, UserRepository userRepository) {
        this.inventoryLogRepository = inventoryLogRepository;
        this.userRepository = userRepository;
    }

    // 1. Endpoint para la Tabla del Dashboard (JSON)
    // Permite al frontend "ver" los datos antes de descargar
    @GetMapping("/inventory-logs")
    public ResponseEntity<List<InventoryLog>> getInventoryLogsJson(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) UUID categoryId) {

        UUID tenantId = getCurrentTenantId();

        // Lógica de fechas (si no envían nada, mostramos el mes actual)
        LocalDateTime startDate = (start != null) ? LocalDateTime.parse(start) : LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endDate = (end != null) ? LocalDateTime.parse(end) : LocalDateTime.now();

        List<InventoryLog> logs;
        if (categoryId != null) {
            logs = inventoryLogRepository.findByCategoryAndDateRange(tenantId, categoryId, startDate, endDate);
        } else {
            logs = inventoryLogRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, startDate, endDate);
        }

        return ResponseEntity.ok(logs);
    }

    // 2. Endpoint para descargar el CSV (Excel)
    @GetMapping("/inventory-logs/csv")
    public void downloadCsv(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) UUID categoryId,
            HttpServletResponse response) throws IOException {

        UUID tenantId = getCurrentTenantId();

        LocalDateTime startDate = (start != null) ? LocalDateTime.parse(start) : LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endDate = (end != null) ? LocalDateTime.parse(end) : LocalDateTime.now();

        // Buscar Datos
        List<InventoryLog> logs;
        if (categoryId != null) {
            logs = inventoryLogRepository.findByCategoryAndDateRange(tenantId, categoryId, startDate, endDate);
        } else {
            logs = inventoryLogRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, startDate, endDate);
        }

        // Configurar cabeceras HTTP para descarga
        response.setContentType("text/csv");
        String filename = "historial_" + startDate.toLocalDate() + "_al_" + endDate.toLocalDate() + ".csv";
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // Escribir el archivo
        PrintWriter writer = response.getWriter();
        writer.println("Fecha,Hora,Producto,Usuario,Accion,Cambio,Stock Final,Motivo,Origen ID"); // Encabezado Excel

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        for (InventoryLog log : logs) {
            String origen = "";
            if (log.getSaleId() != null) origen = "Ticket POS: " + log.getSaleId();
            if (log.getWebOrderId() != null) origen = "Web Order: " + log.getWebOrderId();

            writer.println(
                    log.getCreatedAt().toLocalDate() + "," +
                            log.getCreatedAt().format(timeFmt) + "," +
                            escape(log.getProductNameSnapshot()) + "," +
                            escape(log.getUserNameSnapshot()) + "," +
                            log.getActionType() + "," +
                            log.getQuantityChange() + "," +
                            log.getNewStock() + "," +
                            escape(log.getReason()) + "," +
                            origen
            );
        }
    }

    // Helper para evitar romper el CSV si el texto tiene comas
    private String escape(String data) {
        if (data == null) return "";
        return "\"" + data.replace("\"", "\"\"") + "\"";
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return user.getTenant().getId();
    }
}