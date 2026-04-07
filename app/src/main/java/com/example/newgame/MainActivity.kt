package com.example.newgame

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация элементов интерфейса
        val toggleMode = findViewById<MaterialButtonToggleGroup>(R.id.toggleMode)
        val aiSettings = findViewById<View>(R.id.aiSettingsContainer)
        val btnStart = findViewById<Button>(R.id.buttonStart)
        val autoDifficulty = findViewById<AutoCompleteTextView>(R.id.autoDifficulty)

        // Настройка выпадающего списка сложности
        val diffOptions = arrayOf("Легко", "Средне", "Сложно")
        autoDifficulty.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, diffOptions))
        autoDifficulty.setText(diffOptions[0], false)

        toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                // Если кнопка выбрана, показываем настройки только для режима "Против компьютера"
                aiSettings.visibility = if (checkedId == R.id.btnVsComputer) View.VISIBLE else View.GONE
            }
        }

        // Устанавливаем режим "Два игрока" по умолчанию
        toggleMode.check(R.id.btnTwoPlayers)

        btnStart.setOnClickListener {
            // Переход к игровому экрану
            val intent = Intent(this, GameActivity::class.java)
            // Определяем режим игры (против компьютера или два игрока)
            val vsAI = toggleMode.checkedButtonId == R.id.btnVsComputer
            intent.putExtra("vs_computer", vsAI)
            // Если выбран режим игры с компьютером, добавляем дополнительные параметры
            if (vsAI) {
                // Преобразуем текстовое значение сложности в enum-строку для передачи
                val diffEnum = when (autoDifficulty.text.toString()) {
                    "Легко" -> "EASY"
                    "Средне" -> "MEDIUM"
                    else -> "HARD"
                }
                intent.putExtra("difficulty", diffEnum)

                // Определяем цвет, которым будет играть игрок (черные или белые)
                val playerColor = if (findViewById<RadioButton>(R.id.radioBlack).isChecked) "BLACK" else "WHITE"
                intent.putExtra("player_color", playerColor)
            }
            startActivity(intent)
        }
    }
}