package com.lurebat.keyboard71

class RopeNode(val value: CharSequence) {
    var left: RopeNode? = null
    var right: RopeNode? = null
    var weight = value.length
}

class Rope {
    private var root: RopeNode? = null

    fun length(): Int = root?.weight ?: 0

    fun concatenate(rope: Rope) {
        val newRoot = RopeNode("")
        newRoot.left = root
        newRoot.right = rope.root
        newRoot.weight = root?.weight ?: 0
        root = newRoot
    }

    fun insert(index: Int, value: CharSequence) {
        val split = split(root, index)
        val newNode = RopeNode(value)
        newNode.left = split.first
        newNode.right = split.second
        newNode.weight = split.first?.weight ?: 0
        root = newNode
    }

    fun delete(start: Int, end: Int) {
        val rightSplit = split(root, end)
        val leftSplit = split(rightSplit.first, start)
        val newNode = RopeNode("")
        newNode.left = leftSplit.first
        newNode.right = rightSplit.second
        newNode.weight = leftSplit.first?.weight ?: 0
        root = newNode
    }

    fun get(start: Int, end: Int): CharSequence? {
        val rightSplit = split(root, end)
        val leftSplit = split(rightSplit.first, start)
        return leftSplit.second?.value
    }

    fun report(index: Int): Char? {
        var node = root
        var i = index
        while (node != null) {
            if (node.left != null && node.weight > i) {
                node = node.left
            } else {
                i -= node.weight
                node = node.right
            }
            if (node != null && i < 0) { // check if index is negative
                return node.value[i + node.weight] // adjust index by adding weight
            }
        }
        return null
    }


    private fun split(node: RopeNode?, index: Int): Pair<RopeNode?, RopeNode?> {
        if (node == null) return Pair(null, null)
        return if (node.left != null && node.weight > index) {
            val split = split(node.left, index)
            node.left = split.second
            node.weight -= split.second?.weight ?: 0
            Pair(split.first, node)
        } else {
            val i = index - (node.left?.weight ?: 0)
            val split = split(node.right, i)
            node.right = split.first
            Pair(node, split.second)
        }
    }
}
