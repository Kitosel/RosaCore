package pl.kiosel.rosacore.database;

import java.sql.Connection;

@FunctionalInterface
public interface SqlAction {

    void execute(Connection connection) throws Exception;
}
