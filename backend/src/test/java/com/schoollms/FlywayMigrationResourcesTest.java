package com.schoollms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlywayMigrationResourcesTest {
  private static final List<String> MIGRATIONS =
      List.of(
          "/db/migration/V1__initial_schema.sql",
          "/db/migration/V2__timetable_leave_and_bus.sql",
          "/db/migration/V3__explicit_event_holidays.sql",
          "/db/migration/V4__leave_reviewer_name.sql");

  @Test
  void completeOrderedMigrationSetIsPackagedOnTheRuntimeClasspath() throws IOException {
    for (String migration : MIGRATIONS) {
      try (var stream = getClass().getResourceAsStream(migration)) {
        assertTrue(stream != null, () -> "Missing packaged migration " + migration);
        assertTrue(stream.readAllBytes().length > 0, () -> "Empty migration " + migration);
      }
    }
  }

  @Test
  void baselineCreatesEveryJpaEntityTableAndDoesNotDropData() throws IOException {
    String baseline = resource(MIGRATIONS.getFirst());
    List<String> entityTables =
        List.of(
            "accounts",
            "assessment_results",
            "attendance",
            "bus_routes",
            "class_timetable",
            "fee_records",
            "homework",
            "leave_applications",
            "notifications",
            "school_events",
            "school_messages",
            "students",
            "teacher_classes");

    for (String table : entityTables) {
      boolean inBaseline =
          baseline.matches(
              "(?s).*CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?"
                  + table
                  + "`?\\s*\\(.*");
      boolean createdByForwardMigration =
          table.equals("class_timetable") && resource(MIGRATIONS.get(1)).contains("`class_timetable`");
      assertTrue(inBaseline || createdByForwardMigration, () -> "No migration creates " + table);
    }

    String allMigrations = String.join("\n", MIGRATIONS.stream().map(this::resourceUnchecked).toList());
    assertFalse(allMigrations.matches("(?is).*\\bDROP\\s+(TABLE|DATABASE|SCHEMA)\\b.*"));
    assertFalse(allMigrations.matches("(?is).*\\bTRUNCATE\\b.*"));
  }

  private String resource(String path) throws IOException {
    try (var stream = getClass().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IOException("Missing migration " + path);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private String resourceUnchecked(String path) {
    try {
      return resource(path);
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
