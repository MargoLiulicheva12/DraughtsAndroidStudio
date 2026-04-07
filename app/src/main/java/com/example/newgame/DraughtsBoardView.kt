package com.example.newgame

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast

// Отрисовка доски и обработка касаний
class DraughtsBoardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // Игровое состояние
    private var board: Board = Board()           // Текущее состояние доски
    private var currentPlayer: Piece = Piece.WHITE // Кто сейчас ходит
    private var selectedRow: Int? = null         // Выбранная клетка (ряд)
    private var selectedCol: Int? = null         // Выбранная клетка (столбец)
    var mandatoryPiece: Pair<Int, Int>? = null   // Фигура, которая обязана продолжать взятие

    // Графика
    private var cellSize = 0f                    // Размер одной клетки в пикселях
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG) // Кисть для рисования
    // Цвета клеток
    private val darkCell = Color.parseColor("#795548")   // Темные клетки
    private val lightCell = Color.parseColor("#D7CCC8")  // Светлые клетки
    private val highlight = Color.parseColor("#80FFEB3B") // Подсветка выбранной клетки
    var isFlipped = false                        // Перевернута ли доска (для игры черными снизу)
    var onMoveMade: ((Move) -> Unit)? = null     // Callback при совершении хода
    // Анимация хода
    private var animProgress = 1f                // Прогресс анимации (0-1)
    private var activeMove: Move? = null         // Ход, который сейчас анимируется
    private var animPiece: Piece = Piece.EMPTY   // Фигура, которая летит

    // Обновляет состояние доски и перерисовывает
    fun updateBoard(newBoard: Board, player: Piece) {
        this.board = newBoard
        this.currentPlayer = player
        invalidate()  // Перерисовываем View
    }

    // Запускает анимацию перемещения фигуры
    fun animateMove(move: Move, piece: Piece, onEnd: () -> Unit) {
        activeMove = move
        animPiece = piece
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300  // 300 миллисекунд
            interpolator = DecelerateInterpolator()  // Замедление в конце
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()  // Перерисовываем на каждом кадре
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    activeMove = null
                    animProgress = 1f
                    onEnd()  // Вызываем колбэк после окончания анимации
                }
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}
            })
            start()
        }
    }

    // Вызывается при изменении размера View (высчитываем размер клетки)
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cellSize = minOf(w, h).toFloat() / 8f  // Делим на 8 клеток
    }

    // Основной метод рисования
    override fun onDraw(canvas: Canvas) {
        drawGrid(canvas)    // Рисуем сетку доски
        drawPieces(canvas)  // Рисуем шашки
    }

    // Рисует сетку доски (8x8 клеток)
    private fun drawGrid(canvas: Canvas) {
        for (r in 0..7) {
            for (c in 0..7) {
                // Если доска перевернута - инвертируем координаты
                val dr = if (isFlipped) 7 - r else r
                val dc = if (isFlipped) 7 - c else c

                // Темные и светлые клетки
                paint.color = if ((r + c) % 2 == 1) darkCell else lightCell
                canvas.drawRect(dc * cellSize, dr * cellSize, (dc + 1) * cellSize, (dr + 1) * cellSize, paint)

                // Подсветка выбранной клетки
                if (selectedRow == r && selectedCol == c) {
                    paint.color = highlight
                    canvas.drawRect(dc * cellSize, dr * cellSize, (dc + 1) * cellSize, (dr + 1) * cellSize, paint)
                }
            }
        }
    }

    // Рисует все шашки на доске
    private fun drawPieces(canvas: Canvas) {
        // Рисуем все шашки, кроме той, что сейчас анимируется
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board.getPiece(r, c)
                if (piece == Piece.EMPTY) continue
                // Пропускаем фигуру, которая сейчас летит
                if (activeMove?.fromRow == r && activeMove?.fromCol == c) continue

                val dr = if (isFlipped) 7 - r else r
                val dc = if (isFlipped) 7 - c else c
                drawSinglePiece(canvas, dr.toFloat(), dc.toFloat(), piece)
            }
        }

        // Рисуем анимированную фигуру (летит от начальной к конечной позиции)
        activeMove?.let { m ->
            val startR = (if (isFlipped) 7 - m.fromRow else m.fromRow).toFloat()
            val startC = (if (isFlipped) 7 - m.fromCol else m.fromCol).toFloat()
            val endR = (if (isFlipped) 7 - m.toRow else m.toRow).toFloat()
            val endC = (if (isFlipped) 7 - m.toCol else m.toCol).toFloat()

            // Интерполяция позиции
            val curR = startR + (endR - startR) * animProgress
            val curC = startC + (endC - startC) * animProgress
            drawSinglePiece(canvas, curR, curC, animPiece)
        }
    }

    // Рисует одну шашку в указанных координатах
    private fun drawSinglePiece(canvas: Canvas, displayRow: Float, displayCol: Float, piece: Piece) {
        val cx = displayCol * cellSize + cellSize / 2  // Центр по X
        val cy = displayRow * cellSize + cellSize / 2  // Центр по Y
        val radius = cellSize * 0.4f                   // Радиус круга

        // Заливка фигуры (белая или черная)
        paint.color = if (piece.isWhite()) Color.WHITE else Color.BLACK
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius, paint)

        // Обводка
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.FILL

        // Если дамка - рисуем золотую точку в центре
        if (piece.isKing()) {
            paint.color = Color.parseColor("#FFD700")
            canvas.drawCircle(cx, cy, radius * 0.5f, paint)
        }
    }

    // Обработка касаний экрана
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN || animProgress < 1f) return true

        var c = (event.x / cellSize).toInt()
        var r = (event.y / cellSize).toInt()
        if (isFlipped) { r = 7 - r; c = 7 - c }
        if (r !in 0..7 || c !in 0..7) return true

        // 1. Обработка обязательного продолжения серии ударов (если уже начали бить)
        if (mandatoryPiece != null) {
            val (mR, mC) = mandatoryPiece!!
            val captures = board.getCapturesFrom(mR, mC, board.getPiece(mR, mC))
            val move = captures.find { it.toRow == r && it.toCol == c }

            if (move != null) {
                onMoveMade?.invoke(move)
            } else {
                // Если игрок пытается ходить другой фигурой или в пустое место во время серии
                Toast.makeText(context, "Необходимо завершить взятие этой фигурой!", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        // 2. Если фигура уже выбрана
        if (selectedRow != null && selectedCol != null) {
            val selR = selectedRow!!
            val selC = selectedCol!!
            val piece = board.getPiece(selR, selC)

            // Получаем все разрешенные ходы для текущего игрока (уже включают проверку на обязательный бой)
            val allLegalMoves = board.getAllMoves(currentPlayer)
            val move = allLegalMoves.find { it.fromRow == selR && it.fromCol == selC && it.toRow == r && it.toCol == c }

            if (move != null) {
                onMoveMade?.invoke(move)
                selectedRow = null
                selectedCol = null
                invalidate()
                return true
            } else {
                // ПРОВЕРКА ДЛЯ ТОСТА: если ход не найден в легальных, проверяем, есть ли обязательный бой
                if (board.hasMandatoryCaptures(currentPlayer)) {
                    // Проверяем, был ли этот ход «обычным» (без взятия)
                    val potentialNormalMoves = board.getNormalMovesFrom(selR, selC, piece)
                    val isNormalMoveAttempt = potentialNormalMoves.any { it.toRow == r && it.toCol == c }

                    if (isNormalMoveAttempt) {
                        Toast.makeText(context, "Необходимо съесть шашку противника!", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
            }
        }

        // 3. Выбор фигуры (обычный клик)
        val clickedPiece = board.getPiece(r, c)
        if (clickedPiece != Piece.EMPTY && clickedPiece.isWhite() == currentPlayer.isWhite()) {
            selectedRow = r
            selectedCol = c
            invalidate()
        } else {
            selectedRow = null
            selectedCol = null
            invalidate()
        }

        return true
    }

    // Снимает выделение с выбранной клетки
    fun clearSelection() {
        selectedRow = null
        selectedCol = null
        invalidate()
    }

    // Возвращает текущее состояние доски
    fun getBoard() = board
}