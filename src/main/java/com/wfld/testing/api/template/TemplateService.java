package com.wfld.testing.api.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating JSON messages from Velocity templates.
 * Templates are cached for performance after first load.
 */
@Slf4j
public class TemplateService {
    private static TemplateService instance;
    private final VelocityEngine velocityEngine;
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private TemplateService() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        velocityEngine.setProperty("resource.loader.classpath.class", ClasspathResourceLoader.class.getName());
        velocityEngine.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, false);
        velocityEngine.init();
        log.info("TemplateService initialized");
    }

    /**
     * Get the singleton instance of TemplateService
     *
     * @return TemplateService instance
     */
    public static synchronized TemplateService getInstance() {
        if (instance == null) {
            instance = new TemplateService();
        }
        return instance;
    }

    /**
     * Renders a Velocity template with the provided context variables.
     * Templates are loaded from the classpath (typically src/main/resources or src/test/resources).
     * Templates are cached after first load for performance.
     *
     * @param templatePath path to the template file (e.g., "templates/user-create.json.vm")
     * @param context     map of variables to use in the template
     * @return rendered template as a string
     * @throws TemplateException if template cannot be loaded or rendered
     */
    public String render(String templatePath, Map<String, Object> context) {
        if (templatePath == null || templatePath.trim().isEmpty()) {
            throw new TemplateException("Template path cannot be null or empty");
        }

        try {
            Template template = getTemplate(templatePath);
            VelocityContext velocityContext = createVelocityContext(context);
            
            StringWriter writer = new StringWriter();
            template.merge(velocityContext, writer);
            String result = writer.toString();
            
            log.debug("Successfully rendered template: {}", templatePath);
            return result;
        } catch (Exception e) {
            log.error("Error rendering template: {}", templatePath, e);
            throw new TemplateException("Failed to render template: " + templatePath, e);
        }
    }

    /**
     * Renders a Velocity template with the provided context variables.
     * Convenience method that accepts varargs for simple key-value pairs.
     *
     * @param templatePath path to the template file
     * @param context      varargs of key-value pairs (must be even number of arguments)
     * @return rendered template as a string
     * @throws TemplateException if template cannot be loaded or rendered
     */
    public String render(String templatePath, Object... context) {
        if (context.length % 2 != 0) {
            throw new IllegalArgumentException("Context must have even number of arguments (key-value pairs)");
        }
        
        Map<String, Object> contextMap = new java.util.HashMap<>();
        for (int i = 0; i < context.length; i += 2) {
            if (!(context[i] instanceof String)) {
                throw new IllegalArgumentException("Context keys must be strings");
            }
            contextMap.put((String) context[i], context[i + 1]);
        }
        
        return render(templatePath, contextMap);
    }

    /**
     * Renders a Velocity template using data loaded from a JSON file.
     * The JSON file is loaded from the classpath, parsed, and used as context for the main template.
     * 
     * This is useful when you want to:
     * - Store test data in JSON files with arrays of values
     * - Reuse test data across multiple templates
     * - Separate test data from template structure
     *
     * @param templatePath    path to the main template file (e.g., "templates/user-create.json.vm")
     * @param dataFilePath    path to the JSON data file (e.g., "templates/user-data.json")
     * @param additionalContext optional map of additional variables to merge with the JSON data (takes precedence over JSON data)
     * @return rendered main template as a string
     * @throws TemplateException if template or data file cannot be loaded, or if data file is not valid JSON
     */
    public String render(String templatePath, String dataFilePath, Map<String, Object> additionalContext) {
        if (dataFilePath == null || dataFilePath.trim().isEmpty()) {
            throw new TemplateException("Data file path cannot be null or empty");
        }

        try {
            // Load the JSON data file from classpath
            log.debug("Loading data file: {} from classpath", dataFilePath);
            InputStream dataStream = getClass().getClassLoader().getResourceAsStream(dataFilePath);
            if (dataStream == null) {
                throw new TemplateException("Data file not found on classpath: " + dataFilePath);
            }
            
            // Parse the JSON file into a Map
            log.debug("Parsing data file as JSON");
            Map<String, Object> contextFromData = objectMapper.readValue(
                dataStream, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            // Merge the parsed context with any additional context (additionalContext takes precedence)
            Map<String, Object> mergedContext = new java.util.HashMap<>(contextFromData);
            if (additionalContext != null) {
                mergedContext.putAll(additionalContext);
            }
            
            // Now render the main template with the merged context
            log.debug("Rendering main template: {} with data from file", templatePath);
            return render(templatePath, mergedContext);
        } catch (java.io.IOException e) {
            log.error("Failed to load or parse data file: {}", dataFilePath, e);
            throw new TemplateException("Failed to load or parse data file: " + dataFilePath, e);
        } catch (Exception e) {
            log.error("Error rendering template with data file: {}", templatePath, e);
            throw new TemplateException("Failed to render template with data file: " + templatePath, e);
        }
    }

    /**
     * Renders a Velocity template using data loaded from a JSON file.
     * Convenience method that accepts varargs for additional context variables.
     *
     * @param templatePath     path to the main template file
     * @param dataFilePath     path to the JSON data file
     * @param additionalContext varargs of key-value pairs to merge with JSON data (must be even number of arguments)
     * @return rendered main template as a string
     * @throws TemplateException if template or data file cannot be loaded
     */
    public String render(String templatePath, String dataFilePath, Object... additionalContext) {
        if (additionalContext.length % 2 != 0) {
            throw new IllegalArgumentException("Additional context must have even number of arguments (key-value pairs)");
        }
        
        Map<String, Object> contextMap = new java.util.HashMap<>();
        for (int i = 0; i < additionalContext.length; i += 2) {
            if (!(additionalContext[i] instanceof String)) {
                throw new IllegalArgumentException("Additional context keys must be strings");
            }
            contextMap.put((String) additionalContext[i], additionalContext[i + 1]);
        }
        
        return render(templatePath, dataFilePath, contextMap);
    }

    /**
     * Gets a template, loading it from cache if available, or loading and caching it if not.
     *
     * @param templatePath path to the template file
     * @return Velocity Template instance
     */
    private Template getTemplate(String templatePath) {
        return templateCache.computeIfAbsent(templatePath, path -> {
            try {
                log.debug("Loading template: {}", path);
                Template template = velocityEngine.getTemplate(path, "UTF-8");
                log.debug("Template loaded successfully: {}", path);
                return template;
            } catch (Exception e) {
                log.error("Failed to load template: {}", path, e);
                throw new TemplateException("Template not found or cannot be loaded: " + path, e);
            }
        });
    }

    /**
     * Creates a VelocityContext from a Map of variables
     *
     * @param context map of variables
     * @return VelocityContext
     */
    private VelocityContext createVelocityContext(Map<String, Object> context) {
        VelocityContext velocityContext = new VelocityContext();
        if (context != null) {
            context.forEach(velocityContext::put);
        }
        return velocityContext;
    }

    /**
     * Clears the template cache, forcing templates to be reloaded on next use.
     * Useful for testing or when templates are updated at runtime.
     */
    public void clearCache() {
        log.info("Clearing template cache");
        templateCache.clear();
    }

    /**
     * Removes a specific template from the cache.
     *
     * @param templatePath path to the template to remove from cache
     */
    public void clearCache(String templatePath) {
        if (templateCache.remove(templatePath) != null) {
            log.debug("Removed template from cache: {}", templatePath);
        }
    }

    /**
     * Gets the number of templates currently cached.
     *
     * @return number of cached templates
     */
    public int getCacheSize() {
        return templateCache.size();
    }

    /**
     * Loads a CSV file from the classpath and parses it into a Map.
     * The first row is treated as headers (column names), and the specified data row
     * is parsed into a Map with header names as keys.
     *
     * @param csvFilePath path to the CSV file (e.g., "templates/user-data.csv")
     * @param rowIndex    zero-based index of the data row to parse (0 = first data row after header)
     * @return Map with column headers as keys and row values as values
     * @throws TemplateException if CSV file cannot be loaded or parsed
     */
    public Map<String, String> loadCsvAsMap(String csvFilePath, int rowIndex) {
        if (csvFilePath == null || csvFilePath.trim().isEmpty()) {
            throw new TemplateException("CSV file path cannot be null or empty");
        }

        try {
            log.debug("Loading CSV file: {} from classpath", csvFilePath);
            InputStream csvStream = getClass().getClassLoader().getResourceAsStream(csvFilePath);
            if (csvStream == null) {
                throw new TemplateException("CSV file not found on classpath: " + csvFilePath);
            }

            List<Map<String, String>> allRows = parseCsv(csvStream);
            
            if (allRows.isEmpty()) {
                throw new TemplateException("CSV file is empty: " + csvFilePath);
            }
            
            if (rowIndex < 0 || rowIndex >= allRows.size()) {
                throw new TemplateException(
                    String.format("Row index %d is out of bounds. CSV has %d data row(s)", 
                        rowIndex, allRows.size()));
            }

            log.debug("Successfully loaded CSV row {} from {}", rowIndex, csvFilePath);
            return allRows.get(rowIndex);
        } catch (TemplateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to load or parse CSV file: {}", csvFilePath, e);
            throw new TemplateException("Failed to load or parse CSV file: " + csvFilePath, e);
        }
    }

    /**
     * Loads a CSV file from the classpath and parses the first data row into a Map.
     * Convenience method that uses the first data row (rowIndex = 0).
     *
     * @param csvFilePath path to the CSV file (e.g., "templates/user-data.csv")
     * @return Map with column headers as keys and first row values as values
     * @throws TemplateException if CSV file cannot be loaded or parsed
     */
    public Map<String, String> loadCsvAsMap(String csvFilePath) {
        return loadCsvAsMap(csvFilePath, 0);
    }

    /**
     * Loads a CSV file from the classpath and parses all data rows into a List of Maps.
     *
     * @param csvFilePath path to the CSV file (e.g., "templates/user-data.csv")
     * @return List of Maps, where each Map represents a data row with headers as keys
     * @throws TemplateException if CSV file cannot be loaded or parsed
     */
    public List<Map<String, String>> loadCsvAsList(String csvFilePath) {
        if (csvFilePath == null || csvFilePath.trim().isEmpty()) {
            throw new TemplateException("CSV file path cannot be null or empty");
        }

        try {
            log.debug("Loading CSV file: {} from classpath", csvFilePath);
            InputStream csvStream = getClass().getClassLoader().getResourceAsStream(csvFilePath);
            if (csvStream == null) {
                throw new TemplateException("CSV file not found on classpath: " + csvFilePath);
            }

            List<Map<String, String>> allRows = parseCsv(csvStream);
            log.debug("Successfully loaded {} row(s) from CSV file: {}", allRows.size(), csvFilePath);
            return allRows;
        } catch (TemplateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to load or parse CSV file: {}", csvFilePath, e);
            throw new TemplateException("Failed to load or parse CSV file: " + csvFilePath, e);
        }
    }

    /**
     * Parses a CSV input stream into a List of Maps.
     * First line is treated as headers, subsequent lines as data rows.
     *
     * @param csvStream input stream of the CSV file
     * @return List of Maps, where each Map represents a data row
     */
    private List<Map<String, String>> parseCsv(InputStream csvStream) throws Exception {
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> headers = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);

                if (lineNumber == 0) {
                    // First non-empty line is headers
                    headers = values;
                } else {
                    // Subsequent lines are data rows
                    if (headers == null) {
                        throw new TemplateException("CSV file must have a header row");
                    }

                    if (values.size() != headers.size()) {
                        log.warn("Row {} has {} columns but header has {} columns. " +
                                "Missing values will be empty strings.", 
                                lineNumber, values.size(), headers.size());
                        
                        // Pad with empty strings if row has fewer columns
                        while (values.size() < headers.size()) {
                            values.add("");
                        }
                        // Truncate if row has more columns
                        if (values.size() > headers.size()) {
                            values = values.subList(0, headers.size());
                        }
                    }

                    Map<String, String> rowMap = new HashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        rowMap.put(headers.get(i), i < values.size() ? values.get(i) : "");
                    }
                    rows.add(rowMap);
                }

                lineNumber++;
            }
        }

        return rows;
    }

    /**
     * Parses a single CSV line, handling quoted fields and escaped quotes.
     * Simple CSV parser that handles:
     * - Quoted fields: "value, with comma"
     * - Escaped quotes: "value with ""quotes"""
     * - Unquoted fields: simple,value
     *
     * @param line CSV line to parse
     * @return List of field values
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // Field separator
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }

        // Add last field
        fields.add(currentField.toString().trim());

        return fields;
    }
}

