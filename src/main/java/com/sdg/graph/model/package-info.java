/**
 * This package provides data models for nodes in a knowledge graph. These models are used
 * when converting nodes into JSON format for LLM processing. The models represent software elements
 * like classes, methods, and their relationships. The JSON conversion is handled by using
 * Jackson library in the GraphDataToJsonConverter class
 *
 * @see com.sdg.graph.model.ClassNode
 * @see com.sdg.graph.model.MethodNode
 * @see com.sdg.graph.model.MethodCallNode
 * @see com.sdg.graph.model.ClassFieldNode
 * @see com.sdg.graph.model.ControlFlowNode
 * @see com.sdg.graph.model.SystemStructure
 *
 * @see com.sdg.graph.GraphDatabaseOperations
 * @see com.sdg.graph.CypherConstants
 */
package com.sdg.graph.model;