package id.my.agungdh.api.cli;

import id.my.agungdh.api.ApiApplication;
import org.flywaydb.core.Flyway;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class FlywayCli {

    public static void main(String[] args) {
        String command = args.length > 0 ? args[0] : "migrate";

        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ApiApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        try {
            Flyway flyway = ctx.getBean(Flyway.class);
            javax.sql.DataSource dataSource = ctx.getBean(javax.sql.DataSource.class);

            switch (command) {
                case "migrate" -> {
                    System.out.println("Running Flyway migrate...");
                    flyway.migrate();
                    System.out.println("Migrate complete.");
                }
                case "refresh" -> {
                    System.out.println("Dropping and recreating schema...");
                    try (var conn = dataSource.getConnection();
                         var stmt = conn.createStatement()) {
                        stmt.execute("DROP SCHEMA public CASCADE");
                        stmt.execute("CREATE SCHEMA public");
                    }
                    System.out.println("Running Flyway migrate...");
                    flyway.migrate();
                    System.out.println("Refresh complete.");
                }
                default -> {
                    System.err.println("Unknown command: " + command);
                    System.err.println("Usage: make migrate | make refresh | make help");
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            ctx.close();
        }
    }
}
