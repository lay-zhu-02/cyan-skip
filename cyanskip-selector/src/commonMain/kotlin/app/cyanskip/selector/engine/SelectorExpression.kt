package app.cyanskip.selector.engine

import app.cyanskip.selector.LogicalOperator
import app.cyanskip.selector.property.PropertySelector
import app.cyanskip.selector.relation.RelationSelector

internal sealed class SelectorExpression

internal class UnitSelectorExpression(
    val propertySelectors: List<PropertySelector>,
    val relations: List<RelationSelector>,
) : SelectorExpression()

internal class LogicalSelectorExpression(
    val left: SelectorExpression,
    val operator: LogicalOperator,
    val right: SelectorExpression,
) : SelectorExpression()

internal class NotSelectorExpression(
    val expression: SelectorExpression,
) : SelectorExpression()
