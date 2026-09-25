package com.wayfarer.rpg

import java.security.SecureRandom

enum class Degree { CRITICAL_SUCCESS, SUCCESS, FAILURE, CRITICAL_FAILURE }

data class CheckResult(
    val die: Int,
    val modifier: Int,
    val total: Int,
    val dc: Int,
    val degree: Degree
)

data class DiceRollResult(
    val sides: Int,
    val count: Int,
    val dice: List<Int>,
    val modifier: Int,
    val total: Int
) {
    val expression: String
        get() = count.toString() + "d" + sides +
            when {
                modifier > 0 -> "+" + modifier
                modifier < 0 -> modifier.toString()
                else -> ""
            }
}

object DiceEngine {
    private val rng = SecureRandom()

    fun roll(
        sides: Int,
        count: Int = 1,
        modifier: Int = 0
    ): DiceRollResult {
        require(sides >= 2)
        val safeCount = count.coerceIn(1, 20)
        val dice = List(safeCount) { rng.nextInt(sides) + 1 }
        return DiceRollResult(
            sides = sides,
            count = safeCount,
            dice = dice,
            modifier = modifier,
            total = dice.sum() + modifier
        )
    }

    fun d20(modifier: Int, dc: Int): CheckResult {
        val die = rng.nextInt(20) + 1
        val total = die + modifier
        var rank = when {
            total >= dc + 10 -> 3
            total >= dc -> 2
            total <= dc - 10 -> 0
            else -> 1
        }
        if (die == 20) rank = (rank + 1).coerceAtMost(3)
        if (die == 1) rank = (rank - 1).coerceAtLeast(0)
        val degree = listOf(
            Degree.CRITICAL_FAILURE,
            Degree.FAILURE,
            Degree.SUCCESS,
            Degree.CRITICAL_SUCCESS
        )[rank]
        return CheckResult(die, modifier, total, dc, degree)
    }
}
