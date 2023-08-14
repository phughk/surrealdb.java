package com.surrealdb.refactor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.surrealdb.BaseIntegrationTest;
import com.surrealdb.refactor.driver.BidirectionalSurrealDB;
import com.surrealdb.refactor.driver.StatelessSurrealDB;
import com.surrealdb.refactor.driver.SurrealDBFactory;
import com.surrealdb.refactor.driver.UnauthenticatedSurrealDB;
import com.surrealdb.refactor.types.Credentials;
import com.surrealdb.refactor.types.Param;
import com.surrealdb.refactor.types.Value;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DemoScenarioTest extends BaseIntegrationTest {
    @Test
    public void testDemoScenario() throws Exception {
        // Setup
        URI address = getHttp().orElseThrow(() -> new IllegalStateException("No HTTP server configured"));
        UnauthenticatedSurrealDB<BidirectionalSurrealDB> unauthenticated = new SurrealDBFactory().connectBidirectional(address);

        // Authenticate
        BidirectionalSurrealDB surrealDB = unauthenticated.authenticate(new Credentials("admin", "admin"));

        // Create a multi-statement query
        StringBuilder query = new StringBuilder("INSERT person:lamport VALUES {'name': 'leslie'};\n");
        query.append("UPDATE $whichPerson SET year=$year;\n");
        query.append("DELETE person:lamport;");

        // Create the list of parameters used in the query
        List<Param> params = List.of(
            new Param("whichPerson", Value.fromJson(new JsonPrimitive("person:lamport"))),
            new Param("year", Value.fromJson(new JsonPrimitive(2013)))
        );

        // Execute the query
        List<Value> results = surrealDB.query(query.toString(), params);

        // Validate the results of the multi-statement query
        assertEquals(results.size(), 3);
        assertEquals(results.get(0).intoJson(), asJson(Tuple.of("name", new JsonPrimitive("leslie")), Tuple.of("id", new JsonPrimitive("person:lamport"))));
        assertEquals(results.get(1).intoJson(), asJson(Tuple.of("name", new JsonPrimitive("leslie")), Tuple.of("id", new JsonPrimitive("person:lamport"))));
    }


    //----------------------------------------------------------------
    // Helpers below this point

    private static JsonObject asJson(Tuple<String, JsonElement>... data) {
        JsonObject obj = new JsonObject();
        for (Tuple<String, JsonElement> entry: data) {
            obj.add(entry.key, entry.value);
        }
        return obj;
    }
}

