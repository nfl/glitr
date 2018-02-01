package com.nfl.glitr.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the query complexity multiplier which is participated in query complexity calculation.
 * <p>
 * Supports the mathematical formula as a value. e.g: value = "2 + 2 * 2" = 6.
 * <p>
 * As well, formula supports the variables within of formula. e.g: value = "(#{depth} + #{childScore}) * 10".
 * <p>
 * The list of possible variables:
 * <ul>
 * <li>#{depth} = current depth of annotated node. e.g:
 * <pre>{@code
 * {
 *     player {              depth = 1
 *         playerId          depth = 2
 *     }
 * }
 * }</pre>
 * </li>
 * <li>#{childScore} - score aff all child nodes. e.g:
 * <pre>{@code
 * {
 *     person {              childScore = 3
 *         player {          childScore = 2
 *             season {      childScore = 0
 *                 id
 *             }
 *             team {        childScore = 0
 *                 id
 *             }
 *         }
 *     }
 * }
 * }</pre>
 * </li>
 * <li>#{currentCollectionSize} - the size of collection represented by annotated node or 0 in case it is not a collection.
 * The size of collection is based on the 'first' argument value. e.g:
 * <pre>{@code
 * {
 *     teams(first: 2) {              currentCollectionSize = 2
 *         players(first: 3) {        currentCollectionSize = 3
 *             person {               currentCollectionSize = 0
 *                 id
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * </li>
 * <li>#{totalCollectionsSize} - the size of all parent collections + current one or 0 in case there is no parent collections and current node is not a collection.
 * The size of collection is based on the 'first' argument value. e.g:
 * <pre>{@code
 * {
 *   season {                          totalCollectionsSize = 0
 *      teams(first: 2) {              totalCollectionsSize = 2
 *             players(first: 3) {     totalCollectionsSize = 5
 *                 person {            totalCollectionsSize = 5
 *                     id
 *              }
 *          }
 *      }
 *   }
 * }
 * }</pre>
 * </li>
 * <li>Also, complexity formula supports the set of predefined global variables: #{maxCharacterLimit}, #{maxDepthLimit}, #{maxScoreLimit}, #{defaultMultiplier}</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface GlitrQueryComplexity {
    String value();
}