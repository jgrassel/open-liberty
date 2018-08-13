/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.04.12 at 04:16:16 PM CDT
//

package com.ibm.ws.jpa.diagnostics.ormparser.jaxb.orm20xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IMapKeyClass;

/**
 *
 *
 * @Target({METHOD, FIELD}) @Retention(RUNTIME)
 *                  public @interface MapKeyClass {
 *                  Class value();
 *                  }
 *
 * 
 *
 *                  <p>Java class for map-key-class complex type.
 *
 *                  <p>The following schema fragment specifies the expected content contained within this class.
 *
 *                  <pre>
 * &lt;complexType name="map-key-class">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="class" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 *                  </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "map-key-class")
public class MapKeyClass implements IMapKeyClass {

    @XmlAttribute(name = "class", required = true)
    protected String clazz;

    /**
     * Gets the value of the clazz property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * Sets the value of the clazz property.
     *
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setClazz(String value) {
        this.clazz = value;
    }

}
