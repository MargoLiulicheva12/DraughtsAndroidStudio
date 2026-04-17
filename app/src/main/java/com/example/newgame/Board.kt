package com.example.newgame

// Класс доски для игры в шашки
class Board(private val cells: Array<Array<Piece>> = Array(8) { Array(8) { Piece.EMPTY } })
{
    // Счетчик съеденных белых шашек
    var whiteCaptured = 0
    // Счетчик съеденных черных шашек
    var blackCaptured = 0
    // Счетчик ходов без взятия (для правила 40 ходов - ничья)
    var movesWithoutCapture = 0
    // Хранит все позиции, которые были на доске (для проверки повторений)
    private val positionHistory = mutableListOf<String>()

    // Начальная расстановка шашек при создании новой доски
    init {
        // Если доска пустая, расставляем шашки
        if (cells.all { row -> row.all { it == Piece.EMPTY } }) {
            for (r in 0..7) {
                for (c in 0..7) {
                    // Только на темных клетках
                    if ((r + c) % 2 == 1) {
                        // Верхние 3 ряда - черные шашки
                        if (r <= 2) cells[r][c] = Piece.BLACK
                        // Нижние 3 ряда - белые шашки
                        if (r >= 5) cells[r][c] = Piece.WHITE
                    }
                }
            }
            // Обнуляем счетчики
            whiteCaptured = 0
            blackCaptured = 0
            movesWithoutCapture = 0
        }
    }

    // Возвращает фигуру на указанной клетке
    fun getPiece(r: Int, c: Int) = if (r in 0..7 && c in 0..7) cells[r][c] else Piece.EMPTY

    // Выполняет ход на доске
    fun makeMove(move: Move) {
        // Запоминаем фигуру и очищаем начальную клетку
        val piece = cells[move.fromRow][move.fromCol]
        cells[move.fromRow][move.fromCol] = Piece.EMPTY

        var wasCapture = false  // Был ли захват шашки
        var wasPromotion = false // Было ли превращение в дамку

        // Если есть съеденная шашка - удаляем её и увеличиваем счетчик
        move.captured?.let { (r, c) ->
            val capturedPiece = cells[r][c]
            cells[r][c] = Piece.EMPTY
            if (capturedPiece.isWhite()) blackCaptured++  // Черные съели белую
            else if (capturedPiece.isBlack()) whiteCaptured++ // Белые съели черную
            wasCapture = true
        }

        // Превращаем шашку в дамку, если она дошла до последнего ряда
        var finalPiece = piece
        if (piece == Piece.WHITE && move.toRow == 0) {
            finalPiece = Piece.WHITE_KING
            wasPromotion = true
        }
        if (piece == Piece.BLACK && move.toRow == 7) {
            finalPiece = Piece.BLACK_KING
            wasPromotion = true
        }
        // Ставим фигуру на новое место
        cells[move.toRow][move.toCol] = finalPiece

        // Обновляем счетчик ходов без взятия
        if (wasCapture || wasPromotion) {
            movesWithoutCapture = 0 // Было взятие или превращение - тогда сбрасываем
        } else {
            movesWithoutCapture++ // Обычный ход - увеличиваем
        }

        // Сохраняем позицию для проверки повторений
        savePositionToHistory()
    }

    // Сохраняет текущую позицию в историю
    private fun savePositionToHistory() {
        val positionString = getPositionString()
        positionHistory.add(positionString)
        // Храним только последние 100 позиций, чтобы не переполнять память
        if (positionHistory.size > 100) {
            positionHistory.removeAt(0)
        }
    }

    // Превращает доску в строку для сравнения позиций
    private fun getPositionString(): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            for (c in 0..7) {
                sb.append(cells[r][c].ordinal) // Каждая клетка - цифра (0-4)
            }
        }
        return sb.toString()
    }

    // Проверяет, было ли тройное повторение позиции (ничья)
    fun hasTripleRepetition(): Boolean {
        val currentPos = getPositionString()
        return positionHistory.count { it == currentPos } >= 3
    }

    // Проверяет, не превышен ли лимит ходов без взятия (40 ходов - ничья)
    fun isDrawByMovesLimit(): Boolean {
        return movesWithoutCapture >= 40
    }

    // Проверка на ничью из-за недостатка фигур
    fun isInsufficientMaterial(): Boolean {
        var whiteCount = 0      // Всего белых фигур
        var blackCount = 0      // Всего черных фигур
        var whiteKings = 0      // Белых дамок
        var blackKings = 0      // Черных дамок

        // Подсчитываем кол-во фигур
        for (r in 0..7) {
            for (c in 0..7) {
                val p = cells[r][c]
                if (p.isWhite()) {
                    whiteCount++
                    if (p.isKing()) whiteKings++
                }
                if (p.isBlack()) {
                    blackCount++
                    if (p.isKing()) blackKings++
                }
            }
        }

        // 1 дамка против 1 дамки - ничья
        if (whiteKings == 1 && blackKings == 1 && whiteCount == 1 && blackCount == 1) return true

        // 1 дамка против 1 простой шашки - ничья (теоретически можно выиграть, но сложно)
        if (whiteKings == 1 && blackCount == 1 && blackKings == 0 && whiteCount == 1) return true
        if (blackKings == 1 && whiteCount == 1 && whiteKings == 0 && blackCount == 1) return true

        return false
    }

    // Общая проверка на ничью (все условия)
    fun isDraw(currentPlayer: Piece): Boolean {
        return isDrawByMovesLimit() || hasTripleRepetition() || isInsufficientMaterial()
    }

    // Возвращает все возможные ходы для игрока
    fun getAllMoves(player: Piece): List<Move> {
        val captures = mutableListOf<Move>()  // Ходы со взятием
        val normals = mutableListOf<Move>()   // Обычные ходы

        for (r in 0..7) {
            for (c in 0..7) {
                val p = cells[r][c]
                // Если это фигура текущего игрока
                if (p != Piece.EMPTY && p.isWhite() == player.isWhite()) {
                    captures.addAll(getCapturesFrom(r, c, p))  // Ищем взятия
                    normals.addAll(getNormalMovesFrom(r, c, p)) // Ищем обычные ходы
                }
            }
        }
        // По правилам: если есть взятия, можно ходить только ими
        return if (captures.isNotEmpty()) captures else normals
    }

    // Возвращает все возможные взятия для фигуры на клетке (r,c)
    fun getCapturesFrom(r: Int, c: Int, p: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        // 4 диагональных направления: вверх-влево, вверх-вправо, вниз-влево, вниз-вправо
        val dirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

        for ((dr, dc) in dirs) {
            if (!p.isKing()) {
                // Обычная шашка: может бить только на 2 клетки
                val midR = r + dr; val midC = c + dc      // Клетка с врагом
                val endR = r + 2 * dr; val endC = c + 2 * dc // Клетка за врагом

                if (endR in 0..7 && endC in 0..7) {
                    val target = cells[midR][midC]
                    // Проверяем: есть враг, и клетка за ним пуста
                    if (target != Piece.EMPTY && target.isWhite() != p.isWhite() && cells[endR][endC] == Piece.EMPTY)
                        moves.add(Move(r, c, endR, endC, midR to midC))
                }
            } else {
                // Дамка: может бить на любое расстояние
                var nr = r + dr; var nc = c + dc
                var enemy: Pair<Int, Int>? = null  // Позиция врага, которого бьем

                while (nr + dr in 0..7 && nc + dc in 0..7) {
                    val curr = cells[nr][nc]
                    if (curr != Piece.EMPTY) {
                        // Встретили свою фигуру или уже нашли врага - дальше не идем
                        if (curr.isWhite() == p.isWhite() || enemy != null) break
                        enemy = nr to nc  // Запоминаем врага
                    } else if (enemy != null) {
                        // За врагом есть пустая клетка - можно бить
                        moves.add(Move(r, c, nr, nc, enemy))
                    }
                    nr += dr; nc += dc
                }
                // Проверяем последнюю позицию
                if (nr in 0..7 && nc in 0..7 && cells[nr][nc] == Piece.EMPTY && enemy != null)
                    moves.add(Move(r, c, nr, nc, enemy))
            }
        }
        return moves
    }

    // Возвращает обычные ходы (без взятия) для фигуры
    fun getNormalMovesFrom(r: Int, c: Int, p: Piece): List<Move> {
        val moves = mutableListOf<Move>()

        val directions = if (p.isKing()) {
            arrayOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        } else {
            if (p == Piece.WHITE) arrayOf(Pair(-1, -1), Pair(-1, 1))
            else arrayOf(Pair(1, -1), Pair(1, 1))
        }

        for (d in directions) {
            var nr = r + d.first
            var nc = c + d.second

            if (p.isKing()) {
                while (nr in 0..7 && nc in 0..7 && cells[nr][nc] == Piece.EMPTY) {
                    moves.add(Move(r, c, nr, nc))
                    nr += d.first
                    nc += d.second
                }
            } else {
                if (nr in 0..7 && nc in 0..7 && cells[nr][nc] == Piece.EMPTY) {
                    moves.add(Move(r, c, nr, nc))
                }
            }
        }
        return moves
    }

    // Проверяет, есть ли у игрока обязательные взятия
    fun hasMandatoryCaptures(player: Piece): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = cells[r][c]
                if (p != Piece.EMPTY && p.isWhite() == player.isWhite()) {
                    if (getCapturesFrom(r, c, p).isNotEmpty()) {
                        return true  // Нашли хотя бы одно взятие
                    }
                }
            }
        }
        return false
    }

    // Оценивает позицию для ИИ (положительное число - преимущество игрока)
    fun evaluate(playerColor: Piece): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = cells[r][c]
                if (p == Piece.EMPTY) continue

                val value = if (p.isKing()) 10 else 3  // Дамка дороже
                // Свои фигуры прибавляем, чужие вычитаем
                if (p.isWhite() == playerColor.isWhite()) score += value else score -= value
            }
        }
        return score
    }

    // Определяет победителя (если есть)
    fun getWinner(currentTurnPlayer: Piece): Piece? {
        var whiteCount = 0
        var blackCount = 0

        // Считаем количество фигур
        for (r in 0..7) {
            for (c in 0..7) {
                val p = cells[r][c]
                if (p.isWhite()) whiteCount++
                if (p.isBlack()) blackCount++
            }
        }

        // У белых нет шашек - победили черные
        if (whiteCount == 0) return Piece.BLACK
        // У черных нет шашек - победили белые
        if (blackCount == 0) return Piece.WHITE

        // У текущего игрока нет ходов - он проиграл
        if (getAllMoves(currentTurnPlayer).isEmpty()) {
            return currentTurnPlayer.opponentColor()
        }
        return null // Игра продолжается
    }

    // Создает точную копию доски (для ИИ, чтобы не портить реальную)
    fun copy(): Board {
        val newCells = Array(8) { r -> Array(8) { c -> cells[r][c] } }
        val newBoard = Board(newCells)
        newBoard.whiteCaptured = this.whiteCaptured
        newBoard.blackCaptured = this.blackCaptured
        newBoard.movesWithoutCapture = this.movesWithoutCapture
        return newBoard
    }
}