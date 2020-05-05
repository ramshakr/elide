/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.inheritance;

import static com.yahoo.elide.Elide.JSONAPI_CONTENT_TYPE;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.document;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.field;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selection;
import static com.yahoo.elide.contrib.testhelpers.graphql.GraphQLDSL.selections;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.datum;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.linkage;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relation;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.relationships;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.IntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.MediaType;

@Slf4j
public class InheritanceIT extends IntegrationTest {

    @BeforeEach
    public void createCharacters() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("droid"),
                                        id("C3P0"),
                                        attributes(
                                                attr("primaryFunction", "protocol droid")
                                        )
                                )
                        )
                )
                .post("/droid")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("C3P0"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("hero"),
                                        id("Luke Skywalker"),
                                        attributes(
                                                attr("forceSensitive", true)
                                        )
                                )
                        )
                )
                .post("/hero")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("Luke Skywalker"));
    }

    @Test
    public void testEmployeeHierarchy() {

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("manager"),
                                        id(null)
                                )
                        )
                )
                .post("/manager")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"));

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .accept(JSONAPI_CONTENT_TYPE)
                .body(
                        datum(
                                resource(
                                        type("employee"),
                                        id(null),
                                        attributes(),
                                        relationships(
                                                relation("boss",
                                                        linkage(type("manager"), id("1"))
                                                )
                                        )
                                )
                        )
                )
                .post("/manager/1/minions")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .body("data.id", equalTo("1"),
                        "data.relationships.boss.data.id", equalTo("1")
                );

        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/manager/1")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body("data.id", equalTo("1"),
                        "data.relationships.minions.data.id", contains("1"),
                        "data.relationships.minions.data.type", contains("employee")
                );
    }

    @Test
    public void testJsonApiCharacterHierarchy() {
        given()
                .contentType(JSONAPI_CONTENT_TYPE)
                .when()
                .get("/character")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body(equalTo(data(
                        resource(
                                type("droid"),
                                id("C3P0"),
                                attributes(
                                        attr("primaryFunction", "protocol droid")
                                )
                        ),
                        resource(
                                type("hero"),
                                id("Luke Skywalker"),
                                attributes(
                                        attr("forceSensitive", true)
                                )
                        )
                ).toJSON()));
    }

    @Test
    public void testGraphQLCharacterHierarchy() {

        String query = document(
                selection(
                        field(
                                "character",
                                selections(
                                        field("name")
                                )
                        )
                )
        ).toQuery();

        String envelope = "{ \"query\" : \"%s\" }";
        String formatted = String.format(envelope, query);

        String expected = document(
                selections(
                        field(
                                "character",
                                selections(
                                        field("name", "C3P0")
                                ),
                                selections(
                                        field("name", "Luke Skywalker")
                                )
                        )
                )
        ).toResponse();

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(formatted)
                .post("/graphQL")
                .then()
                .statusCode(org.apache.http.HttpStatus.SC_OK)
                .body(equalTo(expected));
    }

    @Test
    public void testGraphQLDroidFragment() {

        String query = "{ character { edges { node { __typename " +
                "... on Character { name } __typename " +
                "... on Droid { primaryFunction }}}}}";

        String envelope = "{ \"query\" : \"%s\" }";
        String formatted = String.format(envelope, query);

        String expected = document(
                selections(
                        field(
                                "character",
                                selections(
                                        field("__typename", "Droid"),
                                        field("name", "C3P0"),
                                        field("primaryFunction", "protocol droid")
                                ),
                                selections(
                                        field("__typename", "Hero"),
                                        field("name", "Luke Skywalker")
                                )
                        )
                )
        ).toResponse();

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(formatted)
                .post("/graphQL")
                .then()
                .body(equalTo(expected))
                .statusCode(org.apache.http.HttpStatus.SC_OK);
    }
}
