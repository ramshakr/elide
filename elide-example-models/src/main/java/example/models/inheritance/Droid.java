/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.inheritance;

import com.yahoo.elide.annotation.Include;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Include(rootLevel = true)
@DiscriminatorValue("Droid")
@Entity
public class Droid extends Character {
    private String primaryFunction;
}
