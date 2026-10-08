package com.example.satranc.chess

import kotlin.random.Random

enum class Difficulty(val depth: Int, val blunderRate: Double, val budgetMs: Long) {
    EASY(2, 0.45, 350),
    MEDIUM(3, 0.08, 800),
    HARD(4, 0.0, 1400)
}

/**
 * Alfa-beta araması. Kolay seviyede bilerek zayıf hamle seçer;
 * zor seviyede süre dolana kadar derinleşir.
 */
object Engine {
    private val value = intArrayOf(
        0, 100, 320, 330, 500, 900, 0,
        0, 0,
        100, 320, 330, 500, 900, 0
    )

    private val pawnPst = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0
    )

    private val knightPst = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    )

    fun choose(position: Position, difficulty: Difficulty, random: Random = Random.Default): Int {
        val moves = Chess.legal(position)
        if (moves.isEmpty()) return 0
        if (moves.size == 1) return moves.first()
        if (difficulty.blunderRate > 0 && random.nextDouble() < difficulty.blunderRate) {
            return moves[random.nextInt(moves.size)]
        }
        val started = System.nanoTime()
        var best = order(position, moves).first()
        var depth = 1
        while (depth <= difficulty.depth) {
            best = searchRoot(position, moves, depth)
            depth++
            val elapsed = (System.nanoTime() - started) / 1_000_000
            if (elapsed > difficulty.budgetMs) break
        }
        return best
    }

    private fun searchRoot(position: Position, moves: List<Int>, depth: Int): Int {
        var bestMove = moves.first()
        var bestScore = Int.MIN_VALUE
        var alpha = -40000
        val beta = 40000
        for (move in order(position, moves)) {
            val undo = Chess.make(position, move)
            val score = -negamax(position, depth - 1, -beta, -alpha)
            Chess.unmake(position, move, undo)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) alpha = score
        }
        return bestMove
    }

    private fun negamax(position: Position, depth: Int, alphaIn: Int, beta: Int): Int {
        if (depth == 0) return evaluate(position)
        val moves = Chess.legal(position)
        if (moves.isEmpty()) {
            return if (Chess.inCheck(position)) -20000 - depth else 0
        }
        var alpha = alphaIn
        var best = Int.MIN_VALUE / 2
        for (move in order(position, moves)) {
            val undo = Chess.make(position, move)
            val score = -negamax(position, depth - 1, -beta, -alpha)
            Chess.unmake(position, move, undo)
            if (score > best) best = score
            if (score > alpha) alpha = score
            if (alpha >= beta) break
        }
        return best
    }

    fun evaluate(position: Position): Int {
        var score = 0
        for (sq in 0 until 64) {
            val piece = position.board[sq]
            if (piece == 0) continue
            val kind = Chess.typeOf(piece)
            var pst = 0
            val mirror = sq xor 56
            if (kind == Chess.WP) pst = pawnPst[if (Chess.isWhite(piece)) mirror else sq]
            if (kind == Chess.WN) pst = knightPst[if (Chess.isWhite(piece)) mirror else sq]
            val delta = value[piece] + pst
            score += if (Chess.isWhite(piece)) delta else -delta
        }
        return if (position.white) score else -score
    }

    private fun order(position: Position, moves: List<Int>): List<Int> {
        if (moves.size < 2) return moves
        return moves.sortedByDescending { move ->
            val victim = position.board[Chess.to(move)]
            val attacker = position.board[Chess.from(move)]
            when {
                Chess.flag(move) == Chess.FLAG_CASTLE -> 40
                victim != 0 -> value[victim] * 10 - value[attacker]
                Chess.promo(move) != 0 -> 800
                else -> 0
            }
        }
    }
}
