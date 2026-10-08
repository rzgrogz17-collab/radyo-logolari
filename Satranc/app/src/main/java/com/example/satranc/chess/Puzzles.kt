package com.example.satranc.chess

data class Puzzle(
    val title: String,
    val fen: String
)

object Puzzles {
    val all = listOf(
        Puzzle(
            "Çoban matı",
            "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
        ),
        Puzzle(
            "Aptal matı",
            "rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2"
        ),
        Puzzle(
            "Arka sıra",
            "6k1/5ppp/8/8/8/8/8/4R2K w - - 0 1"
        ),
        Puzzle(
            "Vezir matı",
            "7k/8/6K1/8/3Q4/8/8/8 w - - 0 1"
        ),
        Puzzle(
            "Kale koridoru",
            "k7/8/1K6/8/8/8/8/7R w - - 0 1"
        ),
        Puzzle(
            "Şah ve kale",
            "7k/R7/6K1/8/8/8/8/8 w - - 0 1"
        ),
        Puzzle(
            "Atın matı",
            "6rk/6pp/8/6N1/8/8/8/6K1 w - - 0 1"
        ),
        Puzzle(
            "Köşe matı",
            "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1"
        )
    )

    fun mateMoves(fen: String): List<Int> {
        val position = Chess.parseFen(fen)
        val mates = ArrayList<Int>()
        for (move in Chess.legal(position)) {
            val undo = Chess.make(position, move)
            if (Chess.status(position, 1) == Status.CHECKMATE) mates.add(move)
            Chess.unmake(position, move, undo)
        }
        return mates
    }
}
