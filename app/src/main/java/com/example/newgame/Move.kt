package com.example.newgame

// Класс данных, представляющий один ход в игре
data class Move(
    val fromRow: Int,      // Строка начальной позиции (0-7, где 0 - верх)
    val fromCol: Int,      // Столбец начальной позиции (0-7, где 0 - лево)
    val toRow: Int,        // Строка конечной позиции
    val toCol: Int,        // Столбец конечной позиции
    val captured: Pair<Int, Int>? = null  // Позиция съеденной шашки (если есть)
) {
    // Преобразует ход в шахматную нотацию
    fun toNotation(): String {
        val columns = arrayOf("A", "B", "C", "D", "E", "F", "G", "H")

        // Преобразуем координаты в шахматную нотацию: столбцы: A-H, строки: 1-8
        val from = "${columns[fromCol]}${8 - fromRow}"
        val to = "${columns[toCol]}${8 - toRow}"
        // Возвращаем нотацию через тире
        return "$from — $to"
    }
}