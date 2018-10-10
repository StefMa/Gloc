package kotlin2ts.games.chess

enum class Piece {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING,
}

enum class Vertical {
    A, B, C, D, E, F, G, H,
}

enum class Horizontal {
    H1, H2, H3, H4, H5, H6, H7, H8
}

data class Position(
        val vertical: Vertical,
        val horizontal: Horizontal
)

data class PieceOnBoard(
        val piece: Piece,
        val position: Position
)

data class ChessProblem(
        val white: Collection<PieceOnBoard>,
        val black: Collection<PieceOnBoard>,
        val whiteMovesFirst: Boolean = true
)
