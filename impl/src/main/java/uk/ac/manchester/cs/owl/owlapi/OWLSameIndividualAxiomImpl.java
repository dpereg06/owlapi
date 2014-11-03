/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package uk.ac.manchester.cs.owl.owlapi;

import static org.semanticweb.owlapi.util.CollectionFactory.createSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health
 *         Informatics Group
 * @since 2.0.0
 */
public class OWLSameIndividualAxiomImpl extends OWLNaryIndividualAxiomImpl
        implements OWLSameIndividualAxiom {

    private static final long serialVersionUID = 40000L;

    /**
     * @param individuals
     *        individuals
     * @param annotations
     *        annotations on the axiom
     */
    public OWLSameIndividualAxiomImpl(
            @Nonnull Set<? extends OWLIndividual> individuals,
            @Nonnull Collection<? extends OWLAnnotation> annotations) {
        super(individuals, annotations);
    }

    @Override
    public OWLSameIndividualAxiom getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new OWLSameIndividualAxiomImpl(getIndividuals(), NO_ANNOTATIONS);
    }

    @Override
    public OWLSameIndividualAxiom getAnnotatedAxiom(
            Collection<OWLAnnotation> annotations) {
        return new OWLSameIndividualAxiomImpl(getIndividuals(),
                mergeAnnos(annotations));
    }

    @Override
    public OWLSameIndividualAxiom getAnnotatedAxiom(
            Stream<OWLAnnotation> annotations) {
        return new OWLSameIndividualAxiomImpl(getIndividuals(),
                mergeAnnos(annotations));
    }

    @Override
    public Set<OWLSameIndividualAxiom> asPairwiseAxioms() {
        List<OWLIndividual> inds = getIndividualsAsList();
        Set<OWLSameIndividualAxiom> result = new HashSet<>();
        for (int i = 0; i < inds.size() - 1; i++) {
            OWLIndividual indI = inds.get(i);
            OWLIndividual indJ = inds.get(i + 1);
            result.add(new OWLSameIndividualAxiomImpl(new HashSet<>(Arrays
                    .asList(indI, indJ)), NO_ANNOTATIONS));
        }
        return result;
    }

    @Override
    public Set<OWLSameIndividualAxiom> splitToAnnotatedPairs() {
        List<OWLIndividual> individuals = getIndividualsAsList();
        if (individuals.size() == 2) {
            return Collections.singleton(this);
        }
        Set<OWLSameIndividualAxiom> result = new HashSet<>();
        for (int i = 0; i < individuals.size() - 1; i++) {
            OWLIndividual indI = individuals.get(i);
            OWLIndividual indJ = individuals.get(i + 1);
            result.add(new OWLSameIndividualAxiomImpl(new HashSet<>(Arrays
                    .asList(indI, indJ)), getAnnotations()));
        }
        return result;
    }

    @Override
    public boolean containsAnonymousIndividuals() {
        return getIndividuals().stream().anyMatch(i -> i.isAnonymous());
    }

    @Override
    public Set<OWLSubClassOfAxiom> asOWLSubClassOfAxioms() {
        List<OWLClassExpression> nominalsList = new ArrayList<>();
        getIndividuals().forEach(
                i -> nominalsList.add(new OWLObjectOneOfImpl(createSet(i))));
        Set<OWLSubClassOfAxiom> result = new HashSet<>();
        for (int i = 0; i < nominalsList.size() - 1; i++) {
            OWLClassExpression ceI = nominalsList.get(i);
            OWLClassExpression ceJ = nominalsList.get(i + 1);
            result.add(new OWLSubClassOfAxiomImpl(ceI, ceJ, NO_ANNOTATIONS));
            result.add(new OWLSubClassOfAxiomImpl(ceJ, ceI, NO_ANNOTATIONS));
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof OWLSameIndividualAxiom;
    }

    @Override
    public AxiomType<?> getAxiomType() {
        return AxiomType.SAME_INDIVIDUAL;
    }
}
