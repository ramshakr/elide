/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.tests;

import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.query;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL;
import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.spring.controllers.JsonApiController;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import javax.ws.rs.core.MediaType;

/**
 * Example functional test.
 */
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        statements = "INSERT INTO PlayerStats (name,countryCode,playerCountry,createdOn) VALUES\n"
                + "\t\t('SerenaWilliams','USA','United States','2000-10-01');")
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        statements = "DELETE FROM PlayerStats; DELETE FROM Player;")
public class DynamicConfigTest extends IntegrationTest {
    /**
     * This test demonstrates an example test using the JSON-API DSL.
     * @throws InterruptedException
     */
    @Test
    public void jsonApiGetTest() throws InterruptedException {

        when()
                .get("/json/playerStats")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("playerStats"),
                                        id("SerenaWilliams"),
                                        attributes(
                                                attr("countryCode", "USA"),
                                                attr("createdOn", "2000-10-01T04:00Z"),
                                                attr("highScore", null),
                                                attr("playerCountry", "United States")
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
    }
    @Test
    public void jsonApiPatchTest() {
        given()
            .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
            .body(
                datum(
                    resource(
                        type("playerStats"),
                        id("SerenaWilliams"),
                        attributes(
                            attr("playerCountry", "United States of America")
                        )
                    )
                )
            )
            .when()
                .patch("/json/playerStats/SerenaWilliams")
            .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        when()
                .get("/json/playerStats")
                .then()
                .body(equalTo(
                        data(
                                resource(
                                        type("playerStats"),
                                        id("SerenaWilliams"),
                                        attributes(
                                                attr("countryCode", "USA"),
                                                attr("createdOn", "2000-10-01T04:00Z"),
                                                attr("highScore", null),
                                                attr("playerCountry", "United States of America")
                                        )
                                )
                        ).toJSON())
                )
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void jsonApiPostTest() {
        given()
                .contentType(JsonApiController.JSON_API_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("playerStats"),
                                        id("SaniaMirza"),
                                        attributes(
                                                attr("countryCode", "IND"),
                                                attr("createdOn", "2002-03-01T04:00Z"),
                                                attr("highScore", null),
                                                attr("playerCountry", "India")
                                        )
                                )
                        )
                )
                .when()
                .post("/json/playerStats")
                .then()
                .body(equalTo(datum(
                        resource(
                                type("playerStats"),
                                id("SaniaMirza"),
                                attributes(
                                        attr("countryCode", "IND"),
                                        attr("createdOn", "2002-03-01T04:00Z"),
                                        attr("highScore", null),
                                        attr("playerCountry", "India")
                                )
                        )
                ).toJSON()))
                .statusCode(HttpStatus.SC_CREATED);
    }

    @Test
    public void jsonApiDeleteTest() {
        when()
            .delete("/json/playerStats/SerenaWilliams")
        .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * This test demonstrates an example test using the GraphQL DSL.
     */
    @Test
    public void graphqlTest() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body("{ \"query\" : \"" + GraphQLDSL.document(
                query(
                    selection(
                        field("playerStats",
                            selections(
                                field("countryCode"),
                                field("createdOn"),
                                field("playerCountry")
                            )
                        )
                    )
                )
            ).toQuery() + "\" }"
        )
        .when()
            .post("/graphql")
            .then()
            .body(equalTo(GraphQLDSL.document(
                selection(
                    field(
                        "playerStats",
                        selections(
                            field("countryCode", "USA"),
                            field("createdOn", "2000-10-01T04:00Z"),
                            field("playerCountry", "United States")
                        )
                    )
                )
            ).toResponse()))
            .statusCode(HttpStatus.SC_OK);
    }
}
