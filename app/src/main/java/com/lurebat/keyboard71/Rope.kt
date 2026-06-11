package com.lurebat.keyboard71

class RopeNode(val value: CharSequence = "") {
    var left: RopeNode? = null
    var right: RopeNode? = null
    // weight = total character count in this subtree
    var weight: Int = value.length

    val isLeaf: Boolean get() = left == null && right == null
}

class Rope {
    private var root: RopeNode? = null

    fun length(): Int = root?.weight ?: 0

    private fun join(left: RopeNode?, right: RopeNode?): RopeNode? {
        if (left == null) return right
        if (right == null) return left
        val n = RopeNode()
        n.left = left
        n.right = right
        n.weight = left.weight + right.weight
        return n
    }

    fun concatenate(rope: Rope) {
        root = join(root, rope.root)
    }

    fun insert(index: Int, value: CharSequence) {
        if (value.isEmpty()) return
        val clampedIndex = index.coerceIn(0, length())
        val (left, right) = split(root, clampedIndex)
        root = join(join(left, RopeNode(value)), right)
    }

    fun delete(start: Int, end: Int) {
        if (start >= end) return
        val (left, rest) = split(root, start)
        val (_, right) = split(rest, end - start)
        root = join(left, right)
    }

    /** Non-destructive range read: collects leaf text in [start, end) without mutating root. */
    fun get(start: Int, end: Int): CharSequence? {
        if (start >= end || root == null) return null
        val clampedStart = maxOf(start, 0)
        val clampedEnd = minOf(end, length())
        if (clampedStart >= clampedEnd) return null
        val sb = StringBuilder()
        collectLeaves(root, 0, clampedStart, clampedEnd, sb)
        return if (sb.isEmpty()) null else sb
    }

    private fun collectLeaves(
        node: RopeNode?,
        nodeStart: Int,
        start: Int,
        end: Int,
        sb: StringBuilder
    ) {
        if (node == null) return
        val nodeEnd = nodeStart + node.weight
        if (nodeEnd <= start || nodeStart >= end) return
        if (node.isLeaf) {
            val from = maxOf(start - nodeStart, 0)
            val to = minOf(end - nodeStart, node.value.length)
            if (from < to) sb.append(node.value, from, to)
            return
        }
        val leftWeight = node.left?.weight ?: 0
        collectLeaves(node.left, nodeStart, start, end, sb)
        collectLeaves(node.right, nodeStart + leftWeight, start, end, sb)
    }

    fun report(index: Int): Char? {
        var node = root ?: return null
        var i = index
        while (true) {
            if (node.isLeaf) {
                return if (i in node.value.indices) node.value[i] else null
            }
            val leftWeight = node.left?.weight ?: 0
            node = if (i < leftWeight) {
                node.left ?: return null
            } else {
                i -= leftWeight
                node.right ?: return null
            }
        }
    }

    /**
     * Splits the subtree at [index]: returns (everything in [0, index), everything in [index, end)).
     * Mutates the nodes passed in — callers must not reuse [node] after this call.
     */
    private fun split(node: RopeNode?, index: Int): Pair<RopeNode?, RopeNode?> {
        if (node == null) return Pair(null, null)
        if (index <= 0) return Pair(null, node)
        if (index >= node.weight) return Pair(node, null)
        if (node.isLeaf) {
            val leftVal = node.value.subSequence(0, index)
            val rightVal = node.value.subSequence(index, node.value.length)
            return Pair(RopeNode(leftVal), RopeNode(rightVal))
        }
        val leftWeight = node.left?.weight ?: 0
        return if (index <= leftWeight) {
            val (ll, lr) = split(node.left, index)
            node.left = lr
            node.weight = (lr?.weight ?: 0) + (node.right?.weight ?: 0)
            Pair(ll, node)
        } else {
            val (rl, rr) = split(node.right, index - leftWeight)
            node.right = rl
            node.weight = leftWeight + (rl?.weight ?: 0)
            Pair(node, rr)
        }
    }
}
