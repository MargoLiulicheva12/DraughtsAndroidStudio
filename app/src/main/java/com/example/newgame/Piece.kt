package com.example.newgame

// Перечисление всех возможных состояний клетки на доске
enum class Piece {
    EMPTY, WHITE, BLACK, WHITE_KING, BLACK_KING;

    // Является ли фигура белой (простая или дамка)
    fun isWhite() = this == WHITE || this == WHITE_KING
    // Является ли фигура черной (простая или дамка)
    fun isBlack() = this == BLACK || this == BLACK_KING
    // Является ли фигура дамкой
    fun isKing() = this == WHITE_KING || this == BLACK_KING
    // Если фигура белая - возвращает черную, и наоборот
    fun opponentColor(): Piece {
        return if (this.isWhite()) BLACK else WHITE
    }
}