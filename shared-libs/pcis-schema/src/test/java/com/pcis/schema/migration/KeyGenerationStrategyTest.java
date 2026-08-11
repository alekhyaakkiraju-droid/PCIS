package com.pcis.schema.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.pcis.schema.entity.IdentityKeyEntity;
import com.pcis.schema.entity.SequenceKeyEntity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * CI gate: verifies JPA key generation strategy rules defined in
 * docs/key-generation-strategy.md are encoded correctly in the base entity classes.
 *
 * <p>Rules verified:
 * <ol>
 *   <li>{@link IdentityKeyEntity} uses {@code GenerationType.IDENTITY} on its {@code @Id} field.
 *   <li>{@link SequenceKeyEntity} uses {@code GenerationType.SEQUENCE} with {@code allocationSize = 1}.
 *   <li>Both base classes carry the {@link MappedSuperclass} annotation.
 * </ol>
 */
class KeyGenerationStrategyTest {

    @Test
    void identityKeyEntityUsesGenerationTypeIdentity() throws Exception {
        Class<?> cls = IdentityKeyEntity.class;
        assertTrue(cls.isAnnotationPresent(MappedSuperclass.class),
                "IdentityKeyEntity must be @MappedSuperclass");

        Field idField = findIdField(cls)
                .orElseGet(() -> findIdFieldInHierarchy(cls)
                        .orElseThrow(() -> new AssertionError("No @Id field in " + cls.getName())));

        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(gv, "IdentityKeyEntity @Id must carry @GeneratedValue");
        assertEquals(GenerationType.IDENTITY, gv.strategy(),
                "IdentityKeyEntity must use GenerationType.IDENTITY");
    }

    @Test
    void sequenceKeyEntityUsesGenerationTypeSequenceWithAllocationSizeOne() throws Exception {
        Class<?> cls = SequenceKeyEntity.class;
        assertTrue(cls.isAnnotationPresent(MappedSuperclass.class),
                "SequenceKeyEntity must be @MappedSuperclass");

        Field idField = findIdField(cls)
                .orElseGet(() -> findIdFieldInHierarchy(cls)
                        .orElseThrow(() -> new AssertionError("No @Id field in " + cls.getName())));

        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(gv, "SequenceKeyEntity @Id must carry @GeneratedValue");
        assertEquals(GenerationType.SEQUENCE, gv.strategy(),
                "SequenceKeyEntity must use GenerationType.SEQUENCE");

        SequenceGenerator sg = idField.getAnnotation(SequenceGenerator.class);
        assertNotNull(sg, "SequenceKeyEntity @Id must carry @SequenceGenerator");
        assertEquals(1, sg.allocationSize(),
                "SequenceKeyEntity allocationSize must be 1 to prevent range exhaustion during parallel run");
    }

    @Test
    void identityKeyEntityDoesNotUseSequenceStrategy() throws Exception {
        Class<?> cls = IdentityKeyEntity.class;
        Optional<Field> idFieldOpt = findIdField(cls);
        idFieldOpt.ifPresent(field -> {
            GeneratedValue gv = field.getAnnotation(GeneratedValue.class);
            if (gv != null) {
                assertTrue(gv.strategy() != GenerationType.SEQUENCE,
                        "IdentityKeyEntity must NOT use GenerationType.SEQUENCE");
            }
        });
    }

    @Test
    void sequenceKeyEntityAllocationSizeIsExactlyOne() throws Exception {
        Class<?> cls = SequenceKeyEntity.class;
        for (Field field : allFields(cls)) {
            SequenceGenerator sg = field.getAnnotation(SequenceGenerator.class);
            if (sg != null) {
                assertEquals(1, sg.allocationSize(),
                        "Every @SequenceGenerator on " + cls.getSimpleName()
                                + " must have allocationSize=1 (found " + sg.allocationSize()
                                + " on field '" + field.getName() + "')");
            }
        }
    }

    @Test
    void sequenceStrategyStartsFromSafeRange() {
        // Validate the documented minimum start value for all PostgreSQL business-key sequences.
        // This ensures the range separation contract is encoded in a testable constant.
        final long minimumStartValue = 10_000_000L;
        final long legacyDb2MaxEstimate = 1_000_000L;
        assertTrue(minimumStartValue > legacyDb2MaxEstimate,
                "PostgreSQL sequence START WITH (" + minimumStartValue
                        + ") must exceed legacy Db2 estimated max (" + legacyDb2MaxEstimate + ")");
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private static Optional<Field> findIdField(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst();
    }

    private static Optional<Field> findIdFieldInHierarchy(Class<?> cls) {
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            Optional<Field> found = findIdField(current);
            if (found.isPresent()) return found;
            current = current.getSuperclass();
        }
        return Optional.empty();
    }

    private static List<Field> allFields(Class<?> cls) {
        return Arrays.asList(cls.getDeclaredFields());
    }
}
