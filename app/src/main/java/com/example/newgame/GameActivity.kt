package com.example.newgame

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    private lateinit var boardView: DraughtsBoardView   // Доска
    private lateinit var tvTurnTop: TextView            // Статус хода (верхний игрок)
    private lateinit var tvTurnBottom: TextView         // Статус хода (нижний игрок)
    private lateinit var tvLastMoveTop: TextView        // Последний ход (верхний)
    private lateinit var tvLastMoveBottom: TextView     // Последний ход (нижний)
    private lateinit var tvCapturedTop: TextView        // Счет съеденных (верхний)
    private lateinit var tvCapturedBottom: TextView     // Счет съеденных (нижний)

    // Игровые переменные
    private var currentPlayer = Piece.WHITE             // Кто сейчас ходит (белые начинают)
    private var isVsComputer = false                    // Режим игры: true - с компьютером
    private var computerPlayer: ComputerPlayer? = null  // ИИ противник
    private var humanColor = Piece.WHITE                // Цвет игрока-человека (если игра с ИИ)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        boardView = findViewById(R.id.boardView)
        tvTurnTop = findViewById(R.id.tvTurnTop)
        tvTurnBottom = findViewById(R.id.tvTurnBottom)
        tvLastMoveTop = findViewById(R.id.tvLastMoveTop)
        tvLastMoveBottom = findViewById(R.id.tvLastMoveBottom)
        tvCapturedTop = findViewById(R.id.tvCapturedTop)
        tvCapturedBottom = findViewById(R.id.tvCapturedBottom)

        // Получаем параметры из MainActivity
        isVsComputer = intent.getBooleanExtra("vs_computer", false)

        // Параметры для игры с компьютером
        val diffStr = intent.getStringExtra("difficulty") ?: "EASY"
        val colorStr = intent.getStringExtra("player_color") ?: "WHITE"

        val diff = Difficulty.valueOf(diffStr)
        humanColor = Piece.valueOf(colorStr)

        // Настройка режима игры с компьютером
        if (isVsComputer) {
            // Компьютер играет противоположным цветом
            val compColor = if (humanColor == Piece.WHITE) Piece.BLACK else Piece.WHITE
            computerPlayer = ComputerPlayer(compColor, diff)
            // Переворачиваем доску, чтобы игрок всегда был снизу
            boardView.isFlipped = (humanColor == Piece.BLACK)
        }

        // Устанавливаем обработчик ходов
        boardView.onMoveMade = { move: Move ->
            handleMove(move)
        }

        // Начальное обновление интерфейса
        updateUI()
        updateCapturedCounters()

        // Если игра с компьютером и ход компьютера - запускаем его
        checkComputerTurn()
    }

    // Проверяет обязательные взятия перед ходом
    private fun checkMandatoryCaptures(board: Board, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val hasCaptures = board.hasMandatoryCaptures(currentPlayer)

        if (hasCaptures) {
            // Проверяем, является ли текущий ход взятием
            val isCaptureMove = board.getCapturesFrom(fromRow, fromCol, board.getPiece(fromRow, fromCol))
                .any { it.toRow == toRow && it.toCol == toCol }

            if (!isCaptureMove) {
                // Предупреждение игроку
                Toast.makeText(
                    this,
                    "Необходимо съесть шашку противника!",
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
        }
        return true
    }

    // Обработка хода
    private fun handleMove(move: Move) {
        val board = boardView.getBoard()
        val piece = board.getPiece(move.fromRow, move.fromCol)

        // Проверка обязательных взятий (только для человека, ИИ всегда делает правильные ходы)
        if (!checkMandatoryCaptures(board, move.fromRow, move.fromCol, move.toRow, move.toCol)) {
            boardView.isEnabled = true  // Разблокируем доску
            return
        }

        // Блокируем доску на время анимации
        boardView.isEnabled = false

        // Запускаем анимацию хода
        boardView.animateMove(move, piece) {
            // Выполняем ход на доске
            board.makeMove(move)

            // Отображаем нотацию последнего хода
            val notation = move.toNotation()
            if (currentPlayer == Piece.WHITE) {
                tvLastMoveBottom.text = "Ход: $notation"
            } else {
                tvLastMoveTop.text = "Ход: $notation"
            }

            // Обновляем счетчики съеденных шашек
            updateCapturedCounters()

            // Проверяем, может ли фигура продолжить взятие (повторный удар)
            val canJumpAgain = move.captured != null &&
                    board.getCapturesFrom(move.toRow, move.toCol, board.getPiece(move.toRow, move.toCol)).isNotEmpty()

            if (!canJumpAgain) {
                // Нет повторного взятия - переключаем игрока
                currentPlayer = currentPlayer.opponentColor()
                boardView.clearSelection()      // Снимаем выделение
                boardView.mandatoryPiece = null // Сбрасываем обязательную фигуру
            } else {
                // Есть повторное взятие - та же фигура должна продолжать
                boardView.mandatoryPiece = move.toRow to move.toCol
            }

            // Обновляем UI (статус хода)
            updateUI()

            // Проверяем окончание игры
            if (!checkGameOver()) {
                boardView.isEnabled = true
                checkComputerTurn()  // Если ход компьютера - запускаем его
            }
        }
    }

    // Обновление отображения кол-ва съеденных шашек
    private fun updateCapturedCounters() {
        val board = boardView.getBoard()

        if (isVsComputer) {
            // Режим с компьютером: показываем сколько съел игрок и сколько съел компьютер
            if (humanColor == Piece.WHITE) {
                // Игрок белыми - снизу показываем его съеденных (черные шашки)
                tvCapturedBottom.text = "Съедено: ${board.blackCaptured}"
                tvCapturedTop.text = "Съедено: ${board.whiteCaptured}"
            } else {
                // Игрок черными
                tvCapturedBottom.text = "Съедено: ${board.whiteCaptured}"
                tvCapturedTop.text = "Съедено: ${board.blackCaptured}"
            }
        } else {
            // Режим двух игроков: показываем кто сколько съел
            tvCapturedBottom.text = "Съедено белыми: ${board.whiteCaptured}"
            tvCapturedTop.text = "Съедено черными: ${board.blackCaptured}"
        }
    }

    // Проверка: должен ли компьютер сделать ход
    private fun checkComputerTurn() {
        if (isVsComputer && currentPlayer != humanColor) {
            // Блокируем доску во время хода компьютера
            boardView.isEnabled = false

            // Задержка для имитации "обдумывания"
            boardView.postDelayed({
                val move = computerPlayer?.getMove(boardView.getBoard())
                if (move != null) {
                    handleMove(move)  // Выполняем ход компьютера
                }
            }, 600)
        }
    }

    // Обновляет текстовые подсказки о том, чей сейчас ход
    private fun updateUI() {
        // Обновляем отображение доски (подсветка возможных ходов)
        boardView.updateBoard(boardView.getBoard(), currentPlayer)

        // Проверяем обязательные взятия для предупреждения
        val hasCaptures = boardView.getBoard().hasMandatoryCaptures(currentPlayer)
        val captureWarning = if (hasCaptures) " (обязательно съесть!)" else ""

        // Обновляем тексты в зависимости от текущего игрока
        if (currentPlayer == Piece.WHITE) {
            tvTurnBottom.text = "Ваш ход$captureWarning"
            tvTurnTop.text = "Ожидание"
        } else {
            tvTurnBottom.text = "Ожидание"
            tvTurnTop.text = if (isVsComputer) "Робот думает..." else "Ход черных$captureWarning"
        }
    }

    // Проверка: закончилась ли игра (победа или ничья)
    private fun checkGameOver(): Boolean {
        val board = boardView.getBoard()

        // Проверка на победу
        val winner = board.getWinner(currentPlayer)
        if (winner != null) {
            AlertDialog.Builder(this)
                .setTitle("Конец игры")
                .setMessage("Победили ${if (winner == Piece.WHITE) "Белые" else "Черные"}")
                .setCancelable(false)
                .setPositiveButton("В меню") { _, _ -> finish() }
                .show()
            return true
        }

        // Проверка на ничью (40 ходов без взятия, тройное повторение, недостаток фигур)
        if (board.isDraw(currentPlayer)) {
            var drawMessage = "Ничья!\n"
            when {
                board.isDrawByMovesLimit() -> drawMessage += "Превышен лимит ходов без взятия (40 ходов)"
                board.hasTripleRepetition() -> drawMessage += "Тройное повторение позиции"
                board.isInsufficientMaterial() -> drawMessage += "Недостаточно фигур для победы"
            }

            AlertDialog.Builder(this)
                .setTitle("Конец игры")
                .setMessage(drawMessage)
                .setCancelable(false)
                .setPositiveButton("В меню") { _, _ -> finish() }
                .show()
            return true
        }
        return false  // Игра продолжается
    }
}