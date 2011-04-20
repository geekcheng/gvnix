package org.springframework.roo.addon.entity;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;

/**
 * Provides a common set of operations for entities.
 * 
 * @author Alan Stewart
 * @since 1.1.2
 */
public interface EntityOperations {

	/**
	 * Checks for the existence the META-INF/persistence.xml
	 * 
	 * @return true if the META-INF/persistence.xml exists, otherwise false
	 */
	boolean isPersistentClassAvailable();

	/**
	 * Creates a new entity.
	 * 
	 * @param name the entity name (required)
	 * @param createAbstract indicates whether the entity will be an abstract class
	 * @param superclass the super class of the entity
	 * @param annotations the entity's annotations
	 */
	void newEntity(JavaType name, boolean createAbstract, JavaType superclass, List<AnnotationMetadataBuilder> annotations);

	/** 
	 * Creates a new JPA embeddable class.
	 * 
	 * @param name the name of the embeddable class (required)
	 * @param serializable whether the class implements {@link java.io.Serializable}
	 */
	void newEmbeddableClass(JavaType name, boolean serializable);
	
	/**
	 * Creates a new JPA identifier class.
	 * 
	 * @param identifierType the identifier type
	 * @param identifierField the identifier field name
	 * @param identifierColumn the identifier column name
	 */
	void newIdentifier(JavaType identifierType, String identifierField, String identifierColumn);
}