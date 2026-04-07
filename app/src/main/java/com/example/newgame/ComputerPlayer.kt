package com.example.newgame

class ComputerPlayer(val color: Piece, val diff: Difficulty) {

    fun getMove(board: Board): Move? {
        // Получаем все возможные ходы для цвета компьютера
        val moves = board.getAllMoves(color)
        if (moves.isEmpty()) return null

        // Выбор стратегии в зависимости от сложности
        return when (diff) {
            // ЛЕГКО: случайный ход
            Difficulty.EASY -> moves.random()

            // СРЕДНЕ: выбираем ход с максимальной оценкой позиции
            Difficulty.MEDIUM -> moves.maxByOrNull { m ->
                val b = board.copy()
                b.makeMove(m)
                b.evaluate(color)
            }

            // СЛОЖНО: используем алгоритм минимакс с альфа-бета отсечением
            Difficulty.HARD -> {
                var bestMove: Move? = null
                var bestEval = Int.MIN_VALUE

                // Перемешиваем ходы, чтобы ИИ не был предсказуемым при равных оценках
                for (m in moves.shuffled()) {
                    val b = board.copy()
                    b.makeMove(m)

                    // Запускаем минимакс на глубину 4, ход противника (isMax = false)
                    val eval = minimax(b, 4, false, Int.MIN_VALUE, Int.MAX_VALUE)

                    if (eval > bestEval) {
                        bestEval = eval
                        bestMove = m
                    }
                }
                bestMove ?: moves.random() // Запасной вариант
            }
        }
    }

    // Алгоритм минимакс с альфа-бета отсечением
    private fun minimax(board: Board, depth: Int, isMax: Boolean, a: Int, b: Int): Int {
        val turnColor = if (isMax) color else color.opponentColor()

        // Проверка на ничью
        if (board.isDraw(turnColor)) {
            return 0 // Ничья - нейтральная оценка
        }

        // Проверка на победителя
        val winner = board.getWinner(turnColor)
        if (winner != null) {
            return if (winner.isWhite() == color.isWhite()) {
                10000 + depth
            } else {
                -10000 - depth
            }
        }

        if (depth == 0) return board.evaluate(color)
        // Получаем все возможные ходы для текущего игрока
        val moves = board.getAllMoves(turnColor)
        var alpha = a
        var beta = b

        // Максимизирующий игрок (компьютер)
        if (isMax) {
            var maxEval = Int.MIN_VALUE
            for (m in moves) {
                val nb = board.copy()
                nb.makeMove(m)
                val eval = minimax(nb, depth - 1, false, alpha, beta)
                maxEval = maxOf(maxEval, eval)
                alpha = maxOf(alpha, eval)
                if (beta <= alpha) break // Альфа-бета отсечение
            }
            return maxEval
        }
        // Минимизирующий игрок (противник)
        else {
            var minEval = Int.MAX_VALUE
            for (m in moves) {
                val nb = board.copy()
                nb.makeMove(m)
                val eval = minimax(nb, depth - 1, true, alpha, beta)
                minEval = minOf(minEval, eval)
                beta = minOf(beta, eval)
                if (beta <= alpha) break // Альфа-бета отсечение
            }
            return minEval
        }
    }
}