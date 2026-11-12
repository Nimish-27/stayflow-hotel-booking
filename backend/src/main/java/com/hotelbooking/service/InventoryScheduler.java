package com.hotelbooking.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers the inventory top-up: once at startup, then every night just after midnight. */
@Component
public class InventoryScheduler implements ApplicationRunner {

    private static final String DAILY_AT_00_05 = "0 5 0 * * *";

    private final InventorySeeder inventorySeeder;

    public InventoryScheduler(InventorySeeder inventorySeeder) {
        this.inventorySeeder = inventorySeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        inventorySeeder.seedMissingInventory();
    }

    @Scheduled(cron = DAILY_AT_00_05, zone = "${app.timezone}")
    public void dailyTopUp() {
        inventorySeeder.seedMissingInventory();
    }
}
