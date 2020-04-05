/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.dynamic.compile;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.ElideConfigParser;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHydrator;
import com.google.common.collect.Sets;

import org.mdkt.compiler.InMemoryJavaCompiler;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class ElideDynamicEntityCompiler {

    public static ArrayList<String> classNames = new ArrayList<String>();

    public static final String PACKAGE_NAME = "com.yahoo.elide.contrib.dynamicconfig.model.";
    private Map<String, Class<?>> compiledObjects;

    InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();

    HandlebarsHydrator hydrator = new HandlebarsHydrator();

    ElideConfigParser elideConfigParser = new ElideConfigParser();

    ElideTableConfig tableConfig = new ElideTableConfig();

    ElideSecurityConfig securityConfig = new ElideSecurityConfig();
    Map<String, String> tableClasses = new HashMap<String, String>();
    Map<String, String> securityClasses = new HashMap<String, String>();

    public ElideDynamicEntityCompiler(String path) {

        try {

            elideConfigParser.parseConfigPath(path);

            tableConfig = elideConfigParser.getElideTableConfig();
            securityConfig = elideConfigParser.getElideSecurityConfig();
            tableClasses = hydrator.hydrateTableTemplate(tableConfig);
            securityClasses = hydrator.hydrateSecurityTemplate(securityConfig);

            for (Entry<String, String> entry : tableClasses.entrySet()) {
                classNames.add(PACKAGE_NAME + entry.getKey());
            }

            for (Entry<String, String> entry : securityClasses.entrySet()) {
                classNames.add(PACKAGE_NAME + entry.getKey());
            }

            compiler.useParentClassLoader(
                    new ElideDynamicInMemoryClassLoader(ClassLoader.getSystemClassLoader(),
                            Sets.newHashSet(classNames)));

        } catch (NullPointerException | IOException e) {
            log.error("Unable to read Dynamic Configuration " + e.getMessage());
        }
    }

    public void compile(String path) throws Exception {

        for (Map.Entry<String, String> tablePojo : tableClasses.entrySet()) {
            log.info("key: " + tablePojo.getKey() + ", value: " + tablePojo.getValue());
            compiler.addSource(PACKAGE_NAME + tablePojo.getKey(), tablePojo.getValue());
        }

        for (Map.Entry<String, String> secPojo : securityClasses.entrySet()) {
            log.info("key: " + secPojo.getKey() + ", value: " + secPojo.getValue());
            compiler.addSource(PACKAGE_NAME + secPojo.getKey(), secPojo.getValue());
        }

        compiledObjects = compiler.compileAll();

    }

    public ClassLoader getClassLoader() {
        return compiler.getClassloader();
    }

    public Class<?> getCompiled(String name) {
        return compiledObjects.get(name);
    }
}
