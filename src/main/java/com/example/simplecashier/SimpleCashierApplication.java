package com.example.simplecashier;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class SimpleCashierApplication {

    private final Map<String, Double> balances = new HashMap<>();
    private final List<String> transactions = new ArrayList<>();
    private final Counter depositCounter;
    private final Counter withdrawCounter;
    private final Counter totalAccountsCounter;

    public SimpleCashierApplication(MeterRegistry meterRegistry) {
        this.depositCounter = Counter.builder("app_deposits_total")
                .description("Total de depósitos")
                .register(meterRegistry);
        this.withdrawCounter = Counter.builder("app_withdrawals_total")
                .description("Total de levantamentos")
                .register(meterRegistry);
        this.totalAccountsCounter = Counter.builder("app_total_accounts")
                .description("Total de contas.")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        totalAccountsCounter.increment(balances.size());
    }

    public static void main(String[] args) {
        SpringApplication.run(SimpleCashierApplication.class, args);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Aplicação saudável!");
    }

    @GetMapping("/balance/{id}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String id) {
        Double balance = balances.getOrDefault(id, 0.0);
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("balance", balance);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit/{id}")
    public ResponseEntity<Map<String, Object>> deposit(@PathVariable String id, @RequestParam double amount) {
        if (amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valor deve ser positivo"));
        }

        double currentBalance = balances.getOrDefault(id, 0.0);
        double newBalance = currentBalance + amount;
        balances.put(id, newBalance);

        String transaction = "Depósito de " + amount + " na conta " + id;
        transactions.add(transaction);
        depositCounter.increment();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Depósito realizado com sucesso");
        response.put("currentBalance", newBalance);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw/{id}")
    public ResponseEntity<Map<String, Object>> withdraw(@PathVariable String id, @RequestParam double amount) {
        if (amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valor deve ser positivo"));
        }

        double currentBalance = balances.getOrDefault(id, 0.0);
        if (currentBalance < amount) {
            return ResponseEntity.badRequest().body(Map.of("error", "Saldo insuficiente"));
        }

        double newBalance = currentBalance - amount;
        balances.put(id, newBalance);

        String transaction = "Levantamento de " + amount + " da conta " + id;
        transactions.add(transaction);
        withdrawCounter.increment();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Levantamento realizado com sucesso");
        response.put("currentBalance", newBalance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<String>> getTransactions() {
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/metrics")

    public ResponseEntity<Map<String, Object>> getMetrics() {

        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalAccounts", balances.size());

        metrics.put("totalTransactions", transactions.size());

        metrics.put("systemStatus", "OK");

        return ResponseEntity.ok(metrics);

    }
}