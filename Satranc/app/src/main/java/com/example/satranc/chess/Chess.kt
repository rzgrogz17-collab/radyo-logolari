package com.example.satranc.chess

/**
 * Satranç kuralları: hamle üretimi, rok, geçerken alma, terfi,
 * şah / mat / pat, 50 hamle, üçlü tekrar ve yetersiz taş.
 * Tahta karesi 0 = a1 … 63 = h8.
 */
object Chess {
    const val WP = 1
    const val WN = 2
    const val WB = 3
    const val WR = 4
    const val WQ = 5
    const val WK = 6
    const val BP = 9
    const val BN = 10
    const val BB = 11
    const val BR = 12
    const val BQ = 13
    const val BK = 14

    const val CASTLE_WK = 1
    const val CASTLE_WQ = 2
    const val CASTLE_BK = 4
    const val CASTLE_BQ = 8

    const val FLAG_EP = 1
    const val FLAG_CASTLE = 2

    private val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private val KNIGHT = arrayOf(1 to 2, 2 to 1, -1 to 2, -2 to 1, 1 to -2, 2 to -1, -1 to -2, -2 to -1)
    private val ORTH = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    private val DIAG = arrayOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
    private val PIECE_LETTER = charArrayOf(' ', ' ', 'N', 'B', 'R', 'Q', 'K')

    fun file(sq: Int) = sq and 7
    fun rank(sq: Int) = sq shr 3
    fun square(file: Int, rank: Int) = rank * 8 + file
    fun isWhite(piece: Int) = piece in 1..6
    fun typeOf(piece: Int) = if (piece >= 9) piece - 8 else piece

    fun pack(from: Int, to: Int, promo: Int = 0, flag: Int = 0): Int =
        from or (to shl 6) or (promo shl 12) or (flag shl 16)

    fun from(move: Int) = move and 63
    fun to(move: Int) = (move shr 6) and 63
    fun promo(move: Int) = (move shr 12) and 7
    fun flag(move: Int) = (move shr 16) and 3

    fun start(): Position = parseFen(START_FEN)

    fun squareName(sq: Int): String = "${'a' + file(sq)}${'1' + rank(sq)}"

    fun parseSquare(text: String): Int {
        require(text.length >= 2)
        return square(text[0] - 'a', text[1] - '1')
    }

    fun parseFen(fen: String): Position {
        val position = Position()
        val parts = fen.trim().split(Regex("\\s+"))
        var rank = 7
        var file = 0
        for (ch in parts[0]) {
            when {
                ch == '/' -> {
                    rank--
                    file = 0
                }
                ch.isDigit() -> file += ch - '0'
                else -> {
                    position.board[square(file, rank)] = pieceFromChar(ch)
                    file++
                }
            }
        }
        position.white = parts.getOrElse(1) { "w" } != "b"
        val castle = parts.getOrElse(2) { "-" }
        var rights = 0
        if ('K' in castle) rights = rights or CASTLE_WK
        if ('Q' in castle) rights = rights or CASTLE_WQ
        if ('k' in castle) rights = rights or CASTLE_BK
        if ('q' in castle) rights = rights or CASTLE_BQ
        position.castle = rights
        val ep = parts.getOrElse(3) { "-" }
        position.ep = if (ep == "-") -1 else parseSquare(ep)
        position.half = parts.getOrNull(4)?.toIntOrNull() ?: 0
        position.full = parts.getOrNull(5)?.toIntOrNull() ?: 1
        return position
    }

    fun toFen(position: Position): String {
        val board = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val piece = position.board[square(file, rank)]
                if (piece == 0) {
                    empty++
                } else {
                    if (empty > 0) {
                        board.append(empty)
                        empty = 0
                    }
                    board.append(charFromPiece(piece))
                }
            }
            if (empty > 0) board.append(empty)
            if (rank > 0) board.append('/')
        }
        val castle = buildString {
            if (position.castle and CASTLE_WK != 0) append('K')
            if (position.castle and CASTLE_WQ != 0) append('Q')
            if (position.castle and CASTLE_BK != 0) append('k')
            if (position.castle and CASTLE_BQ != 0) append('q')
            if (isEmpty()) append('-')
        }
        val ep = if (position.ep < 0) "-" else squareName(position.ep)
        val side = if (position.white) "w" else "b"
        return "$board $side $castle $ep ${position.half} ${position.full}"
    }

    fun key(position: Position): String = buildString {
        for (sq in 0 until 64) append(position.board[sq].toString(16))
        append(if (position.white) 'w' else 'b')
        append(position.castle)
        append(':')
        append(position.ep)
    }

    fun findKing(position: Position, white: Boolean): Int {
        val target = if (white) WK else BK
        for (sq in 0 until 64) if (position.board[sq] == target) return sq
        return -1
    }

    fun attacked(position: Position, sq: Int, byWhite: Boolean): Boolean {
        if (sq !in 0..63) return true
        val f = file(sq)
        val r = rank(sq)
        if (byWhite) {
            if (r > 0 && f > 0 && position.board[square(f - 1, r - 1)] == WP) return true
            if (r > 0 && f < 7 && position.board[square(f + 1, r - 1)] == WP) return true
        } else {
            if (r < 7 && f > 0 && position.board[square(f - 1, r + 1)] == BP) return true
            if (r < 7 && f < 7 && position.board[square(f + 1, r + 1)] == BP) return true
        }
        for ((df, dr) in KNIGHT) {
            val tf = f + df
            val tr = r + dr
            if (tf !in 0..7 || tr !in 0..7) continue
            val piece = position.board[square(tf, tr)]
            if (piece != 0 && isWhite(piece) == byWhite && typeOf(piece) == WN) return true
        }
        for (df in -1..1) for (dr in -1..1) {
            if (df == 0 && dr == 0) continue
            val tf = f + df
            val tr = r + dr
            if (tf !in 0..7 || tr !in 0..7) continue
            val piece = position.board[square(tf, tr)]
            if (piece != 0 && isWhite(piece) == byWhite && typeOf(piece) == WK) return true
        }
        if (rayHits(position, f, r, byWhite, ORTH, WR)) return true
        if (rayHits(position, f, r, byWhite, DIAG, WB)) return true
        return false
    }

    fun legal(position: Position): List<Int> {
        val pseudo = ArrayList<Int>(48)
        generate(position, pseudo)
        if (pseudo.isEmpty()) return emptyList()
        val legal = ArrayList<Int>(pseudo.size)
        for (move in pseudo) {
            val undo = make(position, move)
            val king = findKing(position, !position.white)
            if (!attacked(position, king, position.white)) legal.add(move)
            unmake(position, move, undo)
        }
        return legal
    }

    fun perft(position: Position, depth: Int): Long {
        if (depth == 0) return 1
        var nodes = 0L
        for (move in legal(position)) {
            val undo = make(position, move)
            nodes += perft(position, depth - 1)
            unmake(position, move, undo)
        }
        return nodes
    }

    fun make(position: Position, move: Int): Undo {
        val from = from(move)
        val to = to(move)
        val promo = promo(move)
        val flag = flag(move)
        val moved = position.board[from]
        val captured = if (flag == FLAG_EP) {
            position.board[if (position.white) to - 8 else to + 8]
        } else {
            position.board[to]
        }
        val undo = Undo(captured, position.castle, position.ep, position.half, position.full, position.white, moved)

        position.board[from] = 0
        if (flag == FLAG_EP) {
            position.board[if (undo.white) to - 8 else to + 8] = 0
        }
        position.board[to] = if (promo != 0) {
            if (undo.white) promo else promo + 8
        } else {
            moved
        }
        if (flag == FLAG_CASTLE) {
            when (to) {
                6 -> {
                    position.board[5] = position.board[7]
                    position.board[7] = 0
                }
                2 -> {
                    position.board[3] = position.board[0]
                    position.board[0] = 0
                }
                62 -> {
                    position.board[61] = position.board[63]
                    position.board[63] = 0
                }
                58 -> {
                    position.board[59] = position.board[56]
                    position.board[56] = 0
                }
            }
        }
        position.castle = position.castle and castleClear(from).inv() and castleClear(to).inv()
        position.ep = -1
        if (typeOf(moved) == WP && kotlin.math.abs(to - from) == 16) {
            position.ep = (from + to) / 2
        }
        position.half = if (captured != 0 || typeOf(moved) == WP) 0 else position.half + 1
        if (!undo.white) position.full++
        position.white = !undo.white
        return undo
    }

    fun unmake(position: Position, move: Int, undo: Undo) {
        val from = from(move)
        val to = to(move)
        val flag = flag(move)
        position.white = undo.white
        position.castle = undo.castle
        position.ep = undo.ep
        position.half = undo.half
        position.full = undo.full
        position.board[from] = undo.moved
        position.board[to] = if (flag == FLAG_EP) 0 else undo.captured
        if (flag == FLAG_EP) {
            position.board[if (undo.white) to - 8 else to + 8] = undo.captured
        }
        if (flag == FLAG_CASTLE) {
            when (to) {
                6 -> {
                    position.board[7] = position.board[5]
                    position.board[5] = 0
                }
                2 -> {
                    position.board[0] = position.board[3]
                    position.board[3] = 0
                }
                62 -> {
                    position.board[63] = position.board[61]
                    position.board[61] = 0
                }
                58 -> {
                    position.board[56] = position.board[59]
                    position.board[59] = 0
                }
            }
        }
    }

    fun inCheck(position: Position): Boolean =
        attacked(position, findKing(position, position.white), !position.white)

    fun status(position: Position, repetitions: Int): Status {
        val moves = legal(position)
        val check = inCheck(position)
        if (moves.isEmpty()) return if (check) Status.CHECKMATE else Status.STALEMATE
        if (position.half >= 100) return Status.FIFTY
        if (repetitions >= 3) return Status.REPETITION
        if (insufficient(position)) return Status.MATERIAL
        return if (check) Status.CHECK else Status.ONGOING
    }

    fun insufficient(position: Position): Boolean {
        var minors = 0
        var other = 0
        for (sq in 0 until 64) {
            val piece = position.board[sq]
            if (piece == 0 || typeOf(piece) == WK) continue
            when (typeOf(piece)) {
                WN, WB -> minors++
                else -> other++
            }
        }
        return other == 0 && minors <= 1
    }

    fun san(position: Position, move: Int): String {
        val from = from(move)
        val to = to(move)
        val piece = position.board[from]
        val kind = typeOf(piece)
        val moveFlag = flag(move)
        if (moveFlag == FLAG_CASTLE) return if (file(to) > file(from)) "O-O" else "O-O-O"
        val capture = position.board[to] != 0 || moveFlag == FLAG_EP
        val text = StringBuilder()
        if (kind == WP) {
            if (capture) text.append('a' + file(from)).append('x')
            text.append(squareName(to))
        } else {
            text.append(PIECE_LETTER[kind])
            val clashes = legal(position).filter { candidate ->
                from(candidate) != from &&
                    to(candidate) == to &&
                    typeOf(position.board[from(candidate)]) == kind &&
                    promo(candidate) == promo(move)
            }
            if (clashes.isNotEmpty()) {
                val fileBusy = clashes.any { file(from(it)) == file(from) }
                val rankBusy = clashes.any { rank(from(it)) == rank(from) }
                when {
                    !fileBusy -> text.append('a' + file(from))
                    !rankBusy -> text.append('1' + rank(from))
                    else -> text.append('a' + file(from)).append('1' + rank(from))
                }
            }
            if (capture) text.append('x')
            text.append(squareName(to))
        }
        if (promo(move) != 0) text.append('=').append(PIECE_LETTER[promo(move)])
        val undo = make(position, move)
        val check = inCheck(position)
        val mate = check && legal(position).isEmpty()
        unmake(position, move, undo)
        if (mate) text.append('#') else if (check) text.append('+')
        return text.toString()
    }

    /** "e2e4", "e7e8q" biçimini yasal hamleye çevirir. */
    fun parseUci(position: Position, uci: String): Int? {
        if (uci.length < 4) return null
        val from = parseSquare(uci.substring(0, 2))
        val to = parseSquare(uci.substring(2, 4))
        val promo = if (uci.length >= 5) {
            when (uci[4].lowercaseChar()) {
                'n' -> WN
                'b' -> WB
                'r' -> WR
                'q' -> WQ
                else -> 0
            }
        } else 0
        return legal(position).firstOrNull { move ->
            from(move) == from && to(move) == to && (promo == 0 || promo(move) == promo)
        }
    }

    private fun generate(position: Position, out: MutableList<Int>) {
        val white = position.white
        for (sq in 0 until 64) {
            val piece = position.board[sq]
            if (piece == 0 || isWhite(piece) != white) continue
            when (typeOf(piece)) {
                WP -> pawnMoves(position, sq, out)
                WN -> leaps(position, sq, KNIGHT, out)
                WB -> slides(position, sq, DIAG, out)
                WR -> slides(position, sq, ORTH, out)
                WQ -> {
                    slides(position, sq, ORTH, out)
                    slides(position, sq, DIAG, out)
                }
                WK -> {
                    leaps(position, sq, kingSteps, out)
                    castles(position, out)
                }
            }
        }
    }

    private val kingSteps = arrayOf(
        1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1
    )

    private fun pawnMoves(position: Position, sq: Int, out: MutableList<Int>) {
        val f = file(sq)
        val r = rank(sq)
        if (position.white) {
            if (r < 7 && position.board[sq + 8] == 0) {
                if (r == 6) addPromotions(out, sq, sq + 8) else out.add(pack(sq, sq + 8))
                if (r == 1 && position.board[sq + 16] == 0) out.add(pack(sq, sq + 16))
            }
            for (df in intArrayOf(-1, 1)) {
                val tf = f + df
                if (tf !in 0..7 || r >= 7) continue
                val to = square(tf, r + 1)
                val occ = position.board[to]
                if (occ != 0 && !isWhite(occ)) {
                    if (r == 6) addPromotions(out, sq, to) else out.add(pack(sq, to))
                } else if (to == position.ep) {
                    out.add(pack(sq, to, flag = FLAG_EP))
                }
            }
        } else {
            if (r > 0 && position.board[sq - 8] == 0) {
                if (r == 1) addPromotions(out, sq, sq - 8) else out.add(pack(sq, sq - 8))
                if (r == 6 && position.board[sq - 16] == 0) out.add(pack(sq, sq - 16))
            }
            for (df in intArrayOf(-1, 1)) {
                val tf = f + df
                if (tf !in 0..7 || r <= 0) continue
                val to = square(tf, r - 1)
                val occ = position.board[to]
                if (occ != 0 && isWhite(occ)) {
                    if (r == 1) addPromotions(out, sq, to) else out.add(pack(sq, to))
                } else if (to == position.ep) {
                    out.add(pack(sq, to, flag = FLAG_EP))
                }
            }
        }
    }

    private fun addPromotions(out: MutableList<Int>, from: Int, to: Int) {
        out.add(pack(from, to, WQ))
        out.add(pack(from, to, WR))
        out.add(pack(from, to, WB))
        out.add(pack(from, to, WN))
    }

    private fun leaps(position: Position, sq: Int, deltas: Array<Pair<Int, Int>>, out: MutableList<Int>) {
        val f = file(sq)
        val r = rank(sq)
        val white = position.white
        for ((df, dr) in deltas) {
            val tf = f + df
            val tr = r + dr
            if (tf !in 0..7 || tr !in 0..7) continue
            val to = square(tf, tr)
            val occ = position.board[to]
            if (occ == 0 || isWhite(occ) != white) out.add(pack(sq, to))
        }
    }

    private fun slides(position: Position, sq: Int, dirs: Array<Pair<Int, Int>>, out: MutableList<Int>) {
        val f0 = file(sq)
        val r0 = rank(sq)
        val white = position.white
        for ((df, dr) in dirs) {
            var f = f0 + df
            var r = r0 + dr
            while (f in 0..7 && r in 0..7) {
                val to = square(f, r)
                val occ = position.board[to]
                if (occ == 0) {
                    out.add(pack(sq, to))
                } else {
                    if (isWhite(occ) != white) out.add(pack(sq, to))
                    break
                }
                f += df
                r += dr
            }
        }
    }

    private fun castles(position: Position, out: MutableList<Int>) {
        if (position.white) {
            if (position.castle and CASTLE_WK != 0 &&
                position.board[5] == 0 && position.board[6] == 0 &&
                position.board[4] == WK && position.board[7] == WR &&
                !attacked(position, 4, false) &&
                !attacked(position, 5, false) &&
                !attacked(position, 6, false)
            ) {
                out.add(pack(4, 6, flag = FLAG_CASTLE))
            }
            if (position.castle and CASTLE_WQ != 0 &&
                position.board[1] == 0 && position.board[2] == 0 && position.board[3] == 0 &&
                position.board[4] == WK && position.board[0] == WR &&
                !attacked(position, 4, false) &&
                !attacked(position, 3, false) &&
                !attacked(position, 2, false)
            ) {
                out.add(pack(4, 2, flag = FLAG_CASTLE))
            }
        } else {
            if (position.castle and CASTLE_BK != 0 &&
                position.board[61] == 0 && position.board[62] == 0 &&
                position.board[60] == BK && position.board[63] == BR &&
                !attacked(position, 60, true) &&
                !attacked(position, 61, true) &&
                !attacked(position, 62, true)
            ) {
                out.add(pack(60, 62, flag = FLAG_CASTLE))
            }
            if (position.castle and CASTLE_BQ != 0 &&
                position.board[57] == 0 && position.board[58] == 0 && position.board[59] == 0 &&
                position.board[60] == BK && position.board[56] == BR &&
                !attacked(position, 60, true) &&
                !attacked(position, 59, true) &&
                !attacked(position, 58, true)
            ) {
                out.add(pack(60, 58, flag = FLAG_CASTLE))
            }
        }
    }

    private fun rayHits(
        position: Position,
        file: Int,
        rank: Int,
        byWhite: Boolean,
        dirs: Array<Pair<Int, Int>>,
        slider: Int
    ): Boolean {
        for ((df, dr) in dirs) {
            var f = file + df
            var r = rank + dr
            while (f in 0..7 && r in 0..7) {
                val piece = position.board[square(f, r)]
                if (piece != 0) {
                    if (isWhite(piece) == byWhite) {
                        val kind = typeOf(piece)
                        if (kind == WQ || kind == slider) return true
                    }
                    break
                }
                f += df
                r += dr
            }
        }
        return false
    }

    private fun castleClear(sq: Int): Int = when (sq) {
        4 -> CASTLE_WK or CASTLE_WQ
        7 -> CASTLE_WK
        0 -> CASTLE_WQ
        60 -> CASTLE_BK or CASTLE_BQ
        63 -> CASTLE_BK
        56 -> CASTLE_BQ
        else -> 0
    }

    private fun pieceFromChar(ch: Char): Int = when (ch) {
        'P' -> WP
        'N' -> WN
        'B' -> WB
        'R' -> WR
        'Q' -> WQ
        'K' -> WK
        'p' -> BP
        'n' -> BN
        'b' -> BB
        'r' -> BR
        'q' -> BQ
        'k' -> BK
        else -> 0
    }

    private fun charFromPiece(piece: Int): Char {
        val ch = when (typeOf(piece)) {
            WP -> 'p'
            WN -> 'n'
            WB -> 'b'
            WR -> 'r'
            WQ -> 'q'
            WK -> 'k'
            else -> '?'
        }
        return if (isWhite(piece)) ch.uppercaseChar() else ch
    }
}

class Position {
    val board = IntArray(64)
    var white: Boolean = true
    var castle: Int = 0
    var ep: Int = -1
    var half: Int = 0
    var full: Int = 1

    fun clone(): Position {
        val copy = Position()
        board.copyInto(copy.board)
        copy.white = white
        copy.castle = castle
        copy.ep = ep
        copy.half = half
        copy.full = full
        return copy
    }
}

class Undo(
    val captured: Int,
    val castle: Int,
    val ep: Int,
    val half: Int,
    val full: Int,
    val white: Boolean,
    val moved: Int
)

enum class Status {
    ONGOING,
    CHECK,
    CHECKMATE,
    STALEMATE,
    FIFTY,
    REPETITION,
    MATERIAL
}
