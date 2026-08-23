package com.ainexus.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationTest {

    @Test
    @DisplayName("TEST 1: application-prod.properties file exists and is parseable")
    void testProdPropertiesFileExists() throws IOException {
        ClassPathResource resource = new ClassPathResource("application-prod.properties");
        assertTrue(resource.exists(), "application-prod.properties must exist in classpath");

        Properties props = PropertiesLoaderUtils.loadProperties(resource);
        assertNotNull(props);
        assertFalse(props.isEmpty());
    }

    @Test
    @DisplayName("TEST 2: Verify production database and connection pool configuration keys")
    void testProdDatabaseConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("application-prod.properties");
        Properties props = PropertiesLoaderUtils.loadProperties(resource);

        assertEquals("${DB_URL:jdbc:postgresql://localhost:5432/ainexus_prod}", props.getProperty("spring.datasource.url"));
        assertEquals("${DB_POOL_MAX:20}", props.getProperty("spring.datasource.hikari.maximum-pool-size"));
        assertEquals("validate", props.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("false", props.getProperty("spring.jpa.show-sql"));
    }

    @Test
    @DisplayName("TEST 3: Verify production Gemini AI and Pinecone configuration keys")
    void testProdAiConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("application-prod.properties");
        Properties props = PropertiesLoaderUtils.loadProperties(resource);

        assertEquals("${GEMINI_API_KEY:}", props.getProperty("app.gemini.api-key"));
        assertEquals("${GEMINI_MODEL:gemini-1.5-flash}", props.getProperty("app.ai.gemini.generation-model"));
        assertEquals("${PINECONE_API_KEY:}", props.getProperty("app.pinecone.api-key"));
        assertEquals("${PINECONE_INDEX:ainexus-index}", props.getProperty("app.pinecone.index-name"));
    }

    @Test
    @DisplayName("TEST 4: Verify production agent and workflow limits")
    void testProdAgentAndWorkflowLimits() throws IOException {
        ClassPathResource resource = new ClassPathResource("application-prod.properties");
        Properties props = PropertiesLoaderUtils.loadProperties(resource);

        assertEquals("${AGENT_MAX_ITERATIONS:10}", props.getProperty("app.agent.max-iterations"));
        assertEquals("${AGENT_MAX_STEPS:8}", props.getProperty("app.agent.max-planning-steps"));
        assertEquals("${AGENT_MAX_REPLANS:3}", props.getProperty("app.agent.max-replanning-attempts"));
        assertEquals("${WORKFLOW_RETRY_LIMIT:3}", props.getProperty("app.workflow.retry-limit"));
    }
}
