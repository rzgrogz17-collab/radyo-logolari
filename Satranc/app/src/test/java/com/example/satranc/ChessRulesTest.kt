package com.example.satranc

import com.example.satranc.chess.Chess
import com.example.satranc.chess.Puzzles
import com.example.satranc.chess.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessRulesTest {
    @Test
    fun startPositionPerft() {
        val position = Chess.start()
        assertEquals(20, Chess.perft(position, 1))
        assertEquals(400, Chess.perft(position, 2))
        assertEquals(8902, Chess.perft(position, 3))
    }

    @Test
    fun kiwipetePerft() {
        val position = Chess.parseFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        assertEquals(48, Chess.perft(position, 1))
        assertEquals(2039, Chess.perft(position, 2))
    }

    @Test
    fun scholarsMate() {
        val game = Chess.start()
        listOf("e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7").forEach { uci ->
            val move = Chess.parseUci(game, uci)
            assertTrue(move != null)
            Chess.make(game, move!!)
        }
        assertEquals(Status.CHECKMATE, Chess.status(game, 1))
    }

    @Test
    fun everyPuzzleHasAMate() {
        Puzzles.all.forEach { puzzle ->
            assertTrue(puzzle.title, Puzzles.mateMoves(puzzle.fen).isNotEmpty())
        }
    }
}
