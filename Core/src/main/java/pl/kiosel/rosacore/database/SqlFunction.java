package pl.kiosel.rosacore.database;

import java.sql.Connection;

@FunctionalInterface
public interface SqlFunction<T> {

    T execute(Connection connection) throws Exception;
}
