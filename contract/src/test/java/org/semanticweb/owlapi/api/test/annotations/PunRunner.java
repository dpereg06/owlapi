package org.semanticweb.owlapi.api.test.annotations;

/**
 * Created by ses on 3/2/15.
 */

import static org.junit.Assert.assertEquals;
import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.pairs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

@SuppressWarnings("javadoc")
public class PunRunner extends org.junit.runner.Runner {

    private final Class<?> testClass;
    private final Map<Description, TestSetting> testSettings = new HashMap<>();
    private Description suiteDescription;

    @SuppressWarnings("null")
    public PunRunner(Class<?> testClass) {
        this.testClass = testClass;
    }

    public static OWLOntology makeOwlOntologyWithDeclarationsAndAnnotationAssertions(
        OWLAnnotationProperty annotationProperty, OWLOntologyManager manager, OWLEntity... entities)
        throws OWLOntologyCreationException {
        Set<OWLAxiom> axioms = new HashSet<>();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        axioms.add(dataFactory.getOWLDeclarationAxiom(annotationProperty));
        for (OWLEntity entity : entities) {
            axioms.add(dataFactory
                .getOWLAnnotationAssertionAxiom(annotationProperty, entity.getIRI(), dataFactory
                    .getOWLAnonymousIndividual()));
            axioms.add(dataFactory.getOWLDeclarationAxiom(entity));
        }
        return manager.createOntology(axioms);
    }

    public static ByteArrayInputStream saveForRereading(OWLOntology o, PrefixDocumentFormat format,
        OWLOntologyManager manager) throws OWLOntologyStorageException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        manager.saveOntology(o, format, out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public Description getDescription() {
        suiteDescription = Description.createSuiteDescription(testClass);
        addAllTests();
        return suiteDescription;
    }

    private void addAllTests() {
        DefaultPrefixManager pm = new DefaultPrefixManager("http://localhost#");
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = m.getOWLDataFactory();
        List<? extends OWLEntity> entities = Arrays
            .asList(df.getOWLClass("a", pm), df.getOWLDatatype("a", pm), df
                    .getOWLAnnotationProperty("a", pm), df.getOWLDataProperty("a", pm),
                df.getOWLObjectProperty("a", pm), df
                    .getOWLNamedIndividual("a", pm));
        List<Class<? extends PrefixDocumentFormat>> formats = new ArrayList<>();
        formats.add(RDFXMLDocumentFormat.class);
        formats.add(TurtleDocumentFormat.class);
        formats.add(FunctionalSyntaxDocumentFormat.class);
        formats.add(ManchesterSyntaxDocumentFormat.class);
        for (Class<? extends PrefixDocumentFormat> formatClass : formats) {
            pairs(entities).forEach(v -> {
                String formatClassName = formatClass.getName();
                int i1 = formatClassName.lastIndexOf('.');
                if (i1 > -1) {
                    formatClassName = formatClassName.substring(i1 + 1);
                }
                String name = String.format("%sVs%sFor%s", v.i.getEntityType(), v.j.getEntityType(),
                    formatClassName);
                Description testDescription = Description.createTestDescription(testClass, name);
                testSettings.put(testDescription, new TestSetting(formatClass, m, v.i, v.j));
                suiteDescription.addChild(testDescription);
            });
            String name = "multiPun for " + formatClass.getName();
            Description testDescription = Description.createTestDescription(testClass, name);
            suiteDescription.addChild(testDescription);
            TestSetting setting = new TestSetting(formatClass, m, df.getOWLClass("a", pm),
                df.getOWLDatatype("a", pm),
                df.getOWLAnnotationProperty("a", pm), df.getOWLDataProperty("a", pm),
                df.getOWLObjectProperty("a", pm),
                df.getOWLNamedIndividual("a", pm));
            testSettings.put(testDescription, setting);
        }
    }

    /**
     * Run the tests for this runner.
     *
     * @param notifier will be notified of events while tests are being run--tests being started,
     * finishing, and failing
     */
    @Override
    public void run(@Nullable RunNotifier notifier) {
        checkNotNull(notifier);
        assert notifier != null;
        for (Map.Entry<Description, TestSetting> entry : testSettings.entrySet()) {
            Description description = entry.getKey();
            notifier.fireTestStarted(description);
            try {
                TestSetting setting = entry.getValue();
                runTestForAnnotationsOnPunnedEntitiesForFormat(setting.formatClass, setting.manager,
                    setting.entities);
            } catch (Throwable t) {
                notifier.fireTestFailure(new Failure(description, t));
            } finally {
                notifier.fireTestFinished(description);
            }
        }
    }

    public void runTestForAnnotationsOnPunnedEntitiesForFormat(
        Class<? extends PrefixDocumentFormat> formatClass,
        OWLOntologyManager m, OWLEntity... entities)
        throws OWLOntologyCreationException, OWLOntologyStorageException,
        IllegalAccessException, InstantiationException {
        OWLOntologyManager ontologyManager;
        OWLDataFactory df;
        synchronized (OWLManager.class) {
            ontologyManager = m;
            ontologyManager.clearOntologies();
            df = ontologyManager.getOWLDataFactory();
        }
        OWLAnnotationProperty annotationProperty = df
            .getOWLAnnotationProperty(":ap", new DefaultPrefixManager(
                "http://localhost#"));
        OWLOntology o = makeOwlOntologyWithDeclarationsAndAnnotationAssertions(annotationProperty,
            ontologyManager,
            entities);
        for (int i = 0; i < 10; i++) {
            PrefixDocumentFormat format = formatClass.newInstance();
            format.setPrefixManager(new DefaultPrefixManager("http://localhost#"));
            ByteArrayInputStream in = saveForRereading(o, format, ontologyManager);
            ontologyManager.removeOntology(o);
            o = ontologyManager.loadOntologyFromOntologyDocument(in);
        }
        assertEquals("annotationCount", entities.length,
            o.axioms(AxiomType.ANNOTATION_ASSERTION).count());
    }

    class TestSetting {

        OWLEntity[] entities;
        Class<? extends PrefixDocumentFormat> formatClass;
        OWLOntologyManager manager;

        public TestSetting(Class<? extends PrefixDocumentFormat> formatClass, OWLOntologyManager m,
            OWLEntity... entities) {
            this.formatClass = formatClass;
            this.entities = entities;
            manager = m;
        }
    }
}
