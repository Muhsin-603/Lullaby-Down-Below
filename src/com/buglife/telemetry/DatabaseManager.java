package com.buglife.telemetry;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buglife.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * DatabaseManager — Connection pool manager for the telemetry vault.
 * 
 * Uses HikariCP for high-performance JDBC connection pooling.
 * Reads configuration from ConfigManager with the "telemetry.db." prefix.
 * 
 * Configuration keys:
 *   telemetry.db.url       — JDBC URL (e.g., jdbc:mysql://localhost:3306/lullaby_telemetry)
 *   telemetry.db.username  — Database username
 *   telemetry.db.password  — Database password
 *   telemetry.db.pool.size — Maximum pool size (default: 5)
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static DatabaseManager instance;

    private HikariDataSource dataSource;
    private boolean available;

    // ====================================================================
    // CONSTRUCTION
    // ====================================================================

    private DatabaseManager() {
        try {
            ConfigManager config = ConfigManager.getInstance();

            String url = config.getString("telemetry.db.url", "jdbc:mysql://localhost:3306/lullaby_telemetry");
            String username = config.getString("telemetry.db.username", "root");
            String password = config.getString("telemetry.db.password", "");
            int poolSize = config.getInt("telemetry.db.pool.size", 5);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setIdleTimeout(60_000);           // 60 seconds
            hikariConfig.setConnectionTimeout(5_000);      // 5 seconds — fail fast
            hikariConfig.setMaxLifetime(300_000);           // 5 minutes
            hikariConfig.setPoolName("Telemetry-Pool");

            // MySQL-specific optimisations
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "64");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "512");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

            this.dataSource = new HikariDataSource(hikariConfig);

            // Validate connectivity with a quick test
            try (Connection conn = dataSource.getConnection()) {
                available = conn.isValid(2);
            }

            if (available) {
                logger.info("DatabaseManager: Connection pool initialised — url={}", url);
            } else {
                logger.warn("DatabaseManager: Pool created but connection validation failed");
            }

        } catch (Exception e) {
            logger.error("DatabaseManager: Failed to initialise connection pool: {}", e.getMessage());
            this.available = false;
            this.dataSource = null;
        }
    }

    // ====================================================================
    // SINGLETON ACCESS
    // ====================================================================

    /**
     * Get the singleton DatabaseManager instance.
     * Creates the pool on first access.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ====================================================================
    // PUBLIC API
    // ====================================================================

    /**
     * Is the database reachable and the pool healthy?
     * 
     * @return true if at least one connection can be obtained
     */
    public boolean isAvailable() {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }
        return available;
    }

    /**
     * Borrow a connection from the pool.
     * Caller is responsible for closing it (returns it to pool).
     * 
     * @return a live JDBC Connection
     * @throws SQLException if the pool is closed or a connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DatabaseManager: Connection pool is not available");
        }
        return dataSource.getConnection();
    }

    // ====================================================================
    // LIFECYCLE
    // ====================================================================

    /**
     * Gracefully shut down the connection pool.
     * All idle connections are closed immediately; active connections are
     * closed as they are returned to the pool.
     * 
     * Safe to call multiple times.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("DatabaseManager: Shutting down connection pool");
            dataSource.close();
            available = false;
        }
        instance = null;
    }
}
