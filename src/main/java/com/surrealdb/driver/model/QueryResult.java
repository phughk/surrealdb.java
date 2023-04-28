package com.surrealdb.driver.model;

import java.util.List;
import lombok.Data;

/**
 * @author Khalid Alharisi
 */
@Data
public class QueryResult<T> {
    private List<T> result;
    private String status;
    private String time;
}
