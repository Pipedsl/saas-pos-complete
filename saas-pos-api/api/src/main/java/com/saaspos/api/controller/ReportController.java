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
import java.time.ZoneId; // <--- Importante
import java.time.ZonedDateTime; // <--- Importante
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
    @GetMapping("/inventory-logs")
    public ResponseEntity<List<InventoryLog>> getInventoryLogsJson(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) UUID categoryId) {

        UUID tenantId = getCurrentTenantId();

        // FIX: Usamos el método helper para parsear fechas con 'Z'
        LocalDateTime startDate = parseDate(start, true);
        LocalDateTime endDate = parseDate(end, false);

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

        // FIX: Usamos el mismo helper aquí
        LocalDateTime startDate = parseDate(start, true);
        LocalDateTime endDate = parseDate(end, false);

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

    // --- NUEVO HELPER PARA PARSEAR FECHAS DE FORMA SEGURA ---
    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        ZoneId chileZone = ZoneId.of("America/Santiago");

        if (dateStr == null) {
            // Default: Principio o fin del mes actual en Chile
            LocalDateTime now = LocalDateTime.now(chileZone);
            return isStart
                    ? now.withDayOfMonth(1).toLocalDate().atStartOfDay()
                    : now;
        }

        try {
            // Intento 1: Formato UTC con 'Z' (El que manda Angular: 2026-01-06T03:00:00.000Z)
            // Lo convertimos a la hora de Chile
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
                    .withZoneSameInstant(chileZone)
                    .toLocalDateTime();
        } catch (Exception e) {
            // Intento 2: Fallback por si llega sin zona (2026-01-06T00:00:00)
            return LocalDateTime.parse(dateStr);
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