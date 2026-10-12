package app.cyanskip.selector.relation

import app.cyanskip.selector.MatchContext
import app.cyanskip.selector.NodeAdapter
import app.cyanskip.selector.TraversalCandidate

internal class RelationSelector(
    val operator: RelationOperator = RelationOperator.Ancestor,
    val relationExpression: RelationExpression = PolynomialExpression(),
) {
    fun <T : Any> traversal(
        context: MatchContext<T>,
        adapter: NodeAdapter<T>,
    ): Sequence<TraversalCandidate<T>> {
        return operator.traversal(context, adapter, relationExpression)
    }

    val isMatchAnyAncestor =
        operator == RelationOperator.Ancestor && relationExpression.matchesAllOffsets

    val isMatchAnyDescendant =
        operator == RelationOperator.Descendant && relationExpression.matchesAllOffsets
}
