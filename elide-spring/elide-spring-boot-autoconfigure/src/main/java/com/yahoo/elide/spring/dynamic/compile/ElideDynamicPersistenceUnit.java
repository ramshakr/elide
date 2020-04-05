/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.dynamic.compile;

import lombok.Data;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

@Data
public class ElideDynamicPersistenceUnit implements PersistenceUnitInfo {

    public ElideDynamicPersistenceUnit(String persistenceUnitName,
    List<String> managedClassNames, Properties properties, ClassLoader loader) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
        this.classLoader = loader;
        this.newTempClassLoader = loader;
    }

    private String persistenceUnitName;
    private String persistenceProviderClassName;
    private PersistenceUnitTransactionType transactionType;
    private DataSource jtaDataSource;
    private DataSource nonJtaDataSource;
    private List<String> mappingFileNames;
    private List<URL> jarFileUrls;
    private URL persistenceUnitRootUrl;
    private List<String> managedClassNames;
    private SharedCacheMode sharedCacheMode;
    private ValidationMode validationMode;
    private Properties properties;
    private String persistenceXMLSchemaVersion;
    private ClassLoader classLoader;
    private ClassLoader newTempClassLoader;

    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    public void addTransformer(ClassTransformer classTransformer) { }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public List<String> getManagedClassNames() {
        return managedClassNames;
    }
}
