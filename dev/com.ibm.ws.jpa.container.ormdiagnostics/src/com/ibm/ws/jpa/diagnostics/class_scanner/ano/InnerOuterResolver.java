/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

import java.util.HashSet;
import java.util.List;

import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInfoType;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.InnerClassesType;

public class InnerOuterResolver {
    private final HashSet<UnresolvedInnerClassReference> innerClassesToResolveSet = new HashSet<UnresolvedInnerClassReference>();
    private final HashSet<UnresolvedOuterClassReference> outerClassesToResolveSet = new HashSet<UnresolvedOuterClassReference>();
    
    public InnerOuterResolver() {
        
    }
    
    public void addUnresolvedInnerClassReference(ClassInfoType outerClass, String unresolvedInnerClass) {
        UnresolvedInnerClassReference uicr = new UnresolvedInnerClassReference(outerClass, unresolvedInnerClass);
        innerClassesToResolveSet.add(uicr);
    }
    
    public void addUnresolvedOuterClassReference(ClassInfoType innerClass, String unresolvedOuterClass) {
        UnresolvedOuterClassReference uocr = new UnresolvedOuterClassReference(innerClass, unresolvedOuterClass);
        outerClassesToResolveSet.add(uocr);
    }
    
    public void resolve(List<ClassInfoType> classList) {
//        System.out.println("Size of innerClassesToResolveSet = " + innerClassesToResolveSet.size());
        for (UnresolvedInnerClassReference uicr : innerClassesToResolveSet) {
            final ClassInfoType outerClass = uicr.getOuterClass();
            final String innerClassName = uicr.getUnresolvedInnerClass();
            
            for (ClassInfoType cit : classList) {
                if (cit.getClassName().equals(innerClassName)) {
                    InnerClassesType ict = outerClass.getInnerclasses();
                    if (ict == null || ict.getInnerclass().size() == 0) {
                        continue;
                    }
                    
                    ClassInfoType removeTarget = null;
                    for (ClassInfoType innerCit : ict.getInnerclass()) {
                        if (innerCit.getClassName().equals(innerClassName)) {
                            removeTarget = innerCit;
                            break;
                        }
                    }
                    
                    if (removeTarget == null) {
                        continue;
                    }
                    ict.getInnerclass().remove(removeTarget);
                    ict.getInnerclass().add(cit);
                    
                    break;
                }
            }
            
//            System.out.println(uicr);
            
        }
//        System.out.println();
//        System.out.println("Size of outerClassesToResolveSet = " + outerClassesToResolveSet.size());
//        for (UnresolvedOuterClassReference uocr : outerClassesToResolveSet) {
//            System.out.println(uocr);
//        }
    }
    
    private class UnresolvedInnerClassReference {
        private ClassInfoType outerClass;
        private String unresolvedInnerClass;
        
        public UnresolvedInnerClassReference(ClassInfoType outerClass, String unresolvedInnerClass) {
            super();
            this.outerClass = outerClass;
            this.unresolvedInnerClass = unresolvedInnerClass;
        }

        public ClassInfoType getOuterClass() {
            return outerClass;
        }

        public String getUnresolvedInnerClass() {
            return unresolvedInnerClass;
        }

        @Override
        public String toString() {
            return "UnresolvedInnerClassReference [outerClass=" + outerClass.getClassName() + ", unresolvedInnerClass="
                    + unresolvedInnerClass + "]";
        }
        
        
    }
    
    private class UnresolvedOuterClassReference {
        private ClassInfoType innerClass;
        private String unresolvedOuterClass;
        
        public UnresolvedOuterClassReference(ClassInfoType innerClass, String unresolvedOuterClass) {
            super();
            this.innerClass = innerClass;
            this.unresolvedOuterClass = unresolvedOuterClass;
        }

        public ClassInfoType getInnerClass() {
            return innerClass;
        }

        public String getUnresolvedOuterClass() {
            return unresolvedOuterClass;
        }

        @Override
        public String toString() {
            return "UnresolvedOuterClassReference [innerClass=" + innerClass.getClassName() + ", unresolvedOuterClass="
                    + unresolvedOuterClass + "]";
        }
        
        
    }
}
